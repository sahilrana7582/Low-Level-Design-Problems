package org.example.exceptions;

public class ShipmentAlreadyAssignedException extends RuntimeException {

    public ShipmentAlreadyAssignedException(String message) {
        super(message);
    }
}
