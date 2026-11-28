package ifsuldeminas.edu.br.ifsystem;

import ifsuldeminas.edu.br.ifsystem.room.provider.*;
import ifsuldeminas.edu.br.ifsystem.room.service.*;
import ifsuldeminas.edu.br.ifsystem.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimetableServiceTest {
 @TempDir Path directory;
 private SpreadsheetRoomProvider provider;
 private TimetableService service;
 @BeforeEach void setup() throws Exception {
  Path csv=directory.resolve("classes.csv");
  Files.writeString(csv,"""
Turma,Dia da semana,Início,Fim,Sala / Local,Disciplina e docente (texto)
Administração,Segunda-feira,07:00,08:30,Sala 114,Matemática
Administração,Segunda-feira,07:00,08:30,Sala 114,Matemática
Outra turma,Segunda-feira,08:00,09:00,Sala 114,Física
Administração,Terça-feira,10:00,11:00,Sala 114,Inglês
Administração,Sexta-feira,07:00,08:00,Sala 114,Português
""");
  var loader=new DefaultResourceLoader();
  provider=new SpreadsheetRoomProvider(csv.toUri().toString(),"","Sala / Local",Duration.ZERO,loader);
  service=new TimetableService(provider,new AcademicCalendar(loader,"classpath:calendar/academic-2026.csv"),"America/Sao_Paulo");
 }
 @Test void readsWeeklyClassesAndDeduplicatesIdenticalRows() {
  assertEquals(4,provider.findWeeklyClasses().size());
  var monday=service.onDate("sala-114",LocalDate.of(2026,11,23));
  assertEquals(2,monday.size());assertEquals(Instant.parse("2026-11-23T10:00:00Z"),monday.getFirst().startTime());
  assertEquals("Administração",monday.getFirst().group());
 }
 @Test void holidaysAndRecessesDoNotRepeatRegularClasses() {
  assertTrue(service.onDate("sala-114",LocalDate.of(2026,11,2)).isEmpty());
  assertTrue(service.onDate("sala-114",LocalDate.of(2026,11,20)).isEmpty());
  assertTrue(service.onDate("sala-114",LocalDate.of(2026,10,13)).isEmpty());
  assertTrue(service.onDate("sala-114",LocalDate.of(2026,12,21)).isEmpty());
 }
 @Test void schoolSaturdaysUseTheReferencedWeekday() {
  assertEquals(2,service.onDate("sala-114",LocalDate.of(2026,8,8)).size());
  assertEquals(1,service.onDate("sala-114",LocalDate.of(2026,9,12)).size());
  assertEquals(1,service.onDate("sala-114",LocalDate.of(2026,11,7)).size());
  assertTrue(service.onDate("sala-114",LocalDate.of(2026,11,14)).isEmpty());
 }
 @Test void overlapIsRejectedButAdjacentIntervalsAreAllowed() {
  assertThrows(ResponseStatusException.class,()->service.requireNoClass("sala-114",Instant.parse("2026-11-23T10:15:00Z"),Instant.parse("2026-11-23T10:30:00Z")));
  assertDoesNotThrow(()->service.requireNoClass("sala-114",Instant.parse("2026-11-23T12:00:00Z"),Instant.parse("2026-11-23T12:30:00Z")));
  assertDoesNotThrow(()->service.requireNoClass("sala-114",Instant.parse("2026-11-23T12:00:00Z"),Instant.parse("2026-11-23T13:00:00Z")));
 }
 @Test void reservationSpanningSeveralDaysStillConflicts() {
  assertThrows(ResponseStatusException.class,()->service.requireNoClass("sala-114",Instant.parse("2026-11-22T22:00:00Z"),Instant.parse("2026-11-24T02:00:00Z")));
 }
 @Test void oldTimetableIsNotAppliedToTheNextYear() {
  assertThrows(ResponseStatusException.class,()->service.onDate("sala-114",LocalDate.of(2027,1,4)));
 }
 @Test void availabilityMergesOverlappingClassesAndReturnsFreeIntervalsAndDetails() {
  var repository=mock(ReservationRepository.class);
  var roomService=new RoomService(provider,repository,service,"America/Sao_Paulo");
  var result=roomService.availability("sala-114",LocalDate.of(2026,11,23));
  assertEquals(1,result.occupied().size());assertEquals("CLASS",result.occupied().getFirst().source());
  assertEquals(Instant.parse("2026-11-23T12:00:00Z"),result.occupied().getFirst().endTime());
  assertEquals(1,result.available().size());assertEquals(2,result.classes().size());
 }
 @Test void overnightReservationsAreRejectedAndOperatingBoundariesAllowed() {
  assertThrows(ResponseStatusException.class,()->service.requireNoClass("sala-114",Instant.parse("2026-11-24T09:59:00Z"),Instant.parse("2026-11-24T10:30:00Z")));
  assertThrows(ResponseStatusException.class,()->service.requireNoClass("sala-114",Instant.parse("2026-11-25T01:00:00Z"),Instant.parse("2026-11-25T02:01:00Z")));
  assertDoesNotThrow(()->service.requireNoClass("sala-114",Instant.parse("2026-11-24T10:00:00Z"),Instant.parse("2026-11-24T11:00:00Z")));
  assertDoesNotThrow(()->service.requireNoClass("sala-114",Instant.parse("2026-11-25T01:00:00Z"),Instant.parse("2026-11-25T02:00:00Z")));
 }
 @Test void malformedClassHoursMakeTheSourceUnavailable() throws Exception {
  Files.writeString(directory.resolve("classes.csv"),"Sala / Local,Dia da semana,Início,Fim\nSala 114,Segunda-feira,08:30,07:00\n");
  assertThrows(ResponseStatusException.class,provider::findWeeklyClasses);
 }
}
