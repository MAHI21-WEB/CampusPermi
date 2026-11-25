package com.campus.permission.service;

import com.campus.permission.enums.RequestStatus;
import com.campus.permission.model.Approval;
import com.campus.permission.model.PermissionRequest;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Computes dashboard statistics and recent request summaries using Java Collections.
 */
public class DashboardService {

    /**
     * Aggregates request counts grouped by {@link RequestStatus}.
     */
    public Map<RequestStatus, Long> getStatusCounts(List<PermissionRequest> requests) {
        Map<RequestStatus, Long> counts = new HashMap<>();
        for (RequestStatus status : RequestStatus.values()) {
            counts.put(status, 0L);
        }
        if (requests == null) {
            return counts;
        }
        for (PermissionRequest request : requests) {
            if (request.getStatus() != null) {
                counts.put(request.getStatus(), counts.getOrDefault(request.getStatus(), 0L) + 1L);
            }
        }
        return counts;
    }

    /**
     * Counts all requests currently awaiting any approval stage.
     */
    public long countPending(Map<RequestStatus, Long> counts) {
        if (counts == null) {
            return 0L;
        }
        return counts.getOrDefault(RequestStatus.PENDING_FACULTY, 0L)
                + counts.getOrDefault(RequestStatus.PENDING_DOSA, 0L)
                + counts.getOrDefault(RequestStatus.PENDING_ASSOCIATE_DEAN, 0L);
    }

    /**
     * Returns up to {@code limit} most recent requests sorted by creation timestamp descending.
     */
    public List<PermissionRequest> getRecentRequests(List<PermissionRequest> requests, int limit) {
        if (requests == null || requests.isEmpty() || limit <= 0) {
            return new ArrayList<>();
        }
        List<PermissionRequest> sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.comparing(
                PermissionRequest::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return new ArrayList<>(sorted.subList(0, Math.min(limit, sorted.size())));
    }

    /**
     * Builds an ordered {@link LinkedList} of lifecycle events for a permission request.
     */
    public LinkedList<String> buildActivityTimeline(PermissionRequest request, List<Approval> approvals) {
        LinkedList<String> timeline = new LinkedList<>();
        if (request == null) {
            return timeline;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        String created = request.getCreatedAt() != null ? request.getCreatedAt().format(fmt) : "N/A";
        timeline.add("Created by " + (request.getSubmittedByName() != null ? request.getSubmittedByName() : "Representative")
                + " on " + created);

        if (approvals != null) {
            for (Approval approval : approvals) {
                String date = approval.getDecisionDate() != null ? approval.getDecisionDate().format(fmt) : "N/A";
                String remarks = approval.getRemarks() != null && !approval.getRemarks().isBlank()
                        ? " — \"" + approval.getRemarks() + "\""
                        : "";
                timeline.add(approval.getApprovalLevel().getDisplayName() + ": "
                        + approval.getDecision().getDisplayName()
                        + " (" + date + ")" + remarks);
            }
        }

        if (request.getStatus() != null) {
            timeline.add("Current Status: " + request.getStatus().getDisplayName());
        }
        return timeline;
    }

    /**
     * Formats an activity timeline by traversing the {@link LinkedList} with an {@link Iterator}.
     */
    public String formatTimelineSummary(LinkedList<String> timeline) {
        if (timeline == null || timeline.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = timeline.iterator();
        int step = 1;
        while (iterator.hasNext()) {
            sb.append(step++).append(". ").append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
}
