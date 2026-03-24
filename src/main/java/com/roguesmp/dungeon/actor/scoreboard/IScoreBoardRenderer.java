package com.roguesmp.dungeon.actor.scoreboard;

import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.dto.DungeonScoreBoard;
import com.roguesmp.dungeon.instance.DungeonInstance;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interface chung cho mọi loại Scoreboard renderer
 * (FastBoard, TAB Scoreboard, hoặc sau này là khác)
 */
public interface IScoreBoardRenderer {

    /**
     * Khởi tạo scoreboard lần đầu với dữ liệu dungeon
     */
    void init(Dungeon template, DungeonInstance instance, List<DungeonScoreBoard.PartyMember> members);

    void updateTime(int remainingSeconds);

    void updateScore(int score);

    void updateObjectives(List<List<String>> objectives);

    void updateMember(int slotIndex, DungeonScoreBoard.PartyMember member);

    void clearMemberSlot(int slotIndex);

    /**
     * Đẩy dữ liệu lên scoreboard thực tế
     */
    void flush();

    /**
     * Hủy scoreboard và trả quyền lại cho plugin khác (nếu cần)
     */
    void destroy();
}
