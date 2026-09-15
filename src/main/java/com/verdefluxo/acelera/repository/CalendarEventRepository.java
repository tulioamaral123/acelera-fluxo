package com.verdefluxo.acelera.repository;

import com.verdefluxo.acelera.model.entity.CalendarEvent;
import com.verdefluxo.acelera.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByUserAndDateBetweenOrderByDateAscStartTimeAsc(
            User user, LocalDate from, LocalDate to);

    Page<CalendarEvent> findByUserOrderByDateDescStartTimeDesc(User user, Pageable pageable);

    Optional<CalendarEvent> findByGoogleEventIdAndUser(String googleEventId, User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CalendarEvent e WHERE e.user = :user AND e.date >= :from AND e.date <= :to")
    void deleteByUserAndDateBetween(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
