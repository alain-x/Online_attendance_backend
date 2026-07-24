package com.online.attendance.sports.match;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @EntityGraph(attributePaths = {"team"})
    @Override
    List<Match> findAll();

    @EntityGraph(attributePaths = {"team"})
    List<Match> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team"})
    List<Match> findByTeamIdAndMatchDateBetween(Long teamId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT m FROM Match m JOIN FETCH m.team WHERE m.team.club.company.id = :companyId")
    List<Match> findByClubCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT m FROM Match m JOIN FETCH m.team WHERE m.id = :id AND m.team.club.company.id = :companyId")
    Optional<Match> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
