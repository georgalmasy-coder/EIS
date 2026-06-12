(function () {
    "use strict";

    const DATA_URL = "/UserMananger";
    const EDIT_URL = "/editdetail";
    const SAVE_URL = "/savedetail";

    // Master data (all rows from the service)
    let allRows = [];

    // Sorting state
    let sortState = { key: null, dir: "asc" }; // dir: "asc" | "desc"

    // DOM references
    const elCustomerName = document.getElementById("customerName");
    const elProjectName = document.getElementById("projectName");
    const elUserName = document.getElementById("userName");
    const elLoadStatus = document.getElementById("loadStatus");

    const elActiveOnly = document.getElementById("activeOnly");
    const elTbody = document.getElementById("tbody");
    const elEmpty = document.getElementById("emptyState");

    // Dialog
    const dlg = document.getElementById("editDialog");
    const dlgStatus = document.getElementById("dlgStatus");
    const dlgId = document.getElementById("dlgId");
    const dlgDescription = document.getElementById("dlgDescription");
    const dlgDate = document.getElementById("dlgDate");
    const btnSave = document.getElementById("btnSave");
    const btnCancel = document.getElementById("btnCancel");

    let currentEditId = null;

    function textOf(parent, tagName) {
        const el = parent.getElementsByTagName(tagName)[0];
        return el ? (el.textContent || "").trim() : "";
    }

    function toBool(value) {
        return String(value || "").trim().toLowerCase() === "true";
    }

    function parseXml(xmlText) {
        const parser = new DOMParser();
        const doc = parser.parseFromString(xmlText, "application/xml");
        if (doc.getElementsByTagName("parsererror").length) {
            throw new Error("Invalid XML returned by service.");
        }
        return doc;
    }

    function setSortIndicators() {
        const keys = ["id", "wsn", "description", "modifiedDate", "modifiedBy", "active"];
        keys.forEach((k) => {
            const si = document.getElementById("si-" + k);
            if (!si) return;
            si.textContent = (sortState.key === k) ? (sortState.dir === "asc" ? "▲" : "▼") : "";
        });
    }

    function compareValues(a, b, key) {
        const av = a[key];
        const bv = b[key];

        if (typeof av === "boolean" || typeof bv === "boolean") {
            return (av ? 1 : 0) - (bv ? 1 : 0);
        }

        const as = (av ?? "").toString().trim();
        const bs = (bv ?? "").toString().trim();

        const an = Number(as);
        const bn = Number(bs);
        const aNum = Number.isFinite(an) && as !== "";
        const bNum = Number.isFinite(bn) && bs !== "";
        if (aNum && bNum) return an - bn;

        return as.localeCompare(bs, "en", { numeric: true, sensitivity: "base" });
    }

    function sortBy(key) {
        if (sortState.key === key) {
            sortState.dir = sortState.dir === "asc" ? "desc" : "asc";
        } else {
            sortState.key = key;
            sortState.dir = "asc";
        }

        allRows.sort((a, b) => {
            const c = compareValues(a, b, key);
            return sortState.dir === "asc" ? c : -c;
        });

        setSortIndicators();
        renderTable();
    }

    function getVisibleRows() {
        const activeOnly = !!elActiveOnly.checked;
        if (!activeOnly) return allRows;
        return allRows.filter(r => r.active === true);
    }

    function renderTable() {
        const visibleRows = getVisibleRows();
        elTbody.innerHTML = "";

        if (!visibleRows.length) {
            elEmpty.style.display = "block";
            elEmpty.textContent = allRows.length
                ? "No rows match the current filter."
                : "No rows returned from the web service.";
            return;
        }

        elEmpty.style.display = "none";

        visibleRows.forEach((r) => {
            const tr = document.createElement("tr");

            const tdId = document.createElement("td");
            tdId.textContent = r.id;
            tdId.className = "col-id";

            const tdWsn = document.createElement("td"); tdWsn.textContent = r.wsn;
            const tdDesc = document.createElement("td"); tdDesc.textContent = r.description;
            const tdModDate = document.createElement("td"); tdModDate.textContent = r.modifiedDate;
            const tdModBy = document.createElement("td"); tdModBy.textContent = r.modifiedBy;
            const tdActive = document.createElement("td"); tdActive.textContent = r.active ? "true" : "false";

            tr.append(tdId, tdWsn, tdDesc, tdModDate, tdModBy, tdActive);
            tr.addEventListener("click", () => openEditDialog(r.id));

            elTbody.appendChild(tr);
        });
    }

    async function openEditDialog(id) {
        currentEditId = id;
        dlgId.textContent = id;
        dlgDescription.value = "";
        dlgDate.value = "";

        dlgStatus.textContent = "Loading…";
        dlg.showModal();

        try {
            const url = EDIT_URL + "?id=" + encodeURIComponent(id);
            const res = await fetch(url, { headers: { "Accept": "application/xml, text/xml" } });
            if (!res.ok) throw new Error("HTTP " + res.status + " from /editdetail");
            const xml = await res.text();
            const doc = parseXml(xml);

            const detail = doc.getElementsByTagName("detail")[0] || doc;
            dlgDescription.value = textOf(detail, "Description");
            dlgDate.value = textOf(detail, "Date");
            dlgStatus.textContent = "Loaded.";
        } catch (e) {
            dlgStatus.textContent = "Failed to load details.";
            console.error(e);
        }
    }

    async function saveAndClose() {
        if (!currentEditId) {
            dlg.close();
            return;
        }

        dlgStatus.textContent = "Saving…";

        try {
            const payload =
                "<detail>" +
                "<ID>" + String(currentEditId) + "</ID>" +
                "<Description>" + escapeXml(dlgDescription.value) + "</Description>" +
                "<Date>" + escapeXml(dlgDate.value) + "</Date>" +
                "</detail>";

            const res = await fetch(SAVE_URL, {
                method: "POST",
                headers: {
                    "Content-Type": "application/xml; charset=UTF-8",
                    "Accept": "application/xml, text/xml"
                },
                body: payload
            });

            if (!res.ok) throw new Error("HTTP " + res.status + " from /savedetail");

            dlgStatus.textContent = "Saved.";
            dlg.close();

            // Refresh the list after save
            await loadUserManagerXml();
        } catch (e) {
            dlgStatus.textContent = "Save failed.";
            console.error(e);
        }
    }

    function escapeXml(s) {
        return String(s ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&apos;");
    }

    function applyUserManagerXml(doc) {


        const userManager = doc.getElementsByTagName("UserManager")[0] || doc;
        const topPanel = userManager.getElementsByTagName("TopPanel")[0];

        elCustomerName.textContent = (topPanel ? textOf(topPanel, "CustomerName") : "") || "—";
        elProjectName.textContent = (topPanel ? textOf(topPanel, "ProjectName") : "") || "—";
        elUserName.textContent = (topPanel ? textOf(topPanel, "UserName") : "") || "—";

        const rowNodes = Array.from(doc.getElementsByTagName("row"));
        allRows = rowNodes.map((node) => ({
            id: textOf(node, "ID"),
            wsn: textOf(node, "WSN"),
            description: textOf(node, "Description"),
            modifiedDate: textOf(node, "ModifiedDate"),
            modifiedBy: textOf(node, "ModifiedBy"),
            active: toBool(textOf(node, "Active"))
        }));

        // Keep checkbox as-is; reset sorting visuals
        sortState = { key: null, dir: "asc" };
        setSortIndicators();
        renderTable();
    }

    async function loadUserManagerXml() {
        elLoadStatus.textContent = "Loading…";
        elEmpty.style.display = "block";
        elEmpty.textContent = "Loading XML from web service…";

        try {
            const res = await fetch(DATA_URL, { headers: { "Accept": "application/xml, text/xml" } });
            if (!res.ok) throw new Error("HTTP " + res.status + " from " + DATA_URL);
            const xml = await res.text();
            const doc = parseXml(xml);

            applyUserManagerXml(doc);
            elLoadStatus.textContent = "Loaded";
        } catch (e) {
            elLoadStatus.textContent = "Error";
            allRows = [];
            renderTable();
            elEmpty.style.display = "block";
            elEmpty.textContent = "Failed to load XML from " + DATA_URL + ".";
            console.error(e);
        }
    }

    // Events
    elActiveOnly.addEventListener("change", renderTable);

    document.querySelectorAll("thead th[data-key]").forEach((th) => {
        th.addEventListener("click", () => sortBy(th.dataset.key));
    });

    btnCancel.addEventListener("click", () => dlg.close());
    btnSave.addEventListener("click", saveAndClose);

    // Init
    setSortIndicators();
    renderTable();
    loadUserManagerXml();
})();