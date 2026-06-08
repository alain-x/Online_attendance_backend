package com.online.attendance.sports.match;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {
    @EntityGraph(attributePaths = {"match", "player", "player.user"})
    List<MatchLineup> findByMatchId(Long matchId);
}
