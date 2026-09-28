package org.example.exceptions;

public class VehicleNotExistException extends RuntimeException {

    public VehicleNotExistException(String message) {
        super(message);
    }
}
