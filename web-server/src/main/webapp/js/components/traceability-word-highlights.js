export function highlightMatchingWords(element, text, matchedWords = []) {
    if (!element) return;
    const normalize = word => word.toLowerCase().normalize("NFC");
    const matches = new Set(matchedWords.map(normalize));
    const fragment = document.createDocumentFragment();
    let position = 0;
    for (const match of text.matchAll(/[\p{L}\p{M}\p{N}]+/gu)) {
        fragment.append(document.createTextNode(text.slice(position, match.index)));
        if (matches.has(normalize(match[0]))) {
            const strong = document.createElement("strong");
            strong.textContent = match[0];
            fragment.append(strong);
        } else {
            fragment.append(document.createTextNode(match[0]));
        }
        position = match.index + match[0].length;
    }
    fragment.append(document.createTextNode(text.slice(position)));
    element.replaceChildren(fragment);
}
