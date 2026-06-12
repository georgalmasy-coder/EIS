package com.bepa.eis.common.utilities;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class XmlUtil {

    public static void writeXml(
            HttpServletResponse response,
            int status,
            String xml
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/xml; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(xml == null ? "" : xml);
    }

}
