package com.roguesmp.dungeon.action;

import com.roguesmp.dungeon.script.DungeonCondition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.dungeon.TrackedMobs;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.dungeon.feature.QuestsFeature;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.item.BaseItem;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.dungeon.script.DungeonValidation;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class DungeonActions {

    public static final Pattern VAR_PLACEHOLDER = Pattern.compile("\\{var\\.([\\w.]+)}");

    public record Signal(String id, int amount, Map<String, DataValue> data) implements DungeonAction {
        public static final Codec<Signal> CODEC = Codec.composite(
                Codec.STRING.fieldOf("id").forGetter(Signal::id),
                Codec.INT.optionalFieldOf("amount", 1).forGetter(Signal::amount),
                Codec.unboundedMap(DataValue.CODEC).optionalFieldOf("data", Map.of()).forGetter(Signal::data),
                Signal::new
        );

        @Override
        public String getTypeId() {
            return "signal";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.emits(id, data.keySet());
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            context.signal(id, amount, player, data);
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
        public void validate(DungeonValidation check) {
            check.readsText(text);
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            String formatted = format(context, player, text);
            targets(context, player, all).forEach(target -> target.sendMessage(Utils.fromString(formatted)));
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

    public record SetVar(String var, DataValue value) implements DungeonAction {
        public static final Codec<SetVar> CODEC = Codec.composite(
                Codec.STRING.fieldOf("var").forGetter(SetVar::var),
                DataValue.CODEC.fieldOf("value").forGetter(SetVar::value),
                SetVar::new
        );

        @Override
        public String getTypeId() {
            return "set_var";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.writesVar(var);
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            context.instance().setVariable(var, value);
            context.save();
        }
    }

    public record AddVar(String var, double amount) implements DungeonAction {
        public static final Codec<AddVar> CODEC = Codec.composite(
                Codec.STRING.fieldOf("var").forGetter(AddVar::var),
                Codec.DOUBLE.optionalFieldOf("amount", 1.0).forGetter(AddVar::amount),
                AddVar::new
        );

        @Override
        public String getTypeId() {
            return "add_var";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.writesVar(var);
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            double current = context.instance().variable(var) instanceof DataValue.NumberValue(double value) ? value : 0;
            context.instance().setVariable(var, DataValue.of(current + amount));
            context.save();
        }
    }

    public record Delay(int ticks) implements DungeonAction {
        public static final Codec<Delay> CODEC = Codec.composite(
                Codec.INT.fieldOf("ticks").forGetter(Delay::ticks),
                Delay::new
        );

        @Override
        public String getTypeId() {
            return "delay";
        }

        // Only meaningful inside a sequence, which waits on it.
        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
        }
    }

    public record Sequence(List<DungeonAction> steps) implements DungeonAction {
        public static final Codec<Sequence> CODEC = Codec.composite(
                Codec.listOf(DungeonAction.CODEC).fieldOf("steps").forGetter(Sequence::steps),
                Sequence::new
        );

        @Override
        public String getTypeId() {
            return "sequence";
        }

        @Override
        public void validate(DungeonValidation check) {
            for (int i = 0; i < steps.size(); i++) steps.get(i).validate(check.at("steps[" + i + "]"));
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            runFrom(context, player, 0);
        }

        private void runFrom(FeatureContext<?> context, @Nullable Player player, int start) {
            for (int i = start; i < steps.size(); i++) {
                if (steps.get(i) instanceof Delay(int ticks)) {
                    int next = i + 1;
                    Utils.runLater(() -> {
                        if (!context.manager().isLive(context.instance())) return;
                        runFrom(context, player != null && player.isOnline() ? player : null, next);
                    }, Math.max(1, ticks));
                    return;
                }
                steps.get(i).run(context, player);
            }
        }
    }

    public record Repeat(int times, List<DungeonCondition> whileConditions, int interval, List<DungeonAction> actions) implements DungeonAction {
        public static final Codec<Repeat> CODEC = Codec.composite(
                Codec.INT.fieldOf("times").forGetter(Repeat::times),
                Codec.listOf(DungeonCondition.CODEC).optionalFieldOf("while", List.of()).forGetter(Repeat::whileConditions),
                Codec.INT.optionalFieldOf("interval", 0).forGetter(Repeat::interval),
                Codec.listOf(DungeonAction.CODEC).fieldOf("actions").forGetter(Repeat::actions),
                Repeat::new
        );

        @Override
        public String getTypeId() {
            return "repeat";
        }

        @Override
        public void validate(DungeonValidation check) {
            if (times <= 0) check.warn("'times' must be positive");
            for (int i = 0; i < whileConditions.size(); i++) whileConditions.get(i).validate(check.at("while[" + i + "]"));
            for (int i = 0; i < actions.size(); i++) actions.get(i).validate(check.at("actions[" + i + "]"));
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            runFrom(context, player, 0);
        }

        private void runFrom(FeatureContext<?> context, @Nullable Player player, int iteration) {
            for (int i = iteration; i < times; i++) {
                if (!DungeonCondition.all(whileConditions, context)) return;
                actions.forEach(action -> action.run(context, player));
                if (interval > 0 && i + 1 < times) {
                    int next = i + 1;
                    Utils.runLater(() -> {
                        if (!context.manager().isLive(context.instance())) return;
                        runFrom(context, player != null && player.isOnline() ? player : null, next);
                    }, interval);
                    return;
                }
            }
        }
    }

    public record If(List<DungeonCondition> conditions, List<DungeonAction> then, List<DungeonAction> otherwise) implements DungeonAction {
        public static final Codec<If> CODEC = Codec.composite(
                Codec.listOf(DungeonCondition.CODEC).fieldOf("conditions").forGetter(If::conditions),
                Codec.listOf(DungeonAction.CODEC).optionalFieldOf("then", List.of()).forGetter(If::then),
                Codec.listOf(DungeonAction.CODEC).optionalFieldOf("else", List.of()).forGetter(If::otherwise),
                If::new
        );

        @Override
        public String getTypeId() {
            return "if";
        }

        @Override
        public void validate(DungeonValidation check) {
            for (int i = 0; i < conditions.size(); i++) conditions.get(i).validate(check.at("conditions[" + i + "]"));
            for (int i = 0; i < then.size(); i++) then.get(i).validate(check.at("then[" + i + "]"));
            for (int i = 0; i < otherwise.size(); i++) otherwise.get(i).validate(check.at("else[" + i + "]"));
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            List<DungeonAction> branch = DungeonCondition.all(conditions, context) ? then : otherwise;
            branch.forEach(action -> action.run(context, player));
        }
    }

    public record SpawnMob(String mob, int count, double x, double y, double z, double spread, String tag) implements DungeonAction {
        public static final Codec<SpawnMob> CODEC = Codec.composite(
                Codec.STRING.fieldOf("mob").forGetter(SpawnMob::mob),
                Codec.INT.optionalFieldOf("count", 1).forGetter(SpawnMob::count),
                Codec.DOUBLE.fieldOf("x").forGetter(SpawnMob::x),
                Codec.DOUBLE.fieldOf("y").forGetter(SpawnMob::y),
                Codec.DOUBLE.fieldOf("z").forGetter(SpawnMob::z),
                Codec.DOUBLE.optionalFieldOf("spread", 0.0).forGetter(SpawnMob::spread),
                Codec.STRING.optionalFieldOf("tag", "").forGetter(SpawnMob::tag),
                SpawnMob::new
        );

        @Override
        public String getTypeId() {
            return "spawn_mob";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.checkMob(mob);
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            World world = context.world();
            if (world == null) return;
            boolean tracked = !tag.isEmpty();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                Location location = new Location(world, x + random.nextDouble(-spread, spread + 1e-9), y, z + random.nextDouble(-spread, spread + 1e-9));
                Entity spawned = spawn(context, world, location);
                if (spawned == null) break;
                if (tracked && spawned instanceof LivingEntity living) context.manager().trackedMobs().track(tag, living);
            }
            if (tracked) context.save();
        }

        private @Nullable Entity spawn(FeatureContext<?> context, World world, Location location) {
            SmpEntity smpEntity = EntityManager.getInstance().spawnEntity(mob, location);
            if (smpEntity != null) return smpEntity.getEntity();
            NamespacedKey key = NamespacedKey.fromString(mob);
            EntityType type = key == null ? null : org.bukkit.Registry.ENTITY_TYPE.get(key);
            if (type == null || !type.isSpawnable()) {
                RogueSmpCore.LOGGER.warn("spawn_mob: unknown mob {} in dungeon {}", mob, context.instance().dungeonId());
                return null;
            }
            return world.spawnEntity(location, type);
        }
    }

    public record GiveItem(String item, int amount, boolean all) implements DungeonAction {
        public static final Codec<GiveItem> CODEC = Codec.composite(
                Codec.STRING.fieldOf("item").forGetter(GiveItem::item),
                Codec.INT.optionalFieldOf("amount", 1).forGetter(GiveItem::amount),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(GiveItem::all),
                GiveItem::new
        );

        @Override
        public String getTypeId() {
            return "give_item";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.checkItem(item);
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            BaseItem base = Registries.ITEM.get(item);
            Material material = base == null ? Material.matchMaterial(item) : null;
            if (base == null && material == null) {
                RogueSmpCore.LOGGER.warn("give_item: unknown item {} in dungeon {}", item, context.instance().dungeonId());
                return;
            }
            for (Player target : targets(context, player, all)) {
                PlayerUtils.giveItem(target, base != null ? base.generateItemStack(amount) : ItemStack.of(material, amount));
            }
        }
    }

    public record SetBlock(int x, int y, int z, BlockData block) implements DungeonAction {
        private static final Codec<BlockData> BLOCK_DATA = Codec.STRING.comapFlatMap(text -> {
            try {
                return DataResult.success(Bukkit.createBlockData(text));
            } catch (IllegalArgumentException e) {
                return DataResult.error("Invalid block " + text);
            }
        }, BlockData::getAsString);

        public static final Codec<SetBlock> CODEC = Codec.composite(
                Codec.INT.fieldOf("x").forGetter(SetBlock::x),
                Codec.INT.fieldOf("y").forGetter(SetBlock::y),
                Codec.INT.fieldOf("z").forGetter(SetBlock::z),
                BLOCK_DATA.fieldOf("block").forGetter(SetBlock::block),
                SetBlock::new
        );

        @Override
        public String getTypeId() {
            return "set_block";
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            World world = context.world();
            if (world != null) world.getBlockAt(x, y, z).setBlockData(block);
        }
    }

    public record ShowTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut, boolean all) implements DungeonAction {
        public static final Codec<ShowTitle> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("title", "").forGetter(ShowTitle::title),
                Codec.STRING.optionalFieldOf("subtitle", "").forGetter(ShowTitle::subtitle),
                Codec.INT.optionalFieldOf("fade_in", 10).forGetter(ShowTitle::fadeIn),
                Codec.INT.optionalFieldOf("stay", 70).forGetter(ShowTitle::stay),
                Codec.INT.optionalFieldOf("fade_out", 20).forGetter(ShowTitle::fadeOut),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(ShowTitle::all),
                ShowTitle::new
        );

        @Override
        public String getTypeId() {
            return "title";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.readsText(title);
            check.readsText(subtitle);
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            Title shown = Title.title(
                    Utils.fromString(format(context, player, title)),
                    Utils.fromString(format(context, player, subtitle)),
                    Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L)));
            targets(context, player, all).forEach(target -> target.showTitle(shown));
        }
    }

    public record PlaySound(Key sound, float volume, float pitch, boolean all) implements DungeonAction {
        public static final Codec<PlaySound> CODEC = Codec.composite(
                Codec.KEY.fieldOf("sound").forGetter(PlaySound::sound),
                Codec.FLOAT.optionalFieldOf("volume", 1f).forGetter(PlaySound::volume),
                Codec.FLOAT.optionalFieldOf("pitch", 1f).forGetter(PlaySound::pitch),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(PlaySound::all),
                PlaySound::new
        );

        @Override
        public String getTypeId() {
            return "play_sound";
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            Sound played = Sound.sound(sound, Sound.Source.MASTER, volume, pitch);
            targets(context, player, all).forEach(target -> target.playSound(played));
        }
    }

    // Starts on the dungeon's quest group, so the dungeon needs the quests feature.
    public record StartQuest(Quest quest) implements DungeonAction {
        public static final Codec<StartQuest> CODEC = Codec.composite(
                Quest.REFERENCE_CODEC.fieldOf("quest").forGetter(StartQuest::quest),
                StartQuest::new
        );

        @Override
        public String getTypeId() {
            return "start_quest";
        }

        @Override
        public void run(FeatureContext<?> context, @Nullable Player player) {
            QuestManager questManager = QuestManager.getInstance();
            GroupHolder holder = QuestsFeature.holder(context.instance());
            if (!questManager.startQuest(holder, quest)) return;
            context.instance().members().forEach(member -> questManager.addEligible(holder, quest.getId(), member));
        }
    }

    public static final Codec<Signal> SIGNAL = register("signal", Signal.CODEC);
    public static final Codec<SetVar> SET_VAR = register("set_var", SetVar.CODEC);
    public static final Codec<AddVar> ADD_VAR = register("add_var", AddVar.CODEC);
    public static final Codec<Message> MESSAGE = register("message", Message.CODEC);
    public static final Codec<Teleport> TELEPORT = register("teleport", Teleport.CODEC);
    public static final Codec<End> END = register("end", End.CODEC);
    public static final Codec<Delay> DELAY = register("delay", Delay.CODEC);
    public static final Codec<Sequence> SEQUENCE = register("sequence", Sequence.CODEC);
    public static final Codec<Repeat> REPEAT = register("repeat", Repeat.CODEC);
    public static final Codec<If> IF = register("if", If.CODEC);
    public static final Codec<SpawnMob> SPAWN_MOB = register("spawn_mob", SpawnMob.CODEC);
    public static final Codec<GiveItem> GIVE_ITEM = register("give_item", GiveItem.CODEC);
    public static final Codec<SetBlock> SET_BLOCK = register("set_block", SetBlock.CODEC);
    public static final Codec<ShowTitle> TITLE = register("title", ShowTitle.CODEC);
    public static final Codec<PlaySound> PLAY_SOUND = register("play_sound", PlaySound.CODEC);
    public static final Codec<StartQuest> START_QUEST = register("start_quest", StartQuest.CODEC);

    public static void loadClass() {

    }

    private static String format(FeatureContext<?> context, @Nullable Player player, String text) {
        String withPlayer = text.replace("{player}", player != null ? player.getName() : "");
        return VAR_PLACEHOLDER.matcher(withPlayer).replaceAll(match -> {
            DataValue value = context.instance().variable(match.group(1));
            return Matcher.quoteReplacement(value != null ? value.display() : "");
        });
    }

    private static List<Player> targets(FeatureContext<?> context, @Nullable Player player, boolean all) {
        if (all || player == null) return context.onlinePlayers();
        return List.of(player);
    }

    private static <T extends DungeonAction> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_ACTION_CODEC.register(typeName, codec);
    }
}
