package com.roguesmp.gui.dungeon;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TrialSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.List;
import java.util.function.Consumer;

public class BossTriggerGui extends BaseGui {

    private final Block block;
    private final Consumer<Player> onTrigger;

    public BossTriggerGui(Block block, Consumer<Player> onTrigger) {
        super(Component.text("Boss"), 3);
        this.block = block;
        this.onTrigger = onTrigger;
    }

    @Override
    public void setup() {
        clearUi();
        addButton(13, DungeonDetailGui.button(Material.TRIAL_SPAWNER, "<red><b>Triệu hồi boss", List.of(Utils.fromString("<yellow>▶ Click để bắt đầu"))), event -> {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            player.closeInventory();
            onTrigger.accept(player);
        });
        fillEmpty(createDecoration(Material.RED_STAINED_GLASS_PANE));
    }

    @Override
    public void onOpenInventory(InventoryOpenEvent event) {
        if (!(block.getState() instanceof TrialSpawner spawner)) return;
        if (spawner.isOminous()) {
            event.setCancelled(true);
            return;
        }
        spawner.setOminous(true);
        spawner.update();
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (!(block.getState() instanceof TrialSpawner spawner)) return;
        spawner.setOminous(false);
        spawner.update();
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
