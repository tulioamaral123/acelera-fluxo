package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.model.dto.CalendarEventDTO;
import com.verdefluxo.acelera.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final GoogleCalendarService calendarService;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDTO>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(calendarService.getEventsForWeek(from, to));
    }
}
