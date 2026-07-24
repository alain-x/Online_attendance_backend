package com.online.attendance.sports.team;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    List<Team> findByClubId(Long clubId);

    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    List<Team> findBySportId(Long sportId);

    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    List<Team> findByCoachId(Long coachId);

    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    List<Team> findByClubCompanyId(Long companyId);

    @EntityGraph(attributePaths = {"sport", "club", "coach"})
    Optional<Team> findByIdAndClubCompanyId(Long id, Long companyId);

    @Query("SELECT t FROM Team t JOIN t.club c WHERE c.company.id = :companyId AND t.id = :id")
    Optional<Team> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
