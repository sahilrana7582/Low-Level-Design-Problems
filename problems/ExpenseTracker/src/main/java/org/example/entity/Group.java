package org.example.entity;

import org.example.exception.UserAlreadyExistException;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private final String id;
    private final String name;
    private final List<User> members = new ArrayList<>();

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void add(User user) {
        if (isMember(user.getId())) {
            throw new UserAlreadyExistException(user.getId());
        }
        members.add(user);
    }

    private boolean isMember(String userId) {
        for (User member : members) {
            if (member.getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<User> getMembers() {
        return new ArrayList<>(members);
    }

    @Override
    public String toString() {
        return "Group{id=" + id + ", name=" + name + ", members=" + members.size() + "}";
    }
}
