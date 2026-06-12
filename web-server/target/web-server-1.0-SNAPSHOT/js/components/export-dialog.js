import {
    closeDialogElement,
    showDialog
} from "../core/dom.js";
import { downloadBlob } from "../core/pdf.js";

function normalizeText(value) {
    return value == null ? "" : String(value);
}

function fileNameForFormat(baseFileName, format) {
    const normalized = normalizeText(baseFileName).trim() || "export";
    const ext = {
        xlsx: "xlsx",
        csv: "csv",
        pdf: "pdf",
        xml: "xml"
    }[format] || "xml";

    return `${normalized}.${ext}`;
}

export function createExportDialog({
                                       dialogId,
                                       openButtonId,
                                       exportUrl,
                                       baseFileName = "export",
                                       onVisibilityChange = null
                                   }) {
    if (!dialogId) throw new Error("dialogId is required.");
    if (!exportUrl) throw new Error("exportUrl is required.");

    let dialog = null;
    let openButton = null;

    function getDialogElements() {
        dialog = document.getElementById(dialogId);
        openButton = openButtonId ? document.getElementById(openButtonId) : null;

        if (!dialog) {
            throw new Error(`Export dialog '${dialogId}' was not found.`);
        }

        return {
            dialog,
            formatXlsx: dialog.querySelector('[data-export-format="xlsx"]'),
            formatCsv: dialog.querySelector('[data-export-format="csv"]'),
            formatPdf: dialog.querySelector('[data-export-format="pdf"]'),
            formatXml: dialog.querySelector('[data-export-format="xml"]'),
            includeInactive: dialog.querySelector('[data-export-include-inactive="true"]'),
            exportBtn: dialog.querySelector('[data-export-action="export"]'),
            cancelBtn: dialog.querySelector('[data-export-action="cancel"]')
        };
    }

    function getSelectedFormat(els) {
        if (els.formatXlsx?.checked) return "xlsx";
        if (els.formatCsv?.checked) return "csv";
        if (els.formatPdf?.checked) return "pdf";
        return "xml";
    }

    function open() {
        const els = getDialogElements();
        showDialog(els.dialog);
    }

    function close() {
        closeDialogElement(dialog);
    }

    function setVisible(visible) {
        if (openButton) {
            openButton.style.display = visible ? "" : "none";
        }

        if (typeof onVisibilityChange === "function") {
            onVisibilityChange(visible);
        }
    }

    async function exportNow() {
        const els = getDialogElements();
        const format = getSelectedFormat(els);
        const includeInaktive = els.includeInactive?.checked ? "true" : "false";

        const url =
            `${exportUrl}${exportUrl.includes("?") ? "&" : "?"}` +
            `cmd=export&format=${encodeURIComponent(format)}&includeInaktive=${encodeURIComponent(includeInaktive)}`;

        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Accept": "*/*"
            }
        });

        if (!response.ok) {
            const text = await response.text().catch(() => "");
            throw new Error(text || `Export failed with HTTP ${response.status}`);
        }

        const blob = await response.blob();

        downloadBlob(blob, fileNameForFormat(baseFileName, format));
        close();
    }

    function bind() {
        const els = getDialogElements();

        openButton?.addEventListener("click", open);

        els.exportBtn?.addEventListener("click", async () => {
            try {
                await exportNow();
            } catch (error) {
                console.error("Export failed:", error);
                window.alert(error?.message || "Export failed.");
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