package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.server.dataprovider.misc.MenuProvider;
import com.bepa.eis.server.dataprovider.misc.MenuProvider.MenuRow;
import com.bepa.eis.common.enums.menu.MenuItemType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MenuTransferTest {
    public static void main(String[] args) throws Exception {
        MenuServlet servlet = new MenuServlet();
        MenuProvider provider = new MenuProvider(null);
        MenuRow parent = row(10, null, "Administration ??? & <test>");
        MenuRow child = row(20, 10, "Child");
        var append = MenuServlet.class.getDeclaredMethod("appendMenuItems", StringBuilder.class, List.class);
        append.setAccessible(true);
        StringBuilder xml = new StringBuilder("<menuExport>");
        append.invoke(servlet, xml, List.of(child, parent));
        xml.append("</menuExport>");
        List<MenuRow> parsed = servlet.parseMenuImport(input(xml.toString()));
        check(parsed.equals(List.of(child, parent)), "All menu fields survive XML export/import");
        provider.validateMenuImport(parsed, List.of());
        provider.validateMenuImport(parsed, List.of(parent));
        invalid(() -> provider.validateMenuImport(List.of(parent, parent), List.of()));
        invalid(() -> provider.validateMenuImport(List.of(child), List.of()));
        invalid(() -> provider.validateMenuImport(List.of(row(10, 20, "Cycle"), child), List.of()));
        invalid(() -> provider.validateMenuImport(List.of(row(10, 30, "Moved"), row(30, null, "Root")), List.of(child)));
        invalid(() -> provider.validateMenuImport(List.of(), List.of()));
        invalid(() -> servlet.parseMenuImport(input("<lookupExport/>")));
        invalid(() -> servlet.parseMenuImport(input(xml.toString().replace("<ParentMenuId>10", "<ParentMenuId>oops"))));
        invalid(() -> servlet.parseMenuImport(input(xml.toString().replace("<Active>false", "<Active>oops"))));
        invalid(() -> servlet.parseMenuImport(input("<!DOCTYPE menuExport [<!ENTITY x SYSTEM 'file:///test'>]><menuExport/>")));
        System.out.println("Menu transfer tests passed: XML round trip, IDs, hierarchy, duplicates and invalid XML values.");
    }
    static MenuRow row(int id, Integer parent, String label) {
        return new MenuRow(id, label, "/web/view?page=test&a=b", parent, MenuItemType.MENU_ITEM.getId(), "", null, 7, true, false, "", false, "Description ? & < >");
    }
    static ByteArrayInputStream input(String xml) { return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)); }
    interface Action { void run() throws Exception; }
    static void invalid(Action action) throws Exception {
        try { action.run(); } catch (Exception expected) { return; }
        throw new AssertionError("Invalid import accepted");
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
