package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.classes.PlayerClass;

public class ClassSelectedObjective extends StateObjective {

    public static final String TYPE_KEY = "class_selected";

    public static final Codec<ClassSelectedObjective> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("class", "").forGetter(ClassSelectedObjective::getClassId),
            ClassSelectedObjective::new
    );

    private final String classId;

    public ClassSelectedObjective(String classId) {
        this.classId = classId;
    }

    public String getClassId() {
        return classId;
    }

    @Override
    protected boolean test(SmpPlayer smpPlayer) {
        PlayerClass current = smpPlayer.getPlayerClass();
        return current != null && (classId.isEmpty() || classId.equals(current.getId()));
    }

    @Override
    protected String label() {
        return classId.isEmpty() ? "Chọn lớp nhân vật" : "Chọn lớp nhân vật " + classId;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }
}
