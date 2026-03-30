package com.ignilumen.daysgoby.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Function;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ArmorLining(LiningType type, ItemStack linerStack) {
    private static final Codec<ArmorLining> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LiningType.CODEC.fieldOf("type").forGetter(ArmorLining::type),
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("liner_stack").forGetter(ArmorLining::linerStack)
    ).apply(instance, ArmorLining::new));

    public static final Codec<ArmorLining> CODEC = Codec.either(FULL_CODEC, LiningType.CODEC)
            .xmap(
                    either -> either.map(Function.identity(), ArmorLining::new),
                    lining -> lining.hasStoredLiner() ? Either.left(lining) : Either.right(lining.type())
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorLining> STREAM_CODEC = StreamCodec.of(
            (buffer, lining) -> {
                buffer.writeEnum(lining.type());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, lining.linerStack());
            },
            buffer -> new ArmorLining(buffer.readEnum(LiningType.class), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer))
    );

    public ArmorLining(LiningType type) {
        this(type, ItemStack.EMPTY);
    }

    public ArmorLining {
        if (type == null) {
            throw new IllegalArgumentException("Armor lining type cannot be null");
        }

        if (linerStack == null || linerStack.isEmpty()) {
            linerStack = ItemStack.EMPTY;
        } else {
            linerStack = linerStack.copy();
            linerStack.setCount(1);
        }
    }

    public boolean hasStoredLiner() {
        return !linerStack.isEmpty();
    }
}
