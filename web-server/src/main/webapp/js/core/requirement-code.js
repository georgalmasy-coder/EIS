export function normalizeRequirementCode(code) {
    return String(code || "").trim();
}

export function isValidRequirementCode(code) {
    const normalizedCode = normalizeRequirementCode(code);

    return normalizedCode.length > 0;
}

export function calculateLevelFromRequirementCode(code) {
    const normalizedCode = normalizeRequirementCode(code);

    if (!normalizedCode) {
        return 0;
    }

    return normalizedCode.split(".").filter((part) => part.trim() !== "").length;
}

export function getParentRequirementCode(code) {
    const normalizedCode = normalizeRequirementCode(code);

    if (!normalizedCode || !normalizedCode.includes(".")) {
        return "";
    }

    const parts = normalizedCode.split(".").filter((part) => part.trim() !== "");

    if (parts.length <= 1) {
        return "";
    }

    return parts.slice(0, -1).join(".");
}

export function getAncestorCodes(code) {
    const normalizedCode = normalizeRequirementCode(code);
    const parts = normalizedCode.split(".").filter((part) => part.trim() !== "");

    if (parts.length <= 1) {
        return [];
    }

    const ancestors = [];

    for (let i = 1; i < parts.length; i += 1) {
        ancestors.push(parts.slice(0, i).join("."));
    }

    return ancestors;
}

export function isRootRequirement(requirement) {
    if (!requirement) {
        return false;
    }

    if (requirement.parentId !== undefined && requirement.parentId !== null && String(requirement.parentId).trim() !== "") {
        return false;
    }

    const code = requirement.code || requirement.requirementCode || requirement.id || "";
    const normalizedCode = normalizeRequirementCode(code);

    return calculateLevelFromRequirementCode(normalizedCode) <= 1;
}

export function getNearestExistingParentCode(requirement, availableRequirementIds) {
    if (!requirement) {
        return "";
    }

    const code = requirement.code || requirement.requirementCode || requirement.id || "";
    const ancestors = getAncestorCodes(code).reverse();

    for (const ancestorCode of ancestors) {
        if (availableRequirementIds?.has?.(ancestorCode)) {
            return ancestorCode;
        }

        if (Array.isArray(availableRequirementIds) && availableRequirementIds.includes(ancestorCode)) {
            return ancestorCode;
        }
    }

    return "";
}