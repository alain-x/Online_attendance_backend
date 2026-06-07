package com.online.attendance.sports.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByTeamId(Long teamId);

    @Query("SELECT cr FROM ChatRoom cr JOIN ChatParticipant cp ON cp.room.id = cr.id WHERE cp.user.id = :userId")
    List<ChatRoom> findByParticipantsUser(@Param("userId") Long userId);
}
