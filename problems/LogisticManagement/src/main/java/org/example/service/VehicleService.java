package org.example.service;

import org.example.contract.VehicleAssignmentService;
import org.example.entity.Shipment;
import org.example.entity.Vehicle;
import org.example.exceptions.VehicleNotAvailableException;
import org.example.exceptions.VehicleNotExistException;

import java.util.Comparator;
import java.util.Optional;
import java.util.TreeSet;

public class VehicleService implements VehicleAssignmentService {
    private final TreeSet<Vehicle> availableVehicles =
            new TreeSet<>(
                    Comparator.comparingDouble(Vehicle::getRemainingCapacity)
                            .thenComparing(Vehicle::getId)
            );

    public void addVehicle(Vehicle vehicle) {
        availableVehicles.add(vehicle);
    }

    @Override
    public void freeWeight(Vehicle vehicle, double weight) {
        Optional<Vehicle> veh = availableVehicles.stream()
                .filter(v -> v.getId().equals(vehicle.getId()))
                .findFirst();

        if(veh.isEmpty()){
            throw new VehicleNotExistException("Vehicle Not Exist: " + vehicle.getId());
        }

        Vehicle vehicle1 = veh.get();

        // Same remove-then-reinsert as getVehicle: the TreeSet orders by remaining capacity,
        // so the vehicle must come out before that value changes and go back in after.
        availableVehicles.remove(vehicle1);
        vehicle1.releaseWeight(weight);
        availableVehicles.add(vehicle1);
    }


    @Override
    public Vehicle getVehicle(Shipment shipment) {

        double requiredWeight = shipment.getTotalWeight();
        Vehicle chosen = null;

        for (Vehicle vehicle : availableVehicles) {

            if (vehicle.getRemainingCapacity() >= requiredWeight) {
                chosen = vehicle;
                break;
            }
        }

        if (chosen == null) {
            throw new VehicleNotAvailableException(
                    "No available vehicle can accommodate shipment "
                            + shipment.getId()
            );
        }

        // The TreeSet orders by remaining capacity, so the vehicle must come out before its
        // weight changes and go back in after, otherwise the set's ordering breaks.
        availableVehicles.remove(chosen);
        chosen.addWeight(requiredWeight);
        availableVehicles.add(chosen);

        return chosen;
    }
}
