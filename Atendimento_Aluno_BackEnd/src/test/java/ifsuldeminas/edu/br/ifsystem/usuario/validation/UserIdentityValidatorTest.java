package ifsuldeminas.edu.br.ifsystem.usuario.validation;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class UserIdentityValidatorTest {
    @Test void acceptsValidCpfWithOrWithoutMask() {
        assertEquals("90000000175",UserIdentityValidator.cpf("900.000.001-75"));
        assertEquals("90000000175",UserIdentityValidator.cpf(" 90000000175 "));
    }
    @Test void rejectsInvalidCpfAndNonNumericContent() {
        for(String value:new String[]{"00000000000","11111111111","90000000174","9000000017","abc90000000175","900000001755","900 000 00175"})
            assertThrows(ResponseStatusException.class,()->UserIdentityValidator.cpf(value),value);
        assertThrows(ResponseStatusException.class,()->UserIdentityValidator.cpf(null));
    }
    @Test void enforcesExactInstitutionalDomainByRoleAndNormalizes() {
        assertEquals("aluno@alunos.ifsuldeminas.edu.br",UserIdentityValidator.email(" ALUNO@ALUNOS.IFSULDEMINAS.EDU.BR ",Perfil.ALUNO));
        for(Perfil role:new Perfil[]{Perfil.PROFESSOR,Perfil.SERVIDOR,Perfil.ADMINISTRADOR}) {
            assertEquals("nome@ifsuldeminas.edu.br",UserIdentityValidator.email("nome@ifsuldeminas.edu.br",role));
            assertThrows(ResponseStatusException.class,()->UserIdentityValidator.email("nome@alunos.ifsuldeminas.edu.br",role));
        }
        assertThrows(ResponseStatusException.class,()->UserIdentityValidator.email("nome@ifsuldeminas.edu.br",Perfil.ALUNO));
    }
    @Test void rejectsMalformedAndSpoofedEmails() {
        for(String value:new String[]{"@alunos.ifsuldeminas.edu.br","nome@@alunos.ifsuldeminas.edu.br","a..b@alunos.ifsuldeminas.edu.br","a.@alunos.ifsuldeminas.edu.br","nome@alunos.ifsuldeminas.edu.br.evil.com","nome com espaco@alunos.ifsuldeminas.edu.br","nome@gmail.com"})
            assertThrows(ResponseStatusException.class,()->UserIdentityValidator.email(value,Perfil.ALUNO),value);
    }
}
