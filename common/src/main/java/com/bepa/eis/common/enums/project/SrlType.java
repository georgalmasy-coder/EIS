package com.bepa.eis.common.enums.project;

public enum SrlType {

    SRL_LEVEL1(
            1,
            "System alternative materiel solutions should have been considered",
            "",
            "crimson",
            true),
    SRL_LEVEL2(
            2,
            "System materiel solution should have been identified",
            "",
            "red",
            true),
    SRL_LEVEL3(
            3,
            "System high-risk immature technologies should have been identified and prototyped",
            "",
            "gray",
            true),
    SRL_LEVEL4(
            4,
            "System performance specifications and constraints should have been defined and the baseline has been allocated",
            "",
            "amber",
            true),
    SRL_LEVEL5(
            5,
            "System high-risk component technology development should have been complete; low-risk system components",
            "",
            "violet",
            true),
    SRL_LEVEL6(
            6,
            "System component integrability should have been validated",
            "",
            "tangerine",
            true),
    SRL_LEVEL7(
            7,
            "System threshold capability should have been demonstrated at operational performance level using operational environment",
            "cyan",
            "",
            true),
    SRL_LEVEL8(
            8,
            "System interoperability should have been demonstrated in an operational environment",
            "",
            "magenta",
            true),
    SRL_LEVEL9(
            9,
            "System has achieved initial operational capability and can satisfy mission objectives",
            "",
            "white",
            true),
    INVALID_SRL_LEVEL(
            -1,
            "Invalid SRL Level",
            "Invalid TRL Level",
            "black",
            false);

    private final int srlLevel;
    private final String srlName;
    private final String srlDescription;
    private final String srlColor;
    private final boolean active;


    // Constructor
    SrlType(int srlLevel, String srlName, String srlDescription, String srlColor, boolean active) {
        this.srlLevel = srlLevel;
        this.srlName = srlName;
        this.srlDescription = srlDescription;
        this.srlColor = srlColor;
        this.active = active;
    }

    public int getTrlLevel() {
        return srlLevel;
    }

    public String getSrlName() {
        return srlName;
    }

    public String getSrlDescription() {
        return srlDescription;
    }

    public String getSrlColor() {
        return srlColor;
    }

    public boolean isActive() {
        return active;
    }

    public static SrlType valueOf(int value) {
        for (SrlType entityDataElement : SrlType.values()) {
            if (entityDataElement.srlLevel == value) return entityDataElement;
        }
        return INVALID_SRL_LEVEL;
    }

}
