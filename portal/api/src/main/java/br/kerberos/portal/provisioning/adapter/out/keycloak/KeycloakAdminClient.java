package br.kerberos.portal.provisioning.adapter.out.keycloak;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Cliente mínimo da Admin REST API do Keycloak, autenticado como a conta de serviço do portal
 * (client credentials no realm master). Só conhece HTTP e JSON — a lógica de provisionamento fica no adapter.
 */
public class KeycloakAdminClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};

    private final RestClient http;
    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public KeycloakAdminClient(RestClient.Builder builder, String baseUrl, String clientId, String clientSecret) {
        this.http = builder.baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Optional<Map<String, Object>> get(String path, Object... vars) {
        try {
            return Optional.ofNullable(http.get().uri("/admin" + path, vars).headers(h -> h.setBearerAuth(token())).retrieve().body(MAP));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> list(String path, Object... vars) {
        List<Map<String, Object>> body = http.get().uri("/admin" + path, vars).headers(h -> h.setBearerAuth(token())).retrieve().body(LIST);
        return body == null ? List.of() : body;
    }

    public List<String> listStrings(String path, Object... vars) {
        List<String> body = http.get().uri("/admin" + path, vars).headers(h -> h.setBearerAuth(token()))
                .retrieve().body(new ParameterizedTypeReference<List<String>>() {});
        return body == null ? List.of() : body;
    }

    /** POST; devolve o id do recurso criado (último segmento do Location), se houver. */
    public Optional<String> post(String path, Object body, Object... vars) {
        URI location = http.post().uri("/admin" + path, vars)
                .headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getLocation();
        if (location == null) {
            return Optional.empty();
        }
        String path0 = location.getPath();
        return Optional.of(path0.substring(path0.lastIndexOf('/') + 1));
    }

    /** POST que tolera "já existe" (409): torna a operação idempotente. */
    public void postIgnoringConflict(String path, Object body, Object... vars) {
        try {
            post(path, body, vars);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.CONFLICT) {
                throw e;
            }
        }
    }

    public void put(String path, Object body, Object... vars) {
        http.put().uri("/admin" + path, vars)
                .headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /** DELETE com corpo (usado pela API de roles compostas). */
    public void deleteWithBody(String path, Object body, Object... vars) {
        http.method(org.springframework.http.HttpMethod.DELETE).uri("/admin" + path, vars)
                .headers(h -> h.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void delete(String path, Object... vars) {
        try {
            http.delete().uri("/admin" + path, vars).headers(h -> h.setBearerAuth(token())).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // já não existe
        }
    }

    private synchronized String token() {
        if (accessToken == null || Instant.now().isAfter(expiresAt.minusSeconds(30))) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            Map<String, Object> response = http.post()
                    .uri("/realms/master/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP);
            if (response == null || response.get("access_token") == null) {
                throw new IllegalStateException("Keycloak não devolveu token para a conta de serviço do portal");
            }
            accessToken = (String) response.get("access_token");
            long expiresIn = ((Number) response.getOrDefault("expires_in", 60)).longValue();
            expiresAt = Instant.now().plusSeconds(expiresIn);
        }
        return accessToken;
    }
}
