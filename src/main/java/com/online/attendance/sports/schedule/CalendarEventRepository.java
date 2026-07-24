package com.online.attendance.sports.schedule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @EntityGraph(attributePaths = {"team", "createdBy"})
    @Override
    List<CalendarEvent> findAll();

    @EntityGraph(attributePaths = {"team", "createdBy"})
    List<CalendarEvent> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team", "createdBy"})
    List<CalendarEvent> findByTeamIdAndStartDateTimeBetween(Long teamId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT ce FROM CalendarEvent ce JOIN FETCH ce.team LEFT JOIN FETCH ce.createdBy WHERE ce.team.club.company.id = :companyId")
    List<CalendarEvent> findByClubCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT ce FROM CalendarEvent ce JOIN FETCH ce.team LEFT JOIN FETCH ce.createdBy WHERE ce.id = :id AND ce.team.club.company.id = :companyId")
    Optional<CalendarEvent> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
