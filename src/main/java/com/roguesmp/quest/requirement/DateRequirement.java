package com.roguesmp.quest.requirement;

import com.google.gson.JsonObject;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.QuestRequirement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DateRequirement implements QuestRequirement {

    // Định nghĩa múi giờ cố định của Việt Nam (ICT - GMT+7)
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final long targetTimestamp;
    private final String formattedDate;

    public DateRequirement(long targetTimestamp) {
        this.targetTimestamp = targetTimestamp;

        if (this.targetTimestamp != -1L) {
            LocalDate date = Instant.ofEpochMilli(this.targetTimestamp)
                    .atZone(VN_ZONE)
                    .toLocalDate();
            this.formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else {
            this.formattedDate = "Vô hạn";
        }
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        List<Component> display = new ArrayList<>();
        boolean isToday = canMeetRequirement(smpPlayer);

        NamedTextColor color = isToday ? NamedTextColor.GREEN : NamedTextColor.RED;
        String prefix = isToday ? "✔ Nhiệm vụ ngày: " : "❌ Chỉ có thể hoàn thành vào ngày: ";

        display.add(Component.text(prefix + formattedDate, color));
        return display;
    }

    @Override
    public boolean canMeetRequirement(SmpPlayer smpPlayer) {
        if (targetTimestamp == -1L) return true;

        // Tính toán thời gian ĐẦU NGÀY HÔM NAY (00:00) chính xác theo múi giờ Việt Nam tính bằng Mili-giây
        long todayStartMillis = LocalDate.now(VN_ZONE)
                .atStartOfDay(VN_ZONE)
                .toInstant()
                .toEpochMilli();

        return todayStartMillis == targetTimestamp;
    }

    public long getTargetTimestamp() {
        return targetTimestamp;
    }
}
