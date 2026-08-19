import { initMenu } from "../components/menu.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { openEditDialog } from "../components/edit-dialog.js";

const DATA_URL = "/project/overview?cmd=overview";

const state = {
    projectId: null
};

async function init() {
    initMenu();
    initHelpDialog();
    mountTopbar();
    
    document.getElementById("btnProjectDetails")?.addEventListener("click", () => {
        if (state.projectId) {
            openEditDialog({
                page: "project-edit",
                mode: "edit",
                id: state.projectId,
                title: "Edit Project",
                onSaved: () => loadProjectOverview()
            });
        }
    });

    await loadProjectOverview();
}

async function loadProjectOverview() {
    const loadStatus = document.getElementById("loadStatus");
    if (loadStatus) loadStatus.textContent = "Loading...";

    try {
        const response = await fetch(DATA_URL, {
            method: "GET",
            headers: {
                "Accept": "application/xml,text/xml,*/*"
            },
            credentials: "same-origin"
        });
        
        if (!response.ok) {
            throw new Error(`Failed to fetch project overview. Status: ${response.status}`);
        }
        
        const xmlText = await response.text();
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(xmlText, "application/xml");
        
        if (xmlDoc.getElementsByTagName("parsererror").length > 0) {
            throw new Error("Invalid XML returned by service.");
        }
        
        // Apply TopPanel metadata
        const topPanel = xmlDoc.getElementsByTagName("TopPanel")[0];
        if (topPanel) {
            applyTopbarMetadata(document, topPanel);
            
            // Image layout specific header
            updateElementText("pageEyebrow", getXmlValue(topPanel, "ProjectName"));
            updateElementText("pageHeading", getXmlValue(topPanel, "WorkspaceHeading"));
            updateElementText("pageHelpText", getXmlValue(topPanel, "WorkspaceHelpText"));
        }

        // Project Info
        const project = xmlDoc.getElementsByTagName("Project")[0];
        if (project) {
            state.projectId = getXmlValue(project, "ProjectId");

            // Sidebar Status
            updateElementText("projectOwner", getXmlOptionText(project, "OwnerId"));
            updateElementText("activeBaseline", "Version " + getXmlValue(project, "Version"));
            updateElementText("nextReview", "TRL review · " + getXmlValue(project, "NextTrlReview"));
            updateElementText("lastUpdated", formatDateTime(getXmlValue(project, "ChangedDateTime")));
            
            const statusBadge = document.getElementById("projectStatusBadge");
            if (statusBadge) {
                const status = getXmlOptionText(project, "ProjectStatus");
                statusBadge.textContent = status || "Unknown";
                statusBadge.className = "status-badge-pill badge-" + (status || "unknown").toLowerCase().replace(/\s+/g, '-');
            }

            // Assets Counts
            const reqCount = getXmlValue(project, "CountRequirement");
            const funcCount = getXmlValue(project, "CountFunctionalStructure");
            const logCount = getXmlValue(project, "CountLogicalStructure");
            const physCount = getXmlValue(project, "CountPhysicalStructure");

            updateElementText("countR", reqCount);
            updateElementText("countF", funcCount);
            updateElementText("countL", logCount);
            updateElementText("countP", physCount);
            
            // Sub-cards in Recommended Step
            updateElementText("physicalCountSub", physCount);
            updateElementText("functionsCoverCount", funcCount);
            updateElementText("requirementsCoverCount", reqCount);

            // Recent Changes (Mocked for now as XML doesn't have a list, but using current project info)
            updateElementText("baselineNameRecent", "Baseline v" + getXmlValue(project, "Version"));
        }

        // Baseline Info
        const baseline = xmlDoc.getElementsByTagName("BaselineElements")[0];
        if (baseline) {
            // Could update more baseline info here
        }

        if (loadStatus) loadStatus.textContent = "Idle";
    } catch (error) {
        console.error("Error loading project overview:", error);
        if (loadStatus) loadStatus.textContent = "Error";
    }
}

function getXmlValue(parent, tagName) {
    const element = parent.getElementsByTagName(tagName)[0];
    return element ? element.textContent.trim() : "";
}

function getXmlOptionText(parent, tagName) {
    const element = parent.getElementsByTagName(tagName)[0];
    if (!element) return "";
    
    const option = element.getElementsByTagName("Option")[0];
    return option ? option.textContent.trim() : element.textContent.trim();
}

function updateElementText(id, text) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = text || "—";
    }
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return "—";
    try {
        const date = new Date(dateTimeStr);
        const day = date.getDate();
        const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        const month = months[date.getMonth()];
        const year = date.getFullYear();
        return `${day} ${month} ${year}`;
    } catch (e) {
        return dateTimeStr;
    }
}

// Start initialization
document.addEventListener("DOMContentLoaded", init);
