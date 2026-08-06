export function createPageReloader(elements, {
    beforeLoad,
    load,
    afterLoad,
    onError
}) {
    return async function reload() {
        try {
            if (typeof beforeLoad === "function") {
                await beforeLoad(elements);
            }

            const loadResult = typeof load === "function"
                ? await load(elements)
                : undefined;

            if (typeof afterLoad === "function") {
                await afterLoad(elements, loadResult);
            }

            return loadResult;
        } catch (error) {
            if (typeof onError === "function") {
                await onError(error, elements);
                return undefined;
            }

            throw error;
        }
    };
}

export async function bootstrapPage({
                                        getElements,
                                        initialize,
                                        initializeMenu = false,
                                        menuInitializer,
                                        beforeLoad,
                                        load,
                                        afterLoad,
                                        onError,
                                        finalize
                                    }) {
    if (typeof getElements !== "function") {
        throw new Error("bootstrapPage: getElements must be a function.");
    }

    const elements = getElements();
    let caughtError = null;

    try {
        if (typeof initialize === "function") {
            await initialize(elements);
        }

        if (initializeMenu) {
            if (typeof menuInitializer !== "function") {
                throw new Error("bootstrapPage: menuInitializer must be provided when initializeMenu is true.");
            }

            await menuInitializer(elements);
        }

        const reload = createPageReloader(elements, {
            beforeLoad,
            load,
            afterLoad,
            onError
        });

        await reload();

        return { elements, reload };
    } catch (error) {
        caughtError = error;
        throw error;
    } finally {
        if (typeof finalize === "function") {
            await finalize(elements, caughtError);
        }
    }
}