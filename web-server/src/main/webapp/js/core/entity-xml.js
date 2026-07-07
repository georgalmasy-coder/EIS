import {
    appendTextElement,
    getDirectChild,
    textOf
} from "./xml.js";

export function parseCreatedBy(node) {
    const createdByNode = node?.getElementsByTagName("CreatedById")?.[0] || null;
    const createdById = createdByNode?.getElementsByTagName("Value")?.[0]?.textContent?.trim() || "";
    const selectedOption = Array.from(createdByNode?.getElementsByTagName("Option") || [])
        .find((option) => (option.getAttribute("selected") || "").toLowerCase() === "true");
    const createdByText = selectedOption?.textContent?.trim() || "";

    return { createdById, createdByText };
}

export function parseEntityNotesFromDoc(doc) {
    const root = doc?.documentElement || doc;
    const notesRoot = root.getElementsByTagName("EntityNotes")?.[0];

    return Array.from(notesRoot?.getElementsByTagName("EntityNote") || []).map((node) => {
        const createdBy = parseCreatedBy(node);

        return {
            entityNotePK: textOf(node, "EntityNotePK").trim(),
            noteText: textOf(node, "NoteText"),
            createdById: createdBy.createdById,
            createdByText: createdBy.createdByText,
            createdTime: textOf(node, "CreatedTime").trim(),
            isNew: false
        };
    });
}

export function parseEntityAttachmentsFromDoc(doc) {
    const root = doc?.documentElement || doc;
    const attachmentsRoot = root.getElementsByTagName("EntityAttachments")?.[0];

    return Array.from(attachmentsRoot?.getElementsByTagName("EntityAttachment") || []).map((node) => {
        const createdBy = parseCreatedBy(node);

        return {
            entityAttachmentPK: textOf(node, "EntityAttachmentPK").trim(),
            fileName: textOf(node, "FileName"),
            contentType: textOf(node, "ContentType"),
            fileSize: textOf(node, "FileSize").trim(),
            description: textOf(node, "Description"),
            isDeleted: textOf(node, "IsDeleted").trim() === "true",
            fileData: textOf(node, "FileData"),
            createdById: createdBy.createdById,
            createdByText: createdBy.createdByText,
            createdTime: textOf(node, "CreatedTime").trim(),
            isNew: false
        };
    });
}

export function parseEntityRelationsFromDoc(doc) {
    const root = doc?.documentElement || doc;
    const relationsRoot = root.getElementsByTagName("EntityRelations")?.[0];

    return Array.from(relationsRoot?.getElementsByTagName("EntityRelation") || []).map((node) => {
        const createdBy = parseCreatedBy(node);

        return {
            entityRelationPK: textOf(node, "EntityRelationPK").trim(),
            entityId: textOf(node, "EntityId").trim(),
            entityType: textOf(node, "EntityType").trim(),
            relatedEntityId: textOf(node, "RelatedEntityId").trim(),
            relatedEntityType: textOf(node, "RelatedEntityType").trim(),
            createdById: createdBy.createdById,
            createdByText: createdBy.createdByText,
            createdTime: textOf(node, "CreatedTime").trim(),
            relatedEntityTypeName: textOf(node, "RelatedEntityTypeName"),
            relatedEntityCode: textOf(node, "RelatedEntityCode"),
            relatedEntityName: textOf(node, "RelatedEntityName"),
            relationTypeName: textOf(node, "RelationTypeName"),
            isDeleted: false,
            isNew: false
        };
    });
}

export function parseEntityLinksFromDoc(doc) {
    const root = doc?.documentElement || doc;
    const linksRoot = root.getElementsByTagName("EntityLinks")?.[0];

    return Array.from(linksRoot?.getElementsByTagName("EntityLink") || []).map((node) => {
        const createdBy = parseCreatedBy(node);

        return {
            entityLinkPK: textOf(node, "EntityLinkPK").trim(),
            description: textOf(node, "Description"),
            linkUrl: textOf(node, "LinkUrl"),
            createdById: createdBy.createdById,
            createdByText: createdBy.createdByText,
            createdTime: textOf(node, "CreatedTime").trim(),
            isNew: false
        };
    });
}

export function buildCreatedByXml(doc, item, selected = true) {
    const createdByNode = doc.createElement("CreatedById");

    const valueNode = doc.createElement("Value");
    valueNode.textContent = item.createdById ?? "";
    createdByNode.appendChild(valueNode);

    const optionNode = doc.createElement("Option");
    optionNode.setAttribute("value", item.createdById ?? "");

    if (selected && ((item.createdById ?? "") !== "" || (item.createdByText ?? "") !== "")) {
        optionNode.setAttribute("selected", "true");
    }

    optionNode.textContent = item.createdByText ?? "";
    createdByNode.appendChild(optionNode);

    return createdByNode;
}

export function buildEntityNotesXml(doc, notes) {
    const root = doc.documentElement;
    let entityNotesNode = getDirectChild(root, "EntityNotes");

    if (!entityNotesNode) {
        entityNotesNode = doc.createElement("EntityNotes");
        root.appendChild(entityNotesNode);
    }

    while (entityNotesNode.firstChild) {
        entityNotesNode.removeChild(entityNotesNode.firstChild);
    }

    notes.forEach((note) => {
        const entityNote = doc.createElement("EntityNote");

        appendTextElement(doc, entityNote, "EntityNotePK", note.entityNotePK ?? "");
        appendTextElement(doc, entityNote, "NoteText", note.noteText ?? "");
        entityNote.appendChild(buildCreatedByXml(doc, note, true));
        appendTextElement(doc, entityNote, "CreatedTime", note.createdTime ?? "");

        entityNotesNode.appendChild(entityNote);
    });

    return entityNotesNode;
}

export function buildEntityAttachmentsXml(doc, attachments) {
    const root = doc.documentElement;
    let attachmentsNode = getDirectChild(root, "EntityAttachments");

    if (!attachmentsNode) {
        attachmentsNode = doc.createElement("EntityAttachments");
        root.appendChild(attachmentsNode);
    }

    while (attachmentsNode.firstChild) {
        attachmentsNode.removeChild(attachmentsNode.firstChild);
    }

    attachments.forEach((attachment) => {
        const entityAttachment = doc.createElement("EntityAttachment");

        appendTextElement(doc, entityAttachment, "EntityAttachmentPK", attachment.entityAttachmentPK ?? "");
        appendTextElement(doc, entityAttachment, "FileName", attachment.fileName ?? "");
        appendTextElement(doc, entityAttachment, "ContentType", attachment.contentType ?? "");
        appendTextElement(doc, entityAttachment, "FileSize", attachment.fileSize ?? "0");
        appendTextElement(doc, entityAttachment, "Description", attachment.description ?? "");
        appendTextElement(doc, entityAttachment, "IsDeleted", attachment.isDeleted ? "true" : "false");
        appendTextElement(doc, entityAttachment, "FileData", attachment.fileData ?? "");

        const createdById = doc.createElement("CreatedById");
        appendTextElement(doc, createdById, "Value", attachment.createdById ?? "");
        entityAttachment.appendChild(createdById);

        appendTextElement(doc, entityAttachment, "CreatedTime", attachment.createdTime ?? "");

        attachmentsNode.appendChild(entityAttachment);
    });

    return attachmentsNode;
}

export function buildEntityRelationsXml(doc, relations) {
    const root = doc.documentElement;
    let relationsNode = getDirectChild(root, "EntityRelations");

    if (!relationsNode) {
        relationsNode = doc.createElement("EntityRelations");
        root.appendChild(relationsNode);
    }

    while (relationsNode.firstChild) {
        relationsNode.removeChild(relationsNode.firstChild);
    }

    relations.forEach((relation) => {
        const entityRelation = doc.createElement("EntityRelation");

        appendTextElement(doc, entityRelation, "EntityRelationPK", relation.entityRelationPK ?? "");
        appendTextElement(doc, entityRelation, "EntityId", relation.entityId ?? "");
        appendTextElement(doc, entityRelation, "EntityType", relation.entityType ?? "");
        appendTextElement(doc, entityRelation, "RelatedEntityId", relation.relatedEntityId ?? "");
        appendTextElement(doc, entityRelation, "RelatedEntityType", relation.relatedEntityType ?? "");
        appendTextElement(doc, entityRelation, "CreatedById", relation.createdById ?? "");
        appendTextElement(doc, entityRelation, "CreatedTime", relation.createdTime ?? "");
        appendTextElement(doc, entityRelation, "RelationTypeName", relation.relationTypeName ?? "");
        appendTextElement(doc, entityRelation, "IsDeleted", relation.isDeleted ? "true" : "false");

        relationsNode.appendChild(entityRelation);
    });

    return relationsNode;
}

export function buildEntityLinksXml(doc, links) {
    const root = doc.documentElement;
    let linksNode = getDirectChild(root, "EntityLinks");

    if (!linksNode) {
        linksNode = doc.createElement("EntityLinks");
        root.appendChild(linksNode);
    }

    while (linksNode.firstChild) {
        linksNode.removeChild(linksNode.firstChild);
    }

    links.forEach((link) => {
        const entityLink = doc.createElement("EntityLink");

        appendTextElement(doc, entityLink, "EntityLinkPK", link.entityLinkPK ?? "");
        appendTextElement(doc, entityLink, "Description", link.description ?? "");
        appendTextElement(doc, entityLink, "LinkUrl", link.linkUrl ?? "");
        entityLink.appendChild(buildCreatedByXml(doc, link, true));
        appendTextElement(doc, entityLink, "CreatedTime", link.createdTime ?? "");

        linksNode.appendChild(entityLink);
    });

    return linksNode;
}
