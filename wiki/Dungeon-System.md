# Dungeon System

Package: [`com.roguesmp.dungeon`](../src/main/java/com/roguesmp/dungeon). JSON: [JSON-Dungeons](JSON-Dungeons.md).

Một lõi nhỏ quản lý instance; mọi luật chơi là **feature** cấu hình trong JSON.

## Thành phần

| Class | Vai trò |
| :--- | :--- |
| `DungeonDefinition` | Định nghĩa bất biến (registry `DUNGEON`): template, hiển thị, biến ban đầu, map feature. |
| `DungeonInstance` | 1 lượt chơi: thành viên, biến, state của từng feature. Lưu ở `dungeon/instances.db`. |
| `DungeonManager` | Singleton lõi: tạo/vào/kết thúc instance, mời/rời, phát sự kiện, tick mỗi giây, lưu. |
| `DungeonWorldManager` | Sao chép Polar template thành world `dungeoninstance:*`, load/unload/xoá. |
| `DungeonListener` | Đổi sự kiện Bukkit thành `DungeonEvent`. |
| `DungeonFeature<S>` | Điểm mở rộng: 1 luật chơi + state `S` theo instance. |
| `DungeonEvent` | Sealed interface; con đường duy nhất để feature nhận sự kiện. |
| `script/` | `EventMatcher`, `ValuePredicate`, `DataValue`, `DungeonCondition`, `DungeonValidation`. |
| `action/` | `DungeonAction` dùng bởi trigger. |
| `room/` | Định nghĩa phòng, objective, effect, schematic. |
| `build/` | `/dungeonbuild` và rương thưởng. |

GUI người chơi ở `gui/dungeon/`. Party (`party/`) và spawner (`spawner/`) là hệ độc lập.

## Vòng đời

1. `create(player, dungeonId)`: gọi `prepare(creator, members)` của mọi feature (party thêm thành viên, có thể từ chối) → sao chép world → `onStart` → lưu → sự kiện `started`.
2. Đang chạy: `onEvent` cho mỗi sự kiện, `onTick` mỗi giây (kể cả khi world đã unload).
3. Khởi động lại server: `onRestore` thay cho `onStart`.
4. `end(instance, reason)`: `onEnd` → đưa người chơi ra hub → xoá world và bản ghi.

Thành viên ngang quyền. Mời/chấp nhận/rời bỏ nằm ở `DungeonManager`; feature `membership` chỉ bật/tắt. Lời mời lưu trong bộ nhớ, hết hạn sau 60 giây.

## Feature

```java
public interface DungeonFeature<S> {
    String getTypeId();
    S createState();
    Codec<S> stateCodec();
    default DataResult<Void> prepare(Player creator, Set<UUID> members) { ... }
    default void validate(String featureId, DungeonValidation check) { }
    default void onStart(FeatureContext<S> context) { }
    default void onRestore(FeatureContext<S> context) { }
    default void onTick(FeatureContext<S> context) { }
    default void onEvent(FeatureContext<S> context, DungeonEvent event) { }
    default void onEnd(FeatureContext<S> context) { }
}
```

- State được lưu dạng JSON theo key feature; feature không cần state dùng `NoState`.
- `FeatureContext` cho truy cập manager, instance, world, người chơi online, `fire`/`signal`, `end`, `save`.
- Tra feature khác: `manager.feature(instance, RoomsFeature.class)`.
- Sự kiện lồng nhau quá 16 cấp bị bỏ (chặn vòng lặp trigger).

Thêm feature: implement, khai báo `CODEC`, đăng ký trong `DungeonFeatures`.

## Script

- `EventMatcher` khớp `id` + `subject` + `data` của sự kiện bằng `ValuePredicate`.
- `DungeonCondition` (dispatch `"type"`, đăng ký trong `DungeonConditions`) kiểm tra trạng thái instance.
- `DungeonAction` (đăng ký trong `DungeonActions`) thực thi; `sequence` + `delay` lên lịch phần còn lại và chỉ chạy tiếp nếu `manager.isLive(instance)`.
- Biến (`DataValue`) nằm trên `DungeonInstance`, khai báo ban đầu ở definition.
- Trigger không có state riêng; `once` lưu thành biến.

Thêm action/condition: record + `CODEC` + đăng ký; override `validate` nếu nó tham chiếu id, biến hoặc bắn sự kiện.

## Kiểm tra khi nạp

`DungeonValidation.validateAll()` chạy trong `RogueSmpCore.loadData()` sau khi registry nạp xong (cả khi reload). Mỗi feature/action/condition khai báo qua `validate`: sự kiện nó nghe/bắn, biến đọc/ghi, id tham chiếu. Cuối cùng so chéo và log cảnh báo theo đường dẫn JSON.

---
◀ [Player Ability System](Player-Ability-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Fx System](Fx-System.md)
