package org.example.contract;

import org.example.entity.Shipment;
import org.example.entity.Vehicle;

public interface VehicleAssignmentService {
    Vehicle getVehicle(Shipment shipment);
}
