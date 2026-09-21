package org.example.entity;

import java.util.Comparator;
import java.util.List;

public class SearchByLeastCapacityWastage implements SearchStrategy{
    @Override
    public Room search(int capacity, List<Room> candidates) {
        return candidates.stream()
                .filter(r -> r.getCapacity() >= capacity)
                .min(Comparator.comparingInt(Room::getCapacity))
                .orElse(null);
    }
}
