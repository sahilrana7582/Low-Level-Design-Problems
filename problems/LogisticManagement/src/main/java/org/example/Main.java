package org.example;

//
//Customers creating shipments.
//A shipment containing one or more packages.
//Pickup from an origin.
//Delivery to a destination.
//Different vehicle types such as Bike, Van, Truck.
//Assigning an appropriate vehicle to a shipment.
//Tracking shipment status.
//Drivers/riders operating vehicles.
//Updating shipment status as it moves through the logistics lifecycle.
//Viewing shipment details and current status.
//Handling successful delivery and failed/cancelled shipments.

import org.example.entity.Driver;
import org.example.entity.PackageItem;
import org.example.entity.Shipment;
import org.example.entity.User;
import org.example.entity.Vehicle;
import org.example.enums.PackageType;
import org.example.enums.ShipmentStatus;
import org.example.enums.VehicleType;
import org.example.exceptions.EmailAlreadyExistException;
import org.example.exceptions.ShipmentAlreadyAssignedException;
import org.example.exceptions.ShipmentAlreadyDeliveredException;
import org.example.exceptions.ShipmentCancelledException;
import org.example.exceptions.ShipmentNotFoundException;
import org.example.exceptions.UserNotExistException;
import org.example.exceptions.VehicleNotAvailableException;
import org.example.service.ShipmentService;
import org.example.service.SimpleCostCalculatorService;
import org.example.service.UserService;
import org.example.service.VehicleService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Main {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        UserService userService = new UserService();
        SimpleCostCalculatorService costCalculatorService = new SimpleCostCalculatorService();
        VehicleService vehicleService = new VehicleService();
        ShipmentService shipmentService = new ShipmentService(userService, costCalculatorService, vehicleService);
        UUID unknownId = UUID.randomUUID();

        // ---------------------------------------------------------------
        section("Users");
        User alice = userService.newUser("Alice", 28, "alice@mail.com");
        User bob = userService.newUser("Bob", 34, "bob@mail.com");
        expect("Alice exists", true, userService.checkUser(alice.getId()));
        expect("unknown user does not exist", false, userService.checkUser(unknownId));
        expectError("same email again", EmailAlreadyExistException.class,
                () -> userService.newUser("Alice Two", 30, "alice@mail.com"));

        // ---------------------------------------------------------------
        section("Driver (wraps a User)");
        Driver aliceDriver = new Driver(alice, "DL-123");
        expect("driver id is assigned", true, aliceDriver.getId() != null);
        expect("driver name comes from the user", "Alice", aliceDriver.getName());
        expect("driver email comes from the user", "alice@mail.com", aliceDriver.getEmail());
        expect("driver age comes from the user", 28, aliceDriver.getAge());
        expect("driver license number", "DL-123", aliceDriver.getLicenseNumber());

        // ---------------------------------------------------------------
        section("Cost calculator: rate per type times quantity");
        expectMoney("GARMENTS x3", 300, costCalculatorService.calculatePackageCost(
                new PackageItem("Shirts", "cotton", 3, PackageType.GARMENTS, 1)));
        expectMoney("ELECTRONICS x2", 1000, costCalculatorService.calculatePackageCost(
                new PackageItem("Phones", "mobiles", 2, PackageType.ELECTRONICS, 1)));
        expectMoney("FOOD x4", 200, costCalculatorService.calculatePackageCost(
                new PackageItem("Snacks", "chips", 4, PackageType.FOOD, 1)));
        expectMoney("MEDICINE x1", 150, costCalculatorService.calculatePackageCost(
                new PackageItem("Tablets", "medicine", 1, PackageType.MEDICINE, 1)));
        expectMoney("DOCUMENTS x5", 150, costCalculatorService.calculatePackageCost(
                new PackageItem("Papers", "documents", 5, PackageType.DOCUMENTS, 1)));

        // ---------------------------------------------------------------
        section("Vehicles: bike 15, van 100, truck 1000");
        Vehicle bike = new Vehicle(UUID.randomUUID(), VehicleType.BIKE);
        Vehicle van = new Vehicle(UUID.randomUUID(), VehicleType.VAN);
        Vehicle truck = new Vehicle(UUID.randomUUID(), VehicleType.TRUCK);
        vehicleService.addVehicle(bike);
        vehicleService.addVehicle(van);
        vehicleService.addVehicle(truck);
        expect("bike remaining capacity", 15.0, bike.getRemainingCapacity());

        // ---------------------------------------------------------------
        section("Create shipments: weight and cost are summed from the packages");
        Shipment s1 = shipmentService.createShipment(alice.getId(), Arrays.asList(
                new PackageItem("Book Set", "paperback", 2, PackageType.DOCUMENTS, 2.0),
                new PackageItem("Phone", "electronics", 1, PackageType.ELECTRONICS, 1.0)
        ));
        expect("s1 status starts as CREATED", ShipmentStatus.CREATED, s1.getStatus());
        expectMoney("s1 total weight", 3.0, s1.getTotalWeight());
        expectMoney("s1 total cost (30x2 + 500x1)", 560, s1.getShipmentCost());

        Shipment s2 = shipmentService.createShipment(bob.getId(), Collections.singletonList(
                new PackageItem("Snacks", "food", 10, PackageType.FOOD, 50.0)
        ));
        Shipment s3 = shipmentService.createShipment(bob.getId(), Collections.singletonList(
                new PackageItem("Rice Bags", "food", 8, PackageType.FOOD, 40.0)
        ));
        Shipment s4 = shipmentService.createShipment(alice.getId(), Collections.singletonList(
                new PackageItem("Furniture", "wood", 1, PackageType.GARMENTS, 15.0)
        ));
        Shipment s5 = shipmentService.createShipment(alice.getId(), Collections.singletonList(
                new PackageItem("Container", "cargo", 1, PackageType.GARMENTS, 2000.0)
        ));
        expectError("create a shipment for an unknown user", UserNotExistException.class,
                () -> shipmentService.createShipment(unknownId, Collections.emptyList()));

        // ---------------------------------------------------------------
        section("Assign vehicle: the smallest vehicle that still fits is chosen");
        Vehicle assignedToS1 = shipmentService.assignVehicle(s1); // weight 3 -> bike (15)
        expect("s1 gets the bike", VehicleType.BIKE, assignedToS1.getVehicleType());
        expect("s1 status is now VEHICLE_ASSIGNED", ShipmentStatus.VEHICLE_ASSIGNED, s1.getStatus());
        expectMoney("bike remaining after s1 (15 - 3)", 12.0, bike.getRemainingCapacity());

        Vehicle assignedToS2 = shipmentService.assignVehicle(s2); // weight 50 -> bike(12) too small, van(100) fits
        expect("s2 gets the van (bike is too small)", VehicleType.VAN, assignedToS2.getVehicleType());
        expectMoney("van remaining after s2 (100 - 50)", 50.0, van.getRemainingCapacity());

        Vehicle assignedToS3 = shipmentService.assignVehicle(s3); // weight 40 -> bike(12) too small, van(50) fits
        expect("s3 also gets the van, not the truck", VehicleType.VAN, assignedToS3.getVehicleType());
        expectMoney("van remaining after s3 (50 - 40)", 10.0, van.getRemainingCapacity());

        Vehicle assignedToS4 = shipmentService.assignVehicle(s4); // weight 15 -> bike(12) and van(10) too small
        expect("s4 needs the truck (bike and van are both too small)", VehicleType.TRUCK, assignedToS4.getVehicleType());
        expectMoney("truck remaining after s4 (1000 - 15)", 985.0, truck.getRemainingCapacity());

        expectError("s5 (weight 2000) fits no vehicle", VehicleNotAvailableException.class,
                () -> shipmentService.assignVehicle(s5));
        expect("s5 status is unchanged after the failed assignment", ShipmentStatus.CREATED, s5.getStatus());

        // ---------------------------------------------------------------
        section("Assign vehicle: repeat rules");
        expectError("assign a vehicle to s1 again", ShipmentAlreadyAssignedException.class,
                () -> shipmentService.assignVehicle(s1));

        Shipment s6 = shipmentService.createShipment(bob.getId(), Collections.singletonList(
                new PackageItem("Letter", "mail", 1, PackageType.DOCUMENTS, 0.5)
        ));
        shipmentService.cancelShipment(s6.getId());
        expectError("assign a vehicle to a cancelled shipment", ShipmentCancelledException.class,
                () -> shipmentService.assignVehicle(s6));

        // ---------------------------------------------------------------
        section("Cancel shipment");
        Shipment s7 = shipmentService.createShipment(alice.getId(), Collections.singletonList(
                new PackageItem("Gift", "box", 1, PackageType.GARMENTS, 1.0)
        ));
        shipmentService.cancelShipment(s7.getId());
        expect("s7 status is CANCELLED", ShipmentStatus.CANCELLED, s7.getStatus());
        expect("cancelling twice does not error (idempotent)", ShipmentStatus.CANCELLED, s7.getStatus());
        shipmentService.cancelShipment(s7.getId());

        shipmentService.deliverShipment(s4.getId());
        expect("s4 status is DELIVERED", ShipmentStatus.DELIVERED, s4.getStatus());
        expectError("cancel an already-delivered shipment", ShipmentAlreadyDeliveredException.class,
                () -> shipmentService.cancelShipment(s4.getId()));
        expectError("cancel an unknown shipment id", ShipmentNotFoundException.class,
                () -> shipmentService.cancelShipment(unknownId));

        // ---------------------------------------------------------------
        section("Deliver shipment");
        expectError("deliver an unknown shipment id", ShipmentNotFoundException.class,
                () -> shipmentService.deliverShipment(unknownId));
        expectError("deliver a cancelled shipment", ShipmentCancelledException.class,
                () -> shipmentService.deliverShipment(s7.getId()));
        // Known gap, not enforced by the existing design: a shipment can be delivered
        // straight from CREATED, with no vehicle ever assigned.
        shipmentService.deliverShipment(s5.getId());
        expect("s5 (never assigned a vehicle) can still be delivered", ShipmentStatus.DELIVERED, s5.getStatus());

        // ---------------------------------------------------------------
        section("Viewing shipments");
        expect("getShipment returns s2", s2.getId(), shipmentService.getShipment(s2.getId()).getId());
        expectError("getShipment of an unknown id", ShipmentNotFoundException.class,
                () -> shipmentService.getShipment(unknownId));
        expectError("getUserShipments of an unknown user", UserNotExistException.class,
                () -> shipmentService.getUserShipments(unknownId));

        List<Shipment> aliceShipments = shipmentService.getUserShipments(alice.getId());
        expect("Alice's shipments", Arrays.asList(s1.getId(), s4.getId(), s5.getId(), s7.getId()), shipmentIds(aliceShipments));
        List<Shipment> bobShipments = shipmentService.getUserShipments(bob.getId());
        expect("Bob's shipments", Arrays.asList(s2.getId(), s3.getId(), s6.getId()), shipmentIds(bobShipments));
        aliceShipments.clear();
        expect("clearing the returned list changes nothing", 4, shipmentService.getUserShipments(alice.getId()).size());

        // ---------------------------------------------------------------
        System.out.println();
        System.out.println("=== Result: " + passed + " passed, " + failed + " failed ===");
    }

    // ----- small helpers -----

    private static List<UUID> shipmentIds(List<Shipment> shipments) {
        return shipments.stream().map(Shipment::getId).toList();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void expect(String scenario, Object expected, Object actual) {
        record(Objects.equals(expected, actual), scenario, String.valueOf(expected), String.valueOf(actual));
    }

    private static void expectMoney(String scenario, double expected, double actual) {
        record(Double.compare(expected, actual) == 0, scenario, String.valueOf(expected), String.valueOf(actual));
    }

    // Runs the action and expects exactly this exception type
    private static void expectError(String scenario, Class<? extends RuntimeException> expected, Runnable action) {
        String actual;
        boolean ok;
        try {
            action.run();
            actual = "no exception";
            ok = false;
        } catch (RuntimeException e) {
            actual = e.getClass().getSimpleName() + " (" + e.getMessage() + ")";
            ok = e.getClass().equals(expected);
        }
        record(ok, scenario, expected.getSimpleName(), actual);
    }

    private static void record(boolean ok, String scenario, String expected, String actual) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println("  " + (ok ? "PASS" : "FAIL") + " | " + scenario
                + " | expected: " + expected + " | actual: " + actual);
    }
}
