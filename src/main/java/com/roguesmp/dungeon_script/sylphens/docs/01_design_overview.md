# 01 — Thiết kế tổng quan

## Bối cảnh
- Server: **When the Dungeon Arise**, RPG Minecraft lấy cảm hứng *Solo Leveling* và *Shield Hero*. Có hệ rank dungeon F → S+.
- Ban đầu có 2 loại dungeon:
  - **Tĩnh**: cố định, nhiều người vào chung, là chỗ farm chung, cấm phá khối.
  - **Động**: chia theo phòng, đánh xong phòng này mở phòng khác, mỗi team một instance.
- Dungeon động bị tạm dừng vì thiếu nhân lực.
- **Bước ngoặt (2026-10-08):** chạy dungeon tĩnh theo instance giống dungeon động, chỉ khác là không sinh phòng vì phòng đã xây sẵn. Nhờ vậy người chơi **được phá khối**, vì instance hỏng thì bỏ đi.
- Hệ quả là bản cũ phải thiết kế lại. Bản cũ có bán kính 170, mặt đảo rộng và chi tiết, bên dưới giấu dungeon, người chơi đi đâu tùy thích. Bản mới phải **mang tính dungeon hơn**:
  - Vẫn có địa hình tự nhiên để khai thác tài nguyên đặc trưng.
  - Đường đi phải rõ ràng.

## Yêu cầu Abx đưa ra cho v2
1. **Bán kính ~100** (đường kính 200). Dựng từ void world, mốc **0,64,0**.
2. **Mặt đảo:**
   - Thung lũng toàn phần, núi cao vây kín, không có phía hở.
   - Sườn núi dốc.
   - Thung lũng lõm dần về giữa.
   - Nhiều thác nhỏ từ núi đổ xuống thành suối, suối chảy về **miệng hố lớn ở tâm**, nước đổ xuống hố.
3. **Layer 1:**
   - Hang Lush, không cần vòm tròn, nhưng phải có **cột đá nhiều cỡ**.
   - Không có sông ngầm.
   - Cao khoảng 15 khối.
   - Có vách ngăn đá ngẫu nhiên.
   - Biome đa dạng (thạch nhũ, nấm, rêu…) với diện tích không đều.
   - Đường vào là hố sụt, người chơi xuống theo dây leo hoặc dòng nước.
4. **Đường xuống:** một ống thẳng từ mặt đất xuyên qua Layer 1, xuống tới **Sảnh Chính** của Layer 2.
5. **Layer 2:**
   - Sảnh chính ở giữa, khoảng 4 cửa.
   - Khoảng **5 tầng**, nối bằng cầu thang và dốc.
   - Nhiều phòng phụ (chủ yếu combat, rương nhỏ) và phòng chính khai thác cao.
   - Mê cung, có checkpoint, dịch chuyển, bẫy.
   - Đi đường nào cuối cùng cũng tới **phòng Thủ Vệ**. Hạ Thủ Vệ thì được dịch chuyển xuống boss.
6. **Layer 3 (boss):**
   - Chỉ vào bằng dịch chuyển.
   - Vào rồi thì chỉ ra được bằng cách thắng hoặc chết.
   - Boss chỉ spawn khi Thủ Vệ đã chết.
   - Bọc bedrock là tùy chọn, đã làm.
7. **Bọc barrier** toàn đảo từ dưới lên trên để người chơi không đào ra ngoài.

## Phản hồi đã áp dụng cho Layer 2 (vòng 2)
Bản Layer 2 đầu tiên bị chê ở các điểm sau, và bản hiện tại đã sửa hết:
- Tầng cách nhau 10 khối là quá thấp. **Đã sửa:** tầng cách 16 khối, phòng cao 5–12, thêm giếng trời cao 25.
- Bố cục đối xứng 4 tuyến. **Đã sửa:** phòng đặt ngẫu nhiên, không đè nhau.
- Quá ít phòng, chỉ đi vài hướng là xuống tầng. **Đã sửa:** 13–23 phòng mỗi tầng, có phòng thử thách lặp mẫu, hành lang dạng mạng lưới, ngõ cụt và mê lộ.
- Lối xuống tầng quá dễ thấy. **Đã sửa:** thang A nằm ở ô sâu nhất của mê lộ, thang B nằm sau tường giả.
- Cần tự nhiên và đa dạng hơn. **Đã sửa:** thêm đường hang thô, hang đá dạng blob, 7 loại phòng tài nguyên.
- Sau đó Abx yêu cầu thêm: **tấm bedrock ngăn giữa các tầng** để người chơi không đào thẳng xuống. Đã làm.

## Luồng người chơi
```
Dịch chuyển tới hang spawn (0,122,80) trong sườn núi nam
  → ra cửa hang, theo đường gấp khúc xuống thung lũng (rừng thông, cỏ hoa; sông phía bắc chảy vào hố)
  → xuống hố sụt: dây leo cave vines 6 hướng, hoặc theo dòng sông đổ vào hồ Layer 1
Layer 1: hang Lush, khai thác quặng thường, nấm, pha lê
  → ống r3.5 xuyên đảo đá giữa hồ → rơi vào hồ sâu 3 giữa Sảnh Chính
Layer 2 tầng 1 → 5:
  combat, rương, bẫy, phòng tài nguyên, checkpoint, dịch chuyển thoát
  lối xuống: thang A (cuối mê lộ) | thang B (sau tường giả) | giếng trời (nhảy, một chiều)
  tầng 5: mê lộ → phòng Thủ Vệ Rễ (một cửa duy nhất)
Thủ Vệ chết → pad tâm (0,-18,0) dịch chuyển xuống boss (Layer 3)
Boss: thắng hoặc chết mới ra
```

## Nguyên tắc dẫn đường (dùng thiết kế, không dùng mũi tên)
- Địa hình dồn về một điểm: mọi dòng suối và độ dốc đều chỉ về hố sụt.
- Ánh sáng: hành lang có đèn trần mỗi 6 ô, **mê lộ chỉ mỗi 11 ô** nên tối hơn, báo hiệu "khu khó".
- Vật liệu phân biệt loại phòng: checkpoint lát polished deepslate, phòng dịch chuyển lát blackstone, mê lộ lát cobblestone rêu, đường hang lát tuff.
