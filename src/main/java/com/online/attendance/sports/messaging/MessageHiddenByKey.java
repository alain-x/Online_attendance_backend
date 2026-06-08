package com.online.attendance.sports.messaging;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageHiddenByKey implements Serializable {
    private Long messageId;
    private Long userId;
}
