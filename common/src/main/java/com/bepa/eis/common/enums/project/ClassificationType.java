package com.bepa.eis.common.enums.project;

public enum ClassificationType {

    CLASS_A_(
            1,
            "A_",
            "Software Control",
            "Cover all A codes",
            true),
    CLASS_AA(
            2,
            "AA",
            "Data Transfer",
            "General transfer of data e.g. file transfers",
            true),
    CLASS_AB(
            3,
            "AB",
            "Protocols and Configuration",
            "Information about the transfer og data e.g. communication protocols",
            true),
    CLASS_B_(
            4,
            "B_",
            "Signals",
            "Cover all B codes",
            true),
    CLASS_BA(
            5,
            "BA",
            "Alarm signal",
            "e.g. High temperature",
            true),
    CLASS_BB(
            6,
            "BB",
            "Command signal",
            "e.g. Start motor",
            true),
    CLASS_BC(
            7,
            "BC",
            "Event signal",
            "e.g. Motor started",
            true),
    CLASS_BD(
            8,
            "BD",
            "Indication signal",
            "e.g. Power on/off or Breaker open/closed",
            true),
    CLASS_BE(
            9,
            "BE",
            "Measuring signal",
            "e.g. Measure motor speed",
            true),
    CLASS_BF(
            10,
            "BF",
            "Set Value signal",
            "e.g. Set motor speed",
            true),
    CLASS_BG(
            11,
            "BG",
            "Power Supply signal",
            "e.g. 12 volt signal",
            true),
    CLASS_C_(
            12,
            "C_",
            "Energy Supply",
            "Covers all C codes",
            true),
    CLASS_CA(
            13,
            "CA",
            "Electrical",
            "e.g. Power supply",
            true),
    CLASS_CB(
            14,
            "CB",
            "Earth & Grounding potential",
            "Earthing or Grounding is being provided e.g. functional earth or protective earth",
            true),
    CLASS_CC(
            15,
            "CC",
            "Heating",
            "Heating is provided",
            true),
    CLASS_CD(
            16,
            "CD",
            "Cooling",
            "Cooling is provided",
            true),
    CLASS_CE(
            17,
            "CE",
            "Mechanical",
            "e.g. drive link, hydraulic/pneumatic etc.",
            true),
    CLASS_CF(
            18,
            "CF",
            "Air",
            "Air is provided",
            true),
    CLASS_CG(
            19,
            "CG",
            "Gas",
            "Gas is provided",
            true),
    CLASS_CH(
            20,
            "CH",
            "Artificial Light",
            "e.g. light bulbs, lamp posts and televisions.",
            true),
    CLASS_CI(
            21,
            "CI",
            "Fuel",
            "",
            true),
    CLASS_CJ(
            22,
            "CJ",
            "Drainage",
            "",
            true),
    CLASS_D_(
            23,
            "D_",
            "Materials",
            "Cover all D codes",
            true),
    CLASS_DA(
            24,
            "DA",
            "Weight",
            "",
            true),
    CLASS_DB(
            25,
            "DB",
            "Dimensions (WxHxD)",
            "",
            true),
    CLASS_DC(
            26,
            "DC",
            "Mechanical connection",
            "",
            true),
    CLASS_DD(
            27,
            "DD",
            "Electrical connection",
            "",
            true),
    CLASS_DE(
            28,
            "DE",
            "Physical data connection",
            "",
            true),
    CLASS_DF(
            29,
            "DF",
            "Object transfer",
            "Components, products etc. transferred to another system",
            true),
    CLASS_DG(
            30,
            "DG",
            "Liquid",
            "Supplied to another system e.g. chemicals",
            true),
    CLASS_E_(
            31,
            "E_",
            "Structural & Spatial",
            "Covers all E codes",
            true),
    CLASS_EA(
            32,
            "EA",
            "Housing and structural support",
            "e.g. foundation, mounting or cubic in rooms",
            true),
    CLASS_EB(
            33,
            "EB",
            "Activity space",
            "Space activity for another system e.g. collisions, movements, classes",
            true),
    CLASS_EC(
            34,
            "EC",
            "Access space",
            "Space for another system e.g during installation, service maintenance etc.",
            true),
    CLASS_F_(
            35,
            "F_",
            "Loads",
            "Cover all F codes",
            true),
    CLASS_FA(
            36,
            "FA",
            "Electrical",
            "",
            true),
    CLASS_FB(
            37,
            "FB",
            "Heat",
            "",
            true),
    CLASS_FC(
            38,
            "FC",
            "Cooling",
            "",
            true),
    CLASS_FD(
            39,
            "FD",
            "Pressure",
            "",
            true),
    CLASS_FE(
            40,
            "FE",
            "Waste/fluid",
            "",
            true),
    CLASS_G_(
            41,
            "G_",
            "Infrastructure",
            "Covers all G codes",
            true),
    CLASS_GA(
            42,
            "GA",
            "Energy transfer infrastructure",
            "The system provides infrastructure, so that energy can be transferred from another system to a third system. System C has to provide infrastructure for system A so that A can transfer energy to system B",
            true),
    CLASS_GB(
            43,
            "GB",
            "Information transfer infrastructure",
            "The system provides infrastructure, so that information can be transferred from another system to a third system. System C has to provide infrastructure for system A so that A can transfer information to system B",
            true),
    CLASS_GC(
            44,
            "GC",
            "Matter transfer infrastructure",
            "The system provides infrastructure, so that matter can be transferred from another system to a third system. System C has to provide infrastructure for system A so that A can transfer matter to system B e.g. piping",
            true),
    INVALID_CLASS(
            -1,
            "??",
            "Invalid Classification",
            "No example",
            false);

    private final int classId;
    private final String code;
    private final String codeDescription;
    private final String codeExample;
    private final boolean active;


    // Constructor
    ClassificationType(int classId, String code, String codeDescription, String codeExample, boolean active) {
        this.classId = classId;
        this.code = code;
        this.codeDescription = codeDescription;
        this.codeExample = codeExample;
        this.active = active;
    }

    public int getClassId() {
        return classId;
    }

    public String getCode() {
        return code;
    }

    public String getCodeDescription() {
        return codeDescription;
    }

    public String getCodeExample() {
        return codeExample;
    }

    public boolean isActive() {
        return active;
    }

    public static ClassificationType valueOf(int value) {
        for (ClassificationType entityDataElement : ClassificationType.values()) {
            if (entityDataElement.classId == value) return entityDataElement;
        }
        return INVALID_CLASS;
    }

}
