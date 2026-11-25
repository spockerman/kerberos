"use client";

import { useCallback, useEffect, useState } from "react";

/** Carrega dados da API com estados de carregando/erro e permite recarregar. */
export function useLoad<T>(load: () => Promise<T>, deps: unknown[]) {
    const [data, setData] = useState<T>();
    const [error, setError] = useState<string>();
    const [loading, setLoading] = useState(true);

    const reload = useCallback(() => {
        setLoading(true);
        load()
            .then(d => {
                setData(d);
                setError(undefined);
            })
            .catch((e: Error) => setError(e.message))
            .finally(() => setLoading(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, deps);

    useEffect(reload, [reload]);
    return { data, error, loading, reload };
}
