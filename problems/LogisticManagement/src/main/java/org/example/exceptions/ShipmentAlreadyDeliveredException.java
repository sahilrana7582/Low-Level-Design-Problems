package org.example.exceptions;

public class ShipmentAlreadyDeliveredException extends RuntimeException {

    public ShipmentAlreadyDeliveredException(String message) {
        super(message);
    }
}
