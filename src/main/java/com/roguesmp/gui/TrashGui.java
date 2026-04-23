package com.roguesmp.gui;

import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TrashGui extends BaseGui {
    public TrashGui(int row) {
        super(Component.text("Thùng rác", NamedTextColor.RED), row);
    }

    @Override
    public void setup() {

    }

    public static void register() {
        new CommandAPICommand("trash")
                .executesPlayer((sender, args) -> {
                    new TrashGui(3).showInventory(sender);
                })
                .register();
    }
}
