package ifsuldeminas.edu.br.ifsystem.room.service;

import ifsuldeminas.edu.br.ifsystem.reservation.domain.ReservationStatus;
import ifsuldeminas.edu.br.ifsystem.reservation.repository.ReservationRepository;
import ifsuldeminas.edu.br.ifsystem.room.domain.Room;
import ifsuldeminas.edu.br.ifsystem.room.provider.RoomDataProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
public class RoomService {
    private final RoomDataProvider rooms;
    private final ReservationRepository reservations;
    private final ZoneId zone;
    private final TimetableService timetable;

    public RoomService(
            RoomDataProvider rooms,
            ReservationRepository reservations,
            TimetableService timetable,
            @Value("${app.rooms.zone:America/Sao_Paulo}") String zone) {
        this.timetable = timetable;
        this.rooms = rooms;
        this.reservations = reservations;
        this.zone = ZoneId.of(zone);
    }

    public List<Room> findAll() {
        return rooms.findAll();
    }

    public Room find(String id) {
        return rooms.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    @Transactional(readOnly = true)
    public Availability availability(String id, LocalDate date) {
        find(id);
        Instant start = date.atTime(7, 0).atZone(zone).toInstant();
        Instant end = date.atTime(23, 0).atZone(zone).toInstant();
        List<TimeSlot> occupied =
                reservations.findOverlapping(id, ReservationStatus.APPROVED, start, end).stream()
                        .map(
                                r ->
                                        new TimeSlot(
                                                r.getStartTime().isBefore(start)
                                                        ? start
                                                        : r.getStartTime(),
                                                r.getEndTime().isAfter(end) ? end : r.getEndTime(), "RESERVATION"))
                        .sorted(Comparator.comparing(TimeSlot::startTime))
                        .toList();
        List<TimeSlot> all = new ArrayList<>(occupied);
        var lessons = timetable.onDate(id, date);
        for (var lesson : lessons) {
            if (lesson.startTime().isBefore(end) && lesson.endTime().isAfter(start))
                all.add(new TimeSlot(lesson.startTime().isBefore(start) ? start : lesson.startTime(),
                    lesson.endTime().isAfter(end) ? end : lesson.endTime(), "CLASS"));
        }
        all.sort(Comparator.comparing(TimeSlot::startTime));
        List<TimeSlot> merged = new ArrayList<>();
        for (var slot : all) {
            if (merged.isEmpty() || merged.getLast().endTime().isBefore(slot.startTime())) merged.add(slot);
            else {
                var previous = merged.removeLast();
                merged.add(new TimeSlot(previous.startTime(), previous.endTime().isAfter(slot.endTime()) ? previous.endTime() : slot.endTime(),
                    Objects.equals(previous.source(),slot.source()) ? previous.source() : "MIXED"));
            }
        }
        occupied = List.copyOf(merged);
        List<TimeSlot> free = new ArrayList<>();
        Instant cursor = start;
        for (TimeSlot slot : occupied) {
            if (cursor.isBefore(slot.startTime())) free.add(new TimeSlot(cursor, slot.startTime(), "AVAILABLE"));
            if (cursor.isBefore(slot.endTime())) cursor = slot.endTime();
        }
        if (cursor.isBefore(end)) free.add(new TimeSlot(cursor, end, "AVAILABLE"));
        return new Availability(id, date, zone.getId(), occupied, List.copyOf(free), lessons);
    }

    public record TimeSlot(Instant startTime, Instant endTime, String source) {}

    public record Availability(
            String roomId,
            LocalDate date,
            String zone,
            List<TimeSlot> occupied,
            List<TimeSlot> available,
            List<TimetableService.ClassInterval> classes) {}
}
