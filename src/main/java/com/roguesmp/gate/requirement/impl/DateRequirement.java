package com.roguesmp.gate.requirement.impl;

import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateRequirement implements Requirement {

    public static final String TYPE_KEY = "date_single";
    public static final Codec<DateRequirement> CODEC = Codec.composite(
            Codec.LONG.fieldOf("targetTimestamp").forGetter(DateRequirement::getTargetTimestamp),
            DateRequirement::new
    );

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
    public Component getDisplay(SmpPlayer smpPlayer) {
        return Component.text("Chỉ trong ngày ", NamedTextColor.GRAY).append(Component.text(formattedDate, NamedTextColor.WHITE));
    }

    @Override
    public boolean test(SmpPlayer smpPlayer) {
        if (targetTimestamp == -1L) return true;

        // Tính toán thời gian ĐẦU NGÀY HÔM NAY (00:00) chính xác theo múi giờ Việt Nam tính bằng Mili-giây
        long todayStartMillis = LocalDate.now(VN_ZONE)
                .atStartOfDay(VN_ZONE)
                .toInstant()
                .toEpochMilli();

        return todayStartMillis == targetTimestamp;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public long getTargetTimestamp() {
        return targetTimestamp;
    }
}
