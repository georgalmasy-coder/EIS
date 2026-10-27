package com.bepa.eis.server.api.web.application.views.basis.systemsbreakdown;

import com.bepa.eis.common.enums.entity.SBSCodeTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Standalone regression tests exercising the upload and preview validation without a database. */
public final class SystemBreakdownImportersTest {
    public static void main(String[] args) throws Exception {
        for (SBSCodeTypes type : SBSCodeTypes.values()) {
            if (!type.isActive()) continue;
            String prefix = type.getPrefix() + "P-ARC-";
            for (String format : List.of("xml", "csv", "xlsx")) {
                List<String> ids = List.of(prefix + "1.1.1.1", prefix + "1.1", prefix + "1", prefix + "1.1.1");
                JsonNode result = preview(format, file(format, ids));
                check(result.get("allValid").asBoolean(), prefix + " rejected in " + format + ": " + result);
                check(result.get("rows").get(0).get("values").get(0).asText().equals(prefix + "1"),
                        "Preview must sort parents before children and preserve the full ID");
            }
        }

        for (String prefix : List.of("", "=", "+", "-", "%", "#")) {
            check(preview("xml", file("xml", List.of(prefix + "1", prefix + "1.1")))
                    .get("allValid").asBoolean(), "Existing numeric format rejected: " + prefix);
        }
        List<String> roots = new ArrayList<>();
        for (int i = 12; i >= 1; i--) roots.add("=P-ARC-" + i);
        JsonNode sorted = preview("xml", file("xml", roots));
        check(sorted.get("allValid").asBoolean(), "Root IDs must sort numerically");
        check(sorted.get("rows").get(1).get("values").get(0).asText().equals("=P-ARC-2"), "Lexical ID sorting");

        for (String invalid : List.of("!P-ARC-1", "=OTHER-1", "==P-ARC-1", "=P-ARC-1..1",
                "=P-ARC-1.a", "=P-ARC-1.1.1.1.1.1")) {
            expectError(List.of(invalid), "ID is invalid");
        }
        check(preview("xml", file("xml", List.of("=P-ARC-1", "=P-ARC-1.1", "=P-ARC-1.1.1",
                "=P-ARC-1.1.1.1", "=P-ARC-1.1.1.1.1"))).get("allValid").asBoolean(), "Five levels rejected");
        expectError(List.of("=P-ARC-1", "=P-ARC-1"), "Duplicate ID");
        expectError(List.of("=P-ARC-1", "+P-ARC-1"), "Duplicate ID");
        expectError(List.of("=P-ARC-1.1"), "Missing parent ID");
        expectError(List.of("=P-ARC-1", "=P-ARC-1.2"), "Missing ID");

        if (args.length > 0) {
            JsonNode result = preview("xml", Files.readAllBytes(Path.of(args[0])));
            check(result.get("allValid").asBoolean(), "Test data rejected: " + result);
            check(result.get("rowCount").asInt() == 1900, "Expected 1,900 physical test entities");
            System.out.println("All 1,900 physical test entities passed import preview validation.");
        }
        System.out.println("Physical import tests passed: all SBS prefixes, XML/CSV/XLSX, sorting, hierarchy, duplicates and invalid IDs.");
    }

    private static void expectError(List<String> ids, String message) throws Exception {
        JsonNode result = preview("xml", file("xml", ids));
        check(!result.get("allValid").asBoolean(), "Invalid IDs accepted: " + ids);
        check(result.get("rows").toString().contains(message), "Missing error '" + message + "': " + result);
    }

    private static byte[] file(String format, List<String> ids) throws Exception {
        if (format.equals("xlsx")) {
            try (var workbook = new XSSFWorkbook(); var bytes = new ByteArrayOutputStream()) {
                var sheet = workbook.createSheet("Physical Architecture");
                int index = 0;
                for (String id : ids) {
                    var row = sheet.createRow(index++);
                    row.createCell(0).setCellValue(id);
                    row.createCell(1).setCellValue(id.split("\\.").length);
                    row.createCell(2).setCellValue("Test boat");
                }
                workbook.write(bytes);
                return bytes.toByteArray();
            }
        }
        StringBuilder content = new StringBuilder();
        if (format.equals("xml")) content.append("<PhysicalStructureExport>");
        for (String id : ids) {
            int level = id.split("\\.").length;
            if (format.equals("xml")) {
                content.append("<physicalStructures><SBS>").append(id).append("</SBS><Level>")
                        .append(level).append("</Level><Name>Test boat</Name></physicalStructures>");
            } else {
                content.append(id).append(',').append(level).append(",Test boat\n");
            }
        }
        if (format.equals("xml")) content.append("</PhysicalStructureExport>");
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static JsonNode preview(String format, byte[] bytes) throws Exception {
        Part part = (Part) Proxy.newProxyInstance(Part.class.getClassLoader(), new Class<?>[]{Part.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSize" -> (long) bytes.length;
                    case "getHeader" -> "form-data; name=\"file\"; filename=\"physical." + format + "\"";
                    case "getInputStream" -> new ByteArrayInputStream(bytes);
                    case "toString" -> "physical." + format;
                    default -> throw new AssertionError("Unexpected Part method: " + method.getName());
                });
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getPart")) return part;
                    throw new AssertionError("Unexpected request method: " + method.getName());
                });
        StringWriter output = new StringWriter();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWriter" -> new PrintWriter(output);
                    case "setStatus", "setCharacterEncoding", "setContentType" -> null;
                    default -> throw new AssertionError("Unexpected response method: " + method.getName());
                });
        new SystemBreakdownImporters(null, request).previewImport(response);
        return new ObjectMapper().readTree(output.toString());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
