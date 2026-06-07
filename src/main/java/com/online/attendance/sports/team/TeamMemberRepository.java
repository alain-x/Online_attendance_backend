package com.online.attendance.sports.team;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
