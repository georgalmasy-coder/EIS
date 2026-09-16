import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";

const DATA_URL = "/project/myprojects?cmd=list";
const SELECT_PROJECT_URL = "/project/myprojects?cmd=select&projectId=";
const EDIT_PROJECT_URL = "/web/view?page=project-edit&mode=edit&id=";
const RETURN_URL = "/web/view?page=myprojects";

function byId(id) {
    return document.getElementById(id);
}

function getElements() {
    return {
        projectName: byId("projectName"),
        loadStatus: byId("loadStatus"),
        projectsGrid: byId("projectsGrid")
    };
}

async function fetchXml(url) {
    const response = await fetch(url, {
        method: "GET",
        headers: {
            "Accept": "application/xml,text/xml,*/*"
        },
        credentials: "same-origin"
    });

    if (!response.ok) {
        throw new Error(`Could not load XML. Status: ${response.status}`);
    }

    const xmlText = await response.text();

    return parseXml(xmlText);
}

function parseXml(xmlText) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(xmlText, "application/xml");

    if (doc.getElementsByTagName("parsererror").length > 0) {
        throw new Error("Invalid XML returned by service.");
    }

    return doc;
}

function getDirectChild(parent, tagName) {
    if (!parent) {
        return null;
    }

    return Array.from(parent.children).find(function (child) {
        return child.tagName === tagName;
    }) || null;
}

function getDirectChildren(parent, tagName) {
    if (!parent) {
        return [];
    }

    return Array.from(parent.children).filter(function (child) {
        return child.tagName === tagName;
    });
}

function directTextOf(parent, tagName) {
    const child = getDirectChild(parent, tagName);

    if (!child) {
        return "";
    }

    return String(child.textContent || "").trim();
}

function getAttribute(element, attributeName, fallback = "") {
    if (!element) {
        return fallback;
    }

    const value = element.getAttribute(attributeName);

    if (value === null || value === undefined || String(value).trim() === "") {
        return fallback;
    }

    return String(value).trim();
}

function getNumberAttribute(element, attributeName, fallback = 0) {
    const value = getAttribute(element, attributeName, "");

    if (value === "") {
        return fallback;
    }

    const parsed = Number(value);

    return Number.isFinite(parsed) ? parsed : fallback;
}

function getBooleanAttribute(element, attributeName, fallback = false) {
    const value = getAttribute(element, attributeName, "");

    if (value === "") {
        return fallback;
    }

    return ["true", "1", "yes", "ja", "y", "on"].includes(value.toLowerCase());
}

function toNumber(value, fallback = 0) {
    const parsed = Number(value);

    if (!Number.isFinite(parsed)) {
        return fallback;
    }

    return parsed;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}

function readProjects(root) {
    const projectsElement = getDirectChild(root, "projects");

    return getDirectChildren(projectsElement, "project").map(function (projectElement) {
        const trlsElement = getDirectChild(projectElement, "trls");

        return {
            projectId: directTextOf(projectElement, "projectid"),
            projectName: directTextOf(projectElement, "projectname"),
            projectOwner: directTextOf(projectElement, "OwnerId"),
            projectCategory: directTextOf(projectElement, "projectcategory"),
            projectStatus: directTextOf(projectElement, "projectstatus"),
            countNotifications: toNumber(directTextOf(projectElement, "countnotifications"), 0),
            countStakeholderRequirement: directTextOf(projectElement, "countstakeholderrequirement"),
            countSystemRequirement: directTextOf(projectElement, "countsystemrequirement"),
            countSystemBreakdown: directTextOf(projectElement, "countsystembreakdown"),
            projectStartDate: directTextOf(projectElement, "StartDate"),
            projectEndDate: directTextOf(projectElement, "EndDate"),
            nextTrlDeadline: directTextOf(projectElement, "nexttrldeadline"),
            trls: readTrls(trlsElement)
        };
    });
}

function readTrls(trlsElement) {
    const trlsFromXml = getDirectChildren(trlsElement, "trl").map(function (trlElement) {
        return {
            code: getAttribute(trlElement, "code", ""),
            description: getAttribute(trlElement, "description", ""),
            count: getNumberAttribute(trlElement, "count", 0),
            enabled: getBooleanAttribute(trlElement, "enabled", false)
        };
    });

    const trlsByCode = new Map();

    trlsFromXml.forEach(function (trl) {
        trlsByCode.set(String(trl.code), trl);
    });

    const trls = [];

    for (let level = 1; level <= 9; level += 1) {
        const key = String(level);
        const trl = trlsByCode.get(key);

        trls.push(trl || {
            code: key,
            description: `TRL ${key}`,
            count: 0,
            enabled: false
        });
    }

    return trls;
}

function parseDateDdmmyyyy(value) {
    const normalized = String(value || "").trim();

    if (!/^\d{8}$/.test(normalized)) {
        return null;
    }

    const day = Number(normalized.substring(0, 2));
    const month = Number(normalized.substring(2, 4)) - 1;
    const year = Number(normalized.substring(4, 8));

    const date = new Date(year, month, day);

    if (
        date.getFullYear() !== year ||
        date.getMonth() !== month ||
        date.getDate() !== day
    ) {
        return null;
    }

    return date;
}

function formatDate(value) {
    const date = parseDateDdmmyyyy(value);

    if (!date) {
        return "—";
    }

    return new Intl.DateTimeFormat("da-DK", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    }).format(date);
}

function daysBetween(start, end) {
    const millisecondsPerDay = 24 * 60 * 60 * 1000;
    return Math.round((end.getTime() - start.getTime()) / millisecondsPerDay);
}

function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value));
}

function calculateTimeline(startDate, endDate, nowDate) {
    if (!startDate || !endDate) {
        return {
            statusClass: "timeline-normal",
            startPercent: 0,
            endPercent: 100,
            nowPercent: 50,
            fillStartPercent: 0,
            fillEndPercent: 100
        };
    }

    let visualStart = new Date(startDate);
    let visualEnd = new Date(endDate);

    if (nowDate < startDate) {
        const plannedDuration = Math.max(1, daysBetween(startDate, endDate));
        const daysBeforeStart = Math.abs(daysBetween(nowDate, startDate));

        visualStart = new Date(startDate);
        visualStart.setDate(
            visualStart.getDate() - Math.min(plannedDuration, daysBeforeStart)
        );
    }

    if (nowDate > endDate) {
        visualEnd = new Date(nowDate);
    }

    const visualDuration = Math.max(1, visualEnd.getTime() - visualStart.getTime());

    const startPercent = clamp(
        ((startDate.getTime() - visualStart.getTime()) / visualDuration) * 100,
        0,
        100
    );

    const endPercent = clamp(
        ((endDate.getTime() - visualStart.getTime()) / visualDuration) * 100,
        0,
        100
    );

    const nowPercent = clamp(
        ((nowDate.getTime() - visualStart.getTime()) / visualDuration) * 100,
        0,
        100
    );

    const plannedDuration = Math.max(1, endDate.getTime() - startDate.getTime());
    const usedRatio = (nowDate.getTime() - startDate.getTime()) / plannedDuration;

    let statusClass = "timeline-normal";

    if (nowDate > endDate) {
        statusClass = "timeline-overdue";
    } else if (usedRatio >= 0.9) {
        statusClass = "timeline-warning";
    }

    return {
        statusClass,
        startPercent,
        endPercent,
        nowPercent,
        fillStartPercent: Math.min(startPercent, endPercent),
        fillEndPercent: Math.max(startPercent, endPercent)
    };
}

function renderProjects(projects, elements) {
    if (!elements.projectsGrid) {
        return;
    }

    elements.projectsGrid.innerHTML = "";

    if (!projects.length) {
        elements.projectsGrid.innerHTML = `<div class="empty">No projects found.</div>`;
        return;
    }

    projects.forEach(function (project) {
        elements.projectsGrid.appendChild(createProjectCard(project));
    });

    elements.projectsGrid.appendChild(createNewProjectCard());
}

function createProjectCard(project) {
    const card = document.createElement("article");
    card.className = "project-card";

    const startDate = parseDateDdmmyyyy(project.projectStartDate);
    const endDate = parseDateDdmmyyyy(project.projectEndDate);
    const timeline = calculateTimeline(startDate, endDate, new Date());

    card.innerHTML = `
        ${renderProjectTopActions(project)}

        <div class="project-card-header">
            <div class="project-meta-block">
                <div class="project-meta-label">Project Name</div>
                <div class="project-meta-value">${escapeHtml(project.projectName)}</div>
            </div>
            <div class="project-meta-block">
                <div class="project-meta-label">Project Owner</div>
                <div class="project-meta-value">${escapeHtml(project.projectOwner)}</div>
            </div>
            <div class="project-meta-block">
                <div class="project-meta-label">Category</div>
                <div class="project-meta-value">${escapeHtml(project.projectCategory)}</div>
            </div>
            <div class="project-meta-block">
                <div class="project-meta-label">Status</div>
                <div class="project-meta-value">${escapeHtml(project.projectStatus)}</div>
            </div>
        </div>

        <div class="project-counts" aria-label="Project counts">
            <div class="project-count">
                <strong>${escapeHtml(project.countStakeholderRequirement)}</strong>
                <span>Stakeholder requirements</span>
            </div>
            <div class="project-count">
                <strong>${escapeHtml(project.countSystemRequirement)}</strong>
                <span>System requirements</span>
            </div>
            <div class="project-count">
                <strong>${escapeHtml(project.countSystemBreakdown)}</strong>
                <span>Physical structures</span>
            </div>
        </div>

        <div class="project-timeline ${timeline.statusClass}">
            <div class="timeline-line">
                <div class="timeline-active"
                     style="left:${timeline.fillStartPercent}%; width:${timeline.fillEndPercent - timeline.fillStartPercent}%">
                </div>
                <div class="timeline-dot timeline-start"
                     style="left:${timeline.startPercent}%"
                     title="Start date: ${escapeHtml(formatDate(project.projectStartDate))}">
                </div>
                <div class="timeline-dot timeline-end"
                     style="left:${timeline.endPercent}%"
                     title="End date: ${escapeHtml(formatDate(project.projectEndDate))}">
                </div>
                <div class="timeline-now"
                     style="left:${timeline.nowPercent}%"
                     title="Current date">
                </div>
            </div>
            <div class="timeline-labels">
                <span class="timeline-label timeline-label-start">${escapeHtml(formatDate(project.projectStartDate))}</span>
                <span class="timeline-label timeline-label-end">${escapeHtml(formatDate(project.projectEndDate))}</span>
            </div>
        </div>

        <div class="trl-title">Technology Readiness Level</div>

        <div class="trl-track">
            ${project.trls.map(renderTrlBubble).join("")}
        </div>

        <div class="project-card-footer">
            <div class="next-trl-deadline">
                <span class="deadline-icon">◷</span>
                <span>Next TRL Deadline</span>
                <strong>${escapeHtml(project.nextTrlDeadline)}</strong>
            </div>

            <a class="work-with-project"
               href="${SELECT_PROJECT_URL}${encodeURIComponent(project.projectId)}">
                Work with project
            </a>
        </div>
    `;

    return card;
}

function renderProjectTopActions(project) {
    const hasProjectId = String(project.projectId || "").trim() !== "";
    const hasNotifications = toNumber(project.countNotifications, 0) > 0;

    if (!hasProjectId && !hasNotifications) {
        return "";
    }

    return `
        <div class="project-top-actions">
            ${renderEditProjectLink(project)}
            ${renderNotificationBadge(project.countNotifications)}
        </div>
    `;
}

function renderNotificationBadge(countNotifications) {
    const count = toNumber(countNotifications, 0);

    if (count <= 0) {
        return "";
    }

    const displayValue = count > 99 ? "99+" : String(count);
    const label = count === 1 ? "notification" : "notifications";

    return `
        <div class="project-notification-badge"
             title="${escapeHtml(displayValue)} ${label}"
             aria-label="${escapeHtml(displayValue)} ${label}">
            ${escapeHtml(displayValue)}
        </div>
    `;
}

function renderEditProjectLink(project) {
    const projectId = String(project.projectId || "").trim();

    if (!projectId) {
        return "";
    }

    const url = `${EDIT_PROJECT_URL}${encodeURIComponent(projectId)}&returnUrl=${encodeURIComponent(RETURN_URL)}`;

    return `
        <a class="project-edit-link"
           href="${url}"
           data-project-id="${escapeHtml(projectId)}"
           title="Edit project"
           aria-label="Edit project ${escapeHtml(project.projectName)}">
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M4 17.25V20h2.75L17.81 8.94l-2.75-2.75L4 17.25zm15.71-10.04a1.003 1.003 0 0 0 0-1.42l-1.5-1.5a1.003 1.003 0 0 0-1.42 0l-1.17 1.17 2.75 2.75 1.34-1z"></path>
            </svg>
        </a>
    `;
}

function renderTrlBubble(trl) {
    let className = "trl-bubble";

    if (!trl.enabled) {
        className += " trl-disabled";
    } else if (trl.count > 0) {
        className += " trl-active";
    } else {
        className += " trl-muted";
    }

    return `
        <span class="${className}"
              title="${escapeHtml(trl.description)}"
              aria-label="TRL ${escapeHtml(trl.code)}: ${escapeHtml(trl.description)}">
            ${escapeHtml(trl.code)}
        </span>
    `;
}

function createNewProjectCard() {
    const card = document.createElement("article");
    card.className = "project-card new-project-card";

    card.innerHTML = `
        <button type="button" class="create-project-button">
            + Create new project
        </button>
    `;

    return card;
}

function applyMyProjectsXml(doc, elements) {
    const root = doc.getElementsByTagName("MyProjects")[0] || doc.documentElement;
    const projects = readProjects(root);

    applyTopbarMetadata(document, doc);
    renderProjects(projects, elements);
}

async function startPage() {
    const elements = getElements();

    try {
        await initMenu();
        initHelpDialog();
        mountTopbar(document);

        elements.projectsGrid?.addEventListener("click", (event) => {
            const link = event.target.closest(".project-edit-link");

            if (!link) {
                const createButton = event.target.closest(".create-project-button");

                if (!createButton) {
                    return;
                }

                event.preventDefault();

                openEditDialog({
                    page: "project-edit",
                    mode: "create",
                    title: "Create Project",
                    onSaved: () => window.location.reload()
                });

                return;
            }

            event.preventDefault();

            const projectId = String(link.getAttribute("data-project-id") || "").trim();

            if (!projectId) {
                return;
            }

            openEditDialog({
                page: "project-edit",
                mode: "edit",
                id: projectId,
                title: "Edit Project",
                onSaved: () => window.location.reload()
            });
        });

        if (elements.loadStatus) {
            elements.loadStatus.textContent = "Loading…";
        }

        if (elements.projectsGrid) {
            elements.projectsGrid.innerHTML = `<div class="empty">Loading projects…</div>`;
        }

        const doc = await fetchXml(DATA_URL);

        applyMyProjectsXml(doc, elements);
        if (elements.loadStatus) {
            elements.loadStatus.textContent = "Loaded";
        }
    } catch (error) {
        console.error(error);

        if (elements.loadStatus) {
            elements.loadStatus.textContent = "Error";
        }

        if (elements.projectsGrid) {
            elements.projectsGrid.innerHTML = `<div class="empty error">Failed to load projects.</div>`;
        }
    }
}

startPage();
