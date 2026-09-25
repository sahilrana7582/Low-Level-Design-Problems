package org.example.exception;

public class UserNotGroupMemberException extends RuntimeException {
    public UserNotGroupMemberException(String userId, String groupId) {
        super("User " + userId + " is not a member of group " + groupId);
    }
}
