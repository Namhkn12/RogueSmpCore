package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandExecutorComponent implements ItemComponent, InteractableComponent {

    public static final Codec<CommandExecutorComponent> CODEC = Codec.STRING.xmap(CommandExecutorComponent::new, CommandExecutorComponent::getBaseCmd);

    private static final NamespacedKey KEY = Keys.of("set_command");

    private final String baseCmd;

    private String loadedCommand;

    public CommandExecutorComponent(String baseCmd) {
        this.baseCmd = Objects.requireNonNullElse(baseCmd, "");

    }

    @Override
    public @NotNull ItemComponent copy() {
        return new CommandExecutorComponent(baseCmd);
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Chuột phải để chạy lệnh, chuột trái để đặt lệnh ", NamedTextColor.GRAY));
        lore.add(Utils.text("Lệnh hiện tại: ", NamedTextColor.GRAY).append(Component.text(loadedCommand, NamedTextColor.AQUA)));
        context.builder().putLines(67, lore);
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        String savedCmd = pdc.get(KEY, PersistentDataType.STRING);
        if (savedCmd == null) {
            loadedCommand = baseCmd;
        } else loadedCommand = savedCmd;
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        pdc.set(KEY, PersistentDataType.STRING, loadedCommand);
    }

    @Override
    public void onItemInteract(SmpPlayer smpPlayer, PlayerInteractEvent event) {
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            openSetCommandDialog(event.getItem(), event.getPlayer());
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.getPlayer().performCommand(loadedCommand);
        }
    }

    public String getBaseCmd() {
        return baseCmd;
    }

    public String getLoadedCommand() {
        return loadedCommand;
    }

    private void openSetCommandDialog(ItemStack neededToSet, Player player) {
        Dialog dialog = DialogBuilder.create(Component.text("Command"))
                .addTextInput("cmd", Component.text("Command không dấu /"))
                .confirmation()
                .yesButton(Component.text("Đặt làm command mới"), null,
                        (response, audience) -> {
                            String input = response.getText("cmd");
                            if (input == null) {
                                loadedCommand = baseCmd;
                            } else {
                                loadedCommand = input;
                            }
                            ItemStack currentItem = player.getEquipment().getItemInMainHand();
                            if (currentItem.equals(neededToSet)) {
                                currentItem.editPersistentDataContainer(CommandExecutorComponent.this::save);
                                player.sendMessage(Component.text("Đặt lệnh mới thành công: ", NamedTextColor.GREEN).append(Component.text(loadedCommand, NamedTextColor.AQUA)));
                            } else {
                                player.sendMessage(Component.text("Vật phẩm trên tay bạn đã thay đổi khi menu này được mở, nên lệnh mới không được đặt!", NamedTextColor.RED));
                            }
                            audience.closeDialog();
                        })
                .noButton(Component.text("Thôi không đặt nữa"), null, (response, audience) -> audience.closeDialog())
                .build();

        player.showDialog(dialog);
    }
}
