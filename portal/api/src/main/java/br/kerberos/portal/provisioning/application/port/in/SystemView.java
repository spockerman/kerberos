package br.kerberos.portal.provisioning.application.port.in;

import java.time.Instant;
import java.util.List;

/** Sistema como o portal o mostra. */
public record SystemView(
        String slug,
        String displayName,
        String webBaseUrl,
        String webClientId,
        String apiAudience,
        List<String> roles,
        boolean multiTenant,
        Instant updatedAt,
        String updatedBy) {}
