package com.roguesmp.dungeon.script;

import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.FeatureContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public record ScriptContext(FeatureContext<?> feature, @Nullable Player player, @Nullable DungeonEvent event,
                            @Nullable Map<String, DataValue> args, Map<String, DataValue> temps, int depth) {

    public static ScriptContext of(FeatureContext<?> feature, @Nullable DungeonEvent event) {
        return new ScriptContext(feature, event == null ? null : event.player(), event, null, new HashMap<>(), 0);
    }

    public static ScriptContext of(FeatureContext<?> feature, @Nullable Player player) {
        return new ScriptContext(feature, player, null, null, new HashMap<>(), 0);
    }

    public ScriptContext withPlayer(@Nullable Player player) {
        return new ScriptContext(feature, player, event, args, temps, depth);
    }

    // Function calls: a fresh scope when the function has parameters, one level deeper either way.
    public ScriptContext call(@Nullable Map<String, DataValue> args) {
        return args == null
                ? new ScriptContext(feature, player, event, this.args, temps, depth + 1)
                : new ScriptContext(feature, player, null, args, new HashMap<>(), depth + 1);
    }

    public @Nullable DataValue context(String key) {
        if (args != null) return args.get(key);
        if (event == null) return null;
        return switch (key) {
            case "player" -> player == null ? null : DataValue.of(player.getName());
            case "subject" -> DataValue.of(event.subject());
            case "event" -> DataValue.of(event.id());
            default -> event.data().get(key);
        };
    }

    public @Nullable DataValue variable(String name, boolean perPlayer) {
        return feature.instance().variable(FeatureContext.varName(name, perPlayer, player));
    }

    public void setVariable(String name, boolean perPlayer, DataValue value) {
        String key = FeatureContext.varName(name, perPlayer, player);
        DataValue old = feature.instance().variable(key);
        feature.instance().setVariable(key, value);
        feature.save();
        if (!value.equals(old)) feature.fire(new DungeonEvent.VarChanged(name, old, value, player));
    }
}
