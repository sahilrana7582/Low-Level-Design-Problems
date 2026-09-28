package org.example.entity;

import org.example.enums.ShipmentStatus;
import org.example.enums.VehicleType;
import org.example.exceptions.ShipmentAlreadyDeliveredException;
import org.example.exceptions.ShipmentCancelledException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Shipment {

    private final UUID id;
    private final UUID userId;
    private final List<PackageItem> packages;

    private ShipmentStatus status;
    private final double totalWeight;
    private Vehicle assignedVehicle;
    private final double shipmentCost;

    public Shipment(
            UUID id,
            UUID userId,
            List<PackageItem> packages,
            double totalWeight,
            double shipmentCost
    ) {
        this.id = id;
        this.userId = userId;
        this.packages = new ArrayList<>(packages);
        this.totalWeight = totalWeight;
        this.shipmentCost = shipmentCost;
        this.status = ShipmentStatus.CREATED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public List<PackageItem> getPackages() {
        return new ArrayList<>(packages);
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    // Kept for convenience: the type of whatever vehicle is currently assigned, if any.
    public VehicleType getAssignedVehicleType() {
        return assignedVehicle == null ? null : assignedVehicle.getVehicleType();
    }

    public Vehicle getAssignedVehicle() {
        return assignedVehicle;
    }

    public double getShipmentCost() {
        return shipmentCost;
    }

    public void assignVehicle(Vehicle vehicle) {
        this.assignedVehicle = vehicle;
        this.status = ShipmentStatus.VEHICLE_ASSIGNED;
    }

    public void cancelShipment() {

        if (this.status == ShipmentStatus.DELIVERED) {
            throw new ShipmentAlreadyDeliveredException(
                    String.format(
                            "Shipment %s is already delivered and cannot be cancelled",
                            this.id
                    )
            );
        }

        this.status = ShipmentStatus.CANCELLED;
        this.assignedVehicle = null;
    }

    public void deliverShipment() {

        if (this.status == ShipmentStatus.CANCELLED) {
            throw new ShipmentCancelledException(
                    String.format(
                            "Shipment %s is cancelled and cannot be delivered",
                            this.id
                    )
            );
        }

        this.status = ShipmentStatus.DELIVERED;
    }
}