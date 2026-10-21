package com.bepa.eis.server.api.web.application.views.master.srlcalculation;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Objects;
import java.util.Set;

import static com.bepa.eis.common.enums.entity.EntityType.SYSTEMS_BREAKDOWN;

public class SrlCalculationDocument extends GenericXmlDocument {
    private static final Logger log = LoggerFactory.getLogger(SrlCalculationDocument.class);

    // DOM user data is retained for calculations and is never serialized into the XML.
    private static final String PRECISE_NORMALIZED_VALUE = "preciseNormalizedValue";
    private static final String PRECISE_LOWER_BOUND = "preciseLowerBound";
    private static final String PRECISE_UPPER_BOUND = "preciseUpperBound";
    private final BigDecimal irlFactor;
    private final BigDecimal trlFactor;
    private final ListOfElements rootElements;

    protected SrlCalculationDocument(WebSession webSession) throws Exception {
        this(webSession,
                CustomerLookupCache.getTrlLookupValues(webSession),
                CustomerLookupCache.getIrlLookupValues(webSession),
                CustomerLookupCache.getSrlLookupValues(webSession),
                new SystemBreakdownProvider(webSession).getAllSystemBreakdown(false),
                new InterfaceMatrixProvider(webSession, SYSTEMS_BREAKDOWN).getLatestInterfaceRecords(
                        webSession.getCustomerId(), webSession.getProjectId(), SYSTEMS_BREAKDOWN));
        rootElements.addElement(new TopPanelProvider(webSession)
                .getTopPanelBySession(PageType.DASHBOARD_SRLS_PAGE).getTopPanelElements());
    }

    SrlCalculationDocument(WebSession webSession, List<LookupValue> trls, List<LookupValue> irls,
                           List<LookupValue> srls, List<SystemBreakdownEntity> systems,
                           List<InterfaceMatrixProvider.InterfaceRecord> interfaces) throws Exception {
        super(webSession);
        rootElements = initXmlDocument(getClass().getSimpleName());
        Element calculation = appendElement(getRoot(), "srlCalculation");
        Element meta = appendElement(calculation, "meta");
        appendLookups(meta, "trl", trls);
        appendLookups(meta, "irl", irls);
        appendLookups(meta, "srl", srls);

        int highestSrlId = highestActiveLookupId(srls);
        int lookupIrlId = highestActiveLookupId(irls);
        int highestAdjustedIrlId = alignLookupMaximum("maxIrlId", lookupIrlId == 0 ? 0 : lookupIrlId - 2, highestSrlId);
        int highestTrlId = alignLookupMaximum("maxTrlId", highestActiveLookupId(trls), highestSrlId);
        // Interface cells accept database IDs and subtract two when they are serialized.
        int highestIrlId = highestAdjustedIrlId + 2;
        // Keep guard digits and round the reciprocal upward so exact HALF_UP ties stay on the correct side.
        irlFactor = highestIrlId > 2
                ? BigDecimal.ONE.divide(BigDecimal.valueOf(highestIrlId - 2), 32, RoundingMode.UP)
                : BigDecimal.ZERO;
        Element lookupMaxima = appendElement(meta, "lookupMaxima");
        attribute(lookupMaxima, "maxTrlId", highestTrlId);
        // Match the adjusted IDs exposed in irlMeta rather than the database IDs.
        attribute(lookupMaxima, "maxIrlId", highestAdjustedIrlId);
        attribute(lookupMaxima, "maxSrlId", highestSrlId);
        trlFactor = highestTrlId > 0
                ? BigDecimal.ONE.divide(BigDecimal.valueOf(highestTrlId), 32, RoundingMode.UP)
                : BigDecimal.ZERO;
        Element systemsMeta = appendElement(meta, "systemsMeta");
        Set<Integer> activeEntityIds = new HashSet<>();
        for (SystemBreakdownEntity system : systems) {
            Integer entityId = system.getEntityId().getValue();
            activeEntityIds.add(entityId);
            Element element = appendElement(systemsMeta, "system");
            attribute(element, "entityId", entityId);
            attribute(element, "code", system.getCode());
            attribute(element, "codeLevel", system.getSbsCodeLevel().getValue());
            attribute(element, "name", system.getName());
            attribute(element, "trlId", system.getTrlId());
            BigDecimal normalizedTrl = system.getTrlId() == null ? BigDecimal.ZERO
                    : normalize(BigDecimal.valueOf(system.getTrlId()), trlFactor);
            element.setUserData(PRECISE_NORMALIZED_VALUE, normalizedTrl, null);
            attribute(element, "normalizedTrl", system.getTrlId() != null && trlFactor.signum() > 0
                    ? normalizedTrl.setScale(3, RoundingMode.HALF_UP).toPlainString()
                    : "0");
        }

        Element interfacesMeta = appendElement(meta, "interfacesMeta");
        for (InterfaceMatrixProvider.InterfaceRecord record : interfaces) {
            // INTERFACES has no Active flag. Only latest records between active systems belong here.
            if (isIncludedInterface(record, activeEntityIds)) {
                Element element = appendElement(interfacesMeta, "interface");
                attribute(element, "fromEntityId", record.fromEntityId());
                attribute(element, "toEntityId", record.toEntityId());
                attribute(element, "irlId", record.irlId() == null ? null : record.irlId() - 2);
            }
        }

        Element calculationElement = appendElement(calculation, "calculation");
        Element systemsInterfaces = appendSystemsInterfaces(
                calculationElement, systems, interfaces, activeEntityIds, highestIrlId);
        Element normalisedInterfaces = appendNormalisedTrlAndIrl(calculationElement, systemsInterfaces);
        Element components = appendComponentSrl(calculationElement, systemsMeta, normalisedInterfaces);
        Element transitionModel = appendSrlTransitionModel(calculationElement, srls);
        NavigableMap<BigDecimal, Element> levelIndex = indexSrlLevels(transitionModel);
        appendComponentSrlLevels(components, levelIndex);
        appendSystemSrl(calculationElement, components, transitionModel, levelIndex);
    }

    private Element appendSrlTransitionModel(Element calculation, List<LookupValue> srls) {
        Element model = appendElement(calculation, "srlTransitionModel");
        if (trlFactor.signum() <= 0 || irlFactor.signum() <= 0) return model;

        List<Integer> levels = srls.stream()
                .filter(LookupValue::isActive)
                .map(LookupValue::getLookupId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .sorted()
                .toList();
        BigDecimal previousReference = null;
        BigDecimal lowerBound = BigDecimal.ZERO;
        for (int index = 0; index < levels.size(); index++) {
            int level = levels.get(index);
            BigDecimal normalizedTrl = normalize(BigDecimal.valueOf(level), trlFactor);
            BigDecimal normalizedIrl = normalize(BigDecimal.valueOf(level), irlFactor);
            // Excel AB19, AB25, ... AB67: identical products averaged over the positive interfaces.
            // The component count cancels out, so no example matrix needs to be constructed.
            BigDecimal reference = normalizedTrl.multiply(normalizedIrl);
            if (previousReference != null) {
                lowerBound = previousReference.add(reference).divide(BigDecimal.valueOf(2));
            }
            boolean lastLevel = index == levels.size() - 1;
            BigDecimal upperBound = lastLevel ? BigDecimal.ONE
                    : reference.add(referenceSrl(levels.get(index + 1))).divide(BigDecimal.valueOf(2));

            Element referenceLevel = appendElement(model, "level");
            attribute(referenceLevel, "srlId", level);
            attribute(referenceLevel, "trlId", level);
            attribute(referenceLevel, "irlId", level);
            attribute(referenceLevel, "normalizedTrl", normalizedTrl.setScale(3, RoundingMode.HALF_UP));
            attribute(referenceLevel, "normalizedIrl", normalizedIrl.setScale(3, RoundingMode.HALF_UP));
            attribute(referenceLevel, "referenceValue", reference.setScale(3, RoundingMode.HALF_UP));
            attribute(referenceLevel, "lowerBound", lowerBound.setScale(3, RoundingMode.HALF_UP));
            attribute(referenceLevel, "upperBound", upperBound.setScale(3, RoundingMode.HALF_UP));
            // Ranges are [lowerBound, upperBound), with 1 included in the last range.
            attribute(referenceLevel, "upperBoundInclusive", lastLevel);
            referenceLevel.setUserData(PRECISE_LOWER_BOUND, lowerBound, null);
            referenceLevel.setUserData(PRECISE_UPPER_BOUND, upperBound, null);
            // Excel column F displays inclusive ranges at three-decimal resolution.
            BigDecimal displayUpper = upperBound.setScale(3, RoundingMode.HALF_UP);
            if (!lastLevel) displayUpper = displayUpper.subtract(new BigDecimal("0.001"));
            attribute(referenceLevel, "range", lowerBound.setScale(3, RoundingMode.HALF_UP)
                    + " - " + displayUpper.toPlainString());
            previousReference = reference;
        }
        return model;
    }

    private NavigableMap<BigDecimal, Element> indexSrlLevels(Element transitionModel) {
        NavigableMap<BigDecimal, Element> index = new TreeMap<>();
        for (Node node = transitionModel.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element level = (Element) node;
            index.put((BigDecimal) level.getUserData(PRECISE_LOWER_BOUND), level);
        }
        return index;
    }

    private void appendComponentSrlLevels(Element components, NavigableMap<BigDecimal, Element> levelIndex) {
        for (Node node = components.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element component = (Element) node;
            BigDecimal srl = (BigDecimal) component.getUserData(PRECISE_NORMALIZED_VALUE);
            // Excel O15:X15: XLOOKUP(component SRL, lower bounds, SRL levels, , -1).
            Map.Entry<BigDecimal, Element> match = levelIndex.floorEntry(srl);
            Element level = match == null ? null : match.getValue();
            attribute(component, "measuredSrlId", level == null ? "" : level.getAttribute("srlId"));
            attribute(component, "measuredSrlRange", level == null ? "" : level.getAttribute("range"));
        }
    }

    private void appendSystemSrl(Element calculation, Element components, Element transitionModel,
                                 NavigableMap<BigDecimal, Element> levelIndex) {
        Element result = appendElement(calculation, "systemSrl");
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Node node = components.getFirstChild(); node != null; node = node.getNextSibling()) {
            sum = sum.add((BigDecimal) node.getUserData(PRECISE_NORMALIZED_VALUE));
            count++;
        }
        // Excel I4 -> 'SRL beregninger'!AC15: mean of component SRLs, including zero results.
        BigDecimal calculatedSrl = count == 0 ? BigDecimal.ZERO
                : sum.divide(BigDecimal.valueOf(count), 32, RoundingMode.HALF_UP);
        attribute(result, "componentCount", count);
        attribute(result, "calculatedSrl", calculatedSrl.setScale(3, RoundingMode.HALF_UP));
        String measuredSrlId = "";
        Map.Entry<BigDecimal, Element> match = count == 0 ? null : levelIndex.floorEntry(calculatedSrl);
        for (Node node = transitionModel.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element level = (Element) node;
            boolean matches = match != null && match.getValue() == level;
            attribute(level, "matchesCalculatedSrl", matches);
            attribute(level, "measuredSrlId", matches ? level.getAttribute("srlId") : "");
            if (matches) measuredSrlId = level.getAttribute("srlId");
        }
        attribute(result, "measuredSrlId", measuredSrlId);
    }

    private BigDecimal referenceSrl(int level) {
        BigDecimal value = BigDecimal.valueOf(level);
        return normalize(value, trlFactor).multiply(normalize(value, irlFactor));
    }

    private Element appendSystemsInterfaces(Element calculation, List<SystemBreakdownEntity> systems,
                                         List<InterfaceMatrixProvider.InterfaceRecord> interfaces,
                                         Set<Integer> activeEntityIds, int highestIrlId) {
        Element matrix = appendElement(calculation, "SystemsInterfaces");
        Map<Integer, SystemBreakdownEntity> systemsById = new HashMap<>();

        // Emit only positive cells, without allocating or iterating an N x N matrix.
        for (SystemBreakdownEntity system : systems) {
            Integer entityId = system.getEntityId().getValue();
            systemsById.put(entityId, system);
            appendInterfaceCell(matrix, system, system, highestIrlId);
        }

        for (InterfaceMatrixProvider.InterfaceRecord record : interfaces) {
            if (isIncludedInterface(record, activeEntityIds)
                    && !Objects.equals(record.fromEntityId(), record.toEntityId())) {
                appendInterfaceCell(matrix, systemsById.get(record.fromEntityId()),
                        systemsById.get(record.toEntityId()), record.irlId());
            }
        }
        return matrix;
    }

    private Element appendNormalisedTrlAndIrl(Element calculation, Element systemsInterfaces) {
        Element normalised = appendElement(calculation, "NormalisedTrlAndIrl");
        if (!systemsInterfaces.hasChildNodes()) return normalised;
        if (irlFactor.signum() <= 0) {
            throw new IllegalStateException("Cannot normalise interface cells: maximum active IRL ID must exceed 2");
        }
        // Traverse only the existing sparse cells; do not build another matrix or cell collection.
        for (Node node = systemsInterfaces.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element source = (Element) node;
            BigDecimal value = normalize(new BigDecimal(source.getAttribute("irlValue")), irlFactor);
            Element cell = appendElement(normalised, "normalisedInterface");
            for (String name : List.of("fromEntityId", "toEntityId", "fromCode", "toCode")) {
                attribute(cell, name, source.getAttribute(name));
            }
            cell.setUserData(PRECISE_NORMALIZED_VALUE, value, null);
            attribute(cell, "normalizedValue", value.setScale(3, RoundingMode.HALF_UP).toPlainString());
        }
        return normalised;
    }

    private Element appendComponentSrl(Element calculation, Element systemsMeta, Element normalisedInterfaces) {
        Element components = appendElement(calculation, "componentSrl");
        Map<String, BigDecimal> trlsById = new HashMap<>();
        Map<String, ComponentSrlTotals> totalsById = new HashMap<>();
        for (Node node = systemsMeta.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element system = (Element) node;
            String entityId = system.getAttribute("entityId");
            trlsById.put(entityId, (BigDecimal) system.getUserData(PRECISE_NORMALIZED_VALUE));
            totalsById.put(entityId, new ComponentSrlTotals());
        }
        for (Node node = normalisedInterfaces.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element relation = (Element) node;
            BigDecimal irl = (BigDecimal) relation.getUserData(PRECISE_NORMALIZED_VALUE);
            if (irl.signum() <= 0) continue;
            ComponentSrlTotals totals = totalsById.get(relation.getAttribute("fromEntityId"));
            // AA: SUMPRODUCT of the row's IRL values and the target systems' normalised TRLs.
            totals.weightedTrlSum = totals.weightedTrlSum.add(
                    irl.multiply(trlsById.get(relation.getAttribute("toEntityId"))));
            totals.interfaceCount++;
        }
        for (Node node = systemsMeta.getFirstChild(); node != null; node = node.getNextSibling()) {
            Element system = (Element) node;
            ComponentSrlTotals totals = totalsById.get(system.getAttribute("entityId"));
            Element component = appendElement(components, "component");
            attribute(component, "entityId", system.getAttribute("entityId"));
            attribute(component, "code", system.getAttribute("code"));
            attribute(component, "weightedTrlSum", totals.weightedTrlSum.setScale(3, RoundingMode.HALF_UP));
            attribute(component, "interfaceCount", totals.interfaceCount);
            // AB: divide the unrounded sum by COUNTIF(row, ">0"), including the diagonal.
            BigDecimal srl = totals.interfaceCount == 0 ? BigDecimal.ZERO
                    : totals.weightedTrlSum.divide(BigDecimal.valueOf(totals.interfaceCount), 32, RoundingMode.HALF_UP);
            component.setUserData(PRECISE_NORMALIZED_VALUE, srl, null);
            attribute(component, "srl", srl.setScale(3, RoundingMode.HALF_UP));
        }
        return components;
    }

    private static class ComponentSrlTotals {
        private BigDecimal weightedTrlSum = BigDecimal.ZERO;
        private int interfaceCount;
    }

    private BigDecimal normalize(BigDecimal value, BigDecimal factor) {
        return value.multiply(factor);
    }

    private void appendInterfaceCell(Element cells, SystemBreakdownEntity fromSystem,
                                     SystemBreakdownEntity toSystem, Integer irlId) {
        // Filter before creating a cell: only adjusted values >= 1 are retained.
        if (irlId == null || irlId <= 2) return;
        int irlValue = irlId - 2;
        Element cell = appendElement(cells, "systemInterface");
        attribute(cell, "fromEntityId", fromSystem.getEntityId().getValue());
        attribute(cell, "toEntityId", toSystem.getEntityId().getValue());
        attribute(cell, "fromCode", fromSystem.getCode());
        attribute(cell, "toCode", toSystem.getCode());
        attribute(cell, "irlValue", irlValue);
    }

    private static boolean isIncludedInterface(InterfaceMatrixProvider.InterfaceRecord record,
                                               Set<Integer> activeEntityIds) {
        // Metadata exposes database IRL IDs minus two; only positive adjusted values participate.
        return record.irlId() != null && record.irlId() > 2
                && activeEntityIds.contains(record.fromEntityId())
                && activeEntityIds.contains(record.toEntityId());
    }

    private static int alignLookupMaximum(String name, int actual, int maxSrlId) {
        if (actual != maxSrlId) {
            log.warn("SRL calculation: {} changed from {} to {} to match maxSrlId", name, actual, maxSrlId);
        }
        return maxSrlId;
    }

    private static int highestActiveLookupId(List<LookupValue> values) {
        return values.stream()
                .filter(LookupValue::isActive)
                .map(LookupValue::getLookupId)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private void appendLookups(Element meta, String type, List<LookupValue> values) {
        Element container = appendElement(meta, type + "Meta");
        for (LookupValue value : values) {
            if (value.isActive()) {
                Element element = appendElement(container, type);
                Integer lookupId = value.getLookupId();
                attribute(element, type + "Id", "irl".equals(type) && lookupId != null ? lookupId - 2 : lookupId);
                attribute(element, "code", value.getLookupCode());
                attribute(element, "name", value.getLookupDescription());
            }
        }
    }

    private Element appendElement(Element parent, String name) {
        Element element = getDoc().createElement(name);
        parent.appendChild(element);
        return element;
    }

    private void attribute(Element element, String name, Object value) {
        element.setAttribute(name, value == null ? "" : value.toString());
    }
}
