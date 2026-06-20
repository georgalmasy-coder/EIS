package com.bepa.eis.common.enums.project;

public enum TrlType {

    TRL_LEVEL1(
            1,
            "Basic principles observed",
            "Identification of the new concept. Identification of the integration of the concept. Identification of expected barriers. Identification of applications. Identification of materials and technologies based on theoretical fundamentals/literature data. Preliminary numerical knowledge. Qualitative description of interactions between technologies.",
            true),
    TRL_LEVEL2(
            2,
            "Technology concept formulated",
            "Enhanced knowledge of technologies, materials and interfaces is acquired. New concept is investigated and refined. First evaluation about the feasibility is performed. Initial numerical knowledge. Qualitative description of interactions between technologies.",
            true),
    TRL_LEVEL3(
            3,
            "Experimental proof of concept",
            "Prototype of the new concept is developed. Numerical knowledge is acquired. Qualitative description of interactions between technologies.",
            true),
    TRL_LEVEL4(
            4,
            "Technology validated in lab",
            "Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies.",
            true),
    TRL_LEVEL5(
            5,
            "Technology validated in relevant environment",
            "Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies.",
            true),
    TRL_LEVEL6(
            6,
            "Technology pilot demonstrated in relevant environment",
            "Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies.",
            true),
    TRL_LEVEL7(
            7,
            "System prototype demonstration in operational environment",
            "Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies.",
            true),
    TRL_LEVEL8(
            8,
            "System complete and qualified",
            "Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies.",
            true),
    TRL_LEVEL9(
            9,
            "Actual system proven in operational environment",
            "Prototype of the new concept is validated in relevant environment. Numerical knowledge is acquired. Qualitative description of interactions between technologies.",
            true),
    INVALID_TRL_LEVEL(
            -1,
            "Invalid TRL Level",
            "Invalid TRL Level",
            false);

    private final int trlLevel;
    private final String trlName;
    private final String trlDescription;
    private final boolean active;


    // Constructor
    TrlType(int trlLevel, String trlName, String trlDescription, boolean active) {
        this.trlLevel = trlLevel;
        this.trlName = trlName;
        this.trlDescription = trlDescription;
        this.active = active;
    }

    public int getTrlLevel() {
        return trlLevel;
    }

    public String getTrlName() {
        return trlName;
    }

    public String getTrlDescription() {
        return trlDescription;
    }

    public boolean isActive() {
        return active;
    }

    public static TrlType valueOf(int value) {
        for (TrlType entityDataElement : TrlType.values()) {
            if (entityDataElement.trlLevel == value) return entityDataElement;
        }
        return INVALID_TRL_LEVEL;
    }

}
