package com.bepa.eis.server.api.web.application.views;

public class OverviewPage  extends AbstractPage {

    private String title;
    private String content;
    private String footer;
    private String header;


    public OverviewPage() {
        super();
    }

    @Override
    public String getPage(String title) {

        String html;

        html = "<html>";
        html += "<head>";
        html += "<title>" + title + "</title>";
        html += "</head>";
        html += "<body>";
        html += "<h1>Hello " + title + "</h1>";
        html += "</body>";
        html += "</html>";

        return html;
    }
}
