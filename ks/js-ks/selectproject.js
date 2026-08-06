(() => {
    "use strict";

    const CUSTOMERS_XML_URL = "/api/customersprojects";
    const PROJECT_SELECTED_URL = "/Menu";

    const form = document.getElementById("selectProjectForm");
    const customerSelect = document.getElementById("customerSelect");
    const projectSelect = document.getElementById("projectSelect");
    const button = document.getElementById("selectProjectBtn");
    const errorEl = document.getElementById("error");

    /** @type {{customerId: string, customerName: string, projects: Array<{projectId: string, projectName: string}>}[]} */
    let customers = [];

    function setError(message) {
        if (errorEl) {
            errorEl.textContent = message || "";
        }
    }

    function setBusy(isBusy) {
        if (form) {
            form.classList.toggle("is-busy", isBusy);
        }

        if (button) {
            button.disabled = isBusy || button.disabled;
        }

        if (!isBusy) {
            updateButtonState();
        }
    }

    function updateButtonState() {
        if (!button || !customerSelect || !projectSelect) {
            return;
        }

        const hasCustomer = !!customerSelect.value;
        const hasProject = !!projectSelect.value;

        button.disabled = !(hasCustomer && hasProject);
    }

    function parseXml(xmlText) {
        const parser = new DOMParser();
        const doc = parser.parseFromString(xmlText, "application/xml");

        if (doc.getElementsByTagName("parsererror").length) {
            throw new Error("The XML response could not be parsed.");
        }

        return doc;
    }

    function textOf(parent, tagName) {
        const el = parent?.getElementsByTagName(tagName)?.[0];

        return el ? (el.textContent || "").trim() : "";
    }

    function clearSelect(selectEl, placeholderText) {
        if (!selectEl) {
            return;
        }

        selectEl.innerHTML = "";

        const opt = document.createElement("option");
        opt.value = "";
        opt.textContent = placeholderText;
        opt.disabled = true;
        opt.selected = true;

        selectEl.appendChild(opt);
    }

    function readCustomersFromXml(doc) {
        const customerNodes = Array.from(doc.getElementsByTagName("customer"));

        return customerNodes
            .map((node) => {
                const customerId = textOf(node, "customerId");
                const customerName = textOf(node, "customerName");

                const projectsParent = node.getElementsByTagName("projects")[0];
                const projectNodes = projectsParent
                    ? Array.from(projectsParent.getElementsByTagName("project"))
                    : [];

                const projects = projectNodes
                    .map((projectNode) => ({
                        projectId: textOf(projectNode, "projectId"),
                        projectName: textOf(projectNode, "projectName")
                    }))
                    .filter((project) => project.projectId);

                return { customerId, customerName, projects };
            })
            .filter((customer) => customer.customerId);
    }

    function renderCustomers() {
        if (!customerSelect) {
            return;
        }

        customerSelect.innerHTML = "";

        customers.forEach((customer) => {
            const opt = document.createElement("option");
            opt.value = customer.customerId;
            opt.textContent = customer.customerName || ("Customer " + customer.customerId);
            customerSelect.appendChild(opt);
        });

        if (customers.length === 1) {
            customerSelect.value = customers[0].customerId;
            customerSelect.disabled = true;
        } else {
            const placeholder = document.createElement("option");
            placeholder.value = "";
            placeholder.textContent = "Select customer…";
            placeholder.disabled = true;
            placeholder.selected = true;

            customerSelect.insertBefore(placeholder, customerSelect.firstChild);
            customerSelect.disabled = false;
        }
    }

    function renderProjectsForSelectedCustomer() {
        clearSelect(projectSelect, "Select project…");

        if (!customerSelect || !projectSelect) {
            return;
        }

        const customerId = customerSelect.value;
        const customer = customers.find((item) => item.customerId === customerId);
        const projects = customer && Array.isArray(customer.projects) ? customer.projects : [];

        if (!projects.length) {
            projectSelect.disabled = true;
            updateButtonState();
            return;
        }

        projects.forEach((project) => {
            const opt = document.createElement("option");
            opt.value = project.projectId;
            opt.textContent = project.projectName || ("Project " + project.projectId);
            projectSelect.appendChild(opt);
        });

        projectSelect.disabled = false;
        updateButtonState();
    }

    async function loadCustomersAndProjects() {
        setError("");
        setBusy(true);

        clearSelect(customerSelect, "Loading…");
        clearSelect(projectSelect, "Select customer first…");

        if (customerSelect) {
            customerSelect.disabled = true;
        }

        if (projectSelect) {
            projectSelect.disabled = true;
        }

        try {
            const res = await fetch(CUSTOMERS_XML_URL, {
                method: "GET",
                headers: {
                    "Accept": "application/xml, text/xml"
                },
                credentials: "same-origin"
            });

            if (!res.ok) {
                throw new Error("HTTP " + res.status + " from " + CUSTOMERS_XML_URL);
            }

            const xmlText = await res.text();
            const doc = parseXml(xmlText);

            customers = readCustomersFromXml(doc);

            if (!customers.length) {
                clearSelect(customerSelect, "No customers available");
                clearSelect(projectSelect, "No projects available");
                setError("No customers were returned from the service.");
                return;
            }

            renderCustomers();

            if (customers.length === 1) {
                renderProjectsForSelectedCustomer();
            } else {
                clearSelect(projectSelect, "Select customer first…");

                if (projectSelect) {
                    projectSelect.disabled = true;
                }

                updateButtonState();
            }
        } catch (error) {
            customers = [];
            clearSelect(customerSelect, "Failed to load customers");
            clearSelect(projectSelect, "Failed to load projects");
            setError("Failed to load customers/projects.");
            console.error(error);
        } finally {
            if (customers.length > 0 && customers.length !== 1 && customerSelect) {
                customerSelect.disabled = false;
            }

            setBusy(false);
        }
    }

    async function postProjectSelected(customerId, projectId) {
        const body = new URLSearchParams();
        body.set("cmd", "selectproject");
        body.set("customerId", customerId);
        body.set("projectId", projectId);

        const res = await fetch(PROJECT_SELECTED_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
            },
            credentials: "same-origin",
            body
        });

        if (!res.ok) {
            throw new Error("HTTP " + res.status + " from " + PROJECT_SELECTED_URL);
        }
    }

    customerSelect?.addEventListener("change", () => {
        setError("");

        if (projectSelect) {
            projectSelect.value = "";
        }

        renderProjectsForSelectedCustomer();
    });

    projectSelect?.addEventListener("change", () => {
        setError("");
        updateButtonState();
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        setError("");

        const customerId = customerSelect?.value || "";
        const projectId = projectSelect?.value || "";

        if (!customerId || !projectId) {
            setError("Please select both a customer and a project.");
            updateButtonState();
            return;
        }

        if (button) {
            button.disabled = true;
        }

        try {
            await postProjectSelected(customerId, projectId);
            window.location.href = "/web/view?page=projectoverview";
        } catch (error) {
            setError("Failed to select project. Please try again.");
            console.error(error);
        } finally {
            updateButtonState();
        }
    });

    updateButtonState();
    loadCustomersAndProjects();
})();
