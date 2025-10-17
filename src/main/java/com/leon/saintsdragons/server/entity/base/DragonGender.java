package com.leon.saintsdragons.server.entity.base;

/**
 * Simple gender enum for dragons.
 * Uses a byte id so we can extend beyond two options later if needed.
 */
public enum DragonGender {
    MALE((byte) 0),
    FEMALE((byte) 1);

    private final byte id;

    DragonGender(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static DragonGender fromId(byte id) {
        for (DragonGender gender : values()) {
            if (gender.id == id) {
                return gender;
            }
        }
        return MALE;
    }
}
