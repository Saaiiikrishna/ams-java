package com.example.attendancesystem.attendance.service;

import com.example.attendancesystem.attendance.dto.ScheduledSessionDto;
import com.example.attendancesystem.attendance.model.ScheduledSession;
import com.example.attendancesystem.attendance.repository.ScheduledSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

@Service
public class ScheduledSessionService {

    @Autowired
    private ScheduledSessionRepository scheduledSessionRepository;

    public ScheduledSessionDto createScheduledSession(ScheduledSessionDto dto) {
        // For microservices independence, use organizationId directly
        ScheduledSession scheduledSession = new ScheduledSession();
        scheduledSession.setName(dto.getName());
        scheduledSession.setDescription(dto.getDescription());
        scheduledSession.setStartTime(dto.getStartTime());
        scheduledSession.setDurationMinutes(dto.getDurationMinutes());
        scheduledSession.setDaysOfWeek(dto.getDaysOfWeek());
        scheduledSession.setAllowedCheckInMethods(dto.getAllowedCheckInMethods());
        scheduledSession.setOrganizationId(dto.getOrganizationId());
        scheduledSession.setActive(true);

        ScheduledSession saved = scheduledSessionRepository.save(scheduledSession);
        return convertToDto(saved);
    }

    public List<ScheduledSessionDto> getScheduledSessionsByOrganization(Long organizationId) {
        List<ScheduledSession> sessions = scheduledSessionRepository.findByOrganizationIdAndActiveTrue(organizationId);
        return sessions.stream().map(this::convertToDto).toList();
    }

    public ScheduledSessionDto getScheduledSessionById(Long id, String entityId) {
        // Simplified for microservices independence
        ScheduledSession session = scheduledSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled session not found"));
        return convertToDto(session);
    }

    public ScheduledSessionDto updateScheduledSession(Long id, ScheduledSessionDto dto) {
        ScheduledSession session = scheduledSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled session not found"));
        
        session.setName(dto.getName());
        session.setDescription(dto.getDescription());
        session.setStartTime(dto.getStartTime());
        session.setDurationMinutes(dto.getDurationMinutes());
        session.setDaysOfWeek(dto.getDaysOfWeek());
        session.setAllowedCheckInMethods(dto.getAllowedCheckInMethods());

        ScheduledSession saved = scheduledSessionRepository.save(session);
        return convertToDto(saved);
    }

    public void deleteScheduledSession(Long id) {
        ScheduledSession session = scheduledSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled session not found"));
        session.setActive(false);
        scheduledSessionRepository.save(session);
    }

    private ScheduledSessionDto convertToDto(ScheduledSession session) {
        ScheduledSessionDto dto = new ScheduledSessionDto();
        dto.setId(session.getId());
        dto.setName(session.getName());
        dto.setDescription(session.getDescription());
        dto.setStartTime(session.getStartTime());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setDaysOfWeek(session.getDaysOfWeek());
        dto.setAllowedCheckInMethods(session.getAllowedCheckInMethods());
        dto.setOrganizationId(session.getOrganizationId());
        dto.setActive(session.isActive());
        return dto;
    }
}
