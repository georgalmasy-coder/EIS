package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.*;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.basis.baseline.Baseline;
import com.bepa.eis.server.api.web.application.views.basis.baseline.BaselineProvider;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ProjectOverview extends GenericXmlDocument {

    private ListOfElements rootElement;
    private TopPanel topPanel;
    private Project project;
    private Notifications notifications;
    private SrlList srlList;

    public ProjectOverview(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.OLD_PROJECT_OVERVIEW_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        if (false) {
            ProjectOverviewProvider projectOverviewProvider = new ProjectOverviewProvider(webSession);
            project = projectOverviewProvider.getProjectByProjectId();
            rootElement.addElement(project.getProjectElements());

        } else {

            List<TrlRecord> trlRecords = getActiveTrlRecords(
                    getWebSession().getCustomerId(),
                    getWebSession().getProjectId()
            );

            ProjectOverviewProvider projectOverviewProvider = new ProjectOverviewProvider(webSession);
            project = projectOverviewProvider.getProjectByProjectId();
            project.setNextTrlReview(getNextTrlDeadLine(trlRecords));
            rootElement.addElement(project.getProjectElements());
            getBaselineByProjectId();
        }

        if (false) {

            NotificationProvider notificationProvider = new NotificationProvider(webSession);
            notifications = notificationProvider.getNotificationsByUserAndProjectId();
            rootElement.addElement(notifications.getNotificationElements());

            SrlProvider srlProvider = new SrlProvider(webSession);
            srlList = srlProvider.getSrlsByProjectId();
            rootElement.addElement(srlList.getSrlElements());
        }
    }

    private void getBaselineByProjectId() {
        BaselineProvider baselineProvider = new BaselineProvider(getWebSession());
        List<Baseline> baselineList = baselineProvider.getBaselines();

        if (baselineList != null && !baselineList.isEmpty()) {
            for (Baseline baseline : baselineList) {
                BaselineElements baselineElements = new BaselineElements(getWebSession(), baseline);
                rootElement.addElement(baselineElements.getBaselineElements());
                return;
            }
        }
    }

    private List<TrlRecord> getActiveTrlRecords(
            Integer customerId,
            Integer projectId
    ) {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());

        return systemBreakdownProvider.getListOfTrlRecords(
                customerId,
                projectId
        );
    }

    private String getNextTrlDeadLine(List<TrlRecord> trlRecordList) {
        Timestamp nextTrlDeadline = null;

        if (trlRecordList != null) {
            for (TrlRecord trlRecord : trlRecordList) {
                Timestamp deadline = trlRecord.getNextTrlDeadline();

                if (deadline == null || !deadline.after(now())) {
                    continue;
                }

                if (nextTrlDeadline == null || deadline.before(nextTrlDeadline)) {
                    nextTrlDeadline = deadline;
                }
            }

            if (trlRecordList.isEmpty()) {
                return "-";
            }

            if (nextTrlDeadline == null) {
                return "Over due";
            }

        }
        return daysUntil(nextTrlDeadline) + " days";
    }

    private Long daysUntil(Timestamp nextTrlDeadline) {
        if (nextTrlDeadline == null) {
            return 0L;
        }

        LocalDate today = now().toLocalDateTime().toLocalDate();
        LocalDate deadlineDate = nextTrlDeadline.toLocalDateTime().toLocalDate();

        return ChronoUnit.DAYS.between(
                today,
                deadlineDate
        );
    }

    private Timestamp now() {
        return Timestamp.from(Instant.now());
    }



}
