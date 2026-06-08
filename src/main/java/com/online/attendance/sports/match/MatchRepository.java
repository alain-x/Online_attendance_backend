package com.online.attendance.sports.match;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @EntityGraph(attributePaths = {"team"})
    @Override
    List<Match> findAll();

    @EntityGraph(attributePaths = {"team"})
    List<Match> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team"})
    List<Match> findByTeamIdAndMatchDateBetween(Long teamId, LocalDateTime start, LocalDateTime end);
}
