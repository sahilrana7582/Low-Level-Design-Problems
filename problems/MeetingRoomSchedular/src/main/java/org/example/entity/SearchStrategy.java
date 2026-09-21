package org.example.entity;

import java.util.List;

public interface SearchStrategy {
    Room search(int capacity, List<Room> candidates);
}
