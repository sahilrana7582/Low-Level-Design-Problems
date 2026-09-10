package entity;

import enums.RoomType;

import java.math.BigDecimal;
import java.util.Objects;

public class Room {

    private final String id;
    private final String name;
    private final RoomType type;
    private final BigDecimal price;

    public Room(
            String id,
            String name,
            RoomType type,
            BigDecimal price
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.price = Objects.requireNonNull(price);

        if (price.signum() < 0) {
            throw new IllegalArgumentException(
                    "Room price cannot be negative"
            );
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RoomType getType() {
        return type;
    }

    public BigDecimal getPrice() {
        return price;
    }
}