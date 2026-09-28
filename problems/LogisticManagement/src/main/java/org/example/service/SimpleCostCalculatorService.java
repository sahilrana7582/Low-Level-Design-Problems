package org.example.service;

import org.example.contract.CostCalculationService;
import org.example.entity.PackageItem;

public class SimpleCostCalculatorService implements CostCalculationService {

    private static final double GARMENTS_COST = 100;
    private static final double ELECTRONICS_COST = 500;
    private static final double FOOD_COST = 50;
    private static final double MEDICINE_COST = 150;
    private static final double DOCUMENTS_COST = 30;


    @Override
    public double calculatePackageCost(PackageItem packageItem) {

        double costPerUnit = switch (packageItem.getType()) {

            case GARMENTS -> GARMENTS_COST;
            case ELECTRONICS -> ELECTRONICS_COST;
            case FOOD -> FOOD_COST;
            case MEDICINE -> MEDICINE_COST;
            case DOCUMENTS -> DOCUMENTS_COST;
        };

        return costPerUnit * packageItem.getQuantity();
    }

}