package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

public class SrlProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(SrlProvider.class);

    private static final String GET_SRLS_BY_PROJECT_ID_SQL =
            "SELECT * FROM SRL " +
            "WHERE CustomerId = ? " +
            "AND  ProjectId = ? " +
            "ORDER BY SRLLevel";

    public SrlProvider(WebSession webSession) {
        super(webSession);
    }

    public SrlList getSrlsByProjectId() throws SQLException {

        SrlList srlList = new SrlList(getWebSession());

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(GET_SRLS_BY_PROJECT_ID_SQL)) {

                setInt(ps, getWebSession().getCustomerId(), 1);
                setInt(ps, getWebSession().getProjectId(), 2);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {

                        Srl srl = srlList.getNewSrl();

                        Integer value = ThreadLocalRandom.current().nextInt(1, 33);
                        String level = rs.getString("SRLLevel");
                        String description = rs.getString("SRLName");
                        String color = rs.getString("color");
                        String hover = "Level : " + level + " - " + description;

                        srl.addAttribute("value", value.toString());
                        srl.addAttribute("label", "Level : " + level);
                        srl.addAttribute("SRLLevel", level);
                        srl.addAttribute("SRLName", description);
                        srl.addAttribute("color", color);
                        srl.addAttribute("hover", hover);

                        srlList.addSrl(srl);

                    }

                }
            }
        }

        return srlList;
    }
}
