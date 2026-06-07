package com.online.attendance.sports.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.createdBy WHERE cr.team.id = :teamId")
    List<ChatRoom> findByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT cr FROM ChatRoom cr JOIN ChatParticipant cp ON cp.room.id = cr.id WHERE cp.user.id = :userId")
    List<ChatRoom> findByParticipantsUser(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.createdBy WHERE cr.type = 'DIRECT' AND cr.id IN (" +
            "SELECT cp1.room.id FROM ChatParticipant cp1 WHERE cp1.user.id = :user1Id" +
            ") AND cr.id IN (" +
            "SELECT cp2.room.id FROM ChatParticipant cp2 WHERE cp2.user.id = :user2Id" +
            ")")
    Optional<ChatRoom> findDirectChat(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.team LEFT JOIN FETCH cr.createdBy WHERE cr.id IN (" +
            "SELECT cp.room.id FROM ChatParticipant cp WHERE cp.user.id = :userId" +
            ") ORDER BY cr.createdAt DESC")
    List<ChatRoom> findMyRooms(@Param("userId") Long userId);
}
