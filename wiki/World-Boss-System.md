# World Boss System

Package: [`com.roguesmp.worldboss`](../src/main/java/com/roguesmp/worldboss). JSON: [JSON-WorldBosses](JSON-WorldBosses.md).

World boss chia làm 2 nửa:

- **JSON** (`WorldBossDefinition`) — chỉ chứa thứ cần chỉnh trên server đang chạy: vị trí, đấu trường, lối ra, điều kiện, chi phí, thời gian, loot.
- **Code** (`WorldBossEncounter`) — "kịch bản" riêng của từng boss: tin nhắn, title, âm thanh, hiệu ứng, mob nào spawn, khi nào phát thưởng.

Luật chung của trận (đếm ngược, khoá đấu trường, loại người chơi, chặn vào, kết thúc) nằm trong `WorldBossManager` và **giống nhau cho mọi boss**. Encounter không cần (và không nên) tự chạy scheduler cho các luật này.

## Thành phần

| Class | Vai trò |
| :--- | :--- |
| [`WorldBossDefinition`](../src/main/java/com/roguesmp/worldboss/definition/WorldBossDefinition.java) | Bản ghi JSON bất biến, registry `Registries.WORLD_BOSS` (`world_bosses/`). |
| [`SpawnMode`](../src/main/java/com/roguesmp/worldboss/definition/SpawnMode.java) | `SummonedSpawn` \| `PeriodicSpawn`, dispatch theo `"type"`. |
| [`WorldBossManager`](../src/main/java/com/roguesmp/worldboss/WorldBossManager.java) | Singleton. Giữ các trận đang diễn ra (tối đa 1 trận / boss), cooldown và lịch xuất hiện (chỉ trong bộ nhớ), tick mỗi trận mỗi giây, mỗi 20 giây mở các boss `periodic` đã đến giờ, phát thưởng. |
| [`WorldBossInstance`](../src/main/java/com/roguesmp/worldboss/runtime/WorldBossInstance.java) | Trạng thái của 1 trận: `ArenaState` (`OPEN`/`LOCKED`), người tham gia, boss, thời gian còn lại. |
| [`WorldBossEncounter`](../src/main/java/com/roguesmp/worldboss/encounter/WorldBossEncounter.java) | Abstract class kịch bản boss, hook mặc định không làm gì. Mỗi trận tạo 1 object mới. |
| [`WorldBossEncounters`](../src/main/java/com/roguesmp/worldboss/encounter/WorldBossEncounters.java) | Đăng ký encounter theo id boss vào `Registries.WORLD_BOSS_ENCOUNTER`. |
| [`Announcer`](../src/main/java/com/roguesmp/worldboss/encounter/Announcer.java) | Gửi chat / title / action bar / âm thanh tới 1 nhóm người, tự thay placeholder. |
| [`ArenaGuard`](../src/main/java/com/roguesmp/worldboss/arena/ArenaGuard.java) | Listener: chặn vào đấu trường, loại khi chết / thoát, đổi điểm hồi sinh, phát hiện boss chết. |
| [`WorldBossSummonGui`](../src/main/java/com/roguesmp/worldboss/gui/WorldBossSummonGui.java) | GUI triệu hồi, mở qua `open_gui` id `world_boss:<id>`. |

## Vòng đời trận

```
GUI (summoned) ─┐
chu kỳ 20s ─────┼─► manager.open() ── OPEN ── onOpen, mỗi giây onCountdown
/worldboss start┘                       │
                                        ▼ hết join window
                                     lock() ── đuổi người không đạt điều kiện (onRemove REQUIREMENT_NOT_MET)
                                        │      spawnBoss → LOCKED → onLock
                                        ▼
                     chết / rời / thoát ── onRemove(DEATH | LEFT_ARENA | LOGOUT)
                                        │
                                        ▼
                  endFight: onVictory | onWipe | onTimeout | onCancel
```

`manager.open()` trả `OpenResult`: `OPENED`, `ALREADY_ACTIVE`, `ON_COOLDOWN`, `WORLD_NOT_LOADED`, `REGION_NOT_FOUND`, `UNKNOWN_ENCOUNTER`. Mọi kết thúc (kể cả server tắt) đi qua 1 đường `endFight`, nên boss luôn được dọn và trận không bị kẹt.

## Viết 1 encounter

Ví dụ đầy đủ: [`GoblinKingEncounter`](../src/main/java/com/roguesmp/entity/boss/goblinking/GoblinKingEncounter.java). Encounter đặt **cạnh boss của nó** (`entity/boss/<tên>/`), giống spell của boss.

```java
public class GoblinKingEncounter extends WorldBossEncounter {

    @Override
    public @Nullable SmpEntity spawnBoss(WorldBossInstance fight) {
        fight.getCenter().getWorld().strikeLightningEffect(fight.getCenter());
        return EntityManager.getInstance().spawnEntity("goblin_burrow/goblin_king", fight.getCenter());
    }

    @Override
    public void onOpen(WorldBossInstance fight) {
        server(fight).withRequirementsHover()
                .chat("<gold>⚔ <boss> <yellow>đang tập hợp quân tại <white><place>!")
                .sound(SoundEventKeys.EVENT_RAID_HORN, 1f, 1f);
    }
}
```

Rồi đăng ký trong `WorldBossEncounters`, **id trùng tên file JSON**:

```java
public static final Supplier<GoblinKingEncounter> GOBLIN_KING = register("goblin_king", GoblinKingEncounter::new);
```

`spawnBoss` nhận **id entity đầy đủ** (đường dẫn trong `entities/`, vd. `goblin_burrow/goblin_king`), không phải id factory. Trả `null` → trận bị huỷ và log lỗi.

### Hook

| Hook | Khi nào | Mặc định |
| :--- | :--- | :--- |
| `spawnBoss(fight)` | Lúc đấu trường đóng. **Bắt buộc.** | — |
| `onOpen(fight)` | Trận vừa mở. | không làm gì |
| `onCountdown(fight, secondsLeft)` | Mỗi giây trong thời gian tập hợp. Action bar đếm ngược đã có sẵn, hook này dùng cho thông báo riêng. | không làm gì |
| `onLock(fight)` | Đấu trường đóng, boss đã spawn. | không làm gì |
| `onRemove(fight, player, reason)` | Người chơi bị loại hoặc bị đuổi. `RemovalReason`: `DEATH`, `LEFT_ARENA`, `LOGOUT`, `REQUIREMENT_NOT_MET`. Với `LOGOUT` lúc tick phát hiện, không có `Player` nên hook không chạy. | không làm gì |
| `onVictory(fight, survivors)` | Boss chết. | gọi `giveRewards(fight)` ngay |
| `onWipe(fight)` / `onTimeout(fight)` / `onCancel(fight)` | Các kết thúc còn lại. | không làm gì |

Hook mặc định im lặng: boss không override thì người chơi không thấy thông báo nào (trừ action bar đếm giờ của manager).

### Phát thưởng

Thưởng **không** tự phát khi boss chết. `onVictory` mặc định gọi `giveRewards(fight)` ngay; muốn chạy hiệu ứng trước thì override và gọi sau:

```java
@Override
public void onVictory(WorldBossInstance fight, List<Player> survivors) {
    participants(fight).title("<gold>CHIẾN THẮNG", "");
    Bukkit.getScheduler().runTaskLater(RogueSmpCore.getInstance(), () -> giveRewards(fight), 60L);
}
```

`giveRewards` chỉ có tác dụng lần đầu. Thưởng đi tới người tham gia **còn sống và đang online lúc gọi** — người thoát game trong lúc chờ, hoặc server tắt trước khi task chạy, sẽ mất thưởng.

### Announcer

Chọn nhóm người nhận, rồi nối các lệnh gửi:

| Helper | Gửi tới |
| :--- | :--- |
| `server(fight)` | Mọi người online. |
| `world(fight)` | Người trong world của boss. |
| `arena(fight)` | Người đang đứng trong đấu trường. |
| `participants(fight)` | Người tham gia còn sống. |
| `player(fight, player)` | 1 người. |

Lệnh: `.chat(String...)`, `.title(title, subtitle)`, `.actionBar(text)`, `.sound(Key, volume, pitch)`. `.withRequirementsHover()` gắn hover liệt kê `requirements` (✔/✘ theo người đọc) vào các dòng `chat` gửi sau nó; boss không có điều kiện thì không gắn gì.

Placeholder (MiniMessage, trộn được với định dạng):

| Tag | Giá trị |
| :--- | :--- |
| `<boss>` | `display_name` |
| `<place>` | Tên hiển thị của region đấu trường (flag tên region, không có thì id) |
| `<x>` `<y>` `<z>` | Toạ độ block của `location` |
| `<seconds>` | Giây còn lại (đếm ngược khi tập hợp, thời gian trận khi đang đánh) |
| `<participants>` | Số người tham gia |
| `<survivors>` | Tên người tham gia còn sống, cách nhau dấu phẩy |
| `<summoner>` | Tên người triệu hồi (rỗng nếu không có) |

Placeholder được tính **lúc tạo announcer**, nên tạo announcer mới cho mỗi lần gửi.

## Lưu ý khi thiết kế

- **Knockback ra khỏi đấu trường = bị loại.** Spell đẩy người chơi qua mép region sẽ loại họ. Làm region rộng hơn tầm đẩy xa nhất của boss.
- **Boss bị unload** (chunk không còn ai) → trận bị huỷ.
- **Kiểm encounter chỉ lúc khởi động.** Thêm boss JSON bằng reload mà chưa có encounter → chỉ báo lỗi khi mở trận.
- **Không có trạng thái lưu file**: trận đang diễn ra khi server tắt luôn bị huỷ; cooldown và lịch chu kỳ reset khi khởi động lại.
