package org.example.exceptions;

import org.example.contract.VehicleAssignmentService;

public class VehicleNotAvailableException extends RuntimeException{

    public VehicleNotAvailableException(String message){
        super(message);
    }
}
