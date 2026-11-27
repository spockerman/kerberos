"use client";

import { useState, type FormEvent } from "react";
import type { PortalApi } from "@/lib/portal-api";
import { href, navigate } from "@/lib/router";

function toSlug(name: string) {
    return name
        .normalize("NFD")
        .replace(/[̀-ͯ]/g, "")
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 41);
}

/** Os "poucos campos": nome, endereço do front, roles extras e se é multi-tenant. */
export function NewSystem({ api }: { api: PortalApi }) {
    const [displayName, setDisplayName] = useState("");
    const [slug, setSlug] = useState("");
    const [slugTouched, setSlugTouched] = useState(false);
    const [webBaseUrl, setWebBaseUrl] = useState("");
    const [roles, setRoles] = useState("");
    const [multiTenant, setMultiTenant] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string>();

    async function submit(e: FormEvent) {
        e.preventDefault();
        setSaving(true);
        setError(undefined);
        try {
            const created = await api.registerSystem({
                slug,
                displayName,
                webBaseUrl,
                roles: roles.split(",").map(r => r.trim()).filter(Boolean),
                multiTenant
            });
            navigate({ name: "system", slug: created.slug });
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setSaving(false);
        }
    }

    return (
        <>
            <div className="page-head">
                <div>
                    <a href={href({ name: "systems" })} className="muted">
                        ← Sistemas
                    </a>
                    <h1>Novo sistema</h1>
                    <p>O portal cria o realm com o template da plataforma, o client do front-end (PKCE) e o audience da API.</p>
                </div>
            </div>

            <div className="split">
                <form className="card form" onSubmit={submit}>
                    <div className="field">
                        <label htmlFor="displayName">Nome do sistema</label>
                        <input
                            id="displayName"
                            required
                            value={displayName}
                            onChange={e => {
                                setDisplayName(e.target.value);
                                if (!slugTouched) setSlug(toSlug(e.target.value));
                            }}
                            placeholder="Financeiro"
                        />
                    </div>
                    <div className="field">
                        <label htmlFor="slug">Identificador</label>
                        <input
                            id="slug"
                            required
                            value={slug}
                            onChange={e => {
                                setSlugTouched(true);
                                setSlug(e.target.value);
                            }}
                            pattern="[a-z][a-z0-9-]{1,40}"
                            placeholder="financeiro"
                        />
                        <small>Vira o nome do realm e dos clients ({slug || "financeiro"}-web, {slug || "financeiro"}-api). Não muda depois.</small>
                    </div>
                    <div className="field">
                        <label htmlFor="webBaseUrl">Endereço do front-end</label>
                        <input
                            id="webBaseUrl"
                            required
                            type="url"
                            value={webBaseUrl}
                            onChange={e => setWebBaseUrl(e.target.value)}
                            placeholder="https://financeiro.empresa.com"
                        />
                        <small>Só a origem, sem caminho. https (http só para localhost).</small>
                    </div>
                    <div className="field">
                        <label htmlFor="roles">Roles do sistema (opcional)</label>
                        <input id="roles" value={roles} onChange={e => setRoles(e.target.value)} placeholder="auditor, tesoureiro" />
                        <small>admin, gestor e usuario já vêm no padrão. Separe por vírgula.</small>
                    </div>
                    <div className="field field--check">
                        <input id="multiTenant" type="checkbox" checked={multiTenant} onChange={e => setMultiTenant(e.target.checked)} />
                        <label htmlFor="multiTenant">Multi-tenant — atende vários clientes (organizações)</label>
                    </div>

                    {error ? <div className="alert alert--error">{error}</div> : null}

                    <div className="row">
                        <button className="btn" type="submit" disabled={saving} id="create-system">
                            {saving ? "Criando…" : "Criar sistema"}
                        </button>
                        <a className="btn btn--secondary" href={href({ name: "systems" })}>
                            Cancelar
                        </a>
                    </div>
                </form>

                <aside className="card">
                    <span className="label">O que o sistema recebe</span>
                    <ul className="muted" style={{ margin: 0, paddingLeft: 18, lineHeight: 1.9 }}>
                        <li>Login no layout da empresa (tema da plataforma)</li>
                        <li>Política de senha, proteção contra força bruta e tokens curtos</li>
                        <li>Roles admin, gestor e usuario</li>
                        <li>Auditoria de login e de administração</li>
                        <li>Organizations para separar clientes (tenants)</li>
                        <li>Um .env pronto para o projeto</li>
                    </ul>
                </aside>
            </div>
        </>
    );
}
