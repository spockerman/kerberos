package br.kerberos.portal.provisioning.adapter.in.web;

import br.kerberos.portal.provisioning.domain.InvalidSpecException;
import br.kerberos.portal.provisioning.domain.SystemSlug;
import br.kerberos.portal.provisioning.domain.SystemSpec;
import java.net.URI;
import java.util.List;

/** Os "poucos campos" do cadastro de sistema. */
public record SystemRequest(String slug, String displayName, String webBaseUrl, List<String> roles, Boolean multiTenant) {

    SystemSpec toSpec() {
        return new SystemSpec(new SystemSlug(slug), displayName, uri(webBaseUrl), roles, multiTenant == null || multiTenant);
    }

    static URI uri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return URI.create(value.strip());
        } catch (IllegalArgumentException e) {
            throw new InvalidSpecException("URL inválida: " + value);
        }
    }
}
