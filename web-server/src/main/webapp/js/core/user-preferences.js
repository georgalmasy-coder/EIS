import { postForm } from "./http.js";

const PREFERENCES_URL = "/api/user-preferences";

const values = new Map();

let loaded = false;

function loadSync() {
    if (loaded) {
        return;
    }

    try {
        const request = new XMLHttpRequest();
        request.open("GET", PREFERENCES_URL, false);
        request.setRequestHeader("Accept", "application/json");
        request.send();

        if (request.status < 200 || request.status >= 300) {
            throw new Error(`HTTP ${request.status} from ${PREFERENCES_URL}`);
        }

        const payload = request.responseText ? JSON.parse(request.responseText) : null;
        Object.entries(payload?.settings ?? {}).forEach(([key, value]) => {
            values.set(key, String(value ?? ""));
        });
    } catch (error) {
        console.warn("Could not load user preferences.", error);
    }
    loaded = true;
}

async function persist(key, value, remove = false) {
    const body = new URLSearchParams();
    body.set("key", key);
    if (remove) {
        body.set("remove", "true");
    } else {
        body.set("value", value);
    }

    await postForm(PREFERENCES_URL, body, {
        headers: {
            "Accept": "application/json"
        }
    }).catch((error) => {
        console.warn("Could not save user preference.", error);
    });
}

loadSync();

export const userPreferences = {
    ready: Promise.resolve(),

    getItem(key) {
        return values.has(key) ? values.get(key) : null;
    },

    setItem(key, value) {
        const normalizedValue = String(value ?? "");
        values.set(key, normalizedValue);
        void persist(key, normalizedValue);
    },

    removeItem(key) {
        values.delete(key);
        void persist(key, "", true);
    }
};
