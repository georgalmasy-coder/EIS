import {
    closeDialogElement,
    showDialog,
    clearChildren
} from "../core/dom.js";
import { formatFileSize } from "../core/format.js";

const ALLOWED_EXTENSIONS = ["xml", "xlsx", "csv"];

function getFileExtension(fileName) {
    const value = fileName == null ? "" : String(fileName);
    const index = value.lastIndexOf(".");

    return index >= 0 ? value.substring(index + 1).toLowerCase() : "";
}

function isAllowedFile(file) {
    if (!file) {
        return false;
    }

    return ALLOWED_EXTENSIONS.includes(getFileExtension(file.name));
}

export function createImportDialog({
                                       dialogId,
                                       openButtonId,
                                       importUrl,
                                       onImportComplete = null,
                                       onVisibilityChange = null
                                   }) {
    if (!dialogId) throw new Error("dialogId is required.");
    if (!importUrl) throw new Error("importUrl is required.");

    let dialog = null;
    let openButton = null;
    let selectedFile = null;
    let previewDialog = null;
    let importProgressDialog = null;

    function getDialogElements() {
        dialog = document.getElementById(dialogId);
        openButton = openButtonId ? document.getElementById(openButtonId) : null;

        if (!dialog) {
            throw new Error(`Import dialog '${dialogId}' was not found.`);
        }

        return {
            dialog,
            fileInput: dialog.querySelector("[data-import-file]"),
            fileInfo: dialog.querySelector("[data-import-file-info]"),
            importBtn: dialog.querySelector('[data-import-action="import"]'),
            cancelBtn: dialog.querySelector('[data-import-action="cancel"]')
        };
    }

    function ensurePreviewDialog() {
        if (previewDialog) {
            return previewDialog;
        }

        previewDialog = document.createElement("dialog");
        previewDialog.className = "import-preview-dialog";
        previewDialog.setAttribute("aria-label", "Import preview dialog");
        previewDialog.innerHTML = `
            <form method="dialog" class="import-preview-form">
                <div class="import-preview-header">
                    <h3 data-preview-title>Import preview</h3>
                    <div class="import-preview-summary" data-preview-summary></div>
                </div>

                <section class="import-preview-content">
                    <div class="import-preview-table-frame">
                        <div class="table-scroll">
                            <table class="import-preview-table">
                                <colgroup data-preview-cols></colgroup>
                                <thead data-preview-head></thead>
                                <tbody data-preview-body></tbody>
                            </table>
                        </div>
                    </div>
                </section>

                <div class="import-preview-footer">
                    <button type="button" class="import-primary-btn" data-preview-action="execute" disabled>Import entities</button>
                    <button type="button" class="import-secondary-btn" data-preview-action="close">Cancel</button>
                </div>
            </form>
        `;

        document.body.appendChild(previewDialog);

        previewDialog.addEventListener("cancel", (event) => {
            event.preventDefault();
            closeDialogElement(previewDialog);
        });

        previewDialog.addEventListener("click", (event) => {
            if (event.target === previewDialog) {
                closePreview();
            }
        });

        previewDialog.querySelector('[data-preview-action="close"]')?.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
            closePreview();
        });

        previewDialog.querySelector('[data-preview-action="execute"]')?.addEventListener("click", async () => {
            const executeButton = previewDialog.querySelector('[data-preview-action="execute"]');

            try {
                await executeImport();
            } catch (error) {
                console.error("Import failed:", error);
                window.alert(error?.message || "Import failed.");
                if (executeButton) {
                    executeButton.disabled = false;
                }
            }
        });

        return previewDialog;
    }

    function ensureImportProgressDialog() {
        if (importProgressDialog) {
            return importProgressDialog;
        }

        importProgressDialog = document.createElement("dialog");
        importProgressDialog.className = "import-progress-dialog";
        importProgressDialog.setAttribute("aria-label", "Import in progress");
        importProgressDialog.innerHTML = `
            <div class="import-progress-dialog-shell" aria-live="polite" aria-busy="true">
                <div class="import-preview-spinner" aria-hidden="true"></div>
                <div class="import-progress-dialog-text">Importing...</div>
            </div>
        `;

        document.body.appendChild(importProgressDialog);

        importProgressDialog.addEventListener("cancel", (event) => {
            event.preventDefault();
        });

        importProgressDialog.addEventListener("click", (event) => {
            if (event.target === importProgressDialog) {
                event.preventDefault();
            }
        });

        return importProgressDialog;
    }

    function setFileInfoClass(fileInfo, className) {
        if (!fileInfo) {
            return;
        }

        fileInfo.classList.remove("has-file");
        fileInfo.classList.remove("is-error");

        if (className) {
            fileInfo.classList.add(className);
        }
    }

    function updateState(els) {
        const valid = isAllowedFile(selectedFile);

        if (els.importBtn) {
            els.importBtn.disabled = !valid;
        }

        if (!els.fileInfo) {
            return;
        }

        if (!selectedFile) {
            els.fileInfo.textContent = "No file selected.";
            setFileInfoClass(els.fileInfo, "");
            return;
        }

        if (!valid) {
            els.fileInfo.textContent = "Only XML, XLSX and CSV files can be imported.";
            setFileInfoClass(els.fileInfo, "is-error");
            return;
        }

        els.fileInfo.textContent = `${selectedFile.name} (${formatFileSize(selectedFile.size, "da-DK", "0 B")})`;
        setFileInfoClass(els.fileInfo, "has-file");
    }

    function reset() {
        const els = getDialogElements();

        selectedFile = null;

        if (els.fileInput) {
            els.fileInput.value = "";
        }

        updateState(els);
    }

    function open() {
        const els = getDialogElements();

        reset();
        showDialog(els.dialog);

        setTimeout(() => els.fileInput?.focus?.(), 0);
    }

    function close() {
        closeDialogElement(dialog);
    }

    function closePreview() {
        if (!previewDialog) {
            return;
        }

        try {
            if (typeof previewDialog.close === "function") {
                previewDialog.close();
            }
        } catch {
            // Ignore and fall through to manual cleanup.
        }

        previewDialog.removeAttribute("open");
        previewDialog.hidden = true;
        previewDialog.style.display = "none";
    }

    function showImportProgress() {
        const progressDialog = ensureImportProgressDialog();
        progressDialog.hidden = false;
        progressDialog.style.display = "";
        showDialog(progressDialog);
    }

    function hideImportProgress() {
        if (!importProgressDialog) {
            return;
        }

        try {
            if (typeof importProgressDialog.close === "function") {
                importProgressDialog.close();
            }
        } catch {
            // Ignore and fall through to manual cleanup.
        }

        importProgressDialog.removeAttribute("open");
        importProgressDialog.hidden = true;
        importProgressDialog.style.display = "none";
    }

    function setVisible(visible) {
        const els = getDialogElements();

        if (openButton) {
            openButton.style.display = visible ? "" : "none";
        }

        updateState(els);

        if (typeof onVisibilityChange === "function") {
            onVisibilityChange(visible);
        }
    }

    async function requestPreview() {
        const els = getDialogElements();

        if (!isAllowedFile(selectedFile)) {
            updateState(els);
            return;
        }

        const formData = new FormData();
        formData.append("file", selectedFile, selectedFile.name);
        formData.append("phase", "preview");

        if (els.importBtn) {
            els.importBtn.disabled = true;
        }

        const response = await fetch(importUrl, {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            const text = await response.text().catch(() => "");
            throw new Error(text || `Import failed with HTTP ${response.status}`);
        }

        const data = await response.json().catch(() => null);
        if (!data) {
            throw new Error("Import preview response was invalid.");
        }

        close();
        showPreview(data);
    }

    async function executeImport() {
        if (!isAllowedFile(selectedFile)) {
            throw new Error("No valid file is selected.");
        }

        const formData = new FormData();
        formData.append("file", selectedFile, selectedFile.name);
        formData.append("phase", "execute");

        const preview = ensurePreviewDialog();
        const executeButton = preview.querySelector('[data-preview-action="execute"]');
        const closeButton = preview.querySelector('[data-preview-action="close"]');

        if (executeButton) {
            executeButton.disabled = true;
        }

        if (closeButton) {
            closeButton.disabled = true;
        }

        showImportProgress();

        try {
            const response = await fetch(importUrl, {
                method: "POST",
                body: formData
            });

            if (!response.ok) {
                const text = await response.text().catch(() => "");
                throw new Error(text || `Import failed with HTTP ${response.status}`);
            }

            closePreview();

            if (typeof onImportComplete === "function") {
                await onImportComplete();
            }
        } catch (error) {
            if (executeButton) {
                executeButton.disabled = false;
            }
            if (closeButton) {
                closeButton.disabled = false;
            }
            throw error;
        } finally {
            hideImportProgress();
        }
    }

    function showPreview(data) {
        const preview = ensurePreviewDialog();
        const titleElement = preview.querySelector("[data-preview-title]");
        const summaryElement = preview.querySelector("[data-preview-summary]");
        const colsElement = preview.querySelector("[data-preview-cols]");
        const headElement = preview.querySelector("[data-preview-head]");
        const bodyElement = preview.querySelector("[data-preview-body]");
        const executeButton = preview.querySelector('[data-preview-action="execute"]');

        if (titleElement) {
            titleElement.textContent = data?.title || "Import preview";
        }

        if (summaryElement) {
            const rowCount = Number(data?.rowCount || data?.rows?.length || 0);
            const errorCount = Number(data?.errorCount || 0);
            summaryElement.textContent = `${rowCount} rows loaded${errorCount > 0 ? `, ${errorCount} invalid` : ", all valid"}`;
        }

        buildPreviewTable(colsElement, headElement, bodyElement, data);

        if (executeButton) {
            executeButton.textContent = data?.executeButtonText || "Import entities";
            const rowCount = Number(data?.rowCount || data?.rows?.length || 0);
            executeButton.disabled = !Boolean(data?.allValid) || rowCount <= 0;
        }

        preview.hidden = false;
        preview.style.display = "";
        showDialog(preview);

        window.requestAnimationFrame(() => {
            const scrollContainer = preview.querySelector(".table-scroll");
            if (scrollContainer) {
                scrollContainer.scrollLeft = 0;
                scrollContainer.scrollTop = 0;
            }
        });
    }

    function buildPreviewTable(colsElement, headElement, bodyElement, data) {
        clearChildren(colsElement);
        clearChildren(headElement);
        clearChildren(bodyElement);

        const columns = Array.isArray(data?.columns) ? data.columns : [];
        const rows = Array.isArray(data?.rows) ? data.rows : [];
        const columnWidths = {
            id: "72px",
            level: "72px",
            status: "88px",
            name: "220px",
            description: "300px",
            error: "260px"
        };

        columns.forEach((column) => {
            const col = document.createElement("col");
            const key = String(column?.key || "").toLowerCase();
            col.style.width = columnWidths[key] || "auto";
            colsElement?.appendChild(col);
        });

        const statusCol = document.createElement("col");
        statusCol.style.width = columnWidths.status;
        colsElement?.appendChild(statusCol);

        const errorCol = document.createElement("col");
        errorCol.style.width = columnWidths.error;
        colsElement?.appendChild(errorCol);

        const headRow = document.createElement("tr");

        columns.forEach((column) => {
            const th = document.createElement("th");
            th.textContent = column?.label || humanizeFieldName(column?.key || "");
            headRow.appendChild(th);
        });

        const statusHeader = document.createElement("th");
        statusHeader.textContent = "Status";
        headRow.appendChild(statusHeader);

        const errorHeader = document.createElement("th");
        errorHeader.textContent = "Error";
        headRow.appendChild(errorHeader);

        headElement?.appendChild(headRow);

        rows.forEach((row) => {
            const tr = document.createElement("tr");
            tr.className = row?.valid ? "is-valid" : "is-invalid";

            const values = Array.isArray(row?.values) ? row.values : [];
            values.forEach((value) => {
                const td = document.createElement("td");
                const content = document.createElement("div");
                content.className = "preview-truncated-cell";
                content.textContent = value || "";
                td.appendChild(content);
                tr.appendChild(td);
            });

            const statusCell = document.createElement("td");
            statusCell.textContent = row?.valid ? "OK" : "Error";
            statusCell.className = row?.valid ? "is-valid" : "is-invalid";
            tr.appendChild(statusCell);

            const errorCell = document.createElement("td");
            const errorContent = document.createElement("div");
            errorContent.className = "preview-truncated-cell preview-error-cell";
            errorContent.textContent = row?.error || "";
            errorCell.appendChild(errorContent);
            tr.appendChild(errorCell);

            bodyElement?.appendChild(tr);
        });

    }

    function humanizeFieldName(value) {
        const normalized = String(value || "").trim();

        if (!normalized) {
            return "";
        }

        switch (normalized.toLowerCase()) {
            case "id":
                return "ID";
            case "level":
                return "Level";
            case "name":
                return "Name";
            case "description":
                return "Description";
            default:
                return normalized.charAt(0).toUpperCase() + normalized.slice(1);
        }
    }

    function bind() {
        const els = getDialogElements();
        ensurePreviewDialog();

        els.importBtn?.setAttribute("type", "button");
        updateState(els);

        openButton?.addEventListener("click", open);

        els.fileInput?.addEventListener("change", () => {
            selectedFile = els.fileInput.files?.[0] || null;
            updateState(els);
        });

        const importForm = els.dialog.querySelector("form");

        importForm?.addEventListener("submit", async (event) => {
            event.preventDefault();
            event.stopPropagation();
            try {
                await requestPreview();
            } catch (error) {
                console.error("Import failed:", error);
                window.alert(error?.message || "Import failed.");
                updateState(getDialogElements());
            }
        });

        els.importBtn?.addEventListener("click", async (event) => {
            event.preventDefault();
            event.stopPropagation();
            try {
                await requestPreview();
            } catch (error) {
                console.error("Import failed:", error);
                window.alert(error?.message || "Import failed.");
                updateState(getDialogElements());
            }
        });

        els.cancelBtn?.addEventListener("click", close);

        els.dialog.addEventListener("cancel", (event) => {
            event.preventDefault();
            close();
        });

        els.dialog.addEventListener("click", (event) => {
            if (event.target === els.dialog) {
                close();
            }
        });
    }

    return {
        bind,
        open,
        close,
        setVisible
    };
}
