package com.roguesmp.utils;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.constant.TransferMode;
import com.roguesmp.gui.MachineGui;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

public class MachineTransferUtils {

    // Bộ nhớ tạm để lưu vị trí (index) chia bài của từng máy, giúp chia đều (Round-Robin) qua các tick
    // WeakHashMap tự động xóa data khi SmpMachine bị đập đi (tránh leak memory)
    private static final WeakHashMap<SmpMachine, Integer> pushRoundRobinMap = new WeakHashMap<>();

    // Lớp nội bộ để gom nhóm mục tiêu (Target) cho dễ xử lý
    private static class TargetInfo {
        Inventory inv;
        int[] slots;
        TargetInfo(Inventory inv, int[] slots) {
            this.inv = inv;
            this.slots = slots;
        }
    }

    public static void processMachineTransfers(SmpMachine machine, Location loc, BlockManager manager) {
        Block currentBlock = loc.getBlock();

        List<TargetInfo> pushTargets = new ArrayList<>();
        List<TargetInfo> pullTargets = new ArrayList<>();
        List<TargetInfo> smartPullTargets = new ArrayList<>();

        // 1. QUÉT 6 HƯỚNG VÀ PHÂN LOẠI CÁC TARGET
        for (BlockFace face : SmpMachine.FACES) {
            TransferMode mode = machine.getTransferMode(face);
            if (mode == TransferMode.NONE) continue;

            Block targetBlock = currentBlock.getRelative(face);
            Inventory targetInv = null;
            int[] targetInputSlots = null;
            int[] targetOutputSlots = null;

            SmpBlock targetSmp = manager.getBlock(targetBlock.getLocation());
            if (targetSmp instanceof SmpMachine) {
                SmpMachine targetMachine = (SmpMachine) targetSmp;
                targetInv = targetMachine.getGui().getInventory();
                MachineGui machineGui = (MachineGui) targetMachine.getGui();
                targetInputSlots = machineGui.getInputSlots();
                targetOutputSlots = machineGui.getOutputSlots();
            } else if (targetBlock.getState() instanceof Container) {
                targetInv = ((Container) targetBlock.getState()).getInventory();
            }

            if (targetInv != null) {
                if (mode == TransferMode.AUTO_PUSH) pushTargets.add(new TargetInfo(targetInv, targetInputSlots));
                else if (mode == TransferMode.AUTO_PULL) pullTargets.add(new TargetInfo(targetInv, targetOutputSlots));
                else if (mode == TransferMode.SMART_PULL) smartPullTargets.add(new TargetInfo(targetInv, targetOutputSlots));
            }
        }

        // 2. THỰC THI (CHỈ CHẠY NẾU CÓ TARGET)
        if (!pushTargets.isEmpty()) doRoundRobinPush(machine, pushTargets);
        if (!pullTargets.isEmpty()) doPull(machine, pullTargets, false);
        if (!smartPullTargets.isEmpty()) doPull(machine, smartPullTargets, true);
    }

    // --- LOGIC PUSH (ROUND ROBIN CHIA ĐỀU) ---
    private static void doRoundRobinPush(SmpMachine sourceMachine, List<TargetInfo> targets) {
        Inventory sourceInv = sourceMachine.getGui().getInventory();
        int startIndex = pushRoundRobinMap.getOrDefault(sourceMachine, 0);

        for (int slot : ( (MachineGui) sourceMachine.getGui()).getOutputSlots()) {
            ItemStack item = sourceInv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;

            int targetCount = targets.size();
            int failedConsecutive = 0; // Đếm số lần nhét xịt liên tiếp (để biết là các rương đều đã full)

            // Thuật toán chia bài: Nhét từng 1 item vào các rương xen kẽ nhau
            while (item.getAmount() > 0 && failedConsecutive < targetCount) {
                TargetInfo target = targets.get(startIndex % targetCount);

                // Tách 1 item ra để nhét thử
                ItemStack oneItem = item.clone();
                oneItem.setAmount(1);

                ItemStack left = moveItem(oneItem, target.inv, target.slots);

                if (left == null || left.getType().isAir() || left.getAmount() == 0) {
                    // Thành công -> Trừ item gốc đi 1, reset bộ đếm lỗi
                    item.setAmount(item.getAmount() - 1);
                    failedConsecutive = 0;
                } else {
                    // Thất bại (Rương này đầy)
                    failedConsecutive++;
                }

                // Bất kể thành công hay thất bại, vẫn xoay tua sang rương tiếp theo
                startIndex++;
            }

            if (item.getAmount() <= 0) {
                sourceInv.setItem(slot, null);
            } else {
                sourceInv.setItem(slot, item);
            }
        }

        // Lưu lại vị trí đang chia dở để tick sau chia tiếp cho công bằng
        pushRoundRobinMap.put(sourceMachine, startIndex);
    }

    // --- LOGIC PULL VÀ SMART PULL ---
    private static void doPull(SmpMachine destMachine, List<TargetInfo> targets, boolean isSmart) {
        Inventory destInv = destMachine.getGui().getInventory();
        int[] destInputSlots =  ((MachineGui) destMachine.getGui()).getInputSlots();

        for (TargetInfo target : targets) {
            Inventory sourceInv = target.inv;
            int[] sourceOutputSlots = target.slots;
            int size = (sourceOutputSlots != null) ? sourceOutputSlots.length : sourceInv.getSize();

            for (int i = 0; i < size; i++) {
                int currentSlot = (sourceOutputSlots != null) ? sourceOutputSlots[i] : i;
                ItemStack itemToPull = sourceInv.getItem(currentSlot);

                if (itemToPull == null || itemToPull.getType().isAir()) continue;

                // FIX LỖI OVER-PULL CHO SMART PULL
                if (isSmart) {
                    int currentCount = countItemsInSlots(destInv, destInputSlots, itemToPull);
                    int max = itemToPull.getMaxStackSize();

                    if (currentCount >= max) continue; // Đã đủ 1 stack -> Chặn không cho hút nữa

                    int allowedToPull = max - currentCount; // Tính toán lượng CHÍNH XÁC còn thiếu

                    // Nếu stack trong rương lớn hơn lượng được phép hút -> Phải chẻ stack
                    if (itemToPull.getAmount() > allowedToPull) {
                        ItemStack toMove = itemToPull.clone();
                        toMove.setAmount(allowedToPull);

                        ItemStack leftOverMove = moveItem(toMove, destInv, destInputSlots);

                        // Tính lượng đã thực sự hút được vào máy
                        int actuallyMoved = allowedToPull - (leftOverMove != null ? leftOverMove.getAmount() : 0);

                        // Trừ đi ở rương nguồn
                        itemToPull.setAmount(itemToPull.getAmount() - actuallyMoved);
                        sourceInv.setItem(currentSlot, itemToPull);
                        continue;
                    }
                }

                // Nếu là Auto Pull bình thường hoặc lượng cần hút <= lượng đang có
                ItemStack leftOver = moveItem(itemToPull.clone(), destInv, destInputSlots);
                if (leftOver == null || leftOver.getAmount() == 0) {
                    sourceInv.setItem(currentSlot, null);
                } else {
                    sourceInv.setItem(currentSlot, leftOver);
                }
            }
        }
    }

    // --- HELPER: CHUYỂN ITEM THỰC TẾ ---
    private static ItemStack moveItem(ItemStack item, Inventory destInv, int[] allowedSlots) {
        if (allowedSlots == null) {
            HashMap<Integer, ItemStack> left = destInv.addItem(item);
            return left.isEmpty() ? null : left.get(0);
        } else {
            int amountLeft = item.getAmount();
            int maxStack = item.getMaxStackSize();

            // 1. Ưu tiên Stack chung đồ trước
            for (int slot : allowedSlots) {
                if (amountLeft <= 0) break;
                ItemStack current = destInv.getItem(slot);
                if (current != null && current.isSimilar(item)) {
                    int space = maxStack - current.getAmount();
                    int add = Math.min(amountLeft, space);
                    current.setAmount(current.getAmount() + add);
                    amountLeft -= add;
                }
            }

            // 2. Tìm ô trống nếu chưa hết
            if (amountLeft > 0) {
                for (int slot : allowedSlots) {
                    if (amountLeft <= 0) break;
                    ItemStack current = destInv.getItem(slot);
                    if (current == null || current.getType().isAir()) {
                        ItemStack newItem = item.clone();
                        int add = Math.min(amountLeft, maxStack);
                        newItem.setAmount(add);
                        destInv.setItem(slot, newItem);
                        amountLeft -= add;
                    }
                }
            }

            if (amountLeft <= 0) return null;
            item.setAmount(amountLeft);
            return item;
        }
    }

    // --- HELPER CHO SMART PULL ---
    private static int countItemsInSlots(Inventory inv, int[] slots, ItemStack checkItem) {
        int count = 0;
        for (int slot : slots) {
            ItemStack current = inv.getItem(slot);
            if (current != null && current.isSimilar(checkItem)) {
                count += current.getAmount();
            }
        }
        return count;
    }
}
