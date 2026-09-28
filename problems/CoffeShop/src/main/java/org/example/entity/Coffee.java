package org.example.entity;

import org.example.enums.CoffeeType;

public interface Coffee {
    void prepare();
    String name();
    CoffeeType getCoffeeType();
}
