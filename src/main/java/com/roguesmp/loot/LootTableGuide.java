package com.roguesmp.loot;

import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.manager.LootTableManager;
import com.roguesmp.loot.repository.ILootTableRepository;
import com.roguesmp.loot.repository.LootTableRepository;
import com.roguesmp.loot.rule.LootRules;
import com.roguesmp.loot.service.ILootService;
import com.roguesmp.loot.service.LootService;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import com.google.gson.Gson;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * ============================================================================
 *  HƯỚNG DẪN SỬ DỤNG LOOT TABLE  (com.roguesmp.loot)
 * ============================================================================
 *
 * File này KHÔNG chạy trong game — nó chỉ là tài liệu + ví dụ code để tham
 * khảo cách dùng hệ thống loot table. Có thể xoá bất cứ lúc nào mà không ảnh
 * hưởng tới plugin.
 *
 * <h2>1. Kiến trúc tổng quan (bottom-up)</h2>
 * <pre>
 *   File JSON  ──►  {@link LootTableRepository}   (đọc & parse JSON)
 *                        │
 *                        ▼
 *                  {@link LootTableManager}       (cache id → LootTable)
 *                        │
 *                        ▼
 *                  {@link LootService}            (roll: weighted random + bonus)
 *                        │
 *                        ▼
 *   Caller (Listener / Controller)  ──►  lootService.roll(id, context)  ──►  List&lt;ItemStack&gt;
 * </pre>
 *
 * Các model bất biến (immutable) trong 1 table:
 * <ul>
 *   <li>{@link LootTable}  — 1 file JSON, gồm nhiều {@link LootPool}.</li>
 *   <li>{@link LootPool}   — 1 "túi" quay: có {@code rolls} (số lần quay) và danh sách {@link LootEntry}.</li>
 *   <li>{@link LootEntry}  — 1 phần thưởng có thể trúng, mang {@code weight} (trọng số).
 *       Kiểu {@link LootEntryType}: {@code ITEM}, {@code LOOT_TABLE} (lồng bảng khác), {@code EMPTY} (trượt).</li>
 * </ul>
 *
 * ----------------------------------------------------------------------------
 * <h2>2. CÁCH TẠO 1 LOOT TABLE (viết file JSON)</h2>
 * ----------------------------------------------------------------------------
 *
 * Đặt file {@code .json} vào thư mục:
 * <pre>
 *   plugins/RogueSmp/{@value LootConfig#LOOT_TABLE_FOLDER}/
 * </pre>
 *
 * <b>ID được suy ra từ đường dẫn file</b> (namespace mặc định là {@code "rogue"}):
 * <pre>
 *   .../loottable/dungeons/dungeon_a_reward.json   ──►  id = "rogue:dungeons/dungeon_a_reward"
 * </pre>
 * (Có thể ghi đè bằng field {@code "id"} trong JSON, nhưng nên để tự suy ra cho khớp path.)
 *
 * <h3>Ví dụ file: loottable/dungeons/dungeon_a_reward.json</h3>
 * <pre>{@code
 * {
 *   // "id" là tuỳ chọn — nếu bỏ trống sẽ tự suy ra từ đường dẫn file.
 *   "id": "rogue:dungeons/dungeon_a_reward",
 *
 *   // 1 table có nhiều pool. Mỗi pool được quay ĐỘC LẬP.
 *   "pools": [
 *     {
 *       "rolls": 2,          // quay 2 lần trong pool này (mặc định 1, tối thiểu 1)
 *       "bonus_rolls": 1.0,  // (tuỳ chọn) hệ số roll thưởng, xem mục LootRule bên dưới
 *       "entries": [
 *         {
 *           "type": "item",      // ITEM: rơi ra 1 item trong ItemRegistry
 *           "weight": 70,        // trọng số càng cao → càng dễ trúng
 *           "item_id": "ruby",   // id trong ItemRegistry (BaseItem)
 *           "min_amount": 1,     // (tuỳ chọn, mặc định 1)
 *           "max_amount": 3      // (tuỳ chọn, mặc định 1) → số lượng random [min, max]
 *         },
 *         {
 *           "type": "loot_table",             // LOOT_TABLE: quay lồng 1 bảng khác
 *           "weight": 20,
 *           "name": "rogue:dungeons/rare_pool" // LƯU Ý: dùng field "name" cho id bảng lồng
 *         },
 *         {
 *           "type": "empty",  // EMPTY: trượt, không rơi gì (chỉ tính weight)
 *           "weight": 10
 *         }
 *       ]
 *     },
 *     {
 *       "rolls": 1,
 *       "entries": [
 *         { "type": "item", "weight": 1, "item_id": "gold_coin", "min_amount": 5, "max_amount": 20 }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <b>Cách quay (roll) hoạt động cho mỗi pool:</b>
 * <ol>
 *   <li>Tính tổng số lần quay = {@code rolls} + bonus (xem mục 4).</li>
 *   <li>Mỗi lần quay: chọn 1 entry theo weighted random
 *       (xác suất = weight / tổng weight của pool).</li>
 *   <li>Thực thi entry đã chọn: ITEM → sinh ItemStack; LOOT_TABLE → đệ quy sang bảng khác
 *       (tối đa 8 tầng, chống vòng lặp); EMPTY → không làm gì.</li>
 * </ol>
 *
 * <h3>Các field JSON tóm tắt</h3>
 * <pre>
 *   Table :  id? (String)        pools (bắt buộc, mảng)
 *   Pool  :  rolls? (int, =1)    bonus_rolls? (double, =0)   entries (bắt buộc, mảng)
 *   Entry :  type (item|loot_table|empty)   weight? (int, =1)
 *            ITEM       → item_id (bắt buộc), min_amount? (=1), max_amount? (=1)
 *            LOOT_TABLE → name    (bắt buộc, = id bảng lồng)
 *            EMPTY      → (không cần field thêm)
 * </pre>
 *
 * ----------------------------------------------------------------------------
 * <h2>3. CÁCH WIRE & GỌI ROLL TRONG CODE</h2>
 * ----------------------------------------------------------------------------
 * Xem {@link #bootstrapExample(Plugin, Gson)} và {@link #rollExamples(ILootService, SmpPlayer)}.
 *
 * ----------------------------------------------------------------------------
 * <h2>4. LOOT RULE & BONUS ROLLS (luck / looting / dungeon tier)</h2>
 * ----------------------------------------------------------------------------
 * Bonus rolls chỉ áp dụng khi pool có {@code bonus_rolls > 0}. Công thức:
 * <pre>
 *   totalRolls = rolls + floor(bonus_rolls * contextModifier)
 *   contextModifier = tổng của tất cả rule.evaluate(context)  (xem {@link LootContext#getBonusRollModifier()})
 * </pre>
 * Các rule dựng sẵn trong {@link LootRules}: {@code Fixed}, {@code LootingEnchant},
 * {@code PlayerLuck}, {@code DungeonTier}, {@code DungeonScoreRule}.
 * Xem ví dụ ở {@link #rollWithRulesExample(ILootService, SmpPlayer, ItemStack)}.
 */
public final class LootTableGuide {

    private LootTableGuide() {
    }

    /**
     * MỤC 3 — Cách khởi tạo chuỗi Repository → Manager → Service.
     *
     * <p>Trong plugin thật, chuỗi này đã được dựng sẵn trong
     * {@code com.roguesmp.dungeon.DungeonRegistry#onEnable(...)} — bạn thường
     * chỉ cần lấy lại {@code ILootService} có sẵn, không tự dựng lại.
     * Ví dụ dưới đây minh hoạ thứ tự phụ thuộc.
     */
    public static ILootService bootstrapExample(Plugin plugin, Gson gson) {
        // 1) Repository: đọc & parse JSON trong thư mục plugins/RogueSmp/loottable/
        ILootTableRepository repository = new LootTableRepository(plugin, gson);

        // 2) Manager: load toàn bộ table vào cache ngay trong constructor.
        //    Có thể gọi manager.load() lại khi /reload.
        LootTableManager manager = new LootTableManager(repository);

        // 3) Service: nơi thực hiện roll. Cần ItemRegistry để dựng ItemStack cho entry ITEM.
        return new LootService(manager, ItemRegistry.getInstance());
    }

    /**
     * MỤC 3 — Các cách gọi roll cơ bản.
     */
    public static void rollExamples(ILootService lootService, SmpPlayer player) {
        String tableId = "rogue:dungeons/dungeon_a_reward";

        // (Nên) kiểm tra tồn tại trước khi roll — tránh log warning vô ích.
        if (!lootService.exists(tableId)) {
            return;
        }

        // Cách A: roll đơn giản, không cần player/rule (drop thường).
        List<ItemStack> simpleDrops = lootService.roll(tableId);

        // Cách B: roll có context (gắn player để item sinh ra theo người chơi,
        //         và cho phép rule cộng bonus rolls).
        LootContext ctx = LootContext.builder(player).build();
        List<ItemStack> drops = lootService.roll(tableId, ctx);

        // Kết quả không bao giờ null, chỉ có thể rỗng. Caller tự chịu trách nhiệm
        // spawn/drop item ra thế giới (đặt vào chest, drop tại vị trí mob, v.v.).
        giveOrDrop(player, drops);
        giveOrDrop(player, simpleDrops);
    }

    /**
     * MỤC 4 — Roll có LootRule để tăng bonus rolls (luck / looting / dungeon tier).
     *
     * <p>Nhớ: rule chỉ có tác dụng nếu pool trong JSON có {@code bonus_rolls > 0}.
     */
    public static List<ItemStack> rollWithRulesExample(
            ILootService lootService,
            SmpPlayer player,
            ItemStack weapon
    ) {
        LootContext ctx = LootContext.builder(player)
                // Cộng cứng +1.0 modifier (ví dụ rương đôi trong dungeon).
                .addRule(new LootRules.Fixed(1.0))
                // Looting III với hệ số 0.5/level → +1.5 modifier.
                .addRule(new LootRules.LootingEnchant(weapon, 0.5))
                // Dungeon tier 3, mỗi tier +0.25 → +0.75 modifier.
                .addRule(new LootRules.DungeonTier(3, 0.25))
                // Đọc chỉ số luck tuỳ biến từ SmpPlayer của bạn.
                .addRule(new LootRules.PlayerLuck(player, p -> readLuck(p) * 0.01))
                .build();

        // Với pool bonus_rolls=1.0 và tổng modifier ở trên (1.0+1.5+0.75+luck),
        // số roll thêm = floor(1.0 * modifier).
        return lootService.roll("rogue:dungeons/dungeon_a_reward", ctx);
    }

    // --- Chỗ nối tạm cho ví dụ (thay bằng logic thật của bạn) ---

    private static double readLuck(SmpPlayer player) {
        return 0.0; // TODO: đọc stat luck thật từ hệ thống của bạn
    }

    private static void giveOrDrop(SmpPlayer player, List<ItemStack> items) {
        // TODO: đặt item vào inventory / rương / drop ra world tuỳ ngữ cảnh.
    }
}
