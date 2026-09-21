package org.example.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class Booking {
    private final String id;
    private final String roomId;
    private final String organizerId;
    private final LocalDateTime start;
    private final LocalDateTime end;

    public Booking(String roomId, String organizerId, LocalDateTime start, LocalDateTime end) {
        this.id = UUID.randomUUID().toString();
        this.roomId = roomId;
        this.organizerId = organizerId;
        this.start = start;
        this.end = end;
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getOrganizerId() { return organizerId; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
}