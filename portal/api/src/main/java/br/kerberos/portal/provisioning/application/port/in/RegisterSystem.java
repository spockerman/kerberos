package br.kerberos.portal.provisioning.application.port.in;

import br.kerberos.portal.provisioning.domain.SystemSpec;

/** Cria (ou conserta, se já existir) um sistema: realm golden, client web e audience da API. Idempotente. */
public interface RegisterSystem {

    SystemView execute(SystemSpec spec);
}
