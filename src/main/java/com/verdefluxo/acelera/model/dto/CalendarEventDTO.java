package com.verdefluxo.acelera.model.dto;

public record CalendarEventDTO(
        String id,
        String title,
        String date,
        String startTime,
        String endTime,
        boolean allDay,
        String colorHex
) {}
