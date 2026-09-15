package com.verdefluxo.acelera.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"google_event_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_event_id", nullable = false)
    private String googleEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 5)
    private String startTime;

    @Column(length = 5)
    private String endTime;

    @Column(nullable = false)
    private boolean allDay;

    @Column(length = 7)
    private String colorHex;

    @Column(nullable = false)
    private LocalDateTime syncedAt;
}
