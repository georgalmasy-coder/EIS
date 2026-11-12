package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import java.time.LocalDate;
import java.util.*;

/** Frozen sparse cells indexed into 32 by 32 blocks; all axes use the original entity order. */
final class InterfaceMatrixSnapshotStore {
    static final int BLOCK_SIZE = 32;
    record Cell(String fromEntityId, String toEntityId, String irlId, String classificationIds,
                String nextIrlMeeting, String changedBy, String changed) {}
    record Block(String snapshotId, int row, int column, List<Cell> cells) {}
    record Patch(String fromEntityId, String toEntityId, Cell cell) {}
    record Update(String snapshotId, List<Patch> patches) {}
    record Owner(String session, Integer customer, Integer project, Integer user, EntityType type) {
        static Owner of(WebSession s, EntityType t) { return new Owner(s.getSessionId(),s.getCustomerId(),s.getProjectId(),s.getUserId(),t); }
    }
    static final class Source {
        final List<String> entities;
        final Map<String,Integer> index;
        final Map<Long,List<Cell>> blocks = new HashMap<>();
        Source(List<String> entities, List<Cell> cells) {
            this.entities=List.copyOf(entities);
            index=new HashMap<>();
            for(int i=0;i<entities.size();i++) index.put(entities.get(i),i);
            // Same precedence as the former XML parser: last version wins for an ordered pair.
            Map<Long,Cell> pairs=new HashMap<>();
            for(var cell:cells) {
                Integer r=index.get(cell.fromEntityId()), c=index.get(cell.toEntityId());
                if(r!=null && c!=null) pairs.put(key(r,c),cell);
            }
            pairs.forEach((pair,cell)-> {
                int r=(int)(pair>>32),c=(int)(long)pair;
                blocks.computeIfAbsent(key(r/BLOCK_SIZE,c/BLOCK_SIZE),ignored->new ArrayList<>()).add(cell);
            });
            blocks.replaceAll((k,v)->List.copyOf(v));
        }
        private Source(Source previous,List<Patch> patches) {
            entities=previous.entities;index=previous.index;blocks.putAll(previous.blocks);
            for(var patch:patches) {
                validatePair(patch.fromEntityId(),patch.toEntityId());
                long block=key(index.get(patch.fromEntityId())/BLOCK_SIZE,index.get(patch.toEntityId())/BLOCK_SIZE);
                var cells=new ArrayList<>(blocks.getOrDefault(block,List.of()));
                cells.removeIf(c->c.fromEntityId().equals(patch.fromEntityId()) && c.toEntityId().equals(patch.toEntityId()));
                if(patch.cell()!=null)cells.add(patch.cell());
                if(cells.isEmpty())blocks.remove(block);else blocks.put(block,List.copyOf(cells));
            }
        }
        void validatePair(String from,String to) {
            if(!index.containsKey(from)||!index.containsKey(to)||from.equals(to))
                throw new IllegalArgumentException("Interface endpoints are not in this snapshot");
        }
        List<Cell> block(int row,int column) {
            if(row<0 || column<0 || row%BLOCK_SIZE!=0 || column%BLOCK_SIZE!=0 || row>=entities.size() || column>=entities.size())
                throw new IllegalArgumentException("Invalid interface block bounds");
            return blocks.getOrDefault(key(row/BLOCK_SIZE,column/BLOCK_SIZE),List.of());
        }
        List<String> filter(boolean overdue, Set<String> irls, LocalDate today) {
            return filter(overdue, irls, today, false);
        }
        List<String> filter(boolean overdue, Set<String> irls, LocalDate today, boolean connectedOnly) {
            if(!overdue && irls.isEmpty() && !connectedOnly) return entities;
            Set<String> matched=new HashSet<>();
            for(var block:blocks.values()) for(var cell:block) {
                if(connectedOnly && cell.fromEntityId().equals(cell.toEntityId())) continue;
                if(!irls.isEmpty() && !irls.contains(cell.irlId())) continue;
                if(overdue) {
                    try { if(!LocalDate.parse(cell.nextIrlMeeting()).isBefore(today)) continue; }
                    catch(RuntimeException ignored) { continue; }
                }
                matched.add(cell.fromEntityId());matched.add(cell.toEntityId());
            }
            return entities.stream().filter(matched::contains).toList();
        }
    }
    private record Entry(Owner owner, Source source, long created) {}
    private final Map<String,Entry> entries=new LinkedHashMap<>(16,.75f,true);
    private final java.util.function.LongSupplier clock;
    InterfaceMatrixSnapshotStore() { this(System::currentTimeMillis); }
    InterfaceMatrixSnapshotStore(java.util.function.LongSupplier clock) { this.clock=clock; }
    synchronized String put(WebSession s, EntityType t, Source source) {
        entries.values().removeIf(e->clock.getAsLong()-e.created()>900_000);
        while(entries.size()>=4) entries.remove(entries.keySet().iterator().next());
        String id=UUID.randomUUID().toString();entries.put(id,new Entry(Owner.of(s,t),source,clock.getAsLong()));return id;
    }
    synchronized Source get(WebSession s, EntityType t, String id) {
        var e=entries.get(id);
        if(e==null || !e.owner().equals(Owner.of(s,t)) || clock.getAsLong()-e.created()>900_000) throw new Expired();
        return e.source();
    }
    synchronized void invalidate(WebSession s,EntityType t) { entries.values().removeIf(e->e.owner().equals(Owner.of(s,t))); }
    synchronized Update update(WebSession s,EntityType t,String id,List<Patch> patches) {
        Source previous=get(s,t,id);
        Source updated=new Source(previous,patches);
        Entry entry=entries.remove(id);
        String next=UUID.randomUUID().toString();
        entries.put(next,new Entry(entry.owner(),updated,entry.created()));
        return new Update(next,List.copyOf(patches));
    }
    private static long key(int r,int c) { return ((long)r<<32)|(c&0xffffffffL); }
    static final class Expired extends RuntimeException { Expired(){super("Interface snapshot expired. Reload the matrix.");} }
}
