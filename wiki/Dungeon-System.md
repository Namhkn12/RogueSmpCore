# Dungeon System

Package: [`com.roguesmp.dungeon`](../src/main/java/com/roguesmp/dungeon)

Subsystem lớn nhất và phân lớp nhiều nhất trong plugin. Phân lớp nghiêm ngặt từ dưới lên trên, nối hoàn toàn bằng tay (constructor injection, không dùng framework DI), được bootstrap riêng biệt khỏi phần còn lại của plugin qua `DungeonRegistry.onEnable(plugin, itemRegistry)` / `onDisable()`.

## Bảy tầng

| Tầng | Package | Vai trò |
| :--- | :--- | :--- |
| 1. Definition | `data/definition/` | Template bất biến, load được từ JSON (`Dungeon`, `Room`, config loot/objective/spawner). **Progress runtime không bao giờ được nằm ở đây.** |
| 2. Runtime | `data/runtime/` (+ `runtime/session/`) | State phiên chơi mutable, sống: `DungeonInstance` (aggregate gốc) gồm `DungeonSession`, `DungeonProgress`, `DungeonPlayer`, `DungeonTimer`, cộng `Party`, `RegionInstance`, `RoomInstance`, `SpawnerInstance`. |
| 3. Repository | `repository/` + `repository/impl/` | Mỗi aggregate có 1 `I*Repository` — persistence JSON mỏng (load/save/delete). |
| 4. Manager | `manager/` | Cache in-memory bên trên repository (vd. `InstanceManager` giữ `Map<String, DungeonInstance>`, load eager lúc khởi tạo, expose `saveAll()`). |
| 5. Service | `service/` + `service/impl/` | Logic nghiệp vụ. Mỗi mối quan tâm có 1 interface `I*Service`, kết hợp nhiều manager/service khác. Một số (`DungeonLifecycleService`, `RoomRuntimeService`, `TreasureService`) chỉ có class cụ thể, không có interface. |
| 6. Controller | `controller/` | Façade cấp tính năng, kết hợp các service (`DungeonFlowController`, `BossRoomController`, `PartyController`, `SchemetaController`, `DungeonTreasureController`, `PlayerActionController`, `SpawnerEventController`). |
| 7. Actor | `actor/command/`, `actor/listener/`, `actor/ui/`, `actor/scoreboard/` | Tầng duy nhất chạm trực tiếp vào input của Bukkit: command CommandAPI, `Listener` của Bukkit, `BaseGui` riêng cho dungeon, tích hợp scoreboard TAB. |

**Khi thay đổi hành vi dungeon, hãy xác định đúng tầng trước** — luật chơi thuộc về `service`/`controller`, không phải `actor`; hình dạng persistence thuộc về `data/runtime` + `repository`, không nên rải rác trong service.

Các package cross-cutting liền kề không nằm trong danh sách 7 tầng nhưng vẫn thuộc `dungeon/`: `config/`, `dto/`, `exception/`, `expansion/` (expansion PlaceholderAPI), `helper/` (Location/Bounds có thể serialize), `itemdisplay/`, `presentation/` (manager âm thanh/particle/effect/message), `task/` (`TaskScheduler`), `utils/`.

## Bootstrap — `DungeonRegistry`

[`dungeon/DungeonRegistry.java`](../src/main/java/com/roguesmp/dungeon/DungeonRegistry.java) tự tay dựng toàn bộ đồ thị phụ thuộc, nghiêm ngặt từ dưới lên, bên trong `onEnable(Plugin plugin)` (⚠️ chữ ký hiện tại **không** còn tham số `ItemRegistry` — gọi đơn giản là `DungeonRegistry.onEnable(this)` từ `RogueSmpCore`):

```
Gson → TaskScheduler → ScoreBoard (DungeonExpansion + ScoreBoardManager) → Logging (Log4Craft_)
     → các manager presentation (Sound/Effect/ScreenMess/Particle/PresentationManager) → DungeonPresenter
     → repository       (8 repo, mỗi cái nhận plugin (+ gson + logger))
     → manager           (Schemeta/Region/Party/Room/Dungeon/Spawner/LootTable/RevivePointPresenter/
                          ReviveManager/SpawnerInstanceManager/InstanceManager)
     → service           (~20 service, mỗi cái nhận manager/service khác)
     → task (PartyInviteTask)
     → controller        (7: DungeonFlowController, BossRoomController, PartyController, SchemetaController,
                          DungeonTreasureController, PlayerActionController, SpawnerEventController)
     → command           (4: TemplateGenCommand, SchemetaCommand, PartyCommand, DungeonCommand)
     → listener          (4: DoorInteractListener, SpawnerEventListener, DungeonListener, LootTableListener)
     → 2 tick đã lên lịch (scoreboard/timer mỗi 20 tick; instanceManager.saveAll() có delay 6000 tick)
```

Đã có thêm 1 subsystem hồi sinh (`ReviveManager`, `IReviveService`/`ReviveService`, `RevivePointPresenter`) và 1 subsystem thưởng/rương kho báu (`IDungeonRewardService`/`DungeonRewardService`, kết hợp với `com.roguesmp.loot.service.ILootService` — xem [Loot System](Loot-System.md) — cộng `ChestOpenAnimation`) kể từ khi trang này được viết lần đầu; cả 2 không nằm trong 7 tầng gốc mà rải theo đúng layer tương ứng (manager/service/presentation). **Dungeon và [Quest System](Quest-System.md) hoàn toàn độc lập** — không có tích hợp nào giữa 2 hệ, phần thưởng dungeon đi qua `ILootService` chứ không qua quest.

Có 1 ngoại lệ cố tình cho constructor injection thuần túy: `RoomRuntimeService` được dựng trước khi `RoomCompletionService` cần đến nó, sau đó được nối ngược lại bằng setter — `roomRuntimeService.setCompletionService(roomCompletionService)` — vì 2 service này phụ thuộc lẫn nhau.

```java
public static void onDisable() {
    if (reviveManager != null) reviveManager.shutdown();
    if (partyManager != null) partyManager.saveAll();
    if (instanceManager != null) instanceManager.saveAll();
}
```

`partyManager`, `instanceManager`, `reviveManager` được giữ làm field `static` chỉ để `onDisable()` — 1 method static riêng — có thể chạm tới chúng.

## Định nghĩa vs. runtime

`data/definition/room/Room.java` — template, chỉ tham chiếu config (không phải object sống):

```java
private String id;
private RoomType type;
private String schemetaId;
private List<ObjectiveConfig> objectives;
private List<RoomEventConfig> roomEvents;
```

`data/runtime/RoomInstance.java` — bản đối ứng sống của nó, chỉ tham chiếu template qua id, thêm state mutable riêng cho từng phiên:

```java
private String roomId;                                // khóa ngoại trỏ về Room definition
private SerializableBounds bounds;
private boolean completed;
private List<Map<String, Object>> objectiveStates;     // snapshot đã serialize
private transient List<IObjective> activeObjectives;   // sống, được dựng lại lúc load — xem repository bên dưới
```

## Ví dụ JSON cho designer — định nghĩa nội dung dungeon

Toàn bộ nội dung `data/definition/` dưới đây dùng Gson field-name thuần (không qua Codec, không `@SerializedName`) — tên field Java chính là tên key JSON.

### Dungeon + phòng (`Dungeon`, `RoomPool`, `RoomEntry`, `Room`)

```json
{
  "id": "crimson_tower",
  "name": "Tháp Đỏ Thẫm",
  "description": "Một ngọn tháp bị nguyền rủa, nơi Hell Knight trú ngụ.",
  "lootTableId": "dungeons/crimson_tower_final",
  "playTime": 30,
  "active": true,
  "minimumRooms": 5,
  "pools": [
    {
      "id": "normal_pool",
      "icon": "IRON_SWORD",
      "name": "Phòng thường",
      "weight": 1.0,
      "min": 3,
      "max": 5,
      "rooms": [
        { "roomId": "crimson_hall_a", "weight": 1.0 },
        { "roomId": "crimson_hall_b", "weight": 0.5 }
      ]
    }
  ],
  "treasureRooms": ["crimson_treasure_a"],
  "bosses": ["hell_knight"]
}
```

`Room` (1 phòng cụ thể, tham chiếu 1 schematic + danh sách objective/room-event):

```json
{
  "id": "crimson_hall_a",
  "type": "NORMAL",
  "schemetaId": "crimson_hall_a_schem",
  "objectives": [
    { "type": "monster_hunter", "params": { "require": 15, "target": "zombie_knight", "score": 50 } }
  ],
  "roomEvents": [
    { "type": "creeping_dread", "params": { "maxTriggers": 4, "spawnChance": 0.25, "minInterval": 40, "maxInterval": 100 } }
  ]
}
```

`RoomType` hợp lệ: `SPAWN, WARMUP, NORMAL, ELITE, CHECKPOINT, SAFE, TRADE, PUZZLE, BOSS, TREASURE, TRAP, LOOT, EVENT`.

### Objective (`ObjectiveConfig` → `ObjectiveFactory`, dispatch bằng switch trên `type`, không qua registry)

Mọi objective đều đọc thêm `score` (int, mặc định 0) từ `params`.

| `type` | `params` |
| :--- | :--- |
| `spawner_breaker` | `require` (int, mặc định 3; alias `required`) |
| `monster_hunter` | `require` (int, mặc định 10), `target` (id mob) |
| `item_collector` | `require` (int, mặc định 1), `target` (id item) |
| `demon_slayer` | như `monster_hunter` nhưng `require` mặc định 1, tính điểm theo phạm vi cả dungeon thay vì riêng từng phòng |

### Spawner (`Spawner` + `BehaviorData` → `BehaviorFactory`, cũng dispatch bằng switch)

```json
{
  "id": "undead_wave_nest",
  "name": "Ổ gọi quái Wave",
  "mobs": { "zombie_knight": 3, "skeleton_archer": 2 },
  "delay": 100, "minDelay": 80, "maxDelay": 140,
  "activeRange": 20, "spawnRange": 8,
  "maxNearBy": 10, "spawnCount": 2,
  "behaviors": [ { "type": "wave", "params": { "waves": 5 } } ]
}
```

Behavior `type` hợp lệ: `protector` (`requireBreak` int, mặc định 3), `wave` (`waves` int, mặc định 3), `teleport` (`max_teleports` mặc định 3, `teleport_radius` mặc định 20.0, `safe_check_radius` mặc định 3.0), `cursed` (không param — random hiệu ứng nguyền lúc spawn).

### Room event khác — `dark_eyes` (`isDisrupt`: boolean), `noop` (không param), `creeping_dread` (xem ví dụ trên).

---

## Pattern Repository

`repository/IInstanceRepository.java` — cố tình tối giản:

```java
public interface IInstanceRepository {
    void save(DungeonInstance instance);
    boolean delete(String sessionId);
    List<DungeonInstance> loadAll();
}
```

`repository/impl/InstanceRepository.java` ghi JSON mỏng (qua 1 `Gson` được inject), mỗi instance 1 file. Vì Gson không thể tự dựng lại field `transient` đa hình, `loadAll()` phải tự sửa chữa sau khi deserialize — `restoreRoomRuntime` dựng lại `activeObjectives`/room event qua `ObjectiveFactory.restore(...)`/`RoomEventFactory.restore(...)`. `InstanceManager` làm bước ngược lại (`snapshotRoomRuntime`) trước mỗi lần save — xem bên dưới.

## Pattern Manager

`manager/InstanceManager.java`:

```java
private final Map<String, DungeonInstance> instances = new LinkedHashMap<>();

public InstanceManager(IInstanceRepository instanceRepository, ScoreBoardManager scoreBoardManager, Log4Craft_ logger) {
    this.instanceRepository = instanceRepository;
    this.scoreBoardManager = scoreBoardManager;
    this.logger = logger;
    loadAll(); // load eager lúc khởi tạo
}

public void saveAll() {
    // snapshot state objective/room-event sống, transient của từng instance thành dạng có thể serialize
    // (ngược lại với restoreRoomRuntime của InstanceRepository), rồi ủy quyền cho instanceRepository.save(instance)
}
```

## Ví dụ lát cắt dọc — điều hướng phòng

`service/impl/RoomNavigationService.java` — ví dụ mẫu mực cho pattern constructor injection, được kết hợp từ 9 collaborator ở tầng thấp hơn:

```java
public RoomNavigationService(IPartyService partyService, InstanceManager instanceManager, RoomManager roomManager,
        ISchematicService schematicService, IDungeonService dungeonService, IRoomService roomService,
        RoomRuntimeService roomRuntimeService, DungeonPresenter presenter, TaskScheduler taskScheduler)
```

Luồng đầy đủ, actor → controller → service:

1. `actor/listener/DoorInteractListener` bắt sự kiện player click phải vào 1 block vault, gọi `DungeonFlowController.handleOpenNextDoor(door, player)`.
2. Controller chỉ đơn giản chuyển tiếp: `navigationService.handleOpenNextDoor(door, player);`
3. `RoomNavigationService.handleOpenNextDoor` tra `Party`/`DungeonInstance`/`DungeonProgress` của player, roll các phòng tiếp theo khả dĩ qua `dungeonService.rollNextRoomFromPool(...)` (nếu chưa roll), rồi mở `new OpenDoorGui(door, nextRooms, this::handleSelectNextRoom)` — lưu ý GUI được truyền 1 **callback trỏ vào service**, chứ không phải 1 tham chiếu tới chính service.
4. Chọn 1 phòng trong GUI sẽ gọi `handleSelectNextRoom`, hàm này dán schematic của phòng (`schematicService.paste(...)`), dựng 1 `RoomInstance`, gọi `roomRuntimeService.setupRoomRuntime(...)` để kích hoạt objective/room event, cập nhật `DungeonProgress` (`markCurrentRoomCleared`, `setCurrentRoom`), bắn `roomRuntimeService.triggerRoomStart`/`triggerRoomEnd`, và dịch chuyển những player bị bỏ lại qua `taskScheduler.runLater(100L, ...)`.

## Ví dụ ở tầng actor

```java
// command — actor/command/DungeonCommand.java
public DungeonCommand(DungeonFlowController flowController, DungeonManager dungeonManager) { ... }
// handler: flowController.handleStartDungeon(dungeonId, player);

// listener — actor/listener/DoorInteractListener.java
public DoorInteractListener(DungeonFlowController flowController, IPartyService partyService) { ... }
// @EventHandler onPlayerInteractDoor(...) lọc block Vault, rồi:
//   dungeonFlowController.handleOpenNextDoor(vault.getBlock(), player);

// ui — actor/ui/OpenDoorGui.java extends com.roguesmp.gui.BaseGui  (xem GUI Framework)
public OpenDoorGui(Block door, List<Room> rooms, SelectRoomCallback selectRoomCallback) { ... }

// scoreboard — actor/scoreboard/TabBoardRenderer.java
public TabBoardRenderer(Player player, DungeonExpansion papiExpansion) { ... }
// resolve %dungeon_name%/%dungeon_time%/%dungeon_score%/%dungeon_obj_N% dựa trên state sống của DungeonInstance/RoomInstance
```

Mọi class ở tầng actor đều mỏng: hoặc phản ứng với 1 event/command/click của Bukkit rồi gọi thẳng vào 1 controller, hoặc (với GUI) đưa cho service 1 callback để gọi lại sau. `ScoreBoardManager.tickUpdate()` điều khiển `TabBoardRenderer`, được `DungeonRegistry` lên lịch mỗi 20 tick song song với `flowController.handleDungeonTimerTick()`.

## Lưu ý & lỗi thường gặp

- **Không bao giờ đặt state runtime/phiên chơi trong `data/definition/`.** Doc comment trên các class đó cảnh báo rõ ràng — nếu bị cám dỗ thêm 1 field mutable vào class định nghĩa `Room`/`Dungeon`, hãy đặt nó ở `RoomInstance`/`DungeonInstance` thay vào đó.
- **Field `transient` trên class runtime cần code save/restore rõ ràng** ở tầng repository (hoặc manager) — Gson không tự dựng lại được các object đa hình như `IObjective`/room event. Theo pattern `restoreRoomRuntime`/`snapshotRoomRuntime` trong `InstanceRepository`/`InstanceManager`.
- **Setter injection là ngoại lệ, không phải chuẩn mực** — chỉ dùng nó (như `RoomRuntimeService`/`RoomCompletionService`) khi 2 component thực sự phụ thuộc lẫn nhau; mọi thứ khác nên dùng constructor injection, nối trong `DungeonRegistry.onEnable`.
- **Actor không nên phụ thuộc trực tiếp vào manager/repository** — hãy đi qua 1 controller (nơi kết hợp các service). Nếu 1 `actor/listener` cần 1 manager, đó thường là dấu hiệu logic nên thuộc về 1 service thay vào đó.
- **Đừng quên phần dọn dẹp trong `onDisable()`** khi thêm 1 manager stateful mới — bất kỳ thứ gì giữ state phiên chơi in-memory cần sống sót qua 1 lần restart đều cần được thêm vào chuỗi save của `DungeonRegistry.onDisable()` (hoặc phải reachable gián tiếp từ thứ gì đó đã có sẵn ở đó).

---
◀ [Player Class System](Player-Class-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Quest System](Quest-System.md)
