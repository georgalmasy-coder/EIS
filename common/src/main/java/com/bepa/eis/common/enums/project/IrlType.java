package com.bepa.eis.common.enums.project;

public enum IrlType {

    IRL_NOT_CHECKED(
            1,
            0,
            "-",
            "Not Checked",
            "Integration between systems NOT Checked",
            true),

    IRL_LEVEL0(
            2,
            0,
            "0",
            "No Integration",
            "No Integration",
            true),

    IRL_LEVEL1(
            3,
            1,
            "1",
            "Concept for integration identified",
            "A high-level concept for integration has been identified",
            true),

    IRL_LEVEL2(
            4,
            2,
            "2",
            "Integration Requirements specified",
            "There is some level of specificity of requirements to characterize the interaction between systems",
            true),

    IRL_LEVEL3(
            5,
            3,
            "3",
            "Detailed Integration Design defined",
            "The detailed integration design has been defined to include all interface details",
            true),

    IRL_LEVEL4(
            6,
            4,
            "4",
            "Validation of functions in lab.",
            "Validation of interrelated functions between integrating systems in a laboratory environment",
            true),

    IRL_LEVEL5(
            7,
            5,
            "5",
            "Validation of functions in relevant environment",
            "Validation of interrelated functions between integrating systems in a relevant environment",
            true),

    IRL_LEVEL6(
            8,
            6,
            "6",
            "Validation of functions in end-to-end environment",
            "Validation of interrelated functions between integrating systems in a relevant end-to-end environment",
            true),

    IRL_LEVEL7(
            9,
            7,
            "7",
            "Fully integrated system prototype demonstrated\t",
            "System prototype integration demonstration in an operational high fidelity environment",
            true),

    IRL_LEVEL8(
            10,
            8,
            "8",
            "System integration completed and qualified via tests\t",
            "System integration completed and mission qualified through test and demonstration in an operational environment",
            true),

    IRL_LEVEL9(
            11,
            9,
            "9",
            "System Integration proved in operations",
            "NSystem Integration is proven through successful mission proven operations capabilities",
            true),

    INVALID_IRL_LEVEL(
            -1,
            -1,
            "?",
            "Invalid IRL",
            "Invalid IRL",
            true);

    private final int irlId;
    private final int irlLevel;
    private final String irLCode;
    private final String irlName;
    private final String irlDescription;
    private final boolean active;


    // Constructor
    IrlType(int irlId, int irlLevel, String irLCode, String irlName, String irlDescription, boolean active) {
        this.irlId = irlId;
        this.irlLevel = irlLevel;
        this.irLCode = irLCode;
        this.irlName = irlName;
        this.irlDescription = irlDescription;
        this.active = active;
    }

    public int getIrlId() {
        return irlId;
    }

    public int getIrlLevel() {
        return irlLevel;
    }

    public String getIrlName() {
        return irlName;
    }

    public String getIrlCode() {
        return irLCode;
    }

    public String getIrlDescription() {
        return irlDescription;
    }

    public boolean isActive() {
        return active;
    }

    public static IrlType valueOf(int value) {
        for (IrlType entityDataElement : IrlType.values()) {
            if (entityDataElement.irlLevel == value) return entityDataElement;
        }
        return INVALID_IRL_LEVEL;
    }

}
