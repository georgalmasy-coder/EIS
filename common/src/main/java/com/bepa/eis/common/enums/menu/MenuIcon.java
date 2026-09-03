package com.bepa.eis.common.enums.menu;

public enum MenuIcon {

    HOME(
            1,
            "Home",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M3 11.25 12 4l9 7.25V20a1 1 0 0 1-1 1h-5.5a1 1 0 0 1-1-1v-5.25h-3V20a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-8.75Z\"></path></svg>"
    ),

    FOLDER(
            2,
            "Folder",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M3 6.5A1.5 1.5 0 0 1 4.5 5h5.12a1.5 1.5 0 0 1 1.06.44L12.5 7h7A1.5 1.5 0 0 1 21 8.5v8A1.5 1.5 0 0 1 19.5 18h-15A1.5 1.5 0 0 1 3 16.5v-10Z\"></path></svg>"
    ),

    COG(
            3,
            "Settings",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M11.6 2.5h.8l.67 2.1a6.8 6.8 0 0 1 1.56.65l2-.99.57.57-1 2.01c.25.5.48 1.02.65 1.57l2.1.67v.8l-2.1.67a6.8 6.8 0 0 1-.65 1.56l1 2-.57.57-2.02-1a6.8 6.8 0 0 1-1.56.65l-.67 2.1h-.8l-.67-2.1a6.8 6.8 0 0 1-1.56-.65l-2.01 1-.57-.57.99-2.01a6.8 6.8 0 0 1-.65-1.56l-2.1-.67v-.8l2.1-.67c.17-.55.4-1.07.65-1.57l-1-2.01.57-.57 2 .99c.5-.25 1.02-.48 1.57-.65l.67-2.1ZM12 9.25A2.75 2.75 0 1 0 12 14.75 2.75 2.75 0 0 0 12 9.25Z\"></path></svg>"
    ),

    DASHBOARD(
            4,
            "Dashboard",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M3 3h8v8H3V3Zm10 0h8v5h-8V3ZM3 13h8v8H3v-8Zm10 4h8v4h-8v-4Z\"></path></svg>"
    ),

    CHART(
            5,
            "Chart",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M4 20V4h2v16H4Zm4 0v-9h2v9H8Zm4 0V8h2v12h-2Zm4 0v-6h2v6h-2Zm4 0v-11h2v11h-2ZM3 21h18v1H3v-1Z\"></path></svg>"
    ),

    REQUIREMENT(
            6,
            "Requirement",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><rect x=\"3.5\" y=\"3.5\" width=\"17\" height=\"17\" rx=\"4\" ry=\"4\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"/><text x=\"12\" y=\"15\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" font-size=\"8.5\" font-weight=\"700\" fill=\"currentColor\">R</text></svg>"
    ),

    FUNCTIONAL(
            7,
            "Functional",
//            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><rect x=\"3.5\" y=\"3.5\" width=\"17\" height=\"17\" rx=\"4\" ry=\"4\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"/><text x=\"12\" y=\"15\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" font-size=\"8.5\" font-weight=\"700\" fill=\"currentColor\">F</text></svg>"
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"><rect x=\"2\" y=\"2\" width=\"20\" height=\"20\" rx=\"6\" fill=\"#1f4b50\"/><text x=\"12\" y=\"15.4\" text-anchor=\"middle\" fill=\"#72c5da\" font-family=\"Inter,Arial,sans-serif\" font-size=\"10\" font-weight=\"700\"  fill=\"currentColor\">F</text></svg>"
    ),

    //<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><rect x="2" y="2" width="20" height="20" rx="6" fill="#1f4b50"/><text x="12" y="15.4" text-anchor="middle" fill="#72c5da" font-family="Inter,Arial,sans-serif" font-size="10" font-weight="700">F</text></svg>

    LOGICAL(
            8,
            "Logical",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><rect x=\"3.5\" y=\"3.5\" width=\"17\" height=\"17\" rx=\"4\" ry=\"4\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"/><text x=\"12\" y=\"15\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" font-size=\"8.5\" font-weight=\"700\" fill=\"currentColor\">L</text></svg>"
    ),

    PHYSICAL(
            9,
            "Physical",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><rect x=\"3.5\" y=\"3.5\" width=\"17\" height=\"17\" rx=\"4\" ry=\"4\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"/><text x=\"12\" y=\"15\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" font-size=\"8.5\" font-weight=\"700\" fill=\"currentColor\">P</text></svg>"
    ),

    LINK(
            10,
            "Link",
            "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M10.6 13.4a1 1 0 0 1 0-1.4l3.4-3.4a3.5 3.5 0 0 1 4.95 4.95l-1.65 1.65a3.5 3.5 0 0 1-4.95 0 .95.95 0 1 1 1.34-1.34 1.6 1.6 0 0 0 2.27 0l1.65-1.65a1.5 1.5 0 1 0-2.12-2.12l-3.4 3.4a1 1 0 0 1-1.42 0ZM13.4 10.6a1 1 0 0 1 0 1.4l-3.4 3.4a3.5 3.5 0 1 1-4.95-4.95l1.65-1.65a3.5 3.5 0 0 1 4.95 0 .95.95 0 1 1-1.34 1.34 1.6 1.6 0 0 0-2.27 0L5.39 12.79a1.5 1.5 0 0 0 2.12 2.12l3.4-3.4a1 1 0 0 1 1.42 0Z\"></path></svg>"
    ),

    LIST(
            11,
            "List",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01\"/></svg>"
    ),

    GRID(
        12,
        "Grid",
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><rect x=\"3\" y=\"3\" width=\"7\" height=\"7\" rx=\"1\"/><rect x=\"14\" y=\"3\" width=\"7\" height=\"7\" rx=\"1\"/><rect x=\"3\" y=\"14\" width=\"7\" height=\"7\" rx=\"1\"/><rect x=\"14\" y=\"14\" width=\"7\" height=\"7\" rx=\"1\"/></svg>"
    ),

    HELP(
            13,
            "Help",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M9.8 9a2.4 2.4 0 1 1 3.4 2.2c-.9.4-1.2 1-1.2 1.8M12 17h.01\"/></svg>"
    ),

    OVERVIEW(
            14,
            "Overview",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01\"/></svg>"
    ),

    PDF(
            15,
            "PDF",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01\"/></svg>"
    ),

    RELATION(
            16,
            "Relation",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M10 13a5 5 0 0 0 7.1 0l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1M14 11a5 5 0 0 0-7.1 0l-2 2A5 5 0 0 0 12 20.1l1.1-1.1\"/></svg>"
    ),

    SEARCH(
            17,
            "Search",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><circle cx=\"11\" cy=\"11\" r=\"7\"/><path d=\"m20 20-4-4\"/></svg>"
    ),

    SETTINGS(
            18,
            "Settings",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><circle cx=\"12\" cy=\"12\" r=\"3\"/><path d=\"M19 12a7 7 0 0 0-.1-1l2-1.5-2-3.4-2.4 1A8 8 0 0 0 14.8 6L14.5 3h-5L9.2 6a8 8 0 0 0-1.7 1.1l-2.4-1-2 3.4L5.1 11a7 7 0 0 0 0 2l-2 1.5 2 3.4 2.4-1A8 8 0 0 0 9.2 18l.3 3h5l.3-3a8 8 0 0 0 1.7-1.1l2.4 1 2-3.4-2-1.5a7 7 0 0 0 .1-1Z\"/></svg>"
    ),

    N2(
            19,
            "N2",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><rect width=\"24\" height=\"24\" rx=\"6\" fill=\"#1f4b50\"/><text x=\"12\" y=\"15.5\" text-anchor=\"middle\" fill=\"#ffffff\" font-family=\"Inter,Arial,sans-serif\" font-size=\"9.5\" font-weight=\"700\">N2</text></svg>"
    )
    /*,

    EIS_APP(
            19,
            "EIS App",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\" role=\"img\" aria-labelledby=\"title\"><title id=\"title\">EiS</title><rect width=\"64\" height=\"64\" rx=\"13\" fill=\"#0b3f31\"/><text x=\"32\" y=\"41\" text-anchor=\"middle\" font-family=\"Arial, Helvetica, sans-serif\" font-size=\"25\" font-weight=\"700\" fill=\"#ffffff\">EiS</text></svg>"
    ),

    EIS_LOGO(
            20,
            "EiS Logo",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 280 64\" role=\"img\" aria-labelledby=\"title\">\n" +
                    "  <title id=\"title\">EiS — Engineering in Systems</title>\n" +
                    "  <g fill=\"#0b3f31\">\n" +
                    "    <text x=\"0\" y=\"43\" font-family=\"Arial, Helvetica, sans-serif\" font-size=\"42\" font-weight=\"700\" letter-spacing=\"-2\">EiS</text>\n" +
                    "    <rect x=\"78\" y=\"12\" width=\"1\" height=\"40\" opacity=\".45\"/>\n" +
                    "    <text x=\"96\" y=\"28\" font-family=\"Arial, Helvetica, sans-serif\" font-size=\"13\" font-weight=\"700\">ENGINEERING</text>\n" +
                    "    <text x=\"96\" y=\"45\" font-family=\"Arial, Helvetica, sans-serif\" font-size=\"13\" font-weight=\"700\">IN SYSTEMS</text>\n" +
                    "  </g>\n" +
                    "</svg>\n"
    )
*/
    ;

    private final int id;
    private final String name;
    private final String svgCode;

    MenuIcon(
            int id,
            String name,
            String svgCode
    ) {
        this.id = id;
        this.name = name;
        this.svgCode = svgCode;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSvgCode() {
        return svgCode;
    }

    public static MenuIcon fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (MenuIcon icon : values()) {
            if (icon.id == id) {
                return icon;
            }
        }

        return null;
    }
}
