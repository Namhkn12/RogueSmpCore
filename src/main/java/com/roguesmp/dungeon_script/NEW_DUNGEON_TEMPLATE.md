# Mẫu prompt cho dungeon mới

Copy phần dưới, điền vào, xóa dòng không cần. Chỗ nào chưa nghĩ ra thì ghi **"tùy bạn"** hoặc **"đề xuất giúp tôi"**:
Claude sẽ tự chọn rồi giải thích, hoặc hỏi lại đúng chỗ đó.

---

## 0. Thông tin chung
- Tên dungeon (tiếng Anh, dùng làm tên thư mục / tiền tố schem): `...`
- Rank / độ khó: `...`
- Chủ đề, cảm giác chung (1–3 câu): `...`
- Ảnh tham khảo / schem mẫu (nếu có): đường dẫn file `.png`, `.schem`, `.litematic`

## 1. Không gian và vị trí
- Dựng ở đâu: world riêng / void world / tâm tọa độ `x, y, z`
- Kích thước tổng (rộng × dài × cao) hoặc bán kính: `...`
- Giới hạn cứng: chiều cao Y min–max, barrier, không được chạm vùng nào
- Instance hay dùng chung: `...`
- Người chơi được phá khối không, phá được khối nào: `...`

## 2. Luồng người chơi (quan trọng nhất)
Viết theo thứ tự người chơi đi qua, mỗi dòng 1 khu:
```
Điểm xuất hiện (dịch chuyển tới đâu) → khu A → khu B → ... → boss → thoát ra bằng cách nào
```

## 3. Từng khu (lặp lại cho mỗi khu)
### Khu: `tên`
- Loại: mặt đất / hang / tầng phòng / mê cung / phòng boss / ...
- Hình dạng + kích thước: `...`
- Vật liệu chính (khối Minecraft, hoặc "dùng deepslate và biến thể"): `...`
- Chi tiết bắt buộc phải có: `...`
- Chi tiết KHÔNG muốn: `...`
- Ánh sáng: đuốc / đèn / light block / tối
- Gameplay ở khu này: combat, bẫy, rương, khai thác, checkpoint...
- Lối vào / lối ra nối với khu nào, có khóa / giấu không

## 4. Boss (nếu có)
- Kiểu đánh: cận chiến / bắn xa / triệu hồi / nhiều phase
- Cần gì trong phòng: sân trống, cột nấp, độ cao khác nhau, nước, vòng triệu hồi...
- Điểm boss xuất hiện, điểm người chơi đến, cách thoát

## 5. Kỹ thuật
- Phiên bản server + FAWE: `MC 26.2, FAWE ...`
- Muốn nhận dạng gì: schem (khuyên dùng) / lệnh FAWE / cả hai
- Dữ liệu cho plugin cần xuất: tọa độ spawn, boss, mob, rương, checkpoint...

## 6. Ưu tiên
- Làm trước: `...`
- Để sau / chưa cần: `...`

---

## Mẹo
- **Một prompt một khu** khi khu đó phức tạp (ví dụ phòng boss). Bố cục tổng thì gửi một lần đầu.
- **Số liệu quan trọng thì ghi số** (cao 3 khối, rộng 80–100). Còn lại mô tả cảm giác là đủ.
- **Liệt kê lớp vật liệu theo thứ tự** như vết nứt boss Sylphens ("đáy blue concrete → 2 lớp kính xanh đậm → 3 lớp xanh nhạt → light block") là cách dễ hiểu nhất.
- **Báo lỗi trong game**: gửi nguyên văn thông báo FAWE + lệnh đã chạy, hoặc ảnh chụp màn hình. "Không dùng được" thì Claude phải đoán.
- **Góp ý sau khi xem**: ghi tên khu + điều muốn đổi ("cột to quá, giảm còn 2/3"), kèm ảnh nếu được.
- Đổi phiên bản server / FAWE thì nói ngay ở đầu prompt.
