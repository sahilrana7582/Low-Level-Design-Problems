package org.example.exceptions;

import java.util.UUID;

public class ShipmentCancelledException extends RuntimeException {

    public ShipmentCancelledException(String message) {
        super(message);
    }
}