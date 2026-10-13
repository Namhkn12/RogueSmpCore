package com.roguesmp.quest.data;

import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.codec.Codec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quest log shared by a live group. Completed quests stay archived here until the group ends.
 */
public class GroupQuestData extends QuestLog {

    public static final Codec<GroupQuestData> CODEC = Codec.composite(
            Codec.UUID.fieldOf("id").forGetter(data -> data.getHolder().id()),
            Codec.STRING.fieldOf("owner").forGetter(data -> data.getHolder().owner()),
            Codec.lenientUnboundedMap(QuestProgress.CODEC).optionalFieldOf("questProgress", new HashMap<>()).forGetter(GroupQuestData::getQuestProgresses),
            GroupQuestData::new
    );

    private GroupHolder holder;

    public GroupQuestData(UUID id, String owner, Map<String, QuestProgress> progresses) {
        this.holder = GroupHolder.unbound(id, owner);
        this.progresses.putAll(progresses);
        this.dirty = false;
    }

    public GroupQuestData(GroupHolder holder) {
        this.holder = holder;
    }

    public GroupQuestData(GroupQuestData toClone) {
        this.holder = toClone.holder;
        this.progresses.putAll(toClone.getQuestProgresses());
    }

    @Override
    public GroupHolder getHolder() {
        return holder;
    }

    public void bind(GroupHolder holder) {
        this.holder = holder;
    }
}
