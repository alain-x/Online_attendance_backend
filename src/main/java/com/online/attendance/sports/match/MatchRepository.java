package com.online.attendance.sports.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTeamId(Long teamId);
    List<Match> findByTeamIdAndMatchDateBetween(Long teamId, LocalDateTime start, LocalDateTime end);
}
