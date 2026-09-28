package org.example.service;

import org.example.contract.CostCalculationService;
import org.example.contract.VehicleAssignmentService;
import org.example.entity.PackageItem;
import org.example.entity.Shipment;
import org.example.entity.Vehicle;
import org.example.enums.ShipmentStatus;
import org.example.exceptions.ShipmentAlreadyAssignedException;
import org.example.exceptions.ShipmentCancelledException;
import org.example.exceptions.ShipmentNotFoundException;
import org.example.exceptions.UserNotExistException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

public class ShipmentService {

    private final List<Shipment> shipmentList;
    private final TreeMap<UUID, List<Shipment>> userShipments;

    private final CostCalculationService costCalculatorService;
    private final UserService userService;
    private final VehicleAssignmentService vehicleAssignmentService;

    public ShipmentService(UserService userService, CostCalculationService costCalculatorService, VehicleAssignmentService vehicleAssignmentService){
        this.userService = userService;
        this.costCalculatorService = costCalculatorService;
        this.vehicleAssignmentService = vehicleAssignmentService;
        this.shipmentList = new ArrayList<>();
        this.userShipments = new TreeMap<>();
    }

    public Shipment createShipment(UUID userId, List<PackageItem> packages){

        if(!userService.checkUser(userId)){
            throw new UserNotExistException(String.format("User: %s is not exist", userId));
        }

        double totalWeight = packages
                .parallelStream()
                .mapToDouble(PackageItem::getWeight)
                .sum();

        double totalShipmentCost = packages
                .stream()
                .mapToDouble(costCalculatorService::calculatePackageCost)
                .sum();

        UUID shipmentId = UUID.randomUUID();

        Shipment shipment = new Shipment(shipmentId, userId, packages, totalWeight, totalShipmentCost);

        shipmentList.add(shipment);
        userShipments.computeIfAbsent(userId, id -> new ArrayList<>()).add(shipment);

        return shipment;
    }

    public Vehicle assignVehicle(Shipment shipment){

        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new ShipmentCancelledException(
                    String.format("Shipment %s is cancelled and cannot be assigned a vehicle", shipment.getId())
            );
        }

        if (shipment.getAssignedVehicleType() != null) {
            throw new ShipmentAlreadyAssignedException(
                    String.format("Shipment %s already has a vehicle assigned", shipment.getId())
            );
        }

        Vehicle vehicle = vehicleAssignmentService.getVehicle(shipment);
        shipment.assignVehicle(vehicle.getVehicleType());
        return vehicle;
    }

    public Shipment getShipment(UUID shipmentId) {
        for (Shipment shipment : shipmentList) {
            if (shipment.getId().equals(shipmentId)) {
                return shipment;
            }
        }
        throw new ShipmentNotFoundException(String.format("Shipment %s not found", shipmentId));
    }

    public List<Shipment> getUserShipments(UUID userId) {
        if (!userService.checkUser(userId)) {
            throw new UserNotExistException(String.format("User: %s is not exist", userId));
        }
        return new ArrayList<>(userShipments.getOrDefault(userId, Collections.emptyList()));
    }

    public void cancelShipment(UUID shipmentId) {
        getShipment(shipmentId).cancelShipment();
    }

    public void deliverShipment(UUID shipmentId) {
        getShipment(shipmentId).deliverShipment();
    }
}
