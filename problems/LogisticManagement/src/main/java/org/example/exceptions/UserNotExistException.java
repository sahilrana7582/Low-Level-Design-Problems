


package org.example.exceptions;

import java.util.UUID;

public class UserNotExistException extends RuntimeException {

    public UserNotExistException(String message) {
        super(message);
    }
}