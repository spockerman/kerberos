package br.kerberos.portal.provisioning.adapter.in.web;

import br.kerberos.portal.provisioning.application.ProvisioningFailedException;
import br.kerberos.portal.provisioning.application.SystemNotFoundException;
import br.kerberos.portal.provisioning.domain.InvalidSpecException;
import br.kerberos.portal.provisioning.domain.NotAllowedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros de domínio/aplicação → HTTP (RFC 9457). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidSpecException.class)
    public ProblemDetail invalid(InvalidSpecException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NotAllowedException.class)
    public ProblemDetail notAllowed(NotAllowedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(SystemNotFoundException.class)
    public ProblemDetail notFound(SystemNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ProvisioningFailedException.class)
    public ProblemDetail failed(ProvisioningFailedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }
}
