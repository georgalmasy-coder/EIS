export function initHelpDialog() {
    const dialog = document.getElementById("helpDialog");
    const titleElement = document.getElementById("helpDialogTitle");
    const contentElement = document.getElementById("helpDialogContent");
    const closeButton = document.getElementById("helpDialogCloseButton");
    const okButton = document.getElementById("helpDialogOkButton");

    if (!dialog || !titleElement || !contentElement) {
        return;
    }

    document.querySelectorAll("[data-help-page]").forEach((button) => {
        button.addEventListener("click", async () => {
            const page = button.getAttribute("data-help-page");
            const title = button.getAttribute("data-help-title") || "Help";

            await openHelpDialog({
                dialog,
                titleElement,
                contentElement,
                page,
                title
            });
        });
    });

    closeButton?.addEventListener("click", () => {
        closeHelpDialog(dialog);
    });

    okButton?.addEventListener("click", () => {
        closeHelpDialog(dialog);
    });
}

async function openHelpDialog(options) {
    const {
        dialog,
        titleElement,
        contentElement,
        page,
        title
    } = options;

    titleElement.textContent = title;
    contentElement.textContent = "Loading help…";

    if (!dialog.open) {
        dialog.showModal();
    }

    try {
        const markdown = await fetchHelpMarkdown(page);
        contentElement.innerHTML = renderMarkdown(markdown);
    } catch (error) {
        console.error("Failed to load help", error);

        contentElement.innerHTML = `
            <div class="help-dialog-error">
                Help could not be loaded. Please try again later.
            </div>
        `;
    }
}

function closeHelpDialog(dialog) {
    if (dialog?.open) {
        dialog.close();
    }
}

async function fetchHelpMarkdown(page) {
    const normalizedPage = String(page || "").trim();

    if (!normalizedPage) {
        throw new Error("Missing help page.");
    }

    const response = await fetch(`/api/help?page=${encodeURIComponent(normalizedPage)}`, {
        method: "GET",
        headers: {
            "Accept": "text/markdown,text/plain,*/*"
        },
        cache: "no-store",
        credentials: "same-origin"
    });

    if (!response.ok) {
        throw new Error(`Help endpoint returned HTTP ${response.status}`);
    }

    return response.text();
}

function renderMarkdown(markdown) {
    const lines = String(markdown || "").replace(/\r\n/g, "\n").split("\n");
    const html = [];
    let paragraph = [];
    let listType = null;
    let codeBlock = false;
    let codeLines = [];
    let blockquote = [];

    function flushParagraph() {
        if (!paragraph.length) {
            return;
        }

        html.push(`<p>${renderInlineMarkdown(paragraph.join(" "))}</p>`);
        paragraph = [];
    }

    function flushList() {
        if (!listType) {
            return;
        }

        html.push(`</${listType}>`);
        listType = null;
    }

    function flushBlockquote() {
        if (!blockquote.length) {
            return;
        }

        html.push(`<blockquote>${blockquote.map(renderInlineMarkdown).join("<br>")}</blockquote>`);
        blockquote = [];
    }

    function flushCodeBlock() {
        if (!codeBlock) {
            return;
        }

        html.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
        codeBlock = false;
        codeLines = [];
    }

    for (const rawLine of lines) {
        const line = rawLine.trimEnd();

        if (line.trim().startsWith("```")) {
            if (codeBlock) {
                flushCodeBlock();
            } else {
                flushParagraph();
                flushList();
                flushBlockquote();
                codeBlock = true;
                codeLines = [];
            }

            continue;
        }

        if (codeBlock) {
            codeLines.push(rawLine);
            continue;
        }

        if (!line.trim()) {
            flushParagraph();
            flushList();
            flushBlockquote();
            continue;
        }

        const headingMatch = line.match(/^(#{1,4})\s+(.+)$/);

        if (headingMatch) {
            flushParagraph();
            flushList();
            flushBlockquote();

            const level = headingMatch[1].length;
            html.push(`<h${level}>${renderInlineMarkdown(headingMatch[2].trim())}</h${level}>`);
            continue;
        }

        const imageMatch = line.match(/^!\[([^\]]*)]\(([^)]+)\)$/);

        if (imageMatch) {
            flushParagraph();
            flushList();
            flushBlockquote();

            const alt = escapeHtml(imageMatch[1]);
            const src = escapeAttribute(imageMatch[2]);

            html.push(`<img src="${src}" alt="${alt}">`);
            continue;
        }

        const unorderedListMatch = line.match(/^\s*[-*]\s+(.+)$/);

        if (unorderedListMatch) {
            flushParagraph();
            flushBlockquote();

            if (listType !== "ul") {
                flushList();
                html.push("<ul>");
                listType = "ul";
            }

            html.push(`<li>${renderInlineMarkdown(unorderedListMatch[1])}</li>`);
            continue;
        }

        const orderedListMatch = line.match(/^\s*\d+\.\s+(.+)$/);

        if (orderedListMatch) {
            flushParagraph();
            flushBlockquote();

            if (listType !== "ol") {
                flushList();
                html.push("<ol>");
                listType = "ol";
            }

            html.push(`<li>${renderInlineMarkdown(orderedListMatch[1])}</li>`);
            continue;
        }

        const blockquoteMatch = line.match(/^>\s?(.+)$/);

        if (blockquoteMatch) {
            flushParagraph();
            flushList();
            blockquote.push(blockquoteMatch[1]);
            continue;
        }

        flushList();
        flushBlockquote();
        paragraph.push(line.trim());
    }

    flushCodeBlock();
    flushParagraph();
    flushList();
    flushBlockquote();

    return html.join("\n");
}

function renderInlineMarkdown(value) {
    return escapeHtml(value)
        .replace(/`([^`]+)`/g, "<code>$1</code>")
        .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
        .replace(/\*([^*]+)\*/g, "<em>$1</em>")
        .replace(/\[([^\]]+)]\(([^)]+)\)/g, (_match, text, href) => {
            return `<a href="${escapeAttribute(href)}" target="_blank" rel="noopener noreferrer">${text}</a>`;
        });
}

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

function escapeAttribute(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/"/g, "&quot;")
        .replace(/</g, "")
        .replace(/>/g, "");
}