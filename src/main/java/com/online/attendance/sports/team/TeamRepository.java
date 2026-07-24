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

    @Query("SELECT t FROM Team t JOIN FETCH t.sport JOIN FETCH t.club LEFT JOIN FETCH t.coach WHERE t.club.company.id = :companyId")
    List<Team> findByClubCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT t FROM Team t JOIN FETCH t.sport JOIN FETCH t.club LEFT JOIN FETCH t.coach WHERE t.id = :id AND t.club.company.id = :companyId")
    Optional<Team> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
