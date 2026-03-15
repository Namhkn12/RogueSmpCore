package com.roguesmp.dungeon.objective;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;

public interface IObjective {
    void start(DungeonInstance instance, Party party);    // gọi khi room bắt đầu
    void process();                   // tick / update định kỳ nếu cần
    void end(DungeonInstance instance, Party party);      // gọi khi complete hoặc fail
    boolean isCompleted();            // RoomInstance hỏi objective xong chưa
    ObjectiveData getData();          // trả về data gốc nếu cần serialize
}
