# dungeon_script — bộ sinh dungeon tĩnh

Script và lệnh FAWE để dựng các dungeon tĩnh của server **When the Dungeon Arise**. Mỗi thư mục con là một dungeon độc lập.

| Dungeon | Mô tả | Bắt đầu |
|---|---|---|
| [Sylphens](sylphens/README.md) | đảo nổi r100 trong void world: mặt đảo thung lũng + 7 thác, Layer 1 hang Lush, Layer 2 5 tầng sinh theo seed, Layer 3 boss | [runbook](sylphens/docs/03_build_runbook.md) |

Quy ước chung và các bẫy FAWE đã gặp: xem [CLAUDE.md](CLAUDE.md).

Bắt đầu dungeon mới:
1. Tạo thư mục `<ten_dungeon>/`, copy [dungeon_request.template.json](dungeon_request.template.json) vào thành `<ten_dungeon>/request.json`.
2. Điền yêu cầu (xem [dungeon_request.example.json](dungeon_request.example.json)). Mẹo viết: [NEW_DUNGEON_TEMPLATE.md](NEW_DUNGEON_TEMPLATE.md).
3. Nhắn Claude: "đọc `<ten_dungeon>/request.json` và bắt đầu". Góp ý sau đó ghi vào mục `feedback` của file.
