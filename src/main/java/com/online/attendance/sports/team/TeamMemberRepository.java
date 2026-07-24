package com.online.attendance.sports.team;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @EntityGraph(attributePaths = {"team", "player", "player.user"})
    List<TeamMember> findByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team", "player", "player.user"})
    List<TeamMember> findByPlayerId(Long playerId);

    @Override
    @EntityGraph(attributePaths = {"team", "player", "player.user"})
    Optional<TeamMember> findById(Long id);

    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.team LEFT JOIN FETCH tm.player LEFT JOIN FETCH tm.player.user WHERE tm.team.club.company.id = :companyId")
    List<TeamMember> findByClubCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.team LEFT JOIN FETCH tm.player LEFT JOIN FETCH tm.player.user WHERE tm.id = :id AND tm.team.club.company.id = :companyId")
    Optional<TeamMember> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
