package br.kerberos.demo.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração de identidade vinda do ambiente (prefixo kerberos.security). */
@ConfigurationProperties(prefix = "kerberos.security")
public record SecurityProperties(String issuer, String jwkSetUri, String audience, List<String> allowedOrigins) {}
