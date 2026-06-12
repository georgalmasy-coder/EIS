package com.bepa.eis.server.api.web.application.misc;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@WebServlet(name = "HelpServlet", urlPatterns = "/api/help")
public class HelpServlet extends HttpServlet {

    private static final Pattern SAFE_HELP_PAGE_NAME = Pattern.compile("^[a-zA-Z0-9_-]+$");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String page = request.getParameter("page");

        if (page == null || page.isBlank() || !SAFE_HELP_PAGE_NAME.matcher(page).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid help page.");
            return;
        }

        String helpResourcePath = "/WEB-INF/help/" + page + ".md";
        ServletContext servletContext = getServletContext();

        try (InputStream inputStream = servletContext.getResourceAsStream(helpResourcePath)) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Help page not found.");
                return;
            }

            byte[] bytes = inputStream.readAllBytes();

            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("text/markdown; charset=UTF-8");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.getOutputStream().write(bytes);
        }
    }
}