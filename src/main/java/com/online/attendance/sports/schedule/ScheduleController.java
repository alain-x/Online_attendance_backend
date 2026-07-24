package com.online.attendance.sports.schedule;

import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.sports.schedule.dto.CalendarEventResponse;
import com.online.attendance.sports.schedule.dto.CreateCalendarEventRequest;
import com.online.attendance.sports.team.Team;
import com.online.attendance.sports.team.TeamRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports/schedule")
@Transactional(readOnly = true)
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);
    private final CalendarEventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final CurrentCompanyService currentCompanyService;

    public ScheduleController(CalendarEventRepository eventRepository, TeamRepository teamRepository,
                              CurrentCompanyService currentCompanyService) {
        this.eventRepository = eventRepository;
        this.teamRepository = teamRepository;
        this.currentCompanyService = currentCompanyService;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping
    public List<CalendarEventResponse> list(@RequestParam(required = false) Long teamId,
                                             @RequestParam(required = false) LocalDateTime start,
                                             @RequestParam(required = false) LocalDateTime end,
                                             Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        List<CalendarEvent> events;
        if (teamId != null && start != null && end != null) {
            var team = teamRepository.findByIdAndClubCompanyId(teamId, companyId);
            if (team.isEmpty()) {
                return List.of();
            }
            events = eventRepository.findByTeamIdAndStartDateTimeBetween(teamId, start, end);
        } else if (teamId != null) {
            var team = teamRepository.findByIdAndClubCompanyId(teamId, companyId);
            if (team.isEmpty()) {
                return List.of();
            }
            events = eventRepository.findByTeamId(teamId);
        } else {
            events = eventRepository.findByClubCompanyId(companyId);
        }
        return events.stream().map(CalendarEventResponse::from).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER', 'PLAYER', 'PARENT')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var event = eventRepository.findByIdAndClubCompanyId(id, companyId);
        if (event.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Event not found"));
        }
        return ResponseEntity.ok(CalendarEventResponse.from(event.get()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateCalendarEventRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        CalendarEvent event = CalendarEvent.builder()
                .team(team)
                .title(request.getTitle())
                .description(request.getDescription())
                .eventType(request.getEventType())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .location(request.getLocation())
                .allDay(request.isAllDay())
                .color(request.getColor())
                .build();
        event = eventRepository.save(event);
        return ResponseEntity.ok(CalendarEventResponse.from(event));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateCalendarEventRequest request, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var existing = eventRepository.findByIdAndClubCompanyId(id, companyId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Event not found"));
        }
        var event = existing.get();
        Team team = teamRepository.findByIdAndClubCompanyId(request.getTeamId(), companyId).orElse(null);
        if (team == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Team not found"));
        }
        event.setTeam(team);
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventType(request.getEventType());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setLocation(request.getLocation());
        event.setAllDay(request.isAllDay());
        event.setColor(request.getColor());
        event = eventRepository.save(event);
        return ResponseEntity.ok().body(CalendarEventResponse.from(event));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'CLUB_ADMIN', 'COACH', 'TEAM_MANAGER')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        Long companyId = currentCompanyService.requireCompanyId(authentication);
        var event = eventRepository.findByIdAndClubCompanyId(id, companyId);
        if (event.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Event not found"));
        }
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
