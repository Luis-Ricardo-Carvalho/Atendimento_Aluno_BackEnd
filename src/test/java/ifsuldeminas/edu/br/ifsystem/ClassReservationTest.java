package ifsuldeminas.edu.br.ifsystem;
import ifsuldeminas.edu.br.ifsystem.reservation.domain.*;
import ifsuldeminas.edu.br.ifsystem.reservation.api.ReservationDtos.*;
import ifsuldeminas.edu.br.ifsystem.reservation.repository.ReservationRepository;
import ifsuldeminas.edu.br.ifsystem.reservation.service.ReservationService;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.*;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.rooms.location=classpath:classes-test.csv","spring.datasource.url=jdbc:h2:mem:class-reservations;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("demo")
class ClassReservationTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder decoder;
 @Autowired UsuarioRepository users;
 @Autowired ReservationRepository reservations;
 @Autowired ReservationService service;
 Usuario operator;
 @BeforeEach void setup(){
  reservations.deleteAll();users.deleteAll();
  operator=users.save(new Usuario("Test operator","90000000175","operator@ifsuldeminas.edu.br","operator","not-used",null,null,null,Perfil.SERVIDOR));
  SecurityContextHolder.getContext().setAuthentication(SsoTestTokens.authentication(operator));
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void cannotRequestAnIntervalOccupiedByAClass(){
  var error=assertThrows(ResponseStatusException.class,()->service.create(new CreateReservation("sala-114",Instant.parse("2026-11-23T10:15:00Z"),Instant.parse("2026-11-23T11:00:00Z"))));
  assertEquals(409,error.getStatusCode().value());assertTrue(error.getReason().contains("aula"));
  assertEquals(0,reservations.count());
 }
 @Test void cannotApproveOldPendingRequestNowConflictingWithTheTimetable(){
  var previous=reservations.saveAndFlush(new Reservation("sala-114",operator,Instant.parse("2026-11-23T10:15:00Z"),Instant.parse("2026-11-23T11:00:00Z")));
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.approve(previous.getId())).getStatusCode().value());
  assertEquals(ReservationStatus.PENDING,reservations.findById(previous.getId()).orElseThrow().getStatus());
 }
 @Test void requestAndApprovalImmediatelyAfterClassAreAllowed(){
  var request=service.create(new CreateReservation("sala-114",Instant.parse("2026-11-23T11:30:00Z"),Instant.parse("2026-11-23T12:00:00Z")));
  assertEquals(ReservationStatus.APPROVED,service.approve(request.id()).status());
 }
}
