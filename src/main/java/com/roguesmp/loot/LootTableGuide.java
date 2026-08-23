package com.roguesmp.loot;

import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.entry.ItemEntry;
import com.roguesmp.loot.manager.LootTableManager;
import com.roguesmp.loot.context.LootOrigin;
import com.roguesmp.loot.event.LootRollEvent;
import com.roguesmp.loot.service.ILootService;
import com.roguesmp.loot.service.LootService;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.inventory.ItemStack;

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
 *   File JSON  ──►  {@link com.roguesmp.registry.Registries#LOOT_TABLE}   (Registry&lt;LootTable&gt;,
 *                        │                                                  decode qua Codec — đệ quy subfolder,
 *                        │                                                  id = đường dẫn tương đối, không namespace)
 *                        ▼
 *                  {@link LootTableManager}       (wrapper mỏng: getTable/exists/reload)
 *                        │
 *                        ▼
 *                  {@link LootService}            (roll: weighted random)
 *                        │
 *                        ▼
 *   Caller (Listener / Controller)  ──►  lootService.roll(id, context)  ──►  List&lt;ItemStack&gt;
 * </pre>
 *
 * Các model bất biến (immutable) trong 1 table:
 * <ul>
 *   <li>{@link LootTable}  — 1 file JSON, gồm nhiều {@link LootPool}.</li>
 *   <li>{@link LootPool}   — 1 "túi" quay: có {@code rolls} (số lần quay) và danh sách {@link LootEntry}.</li>
 *   <li>{@link LootEntry}  — abstract, 1 phần thưởng có thể trúng, mang {@code weight} (trọng số).
 *       3 kiểu built-in sống ở {@code com.roguesmp.loot.entry}: {@link ItemEntry} (rơi item),
 *       {@code NestedTableEntry} (lồng bảng khác), {@code EmptyEntry} (trượt) — cùng cơ chế
 *       polymorphic dispatch mà {@code SmpEffect}/{@code ItemComponent} dùng (xem
 *       {@code com.roguesmp.codec.INFO.md} mục 7). Thêm 1 kiểu entry mới: implement
 *       {@code LootEntry}, khai {@code Codec} qua {@code LootEntry.BASE_CODEC} + field riêng, đăng
 *       ký vào {@code com.roguesmp.loot.entry.LootEntries}.</li>
 * </ul>
 *
 * <p>Không có khái niệm "modifier"/"luck" toàn cục nào chạy ngầm trong engine — mọi ảnh hưởng
 * động (luck, dungeon score, VIP perk, ...) đi qua các event ở mục 4/6/7, tác động trực tiếp lên
 * dữ liệu thật ({@link LootEntry}/{@code ItemStack}) thay vì một lớp số liệu trung gian.
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
 * <b>ID được suy ra từ đường dẫn file, không có namespace, không ghi đè được</b> — id LUÔN trỏ
 * thẳng về đúng file của nó (không như {@code items}/{@code quests}/... vốn còn field {@code "id"}
 * riêng trong JSON, loot table id chỉ đến từ vị trí file — {@link com.roguesmp.registry.Registry#loadFrom}
 * không đọc field {@code "id"} nào cả):
 * <pre>
 *   .../loottable/dungeons/dungeon_a_reward.json   ──►  id = "dungeons/dungeon_a_reward"
 * </pre>
 *
 * <h3>Ví dụ file: loottable/dungeons/dungeon_a_reward.json</h3>
 * <pre>{@code
 * {
 *   // 1 table có nhiều pool. Mỗi pool được quay ĐỘC LẬP.
 *   "pools": [
 *     {
 *       "rolls": 2,          // quay 2 lần trong pool này (mặc định 1, tối thiểu 1)
 *       "entries": [
 *         {
 *           "type": "item",      // ItemEntry: rơi ra 1 item trong ItemRegistry
 *           "weight": 70,        // trọng số càng cao → càng dễ trúng
 *           "item_id": "ruby",   // id trong ItemRegistry (BaseItem)
 *           "min_amount": 1,     // (tuỳ chọn, mặc định 1)
 *           "max_amount": 3      // (tuỳ chọn, mặc định 1) → số lượng random [min, max]
 *         },
 *         {
 *           // item_id bắt đầu bằng "minecraft:" → build thẳng ItemStack vanilla qua Material,
 *           // KHÔNG qua BaseItem/SmpItem (không component, không lore riêng gì cả).
 *           "type": "item",
 *           "weight": 15,
 *           "item_id": "minecraft:diamond",
 *           "min_amount": 1,
 *           "max_amount": 2
 *         },
 *         {
 *           "type": "loot_table",             // NestedTableEntry: quay lồng 1 bảng khác
 *           "weight": 20,
 *           "name": "dungeons/rare_pool" // LƯU Ý: dùng field "name" cho id bảng lồng
 *         },
 *         {
 *           "type": "empty",  // EmptyEntry: trượt, không rơi gì (chỉ tính weight)
 *           "weight": 10
 *         },
 *         {
 *           // Xem mục 5 — entry nào cũng có thể thêm "conditions"/"functions".
 *           "type": "item",
 *           "weight": 5,
 *           "item_id": "legendary_sword",
 *           "conditions": [
 *             { "type": "chance", "chance": 0.1 }
 *           ],
 *           "functions": [
 *             { "type": "set_count", "min": 1, "max": 1 }
 *           ]
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
 *   <li>Nếu pool có {@code conditions} và fail, bỏ qua cả pool — không roll gì (xem mục 5.5).</li>
 *   <li>Lặp {@code rolls} lần, mỗi lần: loại các entry có {@code conditions} không pass, rồi chọn
 *       1 entry trong số còn lại theo weighted random (xác suất = weight / tổng weight còn lại).</li>
 *   <li>Thực thi entry đã chọn: {@code ItemEntry} → sinh ItemStack; {@code NestedTableEntry} →
 *       đệ quy sang bảng khác (tối đa 8 tầng, chống vòng lặp); {@code EmptyEntry} → không làm gì.
 *       Sau đó chạy {@code functions} của entry (nếu có) lên các ItemStack vừa sinh ra.</li>
 * </ol>
 *
 * <h3>Các field JSON tóm tắt</h3>
 * <pre>
 *   Table :  pools (bắt buộc, mảng)
 *   Pool  :  rolls? (int, =1)    entries (bắt buộc, mảng)
 *            conditions? (mảng LootCondition, =[]) — fail 1 cái là bỏ CẢ pool, không roll gì
 *   Entry :  type (item|loot_table|empty)   weight? (int, =1)
 *            conditions? (mảng LootCondition, =[])   functions? (mảng LootFunction, =[])
 *            ItemEntry       → item_id (bắt buộc, "minecraft:..." = vanilla, ngược lại = BaseItem),
 *                              min_amount? (=1), max_amount? (=1)
 *            NestedTableEntry → name    (bắt buộc, = id bảng lồng)
 *            EmptyEntry      → (không cần field thêm)
 * </pre>
 *
 * ----------------------------------------------------------------------------
 * <h2>3. CÁCH WIRE & GỌI ROLL TRONG CODE</h2>
 * ----------------------------------------------------------------------------
 * Xem {@link #getServiceExample()} và {@link #rollExamples(ILootService, SmpPlayer)}.
 *
 * ----------------------------------------------------------------------------
 * <h2>4. HUỶ CẢ LƯỢT ROLL — {@link LootRollEvent}</h2>
 * ----------------------------------------------------------------------------
 * Bắn ra ngay trước khi bắt đầu roll một table gốc. Dùng event này khi cần <b>chặn hẳn</b> 1
 * lượt roll dựa trên nơi gọi — không sửa được gì bên trong lượt roll, chỉ có quyền huỷ:
 * <pre>{@code
 * @EventHandler
 * public void onLootRoll(LootRollEvent e) {
 *     LootContext ctx = e.getContext();
 *     if (ctx.getOrigin() != LootOrigin.CHEST) return;   // lọc theo nơi gọi
 *     Block chest = ctx.getSourceAs(Block.class);        // dữ liệu nơi gọi
 *     SmpPlayer player = ctx.getPlayer();                // người roll
 *     if (isOnCooldown(player)) e.setCancelled(true);    // huỷ → trả về list rỗng
 * }
 * }</pre>
 * Muốn ảnh hưởng entry nào được chọn hoặc item sinh ra thì dùng mục 6/7 thay vì event này.
 * Chỉ bắn một lần cho mỗi lệnh roll ở table gốc — các table lồng nhau ({@code NestedTableEntry})
 * dùng chung context, không bắn lại.
 *
 * ----------------------------------------------------------------------------
 * <h2>5. ĐIỀU KHIỂN TỪNG ENTRY — {@code conditions} / {@code functions}</h2>
 * ----------------------------------------------------------------------------
 * Mục 4 chỉ ảnh hưởng ở cấp <b>table</b> (huỷ toàn bộ). Hai field này ảnh hưởng ở cấp
 * <b>entry</b> — mỗi entry trong 1 pool tự quyết định nó có được cân nhắc hay không, và item nó
 * sinh ra bị biến đổi ra sao.
 *
 * <h3>5.1. {@code conditions} — {@link com.roguesmp.loot.condition.LootCondition}</h3>
 * Danh sách điều kiện; entry chỉ được đưa vào weighted pick nếu <b>tất cả</b> đều pass (khác
 * với weight=0 — entry fail condition bị loại hẳn khỏi tổng weight của pool, không chỉ "khó
 * trúng hơn", các entry còn lại được renormalize odds lên). Các loại có sẵn (đăng ký trong
 * {@link com.roguesmp.loot.condition.LootConditions}):
 * <pre>
 *   "chance" → { "chance": 0.25 }
 *               xác suất phẳng trong [0, 1]
 *   "origin" → { "origins": ["CHEST", "ENTITY", ...] }
 *               chỉ pass khi LootContext.origin nằm trong danh sách
 * </pre>
 *
 * <h3>5.2. {@code functions} — {@link com.roguesmp.loot.function.LootFunction}</h3>
 * Danh sách hàm chạy tuần tự lên các ItemStack entry vừa sinh ra (sau khi được chọn), hàm sau
 * nhận output của hàm trước. Loại có sẵn (đăng ký trong
 * {@link com.roguesmp.loot.function.LootFunctions}):
 * <pre>
 *   "set_count" → { "min": 1, "max": 3 }
 *                  ghi đè amount bằng 1 giá trị random mới trong [min, max]
 * </pre>
 *
 * <h3>5.3. Thêm 1 loại condition/function mới</h3>
 * Cùng cơ chế polymorphic dispatch mà mọi hệ thống khác trong plugin dùng (xem
 * {@code com.roguesmp.codec.INFO.md} mục 7): implement {@code LootCondition}/{@code LootFunction},
 * khai 1 {@code Codec} bằng {@code Codec.composite(...)}, rồi thêm 1 dòng đăng ký vào
 * {@code LootConditions}/{@code LootFunctions}. Không cần đụng vào {@code LootService}.
 *
 * <h3>5.4. Combinator — {@code and} / {@code or} / {@code not}</h3>
 * Conditions trong 1 entry/pool mặc định đã là AND với nhau, nên {@code and} chỉ cần thiết khi
 * lồng trong 1 {@code or}. Ví dụ {@code (origin CHEST OR origin ENTITY) AND chance 0.2}:
 * <pre>{@code
 * "conditions": [
 *   { "type": "or", "conditions": [
 *       { "type": "origin", "origins": ["CHEST"] },
 *       { "type": "origin", "origins": ["ENTITY"] }
 *   ]},
 *   { "type": "chance", "chance": 0.2 }
 * ]
 * }</pre>
 *
 * <h3>5.5. Pool-level conditions — khác entry-level ở chỗ nào?</h3>
 * Entry fail condition → chỉ riêng entry đó bị loại khỏi weighted pick, các entry khác trong
 * cùng pool được renormalize (xem mục 5.1). Pool fail condition → toàn bộ pool bị bỏ qua, không
 * roll bất kỳ entry nào trong đó (kể cả EmptyEntry), pool khác trong cùng table không bị ảnh hưởng.
 * Dùng để gate cả 1 pool theo origin mà không cần tách bảng riêng:
 * <pre>{@code
 * {
 *   "rolls": 1,
 *   "conditions": [
 *     { "type": "origin", "origins": ["CHEST"] }
 *   ],
 *   "entries": [ { "type": "item", "weight": 1, "item_id": "chest_only_relic" } ]
 * }
 * }</pre>
 * Muốn gate theo dữ liệu động (dungeon tier, player stat, ...) thay vì origin cố định, dùng
 * {@link com.roguesmp.loot.event.LootPoolPickEvent} ở mục 7 để ép include/exclude bằng code.
 *
 * ----------------------------------------------------------------------------
 * <h2>6. HẬU-ROLL — {@link com.roguesmp.loot.event.LootRollCompleteEvent}</h2>
 * ----------------------------------------------------------------------------
 * {@link LootRollEvent} (mục 4) bắn TRƯỚC khi roll, chỉ có quyền huỷ. {@code LootRollCompleteEvent}
 * bắn SAU khi cả table đã roll xong, mang theo list ItemStack cuối cùng (mutable) — dùng cho hiệu
 * ứng tác động lên TOÀN BỘ kết quả mà không cần sửa từng entry/table (vd. sự kiện server nhân đôi
 * loot toàn server):
 * <pre>{@code
 * @EventHandler
 * public void onLootRollComplete(LootRollCompleteEvent e) {
 *     if (!doubleDropsWeekend) return;
 *     e.getItems().addAll(new ArrayList<>(e.getItems())); // nhân đôi mọi thứ vừa roll ra
 * }
 * }</pre>
 * Không cancellable — muốn huỷ cả lượt roll thì cancel {@link LootRollEvent} trước khi roll bắt
 * đầu. Chỉ bắn 1 lần cho table gốc, giống {@link LootRollEvent}.
 *
 * ----------------------------------------------------------------------------
 * <h2>7. ĐIỀU KHIỂN LẬP TRÌNH TỪNG ENTRY —
 *     {@link com.roguesmp.loot.event.LootPoolPickEvent} /
 *     {@link com.roguesmp.loot.event.LootEntryResultEvent}</h2>
 * ----------------------------------------------------------------------------
 * Mục 5 gate/biến đổi entry qua JSON. Hai event này làm việc tương tự nhưng bằng code — đi thẳng
 * vào {@link LootEntry}/{@code ItemStack} thật, không cần khai báo condition/function type mới
 * cho một lần dùng. Vì {@code getItemId()}/{@code getNestedTableId()} giờ nằm trên từng subclass
 * (không còn trên {@code LootEntry} nữa), đọc field riêng thì cần {@code instanceof}/pattern
 * matching trước, như ví dụ dưới.
 *
 * <h3>7.1. {@code LootPoolPickEvent} — sửa entry nào được chọn</h3>
 * Bắn 1 lần cho MỖI lượt quay trong 1 pool (tức mỗi vòng lặp {@code rolls}), mang theo danh sách
 * {@code Candidate} — 1 cho mỗi entry trong pool, cầm {@link LootEntry} thật cùng
 * {@code weight}/{@code eligible} đã tính sẵn (sau khi conditions của chính entry đó chạy). Sửa
 * trực tiếp bằng code, kể cả ép include 1 entry đã fail condition hoặc ép loại 1 entry đã pass:
 * <pre>{@code
 * @EventHandler
 * public void onPoolPick(LootPoolPickEvent e) {
 *     SmpPlayer player = e.getContext().getPlayer();
 *     if (player == null) return;
 *
 *     for (LootPoolPickEvent.Candidate c : e.getCandidates()) {
 *         if (!(c.getEntry() instanceof ItemEntry item)) continue;
 *         if (!"legendary_sword".equals(item.getItemId())) continue;
 *         if (hasVipPerk(player)) c.setWeight(c.getWeight() * 2);
 *     }
 * }
 * }</pre>
 *
 * <h3>7.2. {@code LootEntryResultEvent} — sửa item entry vừa sinh ra</h3>
 * Bắn 1 lần cho MỖI entry vừa được chọn, ngay sau khi {@code functions} JSON của chính entry đó
 * chạy xong — mang {@link LootEntry} (biết chính xác entry nào) và list {@code ItemStack} mutable.
 * Là bản lập trình được của {@code functions}, không cần đăng ký 1 {@code LootFunction} type mới:
 * <pre>{@code
 * @EventHandler
 * public void onEntryResult(LootEntryResultEvent e) {
 *     if (!(e.getEntry() instanceof ItemEntry item)) return;
 *     if (!"legendary_sword".equals(item.getItemId())) return;
 *     e.getItems().forEach(stack -> SmpItemUtils.getSmpItem(stack).addLore("Blessed by RNG"));
 * }
 * }</pre>
 *
 * <p>Cả 2 event bắn vô điều kiện cho mỗi lượt pick/entry, kể cả khi không ai lắng nghe.
 */
public final class LootTableGuide {

    private LootTableGuide() {
    }

    /**
     * MỤC 3 — {@link LootService} là singleton toàn plugin, dựng đúng 1 lần trong
     * {@code RogueSmpCore.init()} (ngay sau {@code EntityManager.init(this)}):
     * <pre>{@code
     * LootService.init(new LootTableManager());
     * }</pre>
     * Mọi hệ thống khác (mob chết, rương dungeon, quest, ...) chỉ cần lấy lại instance có sẵn —
     * không tự dựng {@code new LootService(...)} nữa:
     */
    public static ILootService getServiceExample() {
        return LootService.getInstance();
    }

    /**
     * MỤC 3 — Các cách gọi roll cơ bản.
     */
    public static void rollExamples(ILootService lootService, SmpPlayer player) {
        String tableId = "dungeons/dungeon_a_reward";

        // (Nên) kiểm tra tồn tại trước khi roll — tránh log warning vô ích.
        if (!lootService.exists(tableId)) {
            return;
        }

        // Cách A: roll đơn giản, không cần player/rule (drop thường).
        List<ItemStack> simpleDrops = lootService.roll(tableId);

        // Cách B: roll có context — gắn player để item sinh ra theo người chơi,
        //         và khai báo origin để listener LootRollEvent/LootPoolPickEvent biết roll đến từ đâu.
        LootContext ctx = LootContext.builder(player)
                .origin(LootOrigin.CHEST, null)
                .build();
        List<ItemStack> drops = lootService.roll(tableId, ctx);

        // Kết quả không bao giờ null, chỉ có thể rỗng. Caller tự chịu trách nhiệm
        // spawn/drop item ra thế giới (đặt vào chest, drop tại vị trí mob, v.v.).
        giveOrDrop(player, drops);
        giveOrDrop(player, simpleDrops);
    }

    // --- Chỗ nối tạm cho ví dụ (thay bằng logic thật của bạn) ---

    private static void giveOrDrop(SmpPlayer player, List<ItemStack> items) {
        // TODO: đặt item vào inventory / rương / drop ra world tuỳ ngữ cảnh.
    }
}
