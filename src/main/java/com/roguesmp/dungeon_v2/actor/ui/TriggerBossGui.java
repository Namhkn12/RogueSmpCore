package com.roguesmp.dungeon_v2.actor.ui;

import com.roguesmp.gui.BaseGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TrialSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import java.util.function.Consumer;
import static com.roguesmp.dungeon_v2.utils.HiddenItemBuilder.makeHidden;

public class TriggerBossGui extends BaseGui {

    private static final int TOTAL_ROW = 3;
    private static final int TRIGGER_SLOT = 13;

    private final ItemStack filler;
    private final ItemStack trigger;
    private final Block block;
    private final Consumer<Player> onTrigger;

    public TriggerBossGui(Block block, Consumer<Player> onTrigger) {
        super(Component.text("Boss"), TOTAL_ROW);

        filler = makeHidden(ItemStack.of(Material.RED_STAINED_GLASS_PANE));
        trigger = makeHidden(ItemStack.of(Material.TRIAL_SPAWNER));
        this.onTrigger = onTrigger;
        this.block = block;
    }

    @Override
    public void setup() {
        fillEmpty(filler);

        addButton(TRIGGER_SLOT, trigger, event -> {
            Player player = (Player) event.getWhoClicked();
            event.setCancelled(true);
            player.closeInventory();
            onTrigger.accept(player);
        });
    }

    @Override
    public void onOpenInventory(InventoryOpenEvent event) {
        BlockState state = block.getState();

        if (!(state instanceof TrialSpawner spawner)) return;

        if (spawner.isOminous()) {
            event.setCancelled(true);
            return;
        }

        spawner.setOminous(true);
        spawner.update();
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        BlockState state = block.getState();

        if (!(state instanceof TrialSpawner spawner)) return;

        spawner.setOminous(false);
        spawner.update();
    }
}
