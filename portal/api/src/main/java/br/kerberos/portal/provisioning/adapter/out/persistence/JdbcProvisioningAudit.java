package br.kerberos.portal.provisioning.adapter.out.persistence;

import br.kerberos.portal.provisioning.application.port.out.ProvisioningAuditPort;
import br.kerberos.portal.provisioning.domain.AuditEntry;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

public class JdbcProvisioningAudit implements ProvisioningAuditPort {

    private final JdbcClient jdbc;

    public JdbcProvisioningAudit(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(AuditEntry e) {
        jdbc.sql("INSERT INTO audit_trail (at, actor, action, target, outcome, detail) VALUES (:at, :actor, :action, :target, :outcome, :detail)")
                .param("at", Timestamp.from(e.at()))
                .param("actor", e.actor())
                .param("action", e.action())
                .param("target", e.target())
                .param("outcome", e.outcome().name())
                .param("detail", e.detail() == null ? null : e.detail().substring(0, Math.min(2000, e.detail().length())))
                .update();
    }

    @Override
    public List<AuditEntry> latest(int limit) {
        return jdbc.sql("SELECT * FROM audit_trail ORDER BY at DESC, id DESC LIMIT :limit")
                .param("limit", limit)
                .query((rs, row) -> new AuditEntry(
                        rs.getTimestamp("at").toInstant(),
                        rs.getString("actor"),
                        rs.getString("action"),
                        rs.getString("target"),
                        AuditEntry.Outcome.valueOf(rs.getString("outcome")),
                        rs.getString("detail")))
                .list();
    }
}
