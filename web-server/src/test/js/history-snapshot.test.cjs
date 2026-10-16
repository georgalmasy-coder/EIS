const assert = require('node:assert/strict');
const {DatabaseSync} = require('node:sqlite');
const {execFileSync} = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');

// Run the actual Java-generated predicates against fixtures, independently of the production DB.
// Prerequisite: mvn -o -pl web-server -am test-compile -DskipTests
const root = path.resolve(__dirname, '../../../..');
const target = path.join(root, 'web-server/target');
const probe = path.join(target, 'HistorySnapshotProbe.java');
fs.writeFileSync(probe, `
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.views.common.EntityHistorySnapshot;
class HistorySnapshotProbe {
    public static void main(String[] args) {
        for (int version : new int[]{1, 2, 3}) {
            var snapshot = new EntityHistorySnapshot(EntityType.SYSTEM_REQUIREMENT, 42, version);
            System.out.println(snapshot.predicate("ENTITY_RELATIONS", "ER", "CreatedTime", "EntityType", "EntityId", "RelatedEntityType", "RelatedEntityId"));
            System.out.println(snapshot.predicate("INTERFACES", "I", "ChangedDateTime", "EntityType", "FromEntityId", "ToEntityId"));
            System.out.println(snapshot.entityPredicate("E"));
        }
    }
}`);
let predicates;
try {
    const classpath = [path.join(target, 'classes'), path.join(root, 'common/target/classes')].join(path.delimiter);
    predicates = execFileSync('java', ['--class-path', classpath, probe], {encoding: 'utf8'}).trim().split(/\r?\n/);
} finally { fs.unlinkSync(probe); }
const db = new DatabaseSync(':memory:');
db.exec(`
CREATE TABLE ENTITY (CustomerId INT, ProjectId INT, EntityType INT, EntityId INT, Version INT, Latest INT, ChangedDateTime TEXT);
INSERT INTO ENTITY VALUES (1,1,6,42,1,0,'2026-01-10'), (1,1,6,42,2,0,'2026-02-10'), (1,1,6,42,3,1,'2026-03-10');
INSERT INTO ENTITY VALUES (1,1,6,99,1,0,'2026-01-01'), (1,1,6,99,2,1,'2026-02-15');
CREATE TABLE ENTITY_RELATIONS (CustomerId INT, ProjectId INT, EntityType INT, EntityId INT, RelatedEntityType INT, RelatedEntityId INT, Version INT, RelationType INT, CreatedTime TEXT, Latest INT);
INSERT INTO ENTITY_RELATIONS VALUES (1,1,6,42,6,99,1,1,'2026-01-15',0), (1,1,6,42,6,99,2,2,'2026-02-15',0), (1,1,6,42,6,99,3,3,'2026-04-01',1);
-- Another tenant's newer rows must not supersede this tenant's history.
INSERT INTO ENTITY_RELATIONS VALUES (2,1,6,42,6,99,9,1,'2026-01-05',1);
CREATE TABLE INTERFACES (CustomerId INT, ProjectId INT, EntityType INT, FromEntityId INT, ToEntityId INT, Version INT, ChangedDateTime TEXT, Latest INT);
INSERT INTO INTERFACES VALUES (1,1,6,42,99,1,'2026-01-15',0), (1,1,6,42,99,2,'2026-02-15',0), (1,1,6,42,98,1,'2026-02-10',1);
INSERT INTO INTERFACES VALUES (2,1,6,42,99,10,'2026-01-05',1);
`);
function values(sql, column) { return db.prepare(sql).all().map(row => row[column]); }
assert.deepEqual(values(`SELECT ER.Version FROM ENTITY_RELATIONS ER WHERE ER.CustomerId=1 AND ${predicates[0]}`, 'Version'), [1]);
assert.deepEqual(values(`SELECT ER.Version FROM ENTITY_RELATIONS ER WHERE ER.CustomerId=1 AND ${predicates[3]}`, 'Version'), [2]);
assert.deepEqual(values(`SELECT ER.Version FROM ENTITY_RELATIONS ER WHERE ER.CustomerId=1 AND ER.RelationType IN (1,2) AND ${predicates[6]}`, 'Version'), [], 'latest removal must not revive a confirmed historical relation');
assert.deepEqual(values(`SELECT I.Version FROM INTERFACES I WHERE I.CustomerId=1 AND ${predicates[1]}`, 'Version'), [1]);
assert.deepEqual(values(`SELECT I.Version FROM INTERFACES I WHERE I.CustomerId=1 AND ${predicates[4]} ORDER BY ToEntityId`, 'Version'), [1, 2]);
assert.deepEqual(values(`SELECT I.ToEntityId FROM INTERFACES I WHERE I.CustomerId=1 AND ${predicates[7]}`, 'ToEntityId'), [98], 'removed interfaces must not reappear in the current version');
assert.deepEqual(values(`SELECT E.Version FROM ENTITY E WHERE E.CustomerId=1 AND E.EntityId=99 AND ${predicates[2]}`, 'Version'), [1]);
assert.deepEqual(values(`SELECT E.Version FROM ENTITY E WHERE E.CustomerId=1 AND E.EntityId=99 AND ${predicates[5]}`, 'Version'), [2]);
db.close();
console.log('Historical snapshot SQL tests passed.');
