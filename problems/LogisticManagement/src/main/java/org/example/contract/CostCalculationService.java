package org.example.contract;

import org.example.entity.PackageItem;

public interface CostCalculationService {
    double calculatePackageCost(PackageItem packageItem);
}
