package com.online.attendance.sports.parent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentLinkRepository extends JpaRepository<ParentLink, Long> {
    List<ParentLink> findByParentUserId(Long parentUserId);
    List<ParentLink> findByPlayerId(Long playerId);
}
