package ifsuldeminas.edu.br.ifsystem.room.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
public record WeeklyClass(String roomId, DayOfWeek day, LocalTime start, LocalTime end, String group, String description) {}
