package com.roguesmp.dungeon_core.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DungeonActions {

    public record Signal(String signal, int amount) implements DungeonAction {
        public static final Codec<Signal> CODEC = Codec.composite(
                Codec.STRING.fieldOf("signal").forGetter(Signal::signal),
                Codec.INT.optionalFieldOf("amount", 1).forGetter(Signal::amount),
                Signal::new
        );

        @Override
        public String getTypeId() {
            return "signal";
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            context.signal(signal, amount, player);
        }
    }

    public record Message(String text, boolean all) implements DungeonAction {
        public static final Codec<Message> CODEC = Codec.composite(
                Codec.STRING.fieldOf("text").forGetter(Message::text),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(Message::all),
                Message::new
        );

        @Override
        public String getTypeId() {
            return "message";
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            targets(context, player, all).forEach(target -> target.sendMessage(Utils.fromString(text)));
        }
    }

    public record Teleport(double x, double y, double z, float yaw, float pitch, boolean all) implements DungeonAction {
        public static final Codec<Teleport> CODEC = Codec.composite(
                Codec.DOUBLE.fieldOf("x").forGetter(Teleport::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Teleport::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Teleport::z),
                Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Teleport::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(Teleport::pitch),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(Teleport::all),
                Teleport::new
        );

        @Override
        public String getTypeId() {
            return "teleport";
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            World world = context.world();
            if (world == null) return;
            Location location = new Location(world, x, y, z, yaw, pitch);
            targets(context, player, all).forEach(target -> target.teleportAsync(location));
        }
    }

    public record End(String reason) implements DungeonAction {
        public static final Codec<End> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("reason", "đã kết thúc").forGetter(End::reason),
                End::new
        );

        @Override
        public String getTypeId() {
            return "end";
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            context.end(reason);
        }
    }

    public static final Codec<Signal> SIGNAL = register("signal", Signal.CODEC);
    public static final Codec<Message> MESSAGE = register("message", Message.CODEC);
    public static final Codec<Teleport> TELEPORT = register("teleport", Teleport.CODEC);
    public static final Codec<End> END = register("end", End.CODEC);

    public static void loadClass() {

    }

    private static List<Player> targets(FeatureContext<?> context, @Nullable Player player, boolean all) {
        if (all || player == null) return context.onlinePlayers();
        return List.of(player);
    }

    private static <T extends DungeonAction> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_ACTION_CODEC.register(typeName, codec);
    }
}
