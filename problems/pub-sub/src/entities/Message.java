package entities;

public record Message(
        long id,
        long timestamp,
        String payload
) {}