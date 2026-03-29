package com.ignilumen.daysgoby.item;

import com.mojang.serialization.Codec;

public record ArmorLining(LiningType type) {
    public static final Codec<ArmorLining> CODEC = LiningType.CODEC.xmap(ArmorLining::new, ArmorLining::type);
}