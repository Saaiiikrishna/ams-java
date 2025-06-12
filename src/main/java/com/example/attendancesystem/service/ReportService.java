package com.example.attendancesystem.service;

import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Generate session attendance report with attendees and absentees
     */
    public byte[] generateSessionAttendanceReport(Long sessionId, String entityId) {
        try {
            logger.info("Generating session attendance report for session: {} in entity: {}", sessionId, entityId);

            // Get session and organization
            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

            AttendanceSession session = attendanceSessionRepository.findByIdAndOrganization(sessionId, organization)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));

            // Get attendance logs and all subscribers
            List<AttendanceLog> attendanceLogs = attendanceLogRepository.findBySession(session);
            List<Subscriber> allSubscribers = subscriberRepository.findAllByOrganization(organization);

            // Separate attendees and absentees
            Set<Long> attendeeIds = attendanceLogs.stream()
                    .map(log -> log.getSubscriber().getId())
                    .collect(Collectors.toSet());

            List<Subscriber> absentees = allSubscribers.stream()
                    .filter(subscriber -> !attendeeIds.contains(subscriber.getId()))
                    .collect(Collectors.toList());

            return createSessionReportPdf(session, attendanceLogs, absentees, organization);

        } catch (Exception e) {
            logger.error("Failed to generate session attendance report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    /**
     * Generate individual subscriber activity report
     */
    public byte[] generateSubscriberActivityReport(Long subscriberId, String entityId, 
                                                  LocalDateTime startDate, LocalDateTime endDate) {
        try {
            logger.info("Generating subscriber activity report for subscriber: {} in entity: {}", subscriberId, entityId);

            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

            Subscriber subscriber = subscriberRepository.findByIdAndOrganization(subscriberId, organization)
                    .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));

            // Get attendance logs for the subscriber in the date range
            List<AttendanceLog> attendanceLogs = attendanceLogRepository
                    .findBySubscriberAndCheckInTimeBetween(subscriber, startDate, endDate);

            // Get all sessions in the date range for this organization
            List<AttendanceSession> allSessions = attendanceSessionRepository
                    .findByOrganizationAndStartTimeBetween(organization, startDate, endDate);

            // Find missed sessions
            Set<Long> attendedSessionIds = attendanceLogs.stream()
                    .map(log -> log.getSession().getId())
                    .collect(Collectors.toSet());

            List<AttendanceSession> missedSessions = allSessions.stream()
                    .filter(session -> !attendedSessionIds.contains(session.getId()))
                    .collect(Collectors.toList());

            return createSubscriberReportPdf(subscriber, attendanceLogs, missedSessions, 
                                           organization, startDate, endDate);

        } catch (Exception e) {
            logger.error("Failed to generate subscriber activity report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    /**
     * Create PDF for session attendance report
     */
    private byte[] createSessionReportPdf(AttendanceSession session, List<AttendanceLog> attendanceLogs,
                                        List<Subscriber> absentees, Organization organization) throws Exception {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header
        document.add(new Paragraph(organization.getName())
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold());

        document.add(new Paragraph("Session Attendance Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16)
                .setBold());

        // Session details
        document.add(new Paragraph("Session Details")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));

        Table sessionTable = new Table(2);
        sessionTable.setWidth(UnitValue.createPercentValue(100));
        sessionTable.addCell(new Cell().add(new Paragraph("Session Name:").setBold()));
        sessionTable.addCell(new Cell().add(new Paragraph(session.getName())));
        sessionTable.addCell(new Cell().add(new Paragraph("Start Time:").setBold()));
        sessionTable.addCell(new Cell().add(new Paragraph(session.getStartTime().format(DATE_TIME_FORMATTER))));
        sessionTable.addCell(new Cell().add(new Paragraph("End Time:").setBold()));
        sessionTable.addCell(new Cell().add(new Paragraph(
                session.getEndTime() != null ? session.getEndTime().format(DATE_TIME_FORMATTER) : "Ongoing")));
        sessionTable.addCell(new Cell().add(new Paragraph("Total Attendees:").setBold()));
        sessionTable.addCell(new Cell().add(new Paragraph(String.valueOf(attendanceLogs.size()))));
        sessionTable.addCell(new Cell().add(new Paragraph("Total Absentees:").setBold()));
        sessionTable.addCell(new Cell().add(new Paragraph(String.valueOf(absentees.size()))));

        document.add(sessionTable);

        // Attendees section
        if (!attendanceLogs.isEmpty()) {
            document.add(new Paragraph("Attendees")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(20));

            Table attendeesTable = new Table(4);
            attendeesTable.setWidth(UnitValue.createPercentValue(100));
            attendeesTable.addHeaderCell(new Cell().add(new Paragraph("Name").setBold()));
            attendeesTable.addHeaderCell(new Cell().add(new Paragraph("Mobile Number").setBold()));
            attendeesTable.addHeaderCell(new Cell().add(new Paragraph("Check-in Time").setBold()));
            attendeesTable.addHeaderCell(new Cell().add(new Paragraph("Check-out Time").setBold()));

            for (AttendanceLog log : attendanceLogs) {
                attendeesTable.addCell(new Cell().add(new Paragraph(
                        log.getSubscriber().getFirstName() + " " + log.getSubscriber().getLastName())));
                attendeesTable.addCell(new Cell().add(new Paragraph(log.getSubscriber().getMobileNumber())));
                attendeesTable.addCell(new Cell().add(new Paragraph(log.getCheckInTime().format(DATE_TIME_FORMATTER))));
                attendeesTable.addCell(new Cell().add(new Paragraph(
                        log.getCheckOutTime() != null ? log.getCheckOutTime().format(DATE_TIME_FORMATTER) : "Not checked out")));
            }

            document.add(attendeesTable);
        }

        // Absentees section
        if (!absentees.isEmpty()) {
            document.add(new Paragraph("Absentees")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(20));

            Table absenteesTable = new Table(3);
            absenteesTable.setWidth(UnitValue.createPercentValue(100));
            absenteesTable.addHeaderCell(new Cell().add(new Paragraph("Name").setBold()));
            absenteesTable.addHeaderCell(new Cell().add(new Paragraph("Mobile Number").setBold()));
            absenteesTable.addHeaderCell(new Cell().add(new Paragraph("Email").setBold()));

            for (Subscriber absentee : absentees) {
                absenteesTable.addCell(new Cell().add(new Paragraph(
                        absentee.getFirstName() + " " + absentee.getLastName())));
                absenteesTable.addCell(new Cell().add(new Paragraph(absentee.getMobileNumber())));
                absenteesTable.addCell(new Cell().add(new Paragraph(
                        absentee.getEmail() != null ? absentee.getEmail() : "N/A")));
            }

            document.add(absenteesTable);
        }

        // Footer
        document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30)
                .setFontSize(10));

        document.close();
        return baos.toByteArray();
    }

    /**
     * Create PDF for subscriber activity report
     */
    private byte[] createSubscriberReportPdf(Subscriber subscriber, List<AttendanceLog> attendanceLogs,
                                           List<AttendanceSession> missedSessions, Organization organization,
                                           LocalDateTime startDate, LocalDateTime endDate) throws Exception {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header
        document.add(new Paragraph(organization.getName())
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold());

        document.add(new Paragraph("Subscriber Activity Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16)
                .setBold());

        // Subscriber details
        document.add(new Paragraph("Subscriber Details")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));

        Table subscriberTable = new Table(2);
        subscriberTable.setWidth(UnitValue.createPercentValue(100));
        subscriberTable.addCell(new Cell().add(new Paragraph("Name:").setBold()));
        subscriberTable.addCell(new Cell().add(new Paragraph(subscriber.getFirstName() + " " + subscriber.getLastName())));
        subscriberTable.addCell(new Cell().add(new Paragraph("Mobile Number:").setBold()));
        subscriberTable.addCell(new Cell().add(new Paragraph(subscriber.getMobileNumber())));
        subscriberTable.addCell(new Cell().add(new Paragraph("Email:").setBold()));
        subscriberTable.addCell(new Cell().add(new Paragraph(subscriber.getEmail() != null ? subscriber.getEmail() : "N/A")));
        subscriberTable.addCell(new Cell().add(new Paragraph("Report Period:").setBold()));
        subscriberTable.addCell(new Cell().add(new Paragraph(
                startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER))));
        subscriberTable.addCell(new Cell().add(new Paragraph("Sessions Attended:").setBold()));
        subscriberTable.addCell(new Cell().add(new Paragraph(String.valueOf(attendanceLogs.size()))));
        subscriberTable.addCell(new Cell().add(new Paragraph("Sessions Missed:").setBold()));
        subscriberTable.addCell(new Cell().add(new Paragraph(String.valueOf(missedSessions.size()))));

        document.add(subscriberTable);

        // Attended sessions
        if (!attendanceLogs.isEmpty()) {
            document.add(new Paragraph("Attended Sessions")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(20));

            Table attendedTable = new Table(4);
            attendedTable.setWidth(UnitValue.createPercentValue(100));
            attendedTable.addHeaderCell(new Cell().add(new Paragraph("Session Name").setBold()));
            attendedTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
            attendedTable.addHeaderCell(new Cell().add(new Paragraph("Check-in Time").setBold()));
            attendedTable.addHeaderCell(new Cell().add(new Paragraph("Check-out Time").setBold()));

            for (AttendanceLog log : attendanceLogs) {
                attendedTable.addCell(new Cell().add(new Paragraph(log.getSession().getName())));
                attendedTable.addCell(new Cell().add(new Paragraph(log.getCheckInTime().format(DATE_FORMATTER))));
                attendedTable.addCell(new Cell().add(new Paragraph(log.getCheckInTime().format(DATE_TIME_FORMATTER))));
                attendedTable.addCell(new Cell().add(new Paragraph(
                        log.getCheckOutTime() != null ? log.getCheckOutTime().format(DATE_TIME_FORMATTER) : "Not checked out")));
            }

            document.add(attendedTable);
        }

        // Missed sessions
        if (!missedSessions.isEmpty()) {
            document.add(new Paragraph("Missed Sessions")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(20));

            Table missedTable = new Table(3);
            missedTable.setWidth(UnitValue.createPercentValue(100));
            missedTable.addHeaderCell(new Cell().add(new Paragraph("Session Name").setBold()));
            missedTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
            missedTable.addHeaderCell(new Cell().add(new Paragraph("Start Time").setBold()));

            for (AttendanceSession session : missedSessions) {
                missedTable.addCell(new Cell().add(new Paragraph(session.getName())));
                missedTable.addCell(new Cell().add(new Paragraph(session.getStartTime().format(DATE_FORMATTER))));
                missedTable.addCell(new Cell().add(new Paragraph(session.getStartTime().format(DATE_TIME_FORMATTER))));
            }

            document.add(missedTable);
        }

        // Footer
        document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30)
                .setFontSize(10));

        document.close();
        return baos.toByteArray();
    }
}
