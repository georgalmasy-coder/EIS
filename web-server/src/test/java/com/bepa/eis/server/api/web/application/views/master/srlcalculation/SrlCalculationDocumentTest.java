package com.bepa.eis.server.api.web.application.views.master.srlcalculation;

import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider.InterfaceRecord;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;
import java.util.ArrayList;

/** Standalone metadata contract regression test; no database or application server required. */
public final class SrlCalculationDocumentTest {
    public static void main(String[] args) throws Exception {
        LookupValue active = lookup(true);
        LookupValue inactive = lookup(false);
        var document = new SrlCalculationDocument(null, List.of(active, inactive), List.of(active),
                List.of(active), List.of(system(10, 4), system(20, null)),
                List.of(record(10, 20, 3), record(20, 10, null), record(10, 99, 3), record(99, 20, 3)));
        String xml = document.toXmlString();
        check(xml.contains("&amp;") && xml.contains("&lt;"), "XML must escape names and codes");
        var dom = document.getDoc();
        check(dom.getElementsByTagName("trl").getLength() == 1, "Inactive lookup excluded");
        check(dom.getElementsByTagName("irl").getLength() == 1, "IRL metadata");
        check(((Element) dom.getElementsByTagName("irl").item(0)).getAttribute("irlId").equals("5"),
                "IRL metadata ID is reduced by two");
        check(dom.getElementsByTagName("srl").getLength() == 1, "SRL metadata");
        Element maxima = (Element) dom.getElementsByTagName("lookupMaxima").item(0);
        check(maxima.getParentNode().getNodeName().equals("meta"), "Lookup maxima belong to metadata");
        check(maxima.getAttribute("maxTrlId").equals("7")
                && maxima.getAttribute("maxIrlId").equals("7")
                && maxima.getAttribute("maxSrlId").equals("7"), "Lookup maxima align with active SRL maximum");
        Element trl = (Element) dom.getElementsByTagName("trl").item(0);
        check(trl.getAttribute("code").equals("4 - combined label"), "Preserve cached lookup code");
        check(trl.getAttribute("name").equals("Description & <test>"), "Preserve cached lookup description");
        check(trl.getAttribute("trlId").equals("7"), "ID differs from code");
        Element secondSystem = (Element) dom.getElementsByTagName("system").item(1);
        check(secondSystem.getAttribute("trlId").isEmpty(), "Missing TRL stays empty");
        check(((Element) dom.getElementsByTagName("system").item(0)).getAttribute("normalizedTrl").equals("0.571"),
                "System TRL divided by maximum active TRL ID");
        check(secondSystem.getAttribute("normalizedTrl").equals("0"),
                "Missing TRL has zero normalizedTrl");
        check(dom.getElementsByTagName("interface").getLength() == 1, "Exclude inactive endpoints and missing IRL");
        Element firstInterface = (Element) dom.getElementsByTagName("interface").item(0);
        check(firstInterface.getAttribute("irlId").equals("1"), "Interface metadata ID is reduced by two");
        var empty = new SrlCalculationDocument(null, List.of(), List.of(), List.of(), List.of(), List.of());
        check(empty.getDoc().getElementsByTagName("meta").item(0).getChildNodes().getLength() == 6,
                "Keep lookup maxima and all five containers when empty");
        Element emptyMaxima = (Element) empty.getDoc().getElementsByTagName("lookupMaxima").item(0);
        check(emptyMaxima.getAttribute("maxTrlId").equals("0")
                && emptyMaxima.getAttribute("maxIrlId").equals("0")
                && emptyMaxima.getAttribute("maxSrlId").equals("0"), "Empty lookup maxima are zero");
        testSystemsInterfaces();
        testLookupMaximumCorrection();
        testExcludedInterfaceIrlValues();
        testLargeSparseMatrix();
        testComponentSrl();
        testSrlTransitionModel();
        testSystemSrl();
        System.out.println("SRL metadata and sparse matrix tests passed.");
    }

    private static void testExcludedInterfaceIrlValues() throws Exception {
        var systems = List.of(system(1, 4), system(2, 9));
        var records = new ArrayList<InterfaceRecord>();
        records.add(record(1, 2, 3));
        var baseline = new SrlCalculationDocument(null, List.of(irl(9, true)), List.of(irl(11, true)),
                List.of(irl(9, true)), systems, records);
        // Raw IDs 1 and 2 become -1 and 0 in interfacesMeta; also exclude missing/negative raw IDs.
        for (Integer value : new Integer[]{-1, 0, 1, 2, null}) records.add(record(2, 1, value));
        var filtered = new SrlCalculationDocument(null, List.of(irl(9, true)), List.of(irl(11, true)),
                List.of(irl(9, true)), systems, records);
        check(filtered.getDoc().getElementsByTagName("interface").getLength() == 1,
                "Only positive adjusted IRL belongs to interfacesMeta");
        check(filtered.getDoc().getElementsByTagName("systemInterface").getLength() == 3,
                "Keep positive interface and two calculated diagonals");
        check(filtered.toXmlString().equals(baseline.toXmlString()),
                "Excluded IRL values do not affect metadata, counts, component SRL or system SRL");
    }

    private static void testLookupMaximumCorrection() throws Exception {
        // Correct both lower and higher maxima, including empty lookups. Inactive SRL is ignored.
        for (int[] maxima : List.of(new int[]{3, 6}, new int[]{12, 16}, new int[]{9, 11}, new int[]{0, 0})) {
            var document = new SrlCalculationDocument(null,
                    maxima[0] == 0 ? List.of() : List.of(irl(maxima[0], true)),
                    maxima[1] == 0 ? List.of() : List.of(irl(maxima[1], true)),
                    List.of(irl(9, true), irl(99, false)), List.of(system(1, 9)), List.of());
            Element result = (Element) document.getDoc().getElementsByTagName("lookupMaxima").item(0);
            for (String name : List.of("maxTrlId", "maxIrlId", "maxSrlId")) {
                check(result.getAttribute(name).equals("9"), "Correct " + name + " to active SRL maximum");
            }
            check(((Element) document.getDoc().getElementsByTagName("system").item(0))
                    .getAttribute("normalizedTrl").equals("1.000"), "Corrected TRL factor");
            check(((Element) document.getDoc().getElementsByTagName("systemInterface").item(0))
                    .getAttribute("irlValue").equals("9"), "Corrected adjusted IRL diagonal");
            check(((Element) document.getDoc().getElementsByTagName("normalisedInterface").item(0))
                    .getAttribute("normalizedValue").equals("1.000"), "Corrected IRL factor");
        }
    }

    private static void testSystemsInterfaces() throws Exception {
        var document = new SrlCalculationDocument(null, List.of(lookup(true), irl(99, false)),
                List.of(irl(4, true), irl(99, false), irl(9, true), irl(2, true)), List.of(irl(7, true)),
                List.of(system(30, 4), system(10, null), system(20, 0)),
                List.of(record(30, 10, 3), record(10, 30, 5), record(30, 20, 0),
                        record(20, 30, null), record(10, 20, -1), record(30, 99, 7),
                        record(99, 10, 7), record(30, 30, 2), record(20, 10, 1), record(20, 30, 2)));
        Element matrix = (Element) document.getDoc().getElementsByTagName("SystemsInterfaces").item(0);
        var metaSystems = document.getDoc().getElementsByTagName("system");
        check(((Element) metaSystems.item(0)).getAttribute("normalizedTrl").equals("0.571"), "TRL rounding");
        check(((Element) metaSystems.item(2)).getAttribute("normalizedTrl").equals("0.000"), "Zero TRL");
        check(matrix.getParentNode().getNodeName().equals("calculation"), "Matrix under calculation");
        check(matrix.getParentNode().getParentNode().getChildNodes().item(0).getNodeName().equals("meta"),
                "Calculation is a sibling of meta");
        check(matrix.getElementsByTagName("columns").getLength() == 0
                && matrix.getElementsByTagName("rows").getLength() == 0, "No axis containers");
        var cells = matrix.getElementsByTagName("systemInterface");
        Element interfacesMeta = (Element) document.getDoc().getElementsByTagName("interfacesMeta").item(0);
        check(((Element) interfacesMeta.getFirstChild()).getAttribute("irlId").equals("1"),
                "Metadata and matrix use the same adjusted interface value");
        check(document.getDoc().getElementsByTagName("cell").getLength() == 0, "Descriptive interface names");
        check(cells.getLength() == 5, "Only diagonals and positive active interfaces");
        check(matrix.getChildNodes().getLength() == 5, "Cells are direct children of SystemsInterfaces");
        for (int index = 0; index < cells.getLength(); index++) {
            Element cell = (Element) cells.item(index);
            check(Integer.parseInt(cell.getAttribute("irlValue")) >= 1, "Only positive cell values");
            check(cell.getAttribute("fromCode").equals("SYS-" + cell.getAttribute("fromEntityId")),
                    "Source system code");
            check(cell.getAttribute("toCode").equals("SYS-" + cell.getAttribute("toEntityId")),
                    "Target system code");
            if (index < 3) {
                check(cell.getAttribute("fromEntityId").equals(cell.getAttribute("toEntityId")), "Diagonal");
                check(cell.getAttribute("irlValue").equals("7"), "Maximum active ID minus two overrides self interface");
            }
        }
        Element forward = (Element) cells.item(3);
        Element reverse = (Element) cells.item(4);
        check(forward.getAttribute("fromEntityId").equals("30")
                && forward.getAttribute("toEntityId").equals("10")
                && forward.getAttribute("irlValue").equals("1"), "Forward interface minus two");
        check(reverse.getAttribute("fromEntityId").equals("10")
                && reverse.getAttribute("toEntityId").equals("30")
                && reverse.getAttribute("irlValue").equals("3"), "Independent reverse interface minus two");
        Element normalised = (Element) document.getDoc().getElementsByTagName("NormalisedTrlAndIrl").item(0);
        check(normalised.getParentNode() == matrix.getParentNode(), "Normalised matrix is a sibling");
        check(normalised.getChildNodes().getLength() == 5, "Only existing sparse cells are normalised");
        for (int index = 0; index < 5; index++) {
            Element cell = (Element) normalised.getChildNodes().item(index);
            check(cell.getTagName().equals("normalisedInterface"), "Normalised interface element name");
            Element source = (Element) cells.item(index);
            for (String key : List.of("fromEntityId", "toEntityId", "fromCode", "toCode")) {
                check(cell.getAttribute(key).equals(source.getAttribute(key)), "Preserve " + key);
            }
            check(cell.getAttribute("normalizedValue").equals(
                    List.of("1.000", "1.000", "1.000", "0.143", "0.429").get(index)),
                    "Normalise using maximum active ID minus two, rounded to three decimals");
            check(!cell.hasAttribute("irlValue"), "Normalised cells carry normalizedValue");
            check(source.hasAttribute("irlValue"), "Source cell remains unchanged");
        }
        var rounding = new SrlCalculationDocument(null, List.of(), List.of(irl(8, true)), List.of(irl(6, true)),
                List.of(system(1, null), system(2, null)), List.of(record(1, 2, 3)));
        Element roundingMatrix = (Element) rounding.getDoc().getElementsByTagName("NormalisedTrlAndIrl").item(0);
        check(((Element) roundingMatrix.getLastChild()).getAttribute("normalizedValue").equals("0.167"),
                "Round repeating fraction upward");
    }

    private static void testLargeSparseMatrix() throws Exception {
        var systems = new ArrayList<SystemBreakdownEntity>();
        for (int id = 1; id <= 2000; id++) systems.add(system(id, null));
        var document = new SrlCalculationDocument(null, List.of(), List.of(irl(9, true)), List.of(irl(7, true)),
                systems, List.of(record(1, 2000, 3)));
        Element matrix = (Element) document.getDoc().getElementsByTagName("SystemsInterfaces").item(0);
        check(matrix.getChildNodes().getLength() == 2001,
                "Four million combinations require only 2001 cells");
        Element normalised = (Element) document.getDoc().getElementsByTagName("NormalisedTrlAndIrl").item(0);
        check(normalised.getChildNodes().getLength() == 2001, "Normalisation preserves sparse cell count");
        var noPositiveIrl = new SrlCalculationDocument(null, List.of(), List.of(irl(2, true)), List.of(),
                List.of(system(1, null)), List.of(record(1, 1, 3)));
        check(noPositiveIrl.getDoc().getElementsByTagName("systemInterface").getLength() == 0,
                "No positive diagonal when maximum IRL minus two is zero");
        var noIrl = new SrlCalculationDocument(null, List.of(), List.of(), List.of(),
                List.of(system(1, null)), List.of());
        check(noIrl.getDoc().getElementsByTagName("systemInterface").getLength() == 0, "No diagonal without IRL metadata");
    }

    private static LookupValue irl(int id, boolean active) {
        return new LookupValue(1, 1, id, "IRL", "Description", active);
    }

    private static void testComponentSrl() throws Exception {
        var document = new SrlCalculationDocument(null, List.of(irl(9, true)), List.of(irl(11, true)),
                List.of(irl(9, true)), List.of(system(1, 4), system(2, 9), system(3, null), system(4, 0)),
                List.of(record(1, 2, 5), record(1, 3, 3), record(2, 1, 8), record(1, 4, 2)));
        Element result = (Element) document.getDoc().getElementsByTagName("componentSrl").item(0);
        check(result.getParentNode().getNodeName().equals("calculation"), "Component SRL under calculation");
        check(result.getChildNodes().getLength() == 4, "One component per system");
        Element first = (Element) result.getChildNodes().item(0);
        check(first.getTagName().equals("component") && first.getAttribute("entityId").equals("1")
                && first.getAttribute("code").equals("SYS-1"), "Component identity and order");
        // Row 1: 4/9 + 3/9 * 1 + 1/9 * 0 = 7/9; count includes missing target TRL.
        check(first.getAttribute("weightedTrlSum").equals("0.778"), "AA uses unrounded target TRL and IRL");
        check(first.getAttribute("interfaceCount").equals("3"), "Positive interfaces including diagonal");
        check(first.getAttribute("srl").equals("0.259"), "AB divides AA by positive count");
        Element second = (Element) result.getChildNodes().item(1);
        // Row 2: 1 + 6/9 * 4/9; divide before rounding the displayed sum.
        check(second.getAttribute("weightedTrlSum").equals("1.296")
                && second.getAttribute("interfaceCount").equals("2")
                && second.getAttribute("srl").equals("0.648"), "Independent row aggregation");
        for (int index = 2; index < 4; index++) {
            Element component = (Element) result.getChildNodes().item(index);
            check(component.getAttribute("interfaceCount").equals("1")
                    && component.getAttribute("weightedTrlSum").equals("0.000")
                    && component.getAttribute("srl").equals("0.000"), "Missing or zero TRL yields zero");
        }
        var noInterfaces = new SrlCalculationDocument(null, List.of(irl(9, true)), List.of(), List.of(),
                List.of(system(1, 4)), List.of());
        Element zero = (Element) noInterfaces.getDoc().getElementsByTagName("component").item(0);
        check(zero.getAttribute("interfaceCount").equals("0")
                && zero.getAttribute("srl").equals("0.000"), "No division by zero for empty row");
        var excelExample = new SrlCalculationDocument(null, List.of(irl(9, true)), List.of(irl(11, true)),
                List.of(irl(9, true)), List.of(system(1, 4), system(7, 4)), List.of(record(1, 7, 3)));
        Element excelComponent = (Element) excelExample.getDoc().getElementsByTagName("component").item(0);
        // Excel AA4 = 4/9 + (1/9 * 4/9) = 40/81 = 0.493827..., displayed as 0.494.
        check(excelComponent.getAttribute("weightedTrlSum").equals("0.494"), "Match Excel AA4 rounding");
        check(excelComponent.getAttribute("srl").equals("0.247"), "Match Excel AB4 rounding");
        check(!excelExample.toXmlString().contains("preciseNormalizedValue"), "Internal precision stays out of XML");
    }

    private static void testSrlTransitionModel() throws Exception {
        var levels = new ArrayList<LookupValue>();
        for (int level = 9; level >= 1; level--) levels.add(irl(level, true));
        levels.add(irl(99, false));
        var document = new SrlCalculationDocument(null, List.of(irl(9, true)), List.of(irl(11, true)),
                levels, List.of(), List.of());
        Element model = (Element) document.getDoc().getElementsByTagName("srlTransitionModel").item(0);
        check(model.getParentNode().getNodeName().equals("calculation"), "Transition model under calculation");
        check(model.getChildNodes().getLength() == 9, "Only active reference levels");
        List<String> references = List.of("0.012", "0.049", "0.111", "0.198", "0.309", "0.444", "0.605", "0.790", "1.000");
        List<String> boundaries = List.of("0.000", "0.031", "0.080", "0.154", "0.253", "0.377", "0.525", "0.698", "0.895", "1.000");
        for (int index = 0; index < 9; index++) {
            Element level = (Element) model.getChildNodes().item(index);
            check(level.getAttribute("srlId").equals(Integer.toString(index + 1)), "Sorted levels");
            check(level.getAttribute("referenceValue").equals(references.get(index)), "Excel AB reference");
            check(level.getAttribute("lowerBound").equals(boundaries.get(index)), "Excel lower midpoint");
            check(level.getAttribute("upperBound").equals(boundaries.get(index + 1)), "Excel upper midpoint");
            check(level.getAttribute("upperBoundInclusive").equals(Boolean.toString(index == 8)), "No overlapping intervals");
        }
        var differentFactors = new SrlCalculationDocument(null, List.of(irl(10, true)), List.of(irl(10, true)),
                List.of(irl(2, true)), List.of(), List.of());
        Element level = (Element) differentFactors.getDoc().getElementsByTagName("level").item(0);
        check(level.getAttribute("normalizedTrl").equals("1.000")
                && level.getAttribute("normalizedIrl").equals("1.000")
                && level.getAttribute("referenceValue").equals("1.000"), "Factors use corrected SRL maximum");
        var missingFactor = new SrlCalculationDocument(null, List.of(), List.of(irl(11, true)),
                levels, List.of(), List.of());
        check(missingFactor.getDoc().getElementsByTagName("srlTransitionModel").item(0).getChildNodes().getLength() == 9,
                "Missing TRL maximum is corrected using SRL maximum");
    }

    private static void testSystemSrl() throws Exception {
        var levels = new ArrayList<LookupValue>();
        for (int level = 1; level <= 9; level++) levels.add(irl(level, true));
        int[] trls = {4, 4, 5, 7, 5, 5, 4, 3, 5, 4};
        int[][] targets = {{7}, {8}, {8}, {5, 6}, {4, 6}, {4, 5, 9, 10}, {1, 9}, {2, 3, 9}, {6, 7, 8}, {6}};
        var systems = new ArrayList<SystemBreakdownEntity>();
        var interfaces = new ArrayList<InterfaceRecord>();
        for (int index = 0; index < trls.length; index++) {
            systems.add(system(index + 1, trls[index]));
            for (int target : targets[index]) interfaces.add(record(index + 1, target, 3));
        }
        var document = new SrlCalculationDocument(null, levels, List.of(irl(11, true)), levels, systems, interfaces);
        Element componentResults = (Element) document.getDoc().getElementsByTagName("componentSrl").item(0);
        List<String> measuredLevels = List.of("4", "4", "5", "5", "4", "4", "4", "3", "4", "5");
        for (int index = 0; index < 10; index++) {
            Element component = (Element) componentResults.getChildNodes().item(index);
            check(component.getAttribute("measuredSrlId").equals(measuredLevels.get(index)),
                    "Excel O15:X15 component " + (index + 1));
            check(!component.getAttribute("measuredSrlRange").isEmpty(), "Component SRL range");
        }
        Element result = (Element) document.getDoc().getElementsByTagName("systemSrl").item(0);
        check(result.getAttribute("componentCount").equals("10")
                && result.getAttribute("calculatedSrl").equals("0.222")
                && result.getAttribute("measuredSrlId").equals("4"), "Excel I4 and J9 results");
        Element model = (Element) document.getDoc().getElementsByTagName("srlTransitionModel").item(0);
        int matched = 0;
        for (Node node = model.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element level = (Element) node;
            if (level.getAttribute("matchesCalculatedSrl").equals("true")) {
                matched++;
                check(level.getAttribute("measuredSrlId").equals("4")
                        && level.getAttribute("range").equals("0.154 - 0.252"), "Excel F9 range and J9 selection");
            } else check(level.getAttribute("measuredSrlId").isEmpty(), "Unselected Excel J cells stay empty");
        }
        check(matched == 1, "Exactly one selected SRL level");
        var maximum = new SrlCalculationDocument(null, levels, List.of(irl(11, true)), levels,
                List.of(system(1, 9)), List.of());
        Element maximumResult = (Element) maximum.getDoc().getElementsByTagName("systemSrl").item(0);
        check(maximumResult.getAttribute("calculatedSrl").equals("1.000")
                && maximumResult.getAttribute("measuredSrlId").equals("9"), "Maximum readiness");
        check(((Element) maximum.getDoc().getElementsByTagName("component").item(0)).getAttribute("measuredSrlId").equals("9"),
                "Maximum component readiness");
        var zero = new SrlCalculationDocument(null, levels, List.of(irl(11, true)), levels,
                List.of(system(1, null)), List.of());
        check(((Element) zero.getDoc().getElementsByTagName("systemSrl").item(0)).getAttribute("measuredSrlId").equals("1"),
                "Zero readiness selects lowest level");
        check(((Element) zero.getDoc().getElementsByTagName("component").item(0)).getAttribute("measuredSrlId").equals("1"),
                "Zero component readiness selects lowest level");
        var empty = new SrlCalculationDocument(null, levels, List.of(irl(11, true)), levels, List.of(), List.of());
        check(((Element) empty.getDoc().getElementsByTagName("systemSrl").item(0)).getAttribute("measuredSrlId").isEmpty(),
                "No measured level without components");
    }

    private static LookupValue lookup(boolean active) {
        return new LookupValue(1, 1, 7, "4 - combined label", "Description & <test>", active);
    }

    private static SystemBreakdownEntity system(int id, Integer trlId) {
        var system = new SystemBreakdownEntity(null) {
            @Override public String getCode() { return "SYS-" + id; }
            @Override public String getName() { return "System & " + id; }
            @Override public Integer getTrlId() { return trlId; }
        };
        system.setEntityId(id);
        return system;
    }

    private static InterfaceRecord record(int from, int to, Integer irl) {
        return new InterfaceRecord(1, 1, 1, 1, 1, true, 1, null, from, to, irl, null, null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
