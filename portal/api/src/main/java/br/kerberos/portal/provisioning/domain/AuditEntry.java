package br.kerberos.portal.provisioning.domain;

import java.time.Instant;

/** Registro de auditoria: quem fez o quê, quando e com qual resultado. Nunca contém segredos. */
public record AuditEntry(Instant at, String actor, String action, String target, Outcome outcome, String detail) {

    public enum Outcome { SUCCESS, FAILURE }

    public static AuditEntry success(Instant at, String actor, String action, String target, String detail) {
        return new AuditEntry(at, actor, action, target, Outcome.SUCCESS, detail);
    }

    public static AuditEntry failure(Instant at, String actor, String action, String target, String detail) {
        return new AuditEntry(at, actor, action, target, Outcome.FAILURE, detail);
    }
}
