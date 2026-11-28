package ifsuldeminas.edu.br.ifsystem.usuario.validation;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

public final class UserIdentityValidator {
    private UserIdentityValidator() {}
    public static String cpf(String value) {
        if (value == null || !value.strip().matches("(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})"))
            throw invalid("CPF invalido: informe 11 digitos ou a mascara 000.000.000-00");
        String digits = value.strip().replace(".", "").replace("-", "");
        if (digits.chars().distinct().count() == 1) throw invalid("CPF invalido");
        for (int position = 9; position < 11; position++) {
            int sum = 0;
            for (int i = 0; i < position; i++) sum += (digits.charAt(i) - '0') * (position + 1 - i);
            int digit = 11 - sum % 11;
            if (digit >= 10) digit = 0;
            if (digits.charAt(position) - '0' != digit) throw invalid("CPF invalido: digitos verificadores incorretos");
        }
        return digits;
    }
    public static String email(String value, Perfil profile) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        String domain = profile == Perfil.ALUNO ? "alunos.ifsuldeminas.edu.br" : "ifsuldeminas.edu.br";
        String[] parts = normalized.split("@", -1);
        if (parts.length != 2 || !parts[1].equals(domain) || parts[0].length() > 64
                || normalized.length() > 254 || !parts[0].matches("[a-z0-9][a-z0-9._%+\\-]*")
                || parts[0].endsWith(".") || parts[0].contains(".."))
            throw invalid("Email institucional invalido: utilize @" + domain);
        return normalized;
    }
    private static ResponseStatusException invalid(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
