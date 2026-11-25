package br.kerberos.portal.provisioning.domain;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Um sistema da empresa: um realm próprio (template golden), um client web (SPA com PKCE)
 * e o audience da sua API.
 */
public record SystemSpec(
        SystemSlug slug, String displayName, URI webBaseUrl, List<String> extraRoles, boolean multiTenant) {

    /** Roles que todo realm já recebe do template golden. */
    public static final List<String> STANDARD_ROLES = List.of("admin", "gestor", "usuario");

    public SystemSpec {
        Objects.requireNonNull(slug, "nome do sistema obrigatório");
        if (displayName == null || displayName.isBlank() || displayName.length() > 120) {
            throw new InvalidSpecException("Nome de exibição obrigatório (até 120 caracteres)");
        }
        displayName = displayName.strip();
        webBaseUrl = validWebUrl(webBaseUrl);
        extraRoles = extraRoles == null ? List.of() : extraRoles.stream().map(String::strip).filter(r -> !r.isEmpty()).distinct().toList();
        for (String role : extraRoles) {
            if (!role.matches("[a-z][a-z0-9-]{1,40}")) {
                throw new InvalidSpecException("Role inválida: " + role + " (use kebab-case)");
            }
            if (STANDARD_ROLES.contains(role)) {
                throw new InvalidSpecException("A role " + role + " já faz parte do padrão; não precisa informar");
            }
        }
    }

    private static URI validWebUrl(URI url) {
        if (url == null || url.getScheme() == null || url.getHost() == null) {
            throw new InvalidSpecException("URL do front-end obrigatória (ex.: https://financeiro.empresa.com)");
        }
        boolean local = Set.of("localhost", "127.0.0.1").contains(url.getHost());
        if (!"https".equals(url.getScheme()) && !(local && "http".equals(url.getScheme()))) {
            throw new InvalidSpecException("A URL do front-end precisa ser https (http só para localhost)");
        }
        if (url.getRawPath() != null && !url.getRawPath().isEmpty() && !"/".equals(url.getRawPath())
                || url.getQuery() != null || url.getFragment() != null) {
            throw new InvalidSpecException("Informe só a origem do front-end, sem caminho (ex.: https://app.empresa.com)");
        }
        return URI.create(url.getScheme() + "://" + url.getRawAuthority());
    }

    public String realmName() {
        return slug.value();
    }

    public String webClientId() {
        return slug.value() + "-web";
    }

    public String apiAudience() {
        return slug.value() + "-api";
    }

    public List<String> allRoles() {
        return java.util.stream.Stream.concat(STANDARD_ROLES.stream(), extraRoles.stream()).toList();
    }
}
