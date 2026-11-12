package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InterfaceMatrixSnapshotStoreTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void rejected(Runnable action,Class<? extends Throwable> expected){
        try{action.run();}catch(Throwable e){if(expected.isInstance(e))return;throw new AssertionError(e);}
        throw new AssertionError("Expected "+expected.getSimpleName());
    }
    private static WebSession session(String id,int project,int user){
        var s=new WebSession();s.setSessionId(id);s.setCustomerId(1);s.setProjectId(project);s.setUserId(user);return s;
    }
    private static InterfaceMatrixSnapshotStore.Cell cell(String from,String to,String irl,String date){
        return new InterfaceMatrixSnapshotStore.Cell(from,to,irl,"10,20",date,"1","");
    }
    public static void main(String[] args) throws Exception {
        var ids=java.util.stream.IntStream.rangeClosed(1,65).mapToObj(String::valueOf).toList();
        var source=new InterfaceMatrixSnapshotStore.Source(ids,List.of(
                cell("1","2","1","2020-01-02"),cell("1","2","4","2026-01-01"),
                cell("2","1","3","2099-01-01"),cell("65","64","4","2026-01-01"),
                cell("33","34","7","invalid"),cell("missing","1","4","2026-01-01")));
        check(source.block(0,0).size()==2,"ordered pairs must be deduplicated and reverse retained");
        check(source.block(0,0).stream().anyMatch(c->c.fromEntityId().equals("1")&&c.irlId().equals("4")),"last version precedence");
        check(source.block(64,32).size()==1,"edge block at last row");
        check(source.block(0,64).isEmpty(),"known empty block");
        rejected(()->source.block(-32,0),IllegalArgumentException.class);
        rejected(()->source.block(1,0),IllegalArgumentException.class);
        rejected(()->source.block(96,0),IllegalArgumentException.class);
        check(source.filter(true,Set.of("4"),LocalDate.of(2026,11,5)).equals(List.of("1","2","64","65")),"global IRL/overdue filtering and stable axes");
        check(source.filter(false,Set.of(),LocalDate.now()).equals(ids),"unfiltered axes");
        check(source.filter(false,Set.of(),LocalDate.now(),true).equals(List.of("1","2","33","34","64","65")),
                "Connected-only removes isolated entities and retains incoming/outgoing endpoints");
        check(source.filter(true,Set.of("4"),LocalDate.of(2026,11,5),true).equals(List.of("1","2","64","65")),
                "Connected-only combines with overdue and IRL filters");
        var diagonalOnly = new InterfaceMatrixSnapshotStore.Source(List.of("1","2"),List.of(cell("1","1","4","2020-01-01")));
        check(diagonalOnly.filter(false,Set.of(),LocalDate.now(),true).isEmpty(),
                "Self references do not make an item connected to another item");
        AtomicLong clock=new AtomicLong();var store=new InterfaceMatrixSnapshotStore(clock::get);
        var owner=session("a",6,1);var type=EntityType.SYSTEMS_BREAKDOWN;
        String id=store.put(owner,type,source);
        check(store.get(owner,type,id)==source,"snapshot lookup");
        rejected(()->store.get(session("b",6,1),type,id),InterfaceMatrixSnapshotStore.Expired.class);
        rejected(()->store.get(session("a",7,1),type,id),InterfaceMatrixSnapshotStore.Expired.class);
        rejected(()->store.get(session("a",6,2),type,id),InterfaceMatrixSnapshotStore.Expired.class);
        rejected(()->store.get(owner,EntityType.LOGICAL_STRUCTURE,id),InterfaceMatrixSnapshotStore.Expired.class);
        var other=session("a",6,1);other.setCustomerId(2);
        rejected(()->store.get(other,type,id),InterfaceMatrixSnapshotStore.Expired.class);
        clock.set(900001);rejected(()->store.get(owner,type,id),InterfaceMatrixSnapshotStore.Expired.class);
        String evicted=store.put(owner,type,source);for(int i=0;i<4;i++)store.put(owner,type,source);
        rejected(()->store.get(owner,type,evicted),InterfaceMatrixSnapshotStore.Expired.class);
        String current=store.put(owner,type,source);store.invalidate(owner,type);
        rejected(()->store.get(owner,type,current),InterfaceMatrixSnapshotStore.Expired.class);
        String old=store.put(owner,type,source);
        var update=store.update(owner,type,old,List.of(
                new InterfaceMatrixSnapshotStore.Patch("1","2",cell("1","2","9","2099-01-01")),
                new InterfaceMatrixSnapshotStore.Patch("2","1",null)));
        rejected(()->store.get(owner,type,old),InterfaceMatrixSnapshotStore.Expired.class);
        var changed=store.get(owner,type,update.snapshotId());
        check(changed.block(0,0).size()==1 && changed.block(0,0).getFirst().irlId().equals("9"),"targeted pair update/delete");
        check(changed.block(64,32)==source.block(64,32),"unaffected block should be retained");
        check(source.block(0,0).size()==2,"in-flight old snapshot mutated");
        check(changed.filter(true,Set.of("4"),LocalDate.of(2026,11,5)).equals(List.of("64","65")),"filter not updated");
        var created=store.update(owner,type,update.snapshotId(),List.of(
                new InterfaceMatrixSnapshotStore.Patch("3","4",cell("3","4","4","2020-01-01"))));
        check(store.get(owner,type,created.snapshotId()).block(0,0).size()==2,"new interface missing");
        rejected(()->store.update(owner,type,created.snapshotId(),List.of(
                new InterfaceMatrixSnapshotStore.Patch("missing","1",null))),IllegalArgumentException.class);
        var json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                new InterfaceMatrixSnapshotStore.Block("test",64,32,source.block(64,32)));
        check(json.contains("\"fromEntityId\":\"65\"")&&json.contains("\"row\":64"),"block serialization");
        System.out.println("Interface snapshot: sparse blocks, filters, version precedence, ownership, expiry, bounds, invalidation and JSON passed");
    }
}
