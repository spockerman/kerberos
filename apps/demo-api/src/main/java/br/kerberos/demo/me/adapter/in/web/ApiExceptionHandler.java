package br.kerberos.demo.me.adapter.in.web;

import br.kerberos.demo.me.application.NotAuthenticatedException;
import br.kerberos.demo.me.domain.TenantAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz erros de domínio/aplicação para respostas HTTP (RFC 9457 Problem Details). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ProblemDetail tenantAccessDenied(TenantAccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(NotAuthenticatedException.class)
    public ProblemDetail notAuthenticated(NotAuthenticatedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
