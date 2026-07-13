package com.roguesmp.gui.enchant;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InfusionGui extends BaseGui {

    private final Player player;
    private final SmpPlayer smpPlayer;

    public InfusionGui(Player player) {
        super(Component.text("Phù phép", NamedTextColor.DARK_PURPLE), 6);
        this.player = player;
        this.smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
    }

    @Override
    public void setup() {

    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        super.onClickBottomInventory(event);
    }
}
