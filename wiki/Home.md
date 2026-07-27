# Wiki RogueSmpCore

RogueSmpCore là plugin custom-content cho server Minecraft **Paper 1.21.11** (`com.roguesmp`). Plugin xây dựng một tầng framework item/attribute/ability/dungeon tùy chỉnh trên nền Bukkit gốc. Viết bằng Java 21, build bằng Gradle, không có bộ unit test — việc kiểm chứng được thực hiện bằng cách chạy/attach vào một server Paper thật (`./gradlew runServer`).

Mọi thứ đều khởi động từ [`RogueSmpCore`](../src/main/java/com/roguesmp/RogueSmpCore.java) (entrypoint `JavaPlugin`). `onEnable()` chạy theo thứ tự: `init()` (khởi tạo mọi manager/registry singleton), `loadData()` (đọc dữ liệu JSON đã lưu), `initListeners()`, `initCommands()`. Hãy đọc file này đầu tiên khi cần lần theo cách một subsystem bất kỳ được kết nối — đây là nơi duy nhất mọi singleton cấp cao được khởi tạo và sắp xếp thứ tự.

## Danh sách trang

1. **[Codec System](Codec-System.md)** — tầng serialize độc lập định dạng (`Codec`, `MapCodec`, `DynamicOps`, `DataResult`), lấy cảm hứng từ DataFixerUpper của Mojang. Nên đọc trang này trước — hầu hết việc load JSON của các subsystem khác đều dựa trên nó.
2. **[Registry System](Registry-System.md)** — class `Registry<T>` dùng chung, class `Registries` gom mọi registry lại một chỗ, quá trình chuyển đổi (migration) khỏi các singleton tự viết tay cho từng subsystem, và convention singleton Manager (khác, không liên quan) được dùng ở khắp nơi trong plugin.
3. **[Item System](Item-System.md)** — mô hình 2 tầng `BaseItem` (template) / `SmpItem` (wrapper runtime), điểm mở rộng `ItemComponent`, và pipeline lắp ráp `generateItemStack`.
4. **[Attribute System](Attribute-System.md)** — `SmpAttribute`, enum `Attributes` (không có registry riêng — chỉ là các hằng số enum), attribute wrap vanilla vs. attribute custom theo gameplay-event.
5. **[GUI Framework](GUI-Framework.md)** — `BaseGui` + `GuiListener` xử lý click tập trung; các mixin `IHaveBlueprint`/`IHaveInputOutput` cho GUI dạng máy/công thức chế tạo.
6. **[Entity, Boss & Spell System](Entity-Boss-Spell-System.md)** — mô hình định nghĩa/runtime của `BaseEntity`/`SmpEntity`, lịch chạy spell an toàn với Folia, chuyển pha (phase) của boss, và bề mặt hook của `Spell`.
7. **[Player Ability System](Player-Ability-System.md)** — `Ability` + `AbilityInfo`, gán trigger từ JSON, chuỗi xử lý cast (cast chain) của `AbilityLoadout` và cơ chế capture/interceptor.
8. **[Dungeon System](Dungeon-System.md)** — subsystem lớn nhất: bảy tầng phân lớp nghiêm ngặt (definition → runtime → repository → manager → service → controller → actor), tất cả được nối tay trong `DungeonRegistry`.

## Các convention xuyên suốt (áp dụng ở mọi nơi, không chỉ 1 trang)

- **Convention singleton Registry/Manager**: constructor private, `INSTANCE` static, `static void init(...)` được gọi 1 lần từ `RogueSmpCore.init()`, `static getInstance()`. Không có base type dùng chung — đây là 1 khuôn mẫu lặp lại chứ không phải 1 abstraction thật sự. Các registry *dữ liệu* mới nên dùng [`Registries`](Registry-System.md) thay vì kiểu này; convention này vẫn đúng cho các manager có state runtime thật sự. ⚠️ Một số class theo khuôn này (`ItemRegistry`) đang trong quá trình migration — xem [Registry System](Registry-System.md#the-migration-registry-getinstance--registries).
- **Điểm mở rộng dạng interface mặc định no-op**: `ItemComponent`, `SmpAttribute`, `Spell`, `Ability` đều theo cùng 1 khuôn mẫu — chỉ override những hook cần thiết trên 1 bề mặt gameplay-event dùng chung (damage, kill, consume, projectile, combust, ...). Khi thêm 1 điểm mở rộng cross-cutting mới, hãy theo khuôn này thay vì tự nghĩ ra cơ chế khác.
- **PDC key** đi qua `constant/Keys.java` dưới namespace dùng chung `"smp"` — không tự tạo `NamespacedKey` ở nơi khác.
- **Định danh/codec của item component** đi qua `constant/ComponentKeys.java` — xem [Item System](Item-System.md).
- **GUI** đi qua `BaseGui` + `GuiListener` — không đăng ký thêm listener `InventoryClickEvent` thô; xem [GUI Framework](GUI-Framework.md).

## Build & chạy

```sh
./gradlew build          # compile + assemble
./gradlew shadowJar      # tạo fat jar đã relocate (com.roguesmp.libs.glowingentities)
./gradlew runServer      # chạy 1 server Paper 1.21 local với plugin này đã load sẵn
```

Sandbox của `runServer` **không** tự động cài các plugin `depend` lúc runtime (CommandAPI, FastAsyncWorldEdit, TAB, PlaceholderAPI, Multiverse-Core, WorldGuard) — cần tự tay bỏ chúng vào thư mục `plugins/` của server test để chạy thử các tính năng phụ thuộc cứng vào các plugin đó.
