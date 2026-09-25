package service;

import exception.RoomNotFoundException;
import model.Room;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Owns the Room objects and answers "does this room exist?"
public class RoomService {
    private final Map<String, Room> rooms = new LinkedHashMap<>(); // keeps the order rooms were added

    public void addRoom(Room room) {
        if (roomExist(room.getId())) {
            throw new IllegalArgumentException("Room already exists: " + room.getId());
        }
        rooms.put(room.getId(), room);
    }

    public boolean roomExist(String roomId) {
        return rooms.containsKey(roomId);
    }

    public Room getRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }
        return room;
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
}
