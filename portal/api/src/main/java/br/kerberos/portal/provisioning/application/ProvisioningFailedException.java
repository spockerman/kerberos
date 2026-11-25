package br.kerberos.portal.provisioning.application;

/** O provedor de identidade recusou ou falhou ao aplicar a mudança. A operação pode ser repetida. */
public class ProvisioningFailedException extends RuntimeException {

    public ProvisioningFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
