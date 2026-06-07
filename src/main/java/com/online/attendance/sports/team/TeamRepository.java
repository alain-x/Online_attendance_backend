package com.online.attendance.sports.team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByClubId(Long clubId);
    List<Team> findBySportId(Long sportId);
    List<Team> findByCoachId(Long coachId);
}
