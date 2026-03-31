package com.ignilumen.daysgoby.registry;

import com.ignilumen.daysgoby.Daysgoby;
import com.ignilumen.daysgoby.item.TravelJournalItem;
import com.ignilumen.daysgoby.item.XianSwordItem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Daysgoby.MODID);

    public static final DeferredItem<XianSwordItem> XIAN_SWORD =
            ITEMS.register("xian_sword", () -> new XianSwordItem(new Item.Properties()));

    public static final DeferredItem<Item> LINER_SNIPS =
            ITEMS.registerSimpleItem("liner_snips", new Item.Properties().stacksTo(1));

    public static final DeferredItem<TravelJournalItem> TRAVEL_JOURNAL =
            ITEMS.register("travel_journal", () -> new TravelJournalItem(new Item.Properties()));

    private ModItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
