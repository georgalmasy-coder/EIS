package com.bepa.eis.server.api.web.application.views.pro.dashboard.teamwork;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.pro.dashboard.common.DashboardMetaData;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DashboardSystemsTeamworkDocument extends DashboardTeamworkDocument {

    private static final Logger log = LoggerFactory.getLogger(DashboardSystemsTeamworkDocument.class);

    protected PageType getPageType() {
        return PageType.DASHBOARD_SYSTEMS_TEAMWORK_PAGE;
    }

    protected EntityType getEntityType() {
        return EntityType.SYSTEMS_BREAKDOWN;
    }

    public DashboardSystemsTeamworkDocument(WebSession webSession) throws Exception {
        super(webSession);
    }

    public DashboardSystemsTeamworkDocument(WebSession session, List<AbstractEntity> activeSystems,
                                            List<InterfaceMatrixProvider.InterfaceRecord> interfaceRecords) throws Exception {
        super(session, activeSystems, interfaceRecords);
    }

    protected void loadEntities() {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
        List<SystemBreakdownEntity> listOfPhysicalStructures = systemBreakdownProvider.getAllSystemBreakdown(false);

        for (SystemBreakdownEntity systemBreakdownEntity : listOfPhysicalStructures) {
            mapOfEntities.put(systemBreakdownEntity.getEntityId().getValue(), systemBreakdownEntity);
        }
    }

}
