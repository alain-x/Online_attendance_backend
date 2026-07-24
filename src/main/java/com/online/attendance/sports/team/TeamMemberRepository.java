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

    @EntityGraph(attributePaths = {"team", "player", "player.user"})
    @Query("SELECT tm FROM TeamMember tm JOIN tm.team t JOIN t.club c WHERE c.company.id = :companyId")
    List<TeamMember> findByClubCompanyId(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"team", "player", "player.user"})
    @Query("SELECT tm FROM TeamMember tm JOIN tm.team t JOIN t.club c WHERE c.company.id = :companyId AND tm.id = :id")
    Optional<TeamMember> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
