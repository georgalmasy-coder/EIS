import { hasXmlParseError } from "../core/xml.js";

function normalizeText(value) {
    return value == null ? "" : String(value).trim();
}

function getElements(dialogIds) {
    return {
        dialog: document.getElementById(dialogIds.dialogId),
        title: document.getElementById(dialogIds.titleId),
        prompt: document.getElementById(dialogIds.promptId),
        buttons: document.getElementById(dialogIds.buttonsId),
        cancelButton: document.getElementById(dialogIds.cancelButtonId)
    };
}

function setText(element, value) {
    if (element) {
        element.textContent = value;
    }
}

function setDisabled(element, disabled) {
    if (element) {
        element.disabled = disabled === true;
    }
}

function showDialog(dialog) {
    if (!dialog) {
        return;
    }

    if (typeof dialog.showModal === "function" && !dialog.open) {
        dialog.showModal();
        return;
    }

    dialog.setAttribute("open", "open");
}

function closeDialog(dialog) {
    if (!dialog) {
        return;
    }

    if (typeof dialog.close === "function" && dialog.open) {
        dialog.close();
        return;
    }

    dialog.removeAttribute("open");
}

function parseRelationCreationResponse(xmlText) {
    const doc = new DOMParser().parseFromString(xmlText, "application/xml");

    if (hasXmlParseError(doc)) {
        throw new Error("The relation creation endpoint returned invalid XML.");
    }

    const root = doc.documentElement;
    const status = normalizeText(root.getAttribute("status")) || "options";
    const message = normalizeText(root.querySelector("Message")?.textContent)
        || normalizeText(root.querySelector("ErrorMessage")?.textContent)
        || normalizeText(root.textContent);

    if (status === "error") {
        throw new Error(message || "The relation creation endpoint returned an error.");
    }

    const relationTypesNode = root.querySelector("RelationTypes") || root;
    const relationTypeNodes = Array.from(relationTypesNode.getElementsByTagName("RelationType"))
        .filter((node) => node.parentElement === relationTypesNode);

    return {
        status,
        message,
        relationTypes: relationTypeNodes.map((node) => {
        const name = normalizeText(node.textContent)
            || normalizeText(node.getAttribute("name"))
            || normalizeText(node.querySelector("Name")?.textContent)
            || normalizeText(node.getAttribute("id"));

        return {
            id: normalizeText(node.getAttribute("id")),
            name
        };
        }).filter((entry) => entry.name)
    };
}

function buildRequestBody(context, relationTypeName = "") {
    const params = new URLSearchParams();

    params.set("FromEntityId", String(context.fromEntityId));
    params.set("FromEntityType", String(context.fromEntityType));
    params.set("FromEntityCode", String(context.fromEntityCode || ""));
    params.set("FromEntityName", String(context.fromEntityName || ""));
    params.set("ToEntityId", String(context.toEntityId));
    params.set("ToEntityType", String(context.toEntityType));
    params.set("ToEntityCode", String(context.toEntityCode || ""));
    params.set("ToEntityName", String(context.toEntityName || ""));

    if (relationTypeName) {
        params.set("RelationTypeName", relationTypeName);
    }

    return params.toString();
}

function formatEntitySummary(entity) {
    const entityTypeLabel = normalizeText(entity.entityTypeLabel) || "Entity";
    const entityCode = normalizeText(entity.entityCode) || normalizeText(entity.visibleId) || normalizeText(entity.entityId) || "-";
    const name = normalizeText(entity.name) || "-";

    return `${entityTypeLabel}: ${entityCode} - ${name}`;
}

function setDialogStatus(elements, message) {
    setText(elements.status, message);
}

function renderRelationButtons(elements, relationTypes, onCreate) {
    const buttonsContainer = elements.buttons;

    if (!buttonsContainer) {
        return;
    }

    buttonsContainer.innerHTML = "";

    relationTypes.forEach((relationType) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "primary";
        button.textContent = `Create "${relationType.name}" relation`;
        button.dataset.relationTypeName = relationType.name;

        button.addEventListener("click", async () => {
            await onCreate(relationType);
        });

        buttonsContainer.appendChild(button);
    });
}

function setButtonState(elements, disabled) {
    const buttonsContainer = elements.buttons;

    if (!buttonsContainer) {
        return;
    }

    buttonsContainer.querySelectorAll("button").forEach((button) => {
        setDisabled(button, disabled);
    });
}

function clearPrompt(elements) {
    if (elements.prompt) {
        elements.prompt.innerHTML = "";
    }
}

function appendPromptLine(parent, label, value) {
    const line = document.createElement("div");
    line.className = "relationdiagram-dialog-line";

    const labelNode = document.createElement("span");
    labelNode.className = "relationdiagram-dialog-line-label";
    labelNode.textContent = label;

    const valueNode = document.createElement("span");
    valueNode.className = "relationdiagram-dialog-line-value";
    valueNode.textContent = value || "-";

    line.append(labelNode, document.createTextNode(" "), valueNode);
    parent.appendChild(line);
}

function appendPromptText(parent, value) {
    const line = document.createElement("div");
    line.className = "relationdiagram-dialog-line relationdiagram-dialog-line-intro";
    line.textContent = value;
    parent.appendChild(line);
}

function appendPromptSpacer(parent) {
    const spacer = document.createElement("div");
    spacer.className = "relationdiagram-dialog-line relationdiagram-dialog-line-spacer";
    spacer.innerHTML = "&nbsp;";
    parent.appendChild(spacer);
}

export function createRelationCreationDialogController(config = {}) {
    const dialogIds = {
        dialogId: config.dialogId || "relationCreateDialog",
        titleId: config.titleId || "relationCreateDialogTitle",
        promptId: config.promptId || "relationCreateDialogPrompt",
        buttonsId: config.buttonsId || "relationCreateDialogButtons",
        cancelButtonId: config.cancelButtonId || "relationCreateCancelButton"
    };

    const state = {
        activeContext: null,
        requestToken: 0,
        relationTypes: []
    };

    function getRequiredElements() {
        const elements = getElements(dialogIds);

        if (!elements.dialog || !elements.title || !elements.prompt || !elements.buttons || !elements.cancelButton) {
            throw new Error("Missing relation creation dialog elements.");
        }

        return elements;
    }

    function resetDialog(elements) {
        clearPrompt(elements);
        renderRelationButtons(elements, [], async () => {});
        state.activeContext = null;
        state.relationTypes = [];
    }

    async function close() {
        const elements = getRequiredElements();
        state.requestToken += 1;
        closeDialog(elements.dialog);
        resetDialog(elements);
    }

    async function createRelation(context, relationType) {
        const elements = getRequiredElements();

        clearPrompt(elements);
        setText(elements.prompt, `Creating "${relationType.name}" relation...`);
        setButtonState(elements, true);

        const response = await fetch(config.endpointUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "Accept": "application/xml,text/xml,*/*"
            },
            body: buildRequestBody(context, relationType.name),
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(await readResponseMessage(response) || `HTTP ${response.status} ${response.statusText}`);
        }

        await close();

        if (typeof config.onCreated === "function") {
            await config.onCreated({
                context,
                relationType
            });
        }
    }

    async function loadRelationTypes(context) {
        const response = await fetch(config.endpointUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "Accept": "application/xml,text/xml,*/*"
            },
            body: buildRequestBody(context),
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(await readResponseMessage(response) || `HTTP ${response.status} ${response.statusText}`);
        }

        const xmlText = await response.text();
        const parsedResponse = parseRelationCreationResponse(xmlText);
        return parsedResponse.relationTypes;
    }

    async function open(options = {}) {
        const context = {
            fromEntityId: normalizeText(options.fromEntityId),
            fromEntityType: normalizeText(options.fromEntityType),
            toEntityId: normalizeText(options.toEntityId),
            toEntityType: normalizeText(options.toEntityType),
            fromEntityCode: normalizeText(options.fromEntityCode),
            toEntityCode: normalizeText(options.toEntityCode),
            fromEntityLabel: normalizeText(options.fromEntityLabel),
            toEntityLabel: normalizeText(options.toEntityLabel),
            fromEntityName: normalizeText(options.fromEntityName),
            toEntityName: normalizeText(options.toEntityName)
        };

        if (!context.fromEntityId || !context.fromEntityType || !context.toEntityId || !context.toEntityType) {
            throw new Error("Missing relation endpoints.");
        }

        state.activeContext = context;
        state.requestToken += 1;
        const token = state.requestToken;

        const relationTypes = await loadRelationTypes(context);

        if (token !== state.requestToken) {
            return;
        }

        if (!relationTypes.length) {
            throw new Error("No valid relation types were returned for this pair.");
        }

        const elements = getRequiredElements();
        setText(elements.title, config.title || "Create relation");
        clearPrompt(elements);
        appendPromptText(elements.prompt, "Please confirm that you want to create a relation:");
        appendPromptSpacer(elements.prompt);
        appendPromptLine(elements.prompt, "From:", formatEntitySummary({
            entityTypeLabel: context.fromEntityLabel,
            entityId: context.fromEntityId,
            visibleId: context.fromEntityCode,
            entityCode: context.fromEntityCode,
            name: context.fromEntityName
        }));
        appendPromptSpacer(elements.prompt);
        appendPromptLine(elements.prompt, "To:", formatEntitySummary({
            entityTypeLabel: context.toEntityLabel,
            entityId: context.toEntityId,
            visibleId: context.toEntityCode,
            entityCode: context.toEntityCode,
            name: context.toEntityName
        }));
        state.relationTypes = relationTypes;
        renderRelationButtons(elements, relationTypes, async (relationType) => {
            try {
                await createRelation(context, relationType);
            } catch (error) {
                if (token !== state.requestToken) {
                    return;
                }

                clearPrompt(elements);
                appendPromptText(elements.prompt, error.message || "Failed to create the relation.");
                setButtonState(elements, false);
            }
        });
        setButtonState(elements, false);
        showDialog(elements.dialog);
    }

    const elements = getRequiredElements();
    elements.cancelButton.addEventListener("click", () => {
        close().catch(() => {});
    });
    elements.dialog.addEventListener("close", () => {
        state.requestToken += 1;
        resetDialog(elements);
    });

    return {
        open,
        close
    };
}

async function readResponseMessage(response) {
    const responseText = normalizeText(await response.text());

    if (!responseText) {
        return "";
    }

    try {
        const doc = new DOMParser().parseFromString(responseText, "application/xml");

        if (!hasXmlParseError(doc)) {
            const root = doc.documentElement;
            const status = normalizeText(root.getAttribute("status")) || "";
            const message = normalizeText(root.querySelector("Message")?.textContent)
                || normalizeText(root.querySelector("ErrorMessage")?.textContent)
                || normalizeText(root.textContent);

            if (status === "error" || root.tagName === "RelationCreateError") {
                return message;
            }
        }
    } catch {
        // Fall back to the raw text below.
    }

    return responseText;
}
