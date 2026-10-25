# Region Flags

Plugin đăng ký thêm các flag WorldGuard riêng (tiền tố `smp-`) trong [`SmpRegionFlags`](../src/main/java/com/roguesmp/region/SmpRegionFlags.java). Trang này mô tả từng flag: giá trị nhận vào, mặc định, và hành vi.

## Đặt flag

Dùng lệnh của plugin thay vì `/rg flag`:

```
/smpregion flag <region> <flag> <giá trị>
/smpregion unflag <region> <flag>
```

Region của plugin có thể gồm **nhiều mảnh** (region nhóm). Khi đó flag nằm trên 1 region "settings" ẩn, mọi mảnh kế thừa từ nó. `/smpregion flag` luôn ghi vào đúng region settings này; `/rg flag` lên 1 mảnh `<id>_shape_<n>` chỉ ảnh hưởng mảnh đó. GUI editor (`/smpregion wand`) cũng đặt flag theo cách giống `/smpregion flag`.

Giá trị sai cú pháp bị từ chối ngay khi đặt, kèm thông báo lỗi.

## Bảng flag

| Flag | Kiểu | Mặc định | Tác dụng |
| :--- | :--- | :--- | :--- |
| `smp-abilities` | `allow` / `deny` | `allow` | Cho phép người chơi dùng ability |
| `smp-spawners` | `allow` / `deny` | `deny` | Bật spawner tuỳ chỉnh — xem [JSON-Spawners](JSON-Spawners.md#bật-spawner-bằng-worldguard) |
| `smp-display-name` | chuỗi MiniMessage | `<gray>Unknown` | Tên khu vực hiển thị trên scoreboard (`Khu vực: ...`) |
| `smp-ambient-particles` | xem [bên dưới](#smp-ambient-particles) | — | Particle ngẫu nhiên bên trong region |
| `smp-spawn` | xem [bên dưới](#smp-spawn) | tâm region | Điểm spawn của region |
| `smp-enter-teleport` | xem [bên dưới](#smp-enter-teleport) | — | Dịch chuyển người chơi khi đi vào region |

`smp-grouped` là flag nội bộ đánh dấu region nhóm — không tự đặt tay.

## `smp-ambient-particles`

```
<particle> [count] [chance] [view]
```

| Trường | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `particle` | bắt buộc | Tên particle Minecraft (`portal`, `end_rod`, `minecraft:soul`...). Chỉ nhận particle **không cần dữ liệu thêm** — `dust`, `block`, `item`... bị từ chối |
| `count` | `1` | Số particle mỗi đợt, ≥ 1 |
| `chance` | `0.25` | Xác suất mỗi chu kỳ (10 tick) phát 1 đợt, trong khoảng `(0, 1]` |
| `view` | `24` | Người chơi cách region (tính tới mép region) trong khoảng này mới thấy |

```
/smpregion flag gate smp-ambient-particles portal 4 0.8 24
```

- Mỗi particle xuất hiện tại 1 điểm ngẫu nhiên **bên trong hình dạng region** (đúng cả polygon và region nhiều mảnh; mảnh lớn được chọn nhiều hơn theo thể tích).
- Người chơi **ở ngoài** region vẫn thấy, miễn trong tầm `view` — dùng region nhỏ + flag này để đánh dấu cổng dịch chuyển.
- Particle chỉ gửi cho từng người chơi trong tầm, không có ai gần thì không tốn gì.
- Danh sách region có flag được quét lại **mỗi 5 giây**: đặt/sửa/xoá flag cần tối đa 5 giây để có hiệu lực.
- Region cao hết chiều cao world sẽ rải particle lên cả trên trời — nên giới hạn Y của region sát vùng muốn hiển thị.

## `smp-spawn`

```
<x> <y> <z> [yaw pitch]
```

```
/smpregion flag boss_arena smp-spawn 100.5 64 -20.5 90 0
```

- Điểm spawn của **cả region logic** (kể cả region nhiều mảnh), dùng world của chính region nên không ghi tên world.
- `yaw`/`pitch` phải đi cùng nhau; bỏ qua thì = `0 0`.
- Được dùng bởi `/smpregion tp <region>`, nút teleport trong GUI editor, và target `region` của [`smp-enter-teleport`](#smp-enter-teleport).
- Không đặt → dùng tâm mảnh đầu tiên, Y = block cao nhất tại đó (kẹp trong giới hạn Y của mảnh) + 1.

Khác flag `spawn` của WorldGuard: flag gốc nằm trên từng region WorldGuard, không hiểu region nhóm của plugin.

## `smp-enter-teleport`

2 dạng target:

```
region <regionId> [world] [fx|none]
[world] <x> <y> <z> [yaw pitch] [fx|none]
```

| Dạng | Ví dụ | Đến |
| :--- | :--- | :--- |
| Region, cùng world | `region boss_arena` | [`smp-spawn`](#smp-spawn) của region đó trong world người chơi đang đứng (không có thì tâm region) |
| Region, world khác | `region hub minecraft:spawn_world` | Như trên, trong world chỉ định |
| Toạ độ, cùng world | `0.5 80 0.5 0 0` | Đúng toạ độ trong world hiện tại; bỏ `yaw pitch` thì giữ hướng nhìn hiện tại |
| Toạ độ, world khác | `minecraft:spawn_world 0.5 80 0.5` | Đúng toạ độ trong world chỉ định |

`world` là **key đầy đủ có namespace** của world (`minecraft:overworld`, `minecraft:spawn_world`...), không phải tên thư mục; thiếu namespace bị từ chối. Xem key bằng `/execute in` hoặc `World#getKey()`.

**Bỏ world** = world người chơi đang đứng lúc dịch chuyển. Dùng dạng này cho **dungeon**: world instance có tên ngẫu nhiên, nên cổng trong template world phải trỏ `region <id>` hoặc toạ độ không kèm world — khi template được sao ra instance, cổng tự trỏ đúng world của instance đó.

Hậu tố cuối (mặc định `fx`):

- `fx` — người chơi bị Slowness II + Darkness trong 2 giây, nghe tiếng `entity.enderman.teleport`, rồi được dịch chuyển sau **1 giây**.
- `none` — dịch chuyển ngay tick sau, không hiệu ứng.

```
/smpregion flag gate smp-enter-teleport region boss_arena
/smpregion flag exit smp-enter-teleport minecraft:spawn_world 0.5 80 0.5 0 0 none
```

- **Đi được sang world khác** (kể cả world Multiverse).
- Chỉ kích hoạt khi bước vào **chính region có đặt flag**; đi giữa 2 region chồng nhau cùng có flag không kích hoạt lại.
- Target được tra **lúc dịch chuyển**, không phải lúc đặt flag: đổi `smp-spawn` của region đích thì mọi cổng trỏ tới nó tự cập nhật; world chưa nạp lúc đặt flag vẫn hợp lệ.
- Không tìm thấy world/region đích → ghi cảnh báo vào log, người chơi đứng yên.
- Trong lúc chờ 1 giây, đi ra rồi vào lại không kích hoạt lần 2.
- Đừng để điểm đến nằm trong 1 region có `smp-enter-teleport` khác, nếu không người chơi sẽ bị dịch chuyển tiếp.

## Công thức: cổng dịch chuyển

```
/smpregion define gate                     # region nhỏ, vừa khung cổng
/smpregion flag gate smp-ambient-particles portal 4 0.8 24
/smpregion flag gate smp-enter-teleport region boss_arena
/smpregion flag boss_arena smp-spawn 100.5 64 -20.5 90 0
```

Particle cho người chơi thấy cổng từ xa; bước vào → hiệu ứng → tới spawn của `boss_arena`.

## Code

| Lớp | Vai trò |
| :--- | :--- |
| [`SmpRegionFlags`](../src/main/java/com/roguesmp/region/SmpRegionFlags.java) | Khai báo + đăng ký flag (trong `onLoad`) |
| [`region/flag/`](../src/main/java/com/roguesmp/region/flag/) | Kiểu giá trị (`AmbientParticles`, `SpawnPoint`, `EnterTeleport`) và parser; `TextValueFlag<T>` là base cho flag 1 dòng chữ |
| [`RegionAmbientEmitter`](../src/main/java/com/roguesmp/region/RegionAmbientEmitter.java) | Task phát particle |
| [`RegionTeleportListener`](../src/main/java/com/roguesmp/region/listener/RegionTeleportListener.java) | Nghe `RegionEnterEvent`, chạy hiệu ứng + dịch chuyển |
| [`ServerRegionManager.spawnLocation`](../src/main/java/com/roguesmp/region/ServerRegionManager.java) | Tra điểm spawn của region |

Flag giá trị phức tạp mới: kế thừa `TextValueFlag<T>`, cài `parse(String[])` + `format(T)` (`format` phải parse lại được — WorldGuard lưu flag bằng chuỗi này), rồi thêm vào `SmpRegionFlags.register()`.
