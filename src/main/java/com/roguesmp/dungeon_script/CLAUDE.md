# CLAUDE.md — dungeon_script (bộ sinh dungeon tĩnh)

Mỗi thư mục con là **một dungeon**, tự chứa script, lệnh FAWE, schematic và tài liệu của riêng nó. Khi làm việc với dungeon nào, đọc thêm `<dungeon>/CLAUDE.md`.

| Thư mục | Dungeon | Trạng thái |
|---|---|---|
| `sylphens/` | **Sylphens**: đảo nổi r100, 3 layer, Layer 2 sinh theo seed | đang dựng (xem `sylphens/docs/07_status_and_todo.md`) |

## Người dùng và cách làm việc
- Người dùng: **Abx**, dev, đang xây server Minecraft RPG "When the Dungeon Arise".
- **Trả lời bằng tiếng Việt.** Thuật ngữ kỹ thuật, tên file, tên biến giữ tiếng Anh.
- Abx thích kết quả dùng được ngay (lệnh chạy được, file hoàn chỉnh), sửa đúng phạm vi nhỏ nhất, chỉ rõ dòng hoặc biến cần đổi. Khi giải thích thì đưa ví dụ cụ thể trước, cơ chế sau.
- Abx sẽ chỉ ra khi đề xuất sai và mong được sửa lại đúng. **Đừng đoán cú pháp FAWE**, xem `sylphens/docs/06_fawe_notes.md`.
- Abx tự lo phần plugin (dịch chuyển, mob, loot, khóa cửa). Phần việc ở đây là **dựng khối** (terrain, phòng, schematic) và **dữ liệu vị trí** cho plugin.

## Cấu trúc chuẩn của một dungeon
```
<dungeon>/
  CLAUDE.md        bất biến, quy trình kiểm tra, bẫy riêng của dungeon
  README.md        tổng quan
  requirements.txt
  scripts/         script sinh (chạy từ thư mục <dungeon>/, đường dẫn tính theo Path(__file__).parents[1])
  commands/        lệnh FAWE theo thứ tự tên file (sinh tự động, KHÔNG sửa tay)
  output/          schematic + dữ liệu phòng cho plugin (sinh tự động)
  images/          sơ đồ (sinh tự động)
  docs/            thiết kế, thông số, runbook, ...
```
Dungeon mới: tạo thư mục tên tiếng Anh viết thường, giữ đúng bố cục trên, rồi thêm một dòng vào bảng ở đầu file này.

## Quy ước chung
- **Tên schematic có tiền tố là tên dungeon** (ví dụ `sylphens_layer2.schem`), vì `plugins/FastAsyncWorldEdit/schematics/` dùng chung cho mọi dungeon.
- Mỗi lệnh trong `commands/` dài tối đa **256 ký tự** (giới hạn chat). Dùng `//g -r` (tọa độ thật) và `//paste -o`.
- Tính tất định: cùng seed phải ra cùng kết quả. Chỉ dùng RNG có seed và `zlib.crc32`, không dùng `hash()` của Python với chuỗi.

## Bẫy FAWE đã gặp (áp dụng cho mọi dungeon)
- **Server của Abx chạy Minecraft 26.2** (từ 2026-10-09). Tra cú pháp FAWE theo nhánh `main` / adapter `26.x` trên GitHub, không theo WorldEdit 7.4.2 trong Gradle cache. Ví dụ: `//forest` nhận id placed feature (`mega_spruce_checked`…) nhưng vẫn tạo 0 cây vì FAWE đặt cây vào chính khối mặt đất → cây, cỏ hoa nên dựng thẳng trong schem.
- `perlin(seed, x, y, z, freq, octaves, persistence)` trả về **0..1** (trung vị ~0.5), không phải −1..1. Đặt ngưỡng theo bảng phân vị trong `sylphens/docs/06_fawe_notes.md`. Cần noise có dấu thì dùng `perlin(...)*2-1`.
- Mặt nạ `<m` / `>m` chỉ xét ô trên/dưới, **khớp cả ô khí**: không bao giờ dùng làm mặt nạ "from" của `//replace` (sẽ lấp đầy khoảng trống). Đặt nó vào `//gmask` và cho `//replace` một danh sách khối cụ thể (xem `sylphens/docs/06_fawe_notes.md`).
- `//g -r` chạy trên vùng chọn **hình hộp vuông**. Điều kiện nào sinh khối đặc đều phải có giới hạn `r<=...`, nếu không khối sẽ tràn ra 4 góc ngoài đảo và đè lên barrier.

## Công cụ chung (`tools/`)
- `schem_io.py`: `read_schem(path)` đọc Sponge `.schem` v2/v3 và `.litematic` thành mảng `[x, y, z]` + bảng tên khối; `write_schem(path, W, pal, offset)` ghi `.schem` v2. MCEdit `.schematic` cũ (id số) chưa hỗ trợ.
- `fawe_noise.py`: `perlin(seed, x, y, z, freq, octaves, persistence)` y hệt FAWE (0..1), numpy, khớp jar jlibnoise 1.0.0 tới 1e-16. Dùng để tính địa hình / mặt nạ noise trong Python thay vì đoán.
- `schem_view.py <file> [ảnh.png] [--cut Y] [--open] [--x X] [--z Z]`: in kích thước + thống kê khối, vẽ ảnh nhìn từ trên, 2 mặt cắt đứng, isometric. Dùng khi Abx gửi schem để xem, hoặc để kiểm tra schem tự sinh trước khi giao (mở ảnh bằng Read).

## Môi trường
- `src/main/java` chỉ biên dịch file `.java`, nên các script Python ở đây không vào jar plugin. **Không đặt file `.java` trong các thư mục này**, vì Gradle sẽ biên dịch chúng vào plugin.
