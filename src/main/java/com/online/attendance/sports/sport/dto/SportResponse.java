package com.online.attendance.sports.sport.dto;

import com.online.attendance.sports.sport.Sport;

public record SportResponse(Long id, String name, String description, boolean active) {
    public static SportResponse from(Sport sport) {
        return new SportResponse(sport.getId(), sport.getName(), sport.getDescription(), sport.isActive());
    }
}
