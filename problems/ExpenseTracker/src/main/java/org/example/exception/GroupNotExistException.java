package org.example.exception;

public class GroupNotExistException extends RuntimeException {
    public GroupNotExistException(String groupId) {
        super("Group does not exist: " + groupId);
    }
}
