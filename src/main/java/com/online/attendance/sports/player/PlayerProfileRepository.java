package com.online.attendance.sports.player;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    @EntityGraph(attributePaths = {"user", "club"})
    @Override
    List<PlayerProfile> findAll();

    @EntityGraph(attributePaths = {"user", "club"})
    List<PlayerProfile> findByClubId(Long clubId);

    @EntityGraph(attributePaths = {"user", "club"})
    Optional<PlayerProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "club"})
    @Query("SELECT pp FROM PlayerProfile pp JOIN TeamMember tm ON tm.player.id = pp.id WHERE tm.team.id = :teamId")
    List<PlayerProfile> findByTeamId(@Param("teamId") Long teamId);

    @EntityGraph(attributePaths = {"user", "club"})
    @Query("SELECT pp FROM PlayerProfile pp JOIN pp.club c WHERE c.company.id = :companyId")
    List<PlayerProfile> findByClubCompanyId(@Param("companyId") Long companyId);

    @EntityGraph(attributePaths = {"user", "club"})
    @Query("SELECT pp FROM PlayerProfile pp JOIN pp.club c WHERE c.company.id = :companyId AND pp.id = :id")
    Optional<PlayerProfile> findByIdAndClubCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
