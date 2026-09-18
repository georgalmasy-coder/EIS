import { initMenu } from "../components/menu.js";
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
                category: getXmlValue(node, "projectcategory"),
                priority: getXmlValue(node, "projectpriority"),
                status: getXmlValue(node, "projectstatus"),
                statusCode: getXmlValue(node, "projectstatuscode"),
                daysLeft: getXmlValue(node, "daysleft"),
                dateNextTrl: formatDisplayDate(getXmlValue(node, "dateNextTrl")),
                physicalSystemCount: getXmlValue(node, "physicalsystemcount"),
                interfaceCount: getXmlValue(node, "interfacecount"),
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

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
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

function renderValue(value) {
    const normalized = String(value ?? "").trim();

    return normalized === "" ? "&nbsp;" : escapeHtml(normalized);
}

function updateCounts() {
    const activeCount = allProjects.filter(p => p.statusCode !== "ARCHIVED").length;
    const archivedCount = allProjects.filter(p => p.statusCode === "ARCHIVED").length;
    
    document.getElementById("countActive").textContent = activeCount;
    document.getElementById("countArchived").textContent = archivedCount;
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
        
        card.innerHTML = `
            <div class="project-info">
                <h3 class="project-name">${renderValue(project.name)}</h3>
                <p class="project-owner">Owner · ${renderValue(project.owner)}</p>
            </div>
            <div class="project-details-group">
                <div class="project-detail">
                    <span class="project-detail-label">Status</span>
                    <span class="project-detail-text">${renderValue(project.status)}</span>
                </div>
                <div class="project-detail">
                    <span class="project-detail-label">Priority</span>
                    <span class="project-detail-text">${renderValue(project.priority)}</span>
                </div>
                <div class="project-detail">
                    <span class="project-detail-label">Category</span>
                    <span class="project-detail-text">${renderValue(project.category)}</span>
                </div>
                <div class="project-detail">
                    <span class="project-detail-label">Days Left on Project</span>
                    <span class="project-detail-text">${renderValue(project.daysLeft)}</span>
                </div>
                <div class="project-detail">
                    <span class="project-detail-label">Date Next TRL</span>
                    <span class="project-detail-text">${renderValue(project.dateNextTrl)}</span>
                </div>
                <div class="project-detail">
                    <span class="project-detail-label"># of Physical Systems</span>
                    <span class="project-detail-text">${renderValue(project.physicalSystemCount)}</span>
                </div>
                <div class="project-detail">
                    <span class="project-detail-label"># of Physical Interfaces</span>
                    <span class="project-detail-text">${renderValue(project.interfaceCount)}</span>
                </div>
                <div class="project-updated">
                    <span class="project-detail-label">Updated</span>
                    <span class="updated-text">${renderValue(project.lastUpdated)}</span>
                </div>
            </div>
            <div class="project-actions">
                <button class="btn-open-project" data-id="${project.id}">
                    Open &rarr;
                </button>
            </div>
        `;
        
        card.querySelector(".btn-open-project").addEventListener("click", () => {
            window.location.href = SELECT_PROJECT_URL + project.id;
        });
        
        projectsList.appendChild(card);
    });
}

document.addEventListener("DOMContentLoaded", init);
