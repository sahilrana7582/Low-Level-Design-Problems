package entity;

public record RoomBookingHold(
        String holdId,
        String roomId,
        long checkIn,
        long checkOut,
        long expireAt
) {
}