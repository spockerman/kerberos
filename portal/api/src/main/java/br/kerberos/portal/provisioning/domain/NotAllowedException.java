package br.kerberos.portal.provisioning.domain;

/** O operador não tem permissão para a ação. */
public class NotAllowedException extends RuntimeException {

    public NotAllowedException(String message) {
        super(message);
    }
}
