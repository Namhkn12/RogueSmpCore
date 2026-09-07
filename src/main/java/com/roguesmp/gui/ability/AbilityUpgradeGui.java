package com.roguesmp.gui.ability;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AbilityUpgradeGui extends BaseGui {
    private final SmpPlayer smpPlayer;
    private final AbilityInfo<?> info;
    private final int currentLevel;

    public AbilityUpgradeGui(SmpPlayer smpPlayer, AbilityInfo<?> info, int currentLevel) {
        super(Component.text("Xác nhận nâng cấp"), 5);
        this.smpPlayer = smpPlayer;
        this.info = info;
        this.currentLevel = currentLevel;
    }

    @Override
    public void setup() {
        clearUi();
        fillEmpty(FILLER_BLACK);
        int nextLevel = currentLevel + 1;
        List<UpgradeRequirement> requirements = info.getUpgradeRequirement().get(nextLevel);

        if (requirements == null) {
            renderMaxLevel();
        } else {
            renderUpgradePreview(nextLevel, requirements);
        }

        addButton(4, 0, PREV_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            new AbilityDetailGui(smpPlayer, info, currentLevel).showInventory(event.getWhoClicked());
        });
    }

    private void renderUpgradePreview(int nextLevel, List<UpgradeRequirement> requirements) {
        // Same scaling-tag resolver AbilityInfo#getFormattedDescription uses (old -> new
        // comparison when the value changes between levels) - a new :format kind only needs
        // adding once, in AbilityInfo#formatScalingValue, for both to pick it up.
        TagResolver resolvers = TagResolver.resolver(
                info.scalingTagResolver(currentLevel, nextLevel),
                Placeholder.parsed("level", String.valueOf(nextLevel))
        );

        List<Component> previewLore = new ArrayList<>();
        previewLore.add(Utils.text("Xem trước nâng cấp (Cấp " + nextLevel + "):", NamedTextColor.GOLD));
        previewLore.add(Component.empty());

        for (String line : info.getDescription()) {
            previewLore.add(MiniMessage.miniMessage().deserialize(line, resolvers));
        }

        ItemStack previewItem = ItemStack.of(info.getIcon());
        previewItem.setData(DataComponentTypes.ITEM_NAME, info.getFormattedDisplayName());
        previewItem.setData(DataComponentTypes.LORE, ItemLore.lore(previewLore));
        addItem(1, 4, previewItem);

        renderRequirementsAndConfirm(requirements, nextLevel);
    }

    private void renderRequirementsAndConfirm(List<UpgradeRequirement> requirements, int nextLevel) {
        List<Component> costLore = new ArrayList<>();
        costLore.add(Component.empty());

        boolean canAfford = true;
        for (UpgradeRequirement req : requirements) {
            if (!req.canFulfill(smpPlayer)) canAfford = false;
            // Uses your UpgradeRequirement#getDisplay(SmpPlayer)
            costLore.add(req.getDisplay(smpPlayer).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        if (requirements.isEmpty()) costLore.add(Utils.text("Miễn phí", NamedTextColor.GREEN));

        costLore.add(Component.empty());
        costLore.add(Utils.text("Bấm nút bên dưới để nâng cấp!", NamedTextColor.GRAY));

        // Consolidated Requirement Item
        ItemStack reqItem = ItemStack.of(canAfford ? Material.PAPER : Material.BARRIER);
        reqItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Điều kiện nâng cấp", NamedTextColor.YELLOW));
        reqItem.setData(DataComponentTypes.LORE, ItemLore.lore(costLore));
        addItem(3, 4, reqItem);

        // Confirm Button
        ItemStack confirm = ItemStack.of(canAfford ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
        confirm.setData(DataComponentTypes.ITEM_NAME, canAfford ?
                Component.text("NÂNG CẤP", NamedTextColor.GREEN, TextDecoration.BOLD) :
                Component.text("KHÔNG ĐỦ YÊU CẦU", NamedTextColor.RED));

        final boolean finalCanAfford = canAfford;
        addButton(4, 4, confirm, event -> {
            event.setCancelled(true);
            if (!finalCanAfford) return;

            // Execute Upgrade logic
            requirements.forEach(req -> req.consume(smpPlayer));
            smpPlayer.getPlayerData().setAbilityLevel(info.getId(), nextLevel);

            event.getWhoClicked().sendMessage(Component.text("Nâng cấp thành công!", NamedTextColor.GREEN));
            Utils.runLater(() -> new AbilityDetailGui(smpPlayer, info, currentLevel + 1).showInventory(event.getWhoClicked()));
        });
    }

    private void renderMaxLevel() {
        ItemStack max = ItemStack.of(Material.BARRIER);
        max.setData(DataComponentTypes.ITEM_NAME, Component.text("Kĩ năng đã đạt cấp tối đa", NamedTextColor.RED));
        addItem(2, 4, max);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
