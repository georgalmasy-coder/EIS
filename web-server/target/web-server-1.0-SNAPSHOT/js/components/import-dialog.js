import {
    closeDialogElement,
    showDialog
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

    async function importNow() {
        const els = getDialogElements();

        if (!isAllowedFile(selectedFile)) {
            updateState(els);
            return;
        }

        const formData = new FormData();
        formData.append("file", selectedFile, selectedFile.name);

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

        close();

        if (typeof onImportComplete === "function") {
            await onImportComplete();
        }
    }

    function bind() {
        const els = getDialogElements();

        updateState(els);

        openButton?.addEventListener("click", open);

        els.fileInput?.addEventListener("change", () => {
            selectedFile = els.fileInput.files?.[0] || null;
            updateState(els);
        });

        els.importBtn?.addEventListener("click", async () => {
            try {
                await importNow();
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