package org.example.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class Plan {

    private final UUID id;
    private String name;
    private BigDecimal price;
    private Period period;
    private int demoDays;

    public Plan(
            UUID id,
            String name,
            BigDecimal price,
            Period period,
            int demoDays
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.period = period;
        this.demoDays = demoDays;
    }

    public Plan(
            Plan plan
    ) {
        this.id = plan.getId();
        this.name = plan.getName();
        this.price = plan.getPrice();
        this.period = plan.getPeriod();
        this.demoDays = plan.getDemoDays();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Period getPeriod() {
        return period;
    }

    public int getDemoDays() {
        return demoDays;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePrice(BigDecimal price) {
        this.price = price;
    }

    public void updatePeriod(Period period) {
        this.period = period;
    }

    public void updateDemoDays(int demoDays) {
        this.demoDays = demoDays;
    }
}
