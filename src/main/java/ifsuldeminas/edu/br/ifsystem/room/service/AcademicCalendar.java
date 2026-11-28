package ifsuldeminas.edu.br.ifsystem.room.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.apache.commons.csv.CSVFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcademicCalendar {
    private final List<Rule> rules;
    private record Rule(LocalDate start, LocalDate end, String type, DayOfWeek weekday) {
        boolean contains(LocalDate date) { return !date.isBefore(start) && !date.isAfter(end); }
    }
    public AcademicCalendar(ResourceLoader loader,
            @Value("${app.rooms.calendar-location:classpath:calendar/academic-2026.csv}") String location) {
        try (var reader = new InputStreamReader(loader.getResource(location).getInputStream(), StandardCharsets.UTF_8);
             var csv = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
            List<Rule> parsed = new ArrayList<>();
            for (var row : csv) {
                var start = LocalDate.parse(row.get("start")); var end = LocalDate.parse(row.get("end"));
                String type = row.get("type");
                if (end.isBefore(start) || !Set.of("COVERAGE","TERM","NO_CLASS","TEACHING_DAY").contains(type))
                    throw new IllegalArgumentException("Invalid academic calendar rule");
                parsed.add(new Rule(start, end, type, row.get("weekday").isBlank() ? null : DayOfWeek.valueOf(row.get("weekday"))));
            }
            if (parsed.stream().noneMatch(r -> r.type.equals("COVERAGE")) || parsed.stream().noneMatch(r -> r.type.equals("TERM")))
                throw new IllegalArgumentException("Academic calendar needs coverage and term");
            rules = List.copyOf(parsed);
        } catch (IOException | RuntimeException e) { throw new IllegalStateException("Invalid academic calendar: " + location, e); }
    }
    public void requireCoverage(LocalDate date) {
        if (rules.stream().noneMatch(r -> r.type.equals("COVERAGE") && r.contains(date)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Calendario e grade nao configurados para essa data. Atualize o periodo letivo.");
    }
    public DayOfWeek teachingDay(LocalDate date) {
        requireCoverage(date);
        if (rules.stream().anyMatch(r -> r.type.equals("NO_CLASS") && r.contains(date))) return null;
        var override = rules.stream().filter(r -> r.type.equals("TEACHING_DAY") && r.contains(date)).findFirst();
        if (override.isPresent()) return override.get().weekday;
        if (rules.stream().noneMatch(r -> r.type.equals("TERM") && r.contains(date))) return null;
        return date.getDayOfWeek().getValue() < 6 ? date.getDayOfWeek() : null;
    }
}
