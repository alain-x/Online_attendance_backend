package com.online.attendance.sports.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerStatisticRepository extends JpaRepository<PlayerStatistic, Long> {
    List<PlayerStatistic> findByPlayerId(Long playerId);
}
