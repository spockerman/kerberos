package br.kerberos.portal.provisioning.application.port.out;

import br.kerberos.portal.provisioning.domain.Operator;

/** Quem está usando o portal agora. */
public interface CurrentOperatorPort {

    Operator current();
}
