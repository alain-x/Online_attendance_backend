package com.online.attendance.sports.player;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerStatisticRepository extends JpaRepository<PlayerStatistic, Long> {

    @EntityGraph(attributePaths = {"player"})
    @Override
    List<PlayerStatistic> findAll();

    @EntityGraph(attributePaths = {"player"})
    List<PlayerStatistic> findByPlayerId(Long playerId);
}
