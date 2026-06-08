package com.online.attendance.sports.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface MessageHiddenByRepository extends JpaRepository<MessageHiddenBy, MessageHiddenByKey> {

    @Query("SELECT h.messageId FROM MessageHiddenBy h WHERE h.userId = :userId")
    Set<Long> findMessageIdsByUserId(@Param("userId") Long userId);

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);
}
