package com.campus.permission.service;

import com.campus.permission.dao.AttachmentDAO;
import com.campus.permission.enums.ApprovalLevel;
import com.campus.permission.enums.Decision;
import com.campus.permission.enums.RequestStatus;
import com.campus.permission.model.Approval;
import com.campus.permission.model.Attachment;
import com.campus.permission.model.PermissionRequest;
import com.campus.permission.model.PermissionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BackupAndAttachmentTest {

    @TempDir
    Path tempDir;

    @Test
    void attachmentServiceCopiesFileUsingByteStreamsAndPersistsMetadata() throws Exception {
        Path source = tempDir.resolve("event-proposal.txt");
        Files.writeString(source, "HackTU 2026 proposal content");

        List<Attachment> savedList = new ArrayList<>();
        AttachmentDAO stubDao = new AttachmentDAO() {
            private int seq = 1;

            @Override
            public Attachment save(Attachment entity) {
                entity.setAttachmentId(seq++);
                savedList.add(entity);
                return entity;
            }

            @Override public Optional<Attachment> findById(int id) { return Optional.empty(); }
            @Override public List<Attachment> findAll() { return savedList; }
            @Override public List<Attachment> findByRequestId(int requestId) { return savedList; }
            @Override public void deleteById(int attachmentId) { }
        };

        AttachmentService service = new AttachmentService(stubDao, tempDir.resolve("uploads"));
        Attachment saved = service.uploadAttachment(10, source.toFile());

        assertEquals(1, saved.getAttachmentId());
        assertEquals("event-proposal.txt", saved.getFileName());
        File resolved = service.resolveAttachmentFile(saved);
        assertTrue(resolved.exists());
        assertEquals("HackTU 2026 proposal content", Files.readString(resolved.toPath()));
    }

    @Test
    void backupServiceSerializesDeserializesAndWritesTextSummary() throws Exception {
        PermissionRequest req = new PermissionRequest();
        req.setRequestId(42);
        req.setTitle("Annual Tech Fest");
        req.setDescription("3-day technical festival");
        req.setStatus(RequestStatus.APPROVED);
        req.setPermissionType(new PermissionType(1, "Event/Activity Permission", "Events", true));

        BackupService backupService = new BackupService();
        Path backupPath = tempDir.resolve("backup.ser");
        backupService.exportRequestsBackup(List.of(req), backupPath);

        List<PermissionRequest> restored = backupService.restoreRequestsBackup(backupPath);
        assertEquals(1, restored.size());
        assertEquals(42, restored.get(0).getRequestId());
        assertEquals("Annual Tech Fest", restored.get(0).getTitle());
        assertEquals(RequestStatus.APPROVED, restored.get(0).getStatus());

        Path summaryPath = tempDir.resolve("summary.txt");
        backupService.exportSummaryText(List.of(req), summaryPath);
        String summaryContent = Files.readString(summaryPath);
        assertTrue(summaryContent.contains("#42 | Annual Tech Fest | Approved"));
    }

    @Test
    void dashboardServiceBuildsAndIteratesLinkedListTimeline() {
        PermissionRequest req = new PermissionRequest();
        req.setRequestId(7);
        req.setSubmittedByName("Aarav Singh");
        req.setCreatedAt(LocalDateTime.of(2026, 11, 20, 10, 0));
        req.setStatus(RequestStatus.PENDING_DOSA);

        Approval facultyApproval = new Approval();
        facultyApproval.setApprovalLevel(ApprovalLevel.FACULTY);
        facultyApproval.setDecision(Decision.APPROVED);
        facultyApproval.setRemarks("Recommended");
        facultyApproval.setDecisionDate(LocalDateTime.of(2026, 11, 21, 11, 30));

        DashboardService dashboardService = new DashboardService();
        LinkedList<String> timeline = dashboardService.buildActivityTimeline(req, List.of(facultyApproval));
        String formatted = dashboardService.formatTimelineSummary(timeline);

        assertEquals(3, timeline.size());
        assertTrue(formatted.contains("1. Created by Aarav Singh"));
        assertTrue(formatted.contains("2. Faculty Advisor: Approved"));
        assertTrue(formatted.contains("3. Current Status: Pending DOSA Review"));
    }
}
