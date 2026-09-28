package org.example.entity;

import org.example.entity.Cappuccino;
import org.example.entity.Coffee;
import org.example.entity.Expresso;
import org.example.entity.Latte;
import org.example.enums.CoffeeType;

public final class CoffeeFactory {

    private CoffeeFactory() {
    }

    public static Coffee createCoffee(String name, CoffeeType coffeeType) {

        return switch (coffeeType) {
            case Latte -> new Latte(name);
            case Cappuccino -> new Cappuccino(name);
            case Black_Coffee -> new Expresso(name);
            default -> throw new IllegalArgumentException(
                    "Basic coffee is not supported yet"
            );
        };
    }
}