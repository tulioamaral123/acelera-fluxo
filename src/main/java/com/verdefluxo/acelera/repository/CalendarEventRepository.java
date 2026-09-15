package com.verdefluxo.acelera.repository;

import com.verdefluxo.acelera.model.entity.CalendarEvent;
import com.verdefluxo.acelera.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByUserAndDateBetweenOrderByDateAscStartTimeAsc(
            User user, LocalDate from, LocalDate to);

    Page<CalendarEvent> findByUserOrderByDateDescStartTimeDesc(User user, Pageable pageable);

    Optional<CalendarEvent> findByGoogleEventIdAndUser(String googleEventId, User user);

    void deleteByUserAndDateBetween(User user, LocalDate from, LocalDate to);
}
