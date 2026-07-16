package com.roguesmp.gui;

import com.roguesmp.gui.enchant.InfusionGui;
import com.roguesmp.gui.gem.GemSocketingGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

// The blacksmith gui: Allow user to access item modifiers fromm a single gui.
public class BlacksmithGui extends BaseGui {

    private final Player player;
    private final SmpPlayer smpPlayer;

    public BlacksmithGui(Player player) {
        super(Component.text("Thợ rèn"), 3);
        this.player = player;
        this.smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
    }

    @Override
    public void setup() {
        fillEmpty(FILLER_BLACK);

        ItemStack repairIcon = ItemStack.of(Material.ANVIL);
        repairIcon.setData(DataComponentTypes.ITEM_NAME, Component.text("Sửa chữa trang bị", NamedTextColor.GREEN));
        addButton(1, 2, repairIcon, event -> {
            ClickHandler.openGui(new ItemRepairGui(player)).onClick(event);
            player.playSound(Sound.sound(SoundEventKeys.BLOCK_AMETHYST_BLOCK_CHIME, Sound.Source.PLAYER, 1f, 1.2f));
        });

        ItemStack infusionIcon = ItemStack.of(Material.ENCHANTED_BOOK);
        infusionIcon.setData(DataComponentTypes.ITEM_NAME, Component.text("Khắc ấn ký trang bị", NamedTextColor.GOLD));
        addButton(1, 4, infusionIcon, event -> {
            ClickHandler.openGui(new InfusionGui(player)).onClick(event);
            player.playSound(Sound.sound(SoundEventKeys.BLOCK_AMETHYST_BLOCK_CHIME, Sound.Source.PLAYER, 1f, 0.8f));
        });

        ItemStack gemSocketIcon = ItemStack.of(Material.AMETHYST_SHARD);
        gemSocketIcon.setData(DataComponentTypes.ITEM_NAME, Component.text("Khảm ngọc trang bị", NamedTextColor.LIGHT_PURPLE));
        addButton(1, 6, gemSocketIcon, event -> {
            ClickHandler.openGui(new GemSocketingGui(player)).onClick(event);
            player.playSound(Sound.sound(SoundEventKeys.BLOCK_AMETHYST_BLOCK_CHIME, Sound.Source.PLAYER, 1f, 0.8f));
        });
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
