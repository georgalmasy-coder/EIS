import { initMenu, setStoredProjectId } from "../components/menu.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { initHelpDialog } from "../components/help-dialog.js";

const DATA_URL = "/project/myprojects?cmd=list&page=allprojects";
const SELECT_PROJECT_URL = "/project/myprojects?cmd=select&projectId=";

let allProjects = [];
let currentFilter = "active";
let searchQuery = "";

async function init() {
    initMenu();
    initHelpDialog();
    mountTopbar();
    
    setupEventListeners();
    await loadProjects();
}

function setupEventListeners() {
    const btnNewProject = document.getElementById("btnNewProject");
    btnNewProject?.addEventListener("click", () => {
        openEditDialog({
            page: "project-edit",
            mode: "create",
            title: "Create Project",
            onSaved: () => window.location.reload()
        });
    });

    const searchInput = document.getElementById("projectSearch");
    searchInput.addEventListener("input", (e) => {
        searchQuery = e.target.value.toLowerCase();
        renderProjects();
    });

    const filterTabs = document.querySelectorAll(".filter-tab");
    filterTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            filterTabs.forEach(t => t.classList.remove("is-active"));
            tab.classList.add("is-active");
            currentFilter = tab.getAttribute("data-filter");
            renderProjects();
        });
    });
}

async function loadProjects() {
    const projectsList = document.getElementById("projectsList");
    const loadStatus = document.getElementById("loadStatus");
    
    if (loadStatus) loadStatus.textContent = "Loading...";
    projectsList.innerHTML = '<div class="loading-message">Loading projects...</div>';

    try {
        const response = await fetch(DATA_URL, {
            method: "GET",
            headers: {
                "Accept": "application/xml,text/xml,*/*"
            },
            credentials: "same-origin"
        });
        
        if (!response.ok) {
            throw new Error(`Failed to fetch projects. Status: ${response.status}`);
        }
        
        const xmlText = await response.text();
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(xmlText, "application/xml");
        
        if (xmlDoc.getElementsByTagName("parsererror").length > 0) {
            throw new Error("Invalid XML returned by service.");
        }
        
        // Handle TopPanel metadata
        const topPanel = xmlDoc.getElementsByTagName("TopPanel")[0];
        if (topPanel) {
            applyTopbarMetadata(document, topPanel);
        }

        const projectNodes = xmlDoc.getElementsByTagName("project");
        allProjects = Array.from(projectNodes).map(node => {
            return {
                id: getXmlValue(node, "projectid"),
                pk: getXmlValue(node, "projectpk"),
                name: getXmlValue(node, "projectname"),
                owner: getXmlValue(node, "OwnerId"),
                status: getXmlValue(node, "projectstatus"),
                statusCode: getXmlValue(node, "projectstatuscode"),
                nextStep: getXmlValue(node, "nextStep"),
                lastUpdated: formatDisplayDate(getXmlValue(node, "lastUpdated")),
                rawLastUpdated: getXmlValue(node, "lastUpdated"),
                changedDateTime: getXmlValue(node, "changeddatetime")
            };
        });

        updateCounts();
        renderProjects();
        if (loadStatus) loadStatus.textContent = "Idle";
    } catch (error) {
        console.error("Error loading projects:", error);
        projectsList.innerHTML = `<div class="error-message">Error loading projects: ${error.message}</div>`;
        if (loadStatus) loadStatus.textContent = "Error";
    }
}

function getXmlValue(parent, tagName) {
    const element = parent.getElementsByTagName(tagName)[0];
    return element ? element.textContent : "";
}

function formatDisplayDate(dateStr) {
    if (!dateStr || dateStr.length !== 8) return dateStr;
    // Assuming DDMMYYYY format from XML_DATE_FORMAT = "ddMMyyyy"
    const day = dateStr.substring(0, 2);
    const month = dateStr.substring(2, 4);
    const year = dateStr.substring(4, 8);
    
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const monthName = months[parseInt(month, 10) - 1] || month;
    
    return `${day} ${monthName} ${year}`;
}

function updateCounts() {
    const activeCount = allProjects.filter(p => p.statusCode !== "ARCHIVED").length;
    const attentionCount = allProjects.filter(p => p.statusCode === "NEEDS_ATTENTION").length;
    
    document.getElementById("countActive").textContent = activeCount;
    document.getElementById("countNeedsAttention").textContent = attentionCount;
    document.getElementById("projectCountText").textContent = `${activeCount} active projects`;
}

function renderProjects() {
    const projectsList = document.getElementById("projectsList");
    
    let filtered = allProjects.filter(p => {
        const matchesSearch = p.name.toLowerCase().includes(searchQuery) || 
                              p.owner.toLowerCase().includes(searchQuery);
        
        let matchesFilter = true;
        if (currentFilter === "active") {
            matchesFilter = p.statusCode !== "ARCHIVED";
        } else if (currentFilter === "needs-attention") {
            matchesFilter = p.statusCode === "NEEDS_ATTENTION";
        } else if (currentFilter === "archived") {
            matchesFilter = p.statusCode === "ARCHIVED";
        }
        
        return matchesSearch && matchesFilter;
    });

    if (filtered.length === 0) {
        projectsList.innerHTML = '<div class="empty-message">No projects found.</div>';
        return;
    }

    projectsList.innerHTML = "";
    filtered.forEach(project => {
        const card = document.createElement("div");
        card.className = "project-card";
        
        const statusClass = getStatusClass(project.statusCode);
        
        card.innerHTML = `
            <div class="project-info">
                <h3 class="project-name">${project.name}</h3>
                <p class="project-owner">Owner · ${project.owner}</p>
            </div>
            <div class="project-status-badge ${statusClass}">
                ${project.status}
            </div>
            <div class="project-details-group">
                <div class="project-next-step">
                    <span class="next-step-label">Next step</span>
                    <span class="next-step-text">${project.nextStep}</span>
                </div>
                <div class="project-updated">
                    <span class="updated-label">Updated</span>
                    <span class="updated-text">${project.lastUpdated}</span>
                </div>
            </div>
            <div class="project-actions">
                <button class="btn-open-project" data-id="${project.id}">
                    Open &rarr;
                </button>
            </div>
        `;
        
        card.querySelector(".btn-open-project").addEventListener("click", () => {
            setStoredProjectId(project.id);
            window.location.href = SELECT_PROJECT_URL + project.id;
        });
        
        projectsList.appendChild(card);
    });
}

function getStatusClass(code) {
    if (!code) return "status-created";
    switch(code.toUpperCase()) {
        case "PLANNED": return "status-planned";
        case "NEEDS_ATTENTION": return "status-needs-attention";
        case "CREATED": return "status-created";
        default: return "status-created";
    }
}

document.addEventListener("DOMContentLoaded", init);
