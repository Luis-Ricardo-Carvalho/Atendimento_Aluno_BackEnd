package ifsuldeminas.edu.br.ifsystem.room.service;

import ifsuldeminas.edu.br.ifsystem.room.provider.RoomDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class TimetableService {
    private final RoomDataProvider rooms;
    private final AcademicCalendar calendar;
    private final ZoneId zone;
    public record ClassInterval(Instant startTime, Instant endTime, String group, String description) {}
    public TimetableService(RoomDataProvider rooms, AcademicCalendar calendar,
                            @Value("${app.rooms.zone:America/Sao_Paulo}") String zone) {
        this.rooms=rooms; this.calendar=calendar; this.zone=ZoneId.of(zone);
    }
    public List<ClassInterval> onDate(String roomId, LocalDate date) {
        var classes=rooms.findWeeklyClasses();
        if (classes.isEmpty()) return List.of();
        DayOfWeek day=calendar.teachingDay(date);
        if (day==null) return List.of();
        return classes.stream().filter(c -> c.roomId().equals(roomId) && c.day()==day)
            .map(c -> new ClassInterval(date.atTime(c.start()).atZone(zone).toInstant(),
                 date.atTime(c.end()).atZone(zone).toInstant(), c.group(), c.description()))
            .distinct().sorted(Comparator.comparing(ClassInterval::startTime)).toList();
    }
    public void requireNoClass(String roomId, Instant start, Instant end) {
        var localStart = start.atZone(zone);
        var localEnd = end.atZone(zone);
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())
                || localStart.toLocalTime().isBefore(LocalTime.of(7, 0))
                || localEnd.toLocalTime().isAfter(LocalTime.of(23, 0)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Reservas devem ocorrer entre 07:00 e 23:00 do mesmo dia");
        if (rooms.findWeeklyClasses().isEmpty()) return;
        LocalDate first=start.atZone(zone).toLocalDate(), last=end.minusNanos(1).atZone(zone).toLocalDate();
        calendar.requireCoverage(first); calendar.requireCoverage(last);
        // A bounded reservation interval avoids unbounded enumeration of calendar dates.
        if (java.time.temporal.ChronoUnit.DAYS.between(first,last)>366)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A reserva deve ter no maximo 366 dias");
        for(LocalDate day=first; !day.isAfter(last); day=day.plusDays(1))
            if(onDate(roomId,day).stream().anyMatch(c -> c.startTime().isBefore(end) && c.endTime().isAfter(start)))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Sala indisponivel: existe aula nesse horario");
    }
}
