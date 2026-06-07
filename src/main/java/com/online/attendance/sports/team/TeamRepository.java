package com.online.attendance.sports.team;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    List<Team> findByClubId(Long clubId);

    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    List<Team> findBySportId(Long sportId);

    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    List<Team> findByCoachId(Long coachId);

    @Override
    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    List<Team> findAll();

    @Override
    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    Optional<Team> findById(Long id);
}
