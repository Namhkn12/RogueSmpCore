package com.roguesmp.gui.enchant;

import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.enchant.Infusion;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.modification.InfusionUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InfusionGui extends BaseGui {

    private static final int INPUT_ROW = 1, INPUT_COL = 1;

    private static final int GRID_START_ROW = 1;
    private static final int GRID_START_COL = 3;
    private static final int GRID_WIDTH = 5;

    private static final int MAX_INFUSION_LEVEL = 4; // Max Cap is Level 4

    private final Player player;
    private final SmpPlayer smpPlayer;
    private ItemStack targetItemStack = null;

    public InfusionGui(Player player) {
        super(Component.text("Cường Hóa Ấn Ký (Infusion)", NamedTextColor.DARK_PURPLE), 3);
        this.player = player;
        this.smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
    }

    @Override
    public void setup() {
        clearUi();

        // Render input item slot
        if (targetItemStack != null) {
            addButton(INPUT_ROW, INPUT_COL, targetItemStack, this::handleTakeOut);
        } else {
            ItemStack standIn = ItemStack.of(Material.ARMOR_STAND);
            standIn.setData(DataComponentTypes.ITEM_NAME, Component.text("Click trang bị trong túi để đưa vào", NamedTextColor.GREEN));
            addButton(INPUT_ROW, INPUT_COL, standIn, ClickHandler.noAction());
        }

        // Handle option display based on item status
        if (targetItemStack == null) {
            ItemStack hint = new ItemStack(Material.BARRIER);
            hint.setData(DataComponentTypes.ITEM_NAME, Component.text("Hãy bỏ trang bị vào trước!", NamedTextColor.RED));
            addButton(1, 5, hint, ClickHandler.noAction());
        } else {
            SmpItem smpItem = SmpItem.wrap(targetItemStack);

            if (!InfusionUtil.isEnchantable(smpItem)) {
                ItemStack invalid = new ItemStack(Material.BARRIER);
                invalid.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang bị này không thể khảm Ấn Ký!", NamedTextColor.RED));
                addButton(1, 5, invalid, ClickHandler.noAction());
            } else {
                Infusion activeInfusion = InfusionUtil.getActiveInfusion(smpItem);

                if (activeInfusion != null) {
                    renderOption(smpItem, activeInfusion, 1, 5);
                    renderRemovalOption(smpItem, activeInfusion, 2, 5);
                } else {
                    Infusion[] totalOptions = Infusion.values();

                    for (int i = 0; i < totalOptions.length; i++) {
                        int targetRow = GRID_START_ROW + (i / GRID_WIDTH);
                        int targetCol = GRID_START_COL + (i % GRID_WIDTH);

                        if (targetRow > 2) break;

                        renderOption(smpItem, totalOptions[i], targetRow, targetCol);
                    }
                }
            }
        }

        fillEmpty(FILLER_BLACK);
    }

    private void renderOption(SmpItem smpItem, Infusion infusion, int row, int col) {
        SmpEnchant smpEnchant = infusion.getSmpEnchant().getEnchant();
        if (smpEnchant == null) return;

        int currentLevel = InfusionUtil.getLevel(smpItem, infusion);
        boolean isMaxLevel = currentLevel >= MAX_INFUSION_LEVEL;

        ItemStack optionItem = new ItemStack(isMaxLevel ? Material.BEDROCK : smpEnchant.getIcon());
        optionItem.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());

        if (isMaxLevel) {
            optionItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Ấn Ký: " + smpEnchant.getSimpleName() + " [ĐẠT CẤP TỐI ĐA]", NamedTextColor.GOLD, TextDecoration.BOLD));
        } else {
            optionItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Nâng cấp Ấn Ký: " + smpEnchant.getSimpleName(), NamedTextColor.GOLD, TextDecoration.BOLD));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text(smpEnchant.getSimpleDescription(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Utils.text("Cấp độ hiện tại: ", NamedTextColor.GRAY).append(Component.text(currentLevel == 0 ? "Chưa có" : String.valueOf(currentLevel), NamedTextColor.YELLOW)));

        if (isMaxLevel) {
            lore.add(Utils.text("Ấn ký này đã đạt giới hạn cấp độ cao nhất (" + MAX_INFUSION_LEVEL + ").", NamedTextColor.RED));
            optionItem.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

            addButton(getSlot(row, col), optionItem, ClickHandler.noAction());
            return;
        }

        int nextLevel = currentLevel + 1;
        int expCost = InfusionUtil.getUpgradeExpCost(smpItem, infusion);
        int materialCost = InfusionUtil.getUpgradeMaterialCost(smpItem, infusion);
        int playerExp = PlayerUtils.getExp(player);

        lore.add(Utils.text("Nâng cấp lên Cấp: ", NamedTextColor.GRAY).append(Component.text(nextLevel, NamedTextColor.GREEN)));

        if (currentLevel == 0) {
            lore.add(Component.empty());
            lore.add(Utils.text("Chú ý: Mỗi trang bị chỉ có thể sở hữu duy nhất 1 loại Ấn Ký!", NamedTextColor.RED));
        }

        lore.add(Component.empty());
        lore.add(Utils.text("Chi phí nâng cấp:", NamedTextColor.AQUA));

        boolean hasEnoughExp = playerExp >= expCost;
        NamedTextColor expColor = hasEnoughExp ? NamedTextColor.GREEN : NamedTextColor.RED;
        lore.add(Utils.text("- Kinh Nghiệm: ", NamedTextColor.GRAY).append(Component.text(Utils.formatMoney(expCost), expColor))
                .append(Utils.fromString(" <gray>(Hiện có: <green>" + Utils.formatMoney(playerExp) + "<gray>)")));

        BaseItem currencyItem = ItemRegistry.getInstance().getBaseItem(infusion.getCostItemId());
        String currencyName = "UNKNOWN";
        if (currencyItem != null) {
            NameComponent nameComponent = currencyItem.getComponent(ItemComponentKeys.ITEM_NAME);
            currencyName = (nameComponent != null) ? nameComponent.value() : currencyItem.getId();
        }

        int playerCurrentMaterials = PlayerUtils.countItemsInInventory(player, infusion.getCostItemId());
        boolean hasEnoughMat = playerCurrentMaterials >= materialCost;
        NamedTextColor matColor = hasEnoughMat ? NamedTextColor.GREEN : NamedTextColor.RED;

        lore.add(Utils.fromString("<!i><gray>- </gray>" + currencyName + ": ")
                .append(Component.text(materialCost, matColor)).append(Utils.fromString(" <gray>(Hiện có: <green>" + Utils.formatMoney(playerCurrentMaterials) + "<gray>)")));

        lore.add(Component.empty());
        if (hasEnoughMat && hasEnoughExp) {
            lore.add(Utils.text("Click để nâng cấp!", NamedTextColor.GREEN));
        } else {
            lore.add(Utils.text("Không đủ nguyên liệu!", NamedTextColor.RED));
        }

        optionItem.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        addButton(getSlot(row, col), optionItem, event -> {
            event.setCancelled(true);
            executeInfusionClick(smpItem, infusion);
        });
    }

    private void renderRemovalOption(SmpItem smpItem, Infusion infusion, int row, int col) {
        ItemStack removalButton = new ItemStack(Material.REDSTONE_ORE);
        removalButton.setData(DataComponentTypes.ITEM_NAME, Component.text("Hạ Cấp Ấn Ký (Giảm 1 Cấp)", NamedTextColor.RED, TextDecoration.BOLD));

        int currentLevel = InfusionUtil.getLevel(smpItem, infusion);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Utils.text("Hành động này sẽ giảm cấp độ Ấn Ký hiện tại xuống 1 bậc.", NamedTextColor.GRAY));
        lore.add(Utils.text("Cấp độ sau khi hạ: ", NamedTextColor.GRAY).append(Component.text(currentLevel - 1, NamedTextColor.YELLOW)));
        lore.add(Utils.text("Sẽ hoàn trả 50% kinh nghiệm và toàn bộ nguyên liệu.", NamedTextColor.GRAY));

        removalButton.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        addButton(getSlot(row, col), removalButton, event -> {
            event.setCancelled(true);
            executeRemovalClick(smpItem, infusion);
        });
    }

    private void executeInfusionClick(SmpItem smpItem, Infusion infusion) {
        InfusionUtil.OperationStatus status = InfusionUtil.executeUpgrade(player, smpItem, infusion, MAX_INFUSION_LEVEL);

        switch (status) {
            case SUCCESS -> {
                int originalAmount = targetItemStack.getAmount();
                // Regenerate matching physical stack output
                this.targetItemStack = smpItem.generateItemStack(smpPlayer, originalAmount);

                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
                player.spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            }
            case ERROR_NOT_ENCHANTABLE -> {
                player.sendMessage(Component.text("Vật phẩm này không hỗ trợ hệ thống Ấn Ký!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            case ERROR_OTHER_INFUSION_ACTIVE -> {
                player.sendMessage(Component.text("Vật phẩm đã sở hữu một Ấn Ký khác!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            case ERROR_INSUFFICIENT_EXP -> {
                player.sendMessage(Component.text("Bạn không đủ cấp độ kinh nghiệm!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            case ERROR_INSUFFICIENT_MATERIALS -> {
                player.sendMessage(Component.text("Bạn không có đủ nguyên liệu cần thiết!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            case ERROR_MAX_LEVEL -> {
                player.sendMessage(Component.text("Ấn ký đã đạt cấp độ tối đa!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        }
        setup();
    }

    private void executeRemovalClick(SmpItem smpItem, Infusion infusion) {
        InfusionUtil.OperationStatus status = InfusionUtil.executeDowngrade(player, smpItem, infusion);

        if (status == InfusionUtil.OperationStatus.SUCCESS) {
            int originalAmount = targetItemStack.getAmount();
            this.targetItemStack = smpItem.generateItemStack(smpPlayer, originalAmount);

            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.2f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
        setup();
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (!ItemStackUtils.isValidItem(clicked)) return;

        if (targetItemStack == null) {
            targetItemStack = clicked.clone();
            targetItemStack.setAmount(1);
            clicked.setAmount(clicked.getAmount() - 1);
            setup();
        }
    }

    private void handleTakeOut(InventoryClickEvent event) {
        event.setCancelled(true);
        if (targetItemStack == null) return;

        PlayerUtils.giveItem(player, targetItemStack);
        targetItemStack = null;
        setup();
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (targetItemStack != null) {
            PlayerUtils.giveItem(player, targetItemStack);
            targetItemStack = null;
        }
    }
}
