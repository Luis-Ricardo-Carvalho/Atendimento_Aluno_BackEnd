package ifsuldeminas.edu.br.ifsystem.atendimento.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Mesmo formato de erro dos demais módulos (ProblemDetail com "detail"); o handler existente não cobre este pacote. */
@RestControllerAdvice(basePackages = "ifsuldeminas.edu.br.ifsystem.atendimento")
public class AtendimentoExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handle(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ProblemDetail.forStatusAndDetail(
                ex.getStatusCode(), ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflito() {
        return ResponseEntity.status(409).body(
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflicting operation"));
    }
}
