package br.kerberos.portal.provisioning.application.port.in;

import java.time.Instant;
import java.util.List;

public record TenantView(
        String alias, String name, List<String> domains, String loginMethod, String identityProviderRedirectUri, Instant updatedAt) {}
