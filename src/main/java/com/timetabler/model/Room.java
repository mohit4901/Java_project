package com.timetabler.model;

import java.util.Objects;

/**
 * Represents a physical teaching location (lecture hall, classroom, or lab).
 */
public class Room {
    private final String id;
    private final String name;
    private final int capacity;
    private final RoomType type;

    public Room(String id, String name, int capacity, RoomType type) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public RoomType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return id.equals(room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + type.getLabel() + ", Cap: " + capacity + ")";
    }
}
