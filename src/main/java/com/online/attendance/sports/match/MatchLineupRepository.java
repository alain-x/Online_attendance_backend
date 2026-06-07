package com.online.attendance.sports.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {
    List<MatchLineup> findByMatchId(Long matchId);
}
