package com.ignilumen.daysgoby.item;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum LiningType implements StringRepresentable {
    WARMING("warming"),
    COOLING("cooling");

    public static final Codec<LiningType> CODEC = StringRepresentable.fromEnum(LiningType::values);

    private final String serializedName;

    LiningType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}