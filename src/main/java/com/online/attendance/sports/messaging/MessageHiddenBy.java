package com.online.attendance.sports.messaging;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sports_chat_message_hidden")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@IdClass(MessageHiddenByKey.class)
public class MessageHiddenBy {

    @Id
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;
}
