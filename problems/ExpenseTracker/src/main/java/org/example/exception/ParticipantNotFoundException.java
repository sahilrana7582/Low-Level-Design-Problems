package org.example.exception;

public class ParticipantNotFoundException extends RuntimeException {
    public ParticipantNotFoundException(String userId) {
        super("User is not a participant of this expense: " + userId);
    }
}
