package com.online.attendance.sports.parent;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentLinkRepository extends JpaRepository<ParentLink, Long> {
    @EntityGraph(attributePaths = {"parentUser", "player", "player.user"})
    List<ParentLink> findByParentUserId(Long parentUserId);

    @EntityGraph(attributePaths = {"parentUser", "player", "player.user"})
    List<ParentLink> findByPlayerId(Long playerId);
}
