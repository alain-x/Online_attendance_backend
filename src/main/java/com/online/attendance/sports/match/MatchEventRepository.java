package com.online.attendance.sports.match;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
    @EntityGraph(attributePaths = {"match", "player", "player.user"})
    List<MatchEvent> findByMatchId(Long matchId);
}
