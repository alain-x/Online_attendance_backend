package com.online.attendance.sports.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {
    List<PlayerProfile> findByClubId(Long clubId);
    Optional<PlayerProfile> findByUserId(Long userId);

    @Query("SELECT pp FROM PlayerProfile pp JOIN TeamMember tm ON tm.player.id = pp.id WHERE tm.team.id = :teamId")
    List<PlayerProfile> findByTeamId(@Param("teamId") Long teamId);
}
