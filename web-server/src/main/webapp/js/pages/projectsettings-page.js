import { initMenu, getMenuSection, getTopbarMetadata } from "../components/menu.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { textOf } from "../core/xml.js";

async function init() {
    await initMenu();
    initHelpDialog();
    applyTopbarMetadata(document, getTopbarMetadata());
    
    renderSettings();
}

function renderSettings() {
    const section = getMenuSection("[PROJECT-SETTINGS]");
    const grid = document.getElementById("settingsGrid");
    if (!grid) return;
    
    if (!section) {
        grid.innerHTML = '<div class="page-empty">Ingen indstillinger fundet.</div>';
        return;
    }
    
    const subItems = Array.from(section.getElementsByTagName("submain-menu-item"));
    grid.innerHTML = "";
    
    subItems.forEach(item => {
        const title = textOf(item, "display") || "Untitled";
        const url = textOf(item, "url") || "#";
        const description = textOf(item, "description") || ""; 
        
        const card = document.createElement("a");
        card.className = "settings-card";
        card.href = url;
        
        card.innerHTML = `
            <h3 class="settings-card-title">${title}</h3>
            <p class="settings-card-description">${description}</p>
            <div class="settings-card-action">Open live workflow</div>
        `;
        
        grid.appendChild(card);
    });
}

document.addEventListener("DOMContentLoaded", init);
