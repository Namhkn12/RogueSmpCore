package com.roguesmp.dungeon.action;

import com.roguesmp.dungeon.script.DungeonCondition;
import com.roguesmp.dungeon.DungeonDefinition;
import com.roguesmp.dungeon.DungeonEvent;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Keyed;
import org.bukkit.block.Block;
import com.roguesmp.block.BlockPos;
import com.roguesmp.block.BlockRef;
import com.roguesmp.block.manager.BlockManager;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.dungeon.feature.QuestsFeature;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.dungeon.script.ScriptContext;
import com.roguesmp.dungeon.script.ScriptFunction;
import com.roguesmp.dungeon.script.expr.ScriptNumber;
import com.roguesmp.dungeon.script.expr.ScriptText;
import com.roguesmp.dungeon.script.expr.ScriptValue;
import com.roguesmp.dungeon.script.expr.Statements;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.dungeon.script.DungeonValidation;
import com.roguesmp.registry.Registries;
import com.roguesmp.region.RegionSelection;
import com.roguesmp.region.ServerRegionManager;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.roguesmp.utils.Utils;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class DungeonActions {
    private static final ScriptNumber ZERO = ScriptNumber.of(0);
    private static final ScriptNumber ONE = ScriptNumber.of(1);


    public record Signal(String id, int amount, Map<String, ScriptValue> data) implements DungeonAction {
        public static final Codec<Signal> CODEC = Codec.composite(
                Codec.STRING.fieldOf("id").forGetter(Signal::id),
                Codec.INT.optionalFieldOf("amount", 1).forGetter(Signal::amount),
                Codec.unboundedMap(ScriptValue.CODEC).optionalFieldOf("data", Map.of()).forGetter(Signal::data),
                Signal::new
        );

        @Override
        public String getTypeId() {
            return "signal";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.emits(id, data.keySet());
            data.forEach((key, value) -> check.at("data." + key).reads(value));
        }

        @Override
        public void run(ScriptContext context) {
            context.feature().signal(id, amount, context.player(), evaluate(data, context));
        }
    }

    public record Eval(Statements statements) implements DungeonAction {
        public static final Codec<Eval> CODEC = Codec.composite(
                Statements.CODEC.fieldOf("run").forGetter(Eval::statements),
                Eval::new
        );

        @Override
        public String getTypeId() {
            return "eval";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.runs(statements);
        }

        @Override
        public void run(ScriptContext context) {
            statements.run(context);
        }
    }

    public record Cancel() implements DungeonAction {
        public static final Codec<Cancel> CODEC = MapCodec.unit(Cancel::new);

        @Override
        public String getTypeId() {
            return "cancel";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.cancels();
        }

        @Override
        public void run(ScriptContext context) {
            if (context.event() != null) context.event().cancel();
        }
    }

    public record Message(ScriptText text, boolean all) implements DungeonAction {
        public static final Codec<Message> CODEC = Codec.composite(
                ScriptText.CODEC.fieldOf("text").forGetter(Message::text),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(Message::all),
                Message::new
        );

        @Override
        public String getTypeId() {
            return "message";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.reads(text);
        }

        @Override
        public void run(ScriptContext context) {
            String formatted = text.format(context);
            targets(context, all).forEach(target -> target.sendMessage(Utils.fromString(formatted)));
        }
    }

    public record Teleport(ScriptNumber x, ScriptNumber y, ScriptNumber z, float yaw, float pitch, boolean all,
                           Optional<TeleportDestination> destination, boolean fx) implements DungeonAction {
        public static final Codec<Teleport> CODEC = Codec.composite(
                ScriptNumber.CODEC.optionalFieldOf("x", ZERO).forGetter(Teleport::x),
                ScriptNumber.CODEC.optionalFieldOf("y", ZERO).forGetter(Teleport::y),
                ScriptNumber.CODEC.optionalFieldOf("z", ZERO).forGetter(Teleport::z),
                Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Teleport::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(Teleport::pitch),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(Teleport::all),
                TeleportDestination.CODEC.optionalFieldOf("destination").forGetter(Teleport::destination),
                Codec.BOOLEAN.optionalFieldOf("fx", false).forGetter(Teleport::fx),
                Teleport::new
        );

        @Override
        public String getTypeId() {
            return "teleport";
        }

        @Override
        public void validate(DungeonValidation check) {
            readsAll(check, x, y, z);
        }

        @Override
        public void run(ScriptContext context) {
            TeleportOptions options = fx ? TeleportOptions.DEFAULT.withSafe(false) : TeleportOptions.EXACT;
            if (destination.isPresent()) {
                targets(context, all).forEach(target -> TeleportService.getInstance().teleport(target, destination.get(), options));
                return;
            }

            World world = context.feature().world();
            if (world == null) return;
            Location location = new Location(world, x.get(context), y.get(context), z.get(context), yaw, pitch);
            targets(context, all).forEach(target -> TeleportService.getInstance().teleport(target, location, options));
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
        public void run(ScriptContext context) {
            context.feature().end(reason);
        }
    }

    public record SetVar(String var, ScriptValue value, boolean perPlayer) implements DungeonAction {
        public static final Codec<SetVar> CODEC = Codec.composite(
                Codec.STRING.fieldOf("var").forGetter(SetVar::var),
                ScriptValue.CODEC.fieldOf("value").forGetter(SetVar::value),
                Codec.BOOLEAN.optionalFieldOf("player", false).forGetter(SetVar::perPlayer),
                SetVar::new
        );

        @Override
        public String getTypeId() {
            return "set_var";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.writesVar(var);
            check.reads(value);
        }

        @Override
        public void run(ScriptContext context) {
            context.setVariable(var, perPlayer, value.get(context));
        }
    }

    public record AddVar(String var, ScriptValue amount, boolean perPlayer) implements DungeonAction {
        public static final Codec<AddVar> CODEC = Codec.composite(
                Codec.STRING.fieldOf("var").forGetter(AddVar::var),
                ScriptValue.CODEC.optionalFieldOf("amount", ScriptValue.of(DataValue.of(1))).forGetter(AddVar::amount),
                Codec.BOOLEAN.optionalFieldOf("player", false).forGetter(AddVar::perPlayer),
                AddVar::new
        );

        @Override
        public String getTypeId() {
            return "add_var";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.writesVar(var);
            check.reads(amount);
        }

        @Override
        public void run(ScriptContext context) {
            double current = context.variable(var, perPlayer) instanceof DataValue.NumberValue(double value) ? value : 0;
            double added = amount.get(context) instanceof DataValue.NumberValue(double value) ? value : 0;
            context.setVariable(var, perPlayer, DataValue.of(current + added));
        }
    }

    public record Delay(ScriptNumber ticks) implements DungeonAction {
        public static final Codec<Delay> CODEC = Codec.composite(
                ScriptNumber.CODEC.fieldOf("ticks").forGetter(Delay::ticks),
                Delay::new
        );

        @Override
        public String getTypeId() {
            return "delay";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.reads(ticks.refs());
        }

        // Only meaningful inside a sequence, which waits on it.
        @Override
        public void run(ScriptContext context) {
        }
    }

    public record Sequence(List<DungeonAction> steps) implements DungeonAction {
        public static final Codec<Sequence> CODEC = Codec.composite(
                DungeonAction.LIST_CODEC.fieldOf("steps").forGetter(Sequence::steps),
                Sequence::new
        );

        @Override
        public String getTypeId() {
            return "sequence";
        }

        @Override
        public void validate(DungeonValidation check) {
            DungeonAction.validateAll(steps, check.at("steps"));
        }

        @Override
        public void run(ScriptContext context) {
            runFrom(context, 0);
        }

        private void runFrom(ScriptContext context, int start) {
            for (int i = start; i < steps.size(); i++) {
                if (steps.get(i) instanceof Delay delay) {
                    int next = i + 1;
                    Utils.runLater(() -> {
                        if (!context.feature().manager().isLive(context.feature().instance())) return;
                        runFrom(context.withPlayer(context.player() != null && context.player().isOnline() ? context.player() : null), next);
                    }, Math.max(1, delay.ticks().getInt(context)));
                    return;
                }
                steps.get(i).run(context);
            }
        }
    }

    public record Repeat(ScriptNumber times, List<DungeonCondition> whileConditions, ScriptNumber interval, List<DungeonAction> actions) implements DungeonAction {
        public static final Codec<Repeat> CODEC = Codec.composite(
                ScriptNumber.CODEC.fieldOf("times").forGetter(Repeat::times),
                DungeonCondition.LIST_CODEC.optionalFieldOf("while", List.of()).forGetter(Repeat::whileConditions),
                ScriptNumber.CODEC.optionalFieldOf("interval", ZERO).forGetter(Repeat::interval),
                DungeonAction.LIST_CODEC.fieldOf("do", "actions").forGetter(Repeat::actions),
                Repeat::new
        );

        @Override
        public String getTypeId() {
            return "repeat";
        }

        @Override
        public void validate(DungeonValidation check) {
            if (times.isConstant() && times.constant() <= 0) check.warn("'times' must be positive");
            readsAll(check, times, interval);
            DungeonCondition.validateAll(whileConditions, check.at("while"));
            DungeonAction.validateAll(actions, check.at("actions"));
        }

        @Override
        public void run(ScriptContext context) {
            runFrom(context, 0, times.getInt(context), interval.getInt(context));
        }

        private void runFrom(ScriptContext context, int iteration, int times, int interval) {
            for (int i = iteration; i < times; i++) {
                if (!DungeonCondition.all(whileConditions, context)) return;
                DungeonAction.runAll(actions, context);
                if (interval > 0 && i + 1 < times) {
                    int next = i + 1;
                    Utils.runLater(() -> {
                        if (!context.feature().manager().isLive(context.feature().instance())) return;
                        runFrom(context.withPlayer(context.player() != null && context.player().isOnline() ? context.player() : null), next, times, interval);
                    }, interval);
                    return;
                }
            }
        }
    }

    public record If(List<DungeonCondition> conditions, List<DungeonAction> then, List<DungeonAction> otherwise) implements DungeonAction {
        public static final Codec<If> CODEC = Codec.composite(
                DungeonCondition.LIST_CODEC.fieldOf("if", "conditions").forGetter(If::conditions),
                DungeonAction.LIST_CODEC.optionalFieldOf("then", List.of()).forGetter(If::then),
                DungeonAction.LIST_CODEC.optionalFieldOf("else", List.of()).forGetter(If::otherwise),
                If::new
        );

        @Override
        public String getTypeId() {
            return "if";
        }

        @Override
        public void validate(DungeonValidation check) {
            DungeonCondition.validateAll(conditions, check.at("if"));
            DungeonAction.validateAll(then, check.at("then"));
            DungeonAction.validateAll(otherwise, check.at("else"));
        }

        @Override
        public void run(ScriptContext context) {
            List<DungeonAction> branch = DungeonCondition.all(conditions, context) ? then : otherwise;
            DungeonAction.runAll(branch, context);
        }
    }

    public record SpawnMob(String mob, ScriptNumber count, ScriptNumber x, ScriptNumber y, ScriptNumber z, ScriptNumber spread, String tag) implements DungeonAction {
        public static final Codec<SpawnMob> CODEC = Codec.composite(
                Codec.STRING.fieldOf("mob").forGetter(SpawnMob::mob),
                ScriptNumber.CODEC.optionalFieldOf("count", ONE).forGetter(SpawnMob::count),
                ScriptNumber.CODEC.fieldOf("x").forGetter(SpawnMob::x),
                ScriptNumber.CODEC.fieldOf("y").forGetter(SpawnMob::y),
                ScriptNumber.CODEC.fieldOf("z").forGetter(SpawnMob::z),
                ScriptNumber.CODEC.optionalFieldOf("spread", ZERO).forGetter(SpawnMob::spread),
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
            readsAll(check, count, x, y, z, spread);
        }

        @Override
        public void run(ScriptContext context) {
            World world = context.feature().world();
            if (world == null) return;
            boolean tracked = !tag.isEmpty();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int count = this.count.getInt(context);
            double x = this.x.get(context), y = this.y.get(context), z = this.z.get(context), spread = Math.max(0, this.spread.get(context));
            for (int i = 0; i < count; i++) {
                Location location = new Location(world, x + random.nextDouble(-spread, spread + 1e-9), y, z + random.nextDouble(-spread, spread + 1e-9));
                Entity spawned = spawn(context.feature(), world, location);
                if (spawned == null) break;
                if (tracked && spawned instanceof LivingEntity living) context.feature().manager().trackedMobs().track(tag, living);
            }
            if (tracked) context.feature().save();
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

    public record GiveItem(String item, ScriptNumber amount, boolean all) implements DungeonAction {
        public static final Codec<GiveItem> CODEC = Codec.composite(
                Codec.STRING.fieldOf("item").forGetter(GiveItem::item),
                ScriptNumber.CODEC.optionalFieldOf("amount", ONE).forGetter(GiveItem::amount),
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
            check.reads(amount.refs());
        }

        @Override
        public void run(ScriptContext context) {
            int amount = this.amount.getInt(context);
            if (amount <= 0) return;
            if (ItemRef.toItemStack(item, amount) == null) {
                RogueSmpCore.LOGGER.warn("give_item: unknown item {} in dungeon {}", item, context.feature().instance().dungeonId());
                return;
            }
            for (Player target : targets(context, all)) PlayerUtils.giveItem(target, ItemRef.toItemStack(item, amount));
        }
    }

    public record SetBlock(int x, int y, int z, BlockRef block) implements DungeonAction {
        public static final Codec<SetBlock> CODEC = Codec.composite(
                Codec.INT.fieldOf("x").forGetter(SetBlock::x),
                Codec.INT.fieldOf("y").forGetter(SetBlock::y),
                Codec.INT.fieldOf("z").forGetter(SetBlock::z),
                BlockRef.CODEC.fieldOf("block").forGetter(SetBlock::block),
                SetBlock::new
        );

        @Override
        public String getTypeId() {
            return "set_block";
        }

        @Override
        public void validate(DungeonValidation check) {
            checkBlock(check, block);
        }

        @Override
        public void run(ScriptContext context) {
            World world = context.feature().world();
            if (world != null) placeBlock(world.getBlockAt(x, y, z), block);
        }
    }

    public record FillBlock(BlockVector3 from, BlockVector3 to, BlockRef block, Optional<String> replace) implements DungeonAction {
        private static final int MAX_VOLUME = 32768;

        public static final Codec<FillBlock> CODEC = Codec.composite(
                CreateRegion.POSITION.fieldOf("from").forGetter(FillBlock::from),
                CreateRegion.POSITION.fieldOf("to").forGetter(FillBlock::to),
                BlockRef.CODEC.fieldOf("block").forGetter(FillBlock::block),
                Codec.STRING.optionalFieldOf("replace").forGetter(FillBlock::replace),
                FillBlock::new
        );

        @Override
        public String getTypeId() {
            return "fill_block";
        }

        @Override
        public void validate(DungeonValidation check) {
            BlockVector3 size = from.getMaximum(to).subtract(from.getMinimum(to)).add(1, 1, 1);
            long volume = (long) size.x() * size.y() * size.z();
            if (volume > MAX_VOLUME) check.warn("Fill of " + volume + " blocks exceeds " + MAX_VOLUME + ".");
            checkBlock(check.at("block"), block);
            replace.ifPresent(id -> {
                DataResult<BlockRef> parsed = BlockRef.parse(id);
                if (!parsed.isSuccess()) check.at("replace").warn(parsed.error());
                else checkBlock(check.at("replace"), parsed.result());
            });
        }

        @Override
        public void run(ScriptContext context) {
            World world = context.feature().world();
            if (world == null) return;

            BlockVector3 min = from.getMinimum(to);
            BlockVector3 max = from.getMaximum(to);
            for (int x = min.x(); x <= max.x(); x++) {
                for (int y = min.y(); y <= max.y(); y++) {
                    for (int z = min.z(); z <= max.z(); z++) {
                        Block target = world.getBlockAt(x, y, z);
                        if (replace.isPresent() && !replace.get().equals(DungeonEvent.blockId(target))) continue;
                        placeBlock(target, block);
                    }
                }
            }
        }
    }

    public record ShowTitle(ScriptText title, ScriptText subtitle, int fadeIn, int stay, int fadeOut, boolean all) implements DungeonAction {
        public static final Codec<ShowTitle> CODEC = Codec.composite(
                ScriptText.CODEC.optionalFieldOf("title", ScriptText.EMPTY).forGetter(ShowTitle::title),
                ScriptText.CODEC.optionalFieldOf("subtitle", ScriptText.EMPTY).forGetter(ShowTitle::subtitle),
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
            check.reads(title);
            check.reads(subtitle);
        }

        @Override
        public void run(ScriptContext context) {
            Title shown = Title.title(
                    Utils.fromString(title.format(context)),
                    Utils.fromString(subtitle.format(context)),
                    Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L)));
            targets(context, all).forEach(target -> target.showTitle(shown));
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
        public void run(ScriptContext context) {
            Sound played = Sound.sound(sound, Sound.Source.MASTER, volume, pitch);
            targets(context, all).forEach(target -> target.playSound(played));
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
        public void run(ScriptContext context) {
            QuestManager questManager = QuestManager.getInstance();
            GroupHolder holder = QuestsFeature.holder(context.feature().instance());
            if (!questManager.startQuest(holder, quest)) return;
            context.feature().instance().members().forEach(member -> questManager.addEligible(holder, quest.getId(), member));
        }
    }

    public record CreateRegion(String id, List<Box> shapes, boolean replace, Optional<Integer> priority, Map<String, String> flags) implements DungeonAction {
        private static final Codec<BlockVector3> POSITION = Codec.listOf(Codec.INT).comapFlatMap(
                coords -> coords.size() == 3
                        ? DataResult.success(BlockVector3.at(coords.get(0), coords.get(1), coords.get(2)))
                        : DataResult.error("Position needs [x, y, z]"),
                vector -> List.of(vector.x(), vector.y(), vector.z())
        );

        public record Box(BlockVector3 from, BlockVector3 to) {
            public static final Codec<Box> CODEC = Codec.composite(
                    POSITION.fieldOf("from").forGetter(Box::from),
                    POSITION.fieldOf("to").forGetter(Box::to),
                    Box::new
            );
        }

        public static final Codec<CreateRegion> CODEC = Codec.composite(
                Codec.STRING.fieldOf("id").forGetter(CreateRegion::id),
                Codec.listOf(Box.CODEC).fieldOf("shapes").forGetter(CreateRegion::shapes),
                Codec.BOOLEAN.optionalFieldOf("replace", true).forGetter(CreateRegion::replace),
                Codec.INT.optionalFieldOf("priority").forGetter(CreateRegion::priority),
                Codec.unboundedMap(Codec.STRING).optionalFieldOf("flags", Map.of()).forGetter(CreateRegion::flags),
                CreateRegion::new
        );

        @Override
        public String getTypeId() {
            return "create_region";
        }

        @Override
        public void validate(DungeonValidation check) {
            if (shapes.isEmpty()) check.at("shapes").warn("Region needs at least one shape.");
            flags.forEach((flag, value) -> validateFlag(check.at("flags." + flag), flag, value));
        }

        @Override
        public void run(ScriptContext context) {
            World world = context.feature().world();
            if (world == null || shapes.isEmpty()) return;

            ServerRegionManager regions = ServerRegionManager.getInstance();
            for (int i = 0; i < shapes.size(); i++) {
                Box box = shapes.get(i);
                RegionSelection selection = new RegionSelection(world, List.of(box.from(), box.to()), null, null);
                if (i == 0 && replace) regions.replaceShape(world, id, selection);
                else regions.merge(world, id, selection);
            }
            priority.ifPresent(value -> regions.setPriority(world, id, value));
            flags.forEach((flag, value) -> applyFlag(world, id, flag, value, RegionGroup.ALL));
            regions.saveChanges(world);
        }
    }

    public record RegionFlag(String region, String flag, Optional<String> value, RegionGroup group) implements DungeonAction {
        public static final Codec<RegionFlag> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("region", ServerRegionManager.GLOBAL_ID).forGetter(RegionFlag::region),
                Codec.STRING.fieldOf("flag").forGetter(RegionFlag::flag),
                Codec.STRING.optionalFieldOf("value").forGetter(RegionFlag::value),
                Codec.enumOf(RegionGroup.class).optionalFieldOf("group", RegionGroup.ALL).forGetter(RegionFlag::group),
                RegionFlag::new
        );

        @Override
        public String getTypeId() {
            return "region_flag";
        }

        @Override
        public void validate(DungeonValidation check) {
            if (value.isPresent()) validateFlag(check, flag, value.get());
            else if (WorldGuard.getInstance().getFlagRegistry().get(flag) == null) check.warn("Unknown flag '" + flag + "'.");
        }

        @Override
        public void run(ScriptContext context) {
            World world = context.feature().world();
            if (world == null) return;

            ServerRegionManager regions = ServerRegionManager.getInstance();
            if (regions.get(world, region) == null) {
                RogueSmpCore.LOGGER.warn("region_flag: unknown region {} in dungeon {}", region, context.feature().instance().dungeonId());
                return;
            }
            if (value.isPresent()) {
                applyFlag(world, region, flag, value.get(), group);
            } else {
                Flag<?> worldGuardFlag = WorldGuard.getInstance().getFlagRegistry().get(flag);
                if (worldGuardFlag != null) regions.clearFlag(world, region, worldGuardFlag);
            }
            regions.saveChanges(world);
        }
    }

    public record RemoveRegion(String id) implements DungeonAction {
        public static final Codec<RemoveRegion> CODEC = Codec.composite(
                Codec.STRING.fieldOf("id").forGetter(RemoveRegion::id),
                RemoveRegion::new
        );

        @Override
        public String getTypeId() {
            return "remove_region";
        }

        @Override
        public void run(ScriptContext context) {
            World world = context.feature().world();
            if (world == null) return;

            ServerRegionManager regions = ServerRegionManager.getInstance();
            if (regions.get(world, id) == null) {
                RogueSmpCore.LOGGER.warn("remove_region: unknown region {} in dungeon {}", id, context.feature().instance().dungeonId());
                return;
            }
            regions.remove(world, id);
            regions.saveChanges(world);
        }
    }

    public record TakeItem(String item, ScriptNumber amount, boolean all) implements DungeonAction {
        public static final Codec<TakeItem> CODEC = Codec.composite(
                Codec.STRING.fieldOf("item").forGetter(TakeItem::item),
                ScriptNumber.CODEC.optionalFieldOf("amount", ONE).forGetter(TakeItem::amount),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(TakeItem::all),
                TakeItem::new
        );

        @Override
        public String getTypeId() {
            return "take_item";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.checkItem(item);
            check.reads(amount.refs());
        }

        @Override
        public void run(ScriptContext context) {
            int amount = this.amount.getInt(context);
            if (amount > 0) targets(context, all).forEach(target -> PlayerUtils.removeItem(target, item, amount));
        }
    }

    public record Effect(PotionEffectType effect, ScriptNumber duration, ScriptNumber amplifier, boolean clear, boolean all) implements DungeonAction {
        private static final Codec<PotionEffectType> EFFECT_TYPE = registryCodec(Registry.EFFECT, "effect");

        public static final Codec<Effect> CODEC = Codec.composite(
                EFFECT_TYPE.fieldOf("effect").forGetter(Effect::effect),
                ScriptNumber.CODEC.optionalFieldOf("duration", ScriptNumber.of(200)).forGetter(Effect::duration),
                ScriptNumber.CODEC.optionalFieldOf("amplifier", ZERO).forGetter(Effect::amplifier),
                Codec.BOOLEAN.optionalFieldOf("clear", false).forGetter(Effect::clear),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(Effect::all),
                Effect::new
        );

        @Override
        public String getTypeId() {
            return "effect";
        }

        @Override
        public void validate(DungeonValidation check) {
            readsAll(check, duration, amplifier);
        }

        @Override
        public void run(ScriptContext context) {
            int duration = this.duration.getInt(context);
            int amplifier = Math.max(0, this.amplifier.getInt(context));
            for (Player target : targets(context, all)) {
                if (clear) target.removePotionEffect(effect);
                else target.addPotionEffect(new PotionEffect(effect, duration, amplifier));
            }
        }
    }

    public record Particles(Particle particle, ScriptNumber x, ScriptNumber y, ScriptNumber z, ScriptNumber count, double spread, double speed) implements DungeonAction {
        private static final Codec<Particle> PARTICLE_TYPE = registryCodec(Registry.PARTICLE_TYPE, "particle");

        public static final Codec<Particles> CODEC = Codec.composite(
                PARTICLE_TYPE.fieldOf("particle").forGetter(Particles::particle),
                ScriptNumber.CODEC.fieldOf("x").forGetter(Particles::x),
                ScriptNumber.CODEC.fieldOf("y").forGetter(Particles::y),
                ScriptNumber.CODEC.fieldOf("z").forGetter(Particles::z),
                ScriptNumber.CODEC.optionalFieldOf("count", ScriptNumber.of(20)).forGetter(Particles::count),
                Codec.DOUBLE.optionalFieldOf("spread", 0.5).forGetter(Particles::spread),
                Codec.DOUBLE.optionalFieldOf("speed", 0.0).forGetter(Particles::speed),
                Particles::new
        );

        @Override
        public String getTypeId() {
            return "particles";
        }

        @Override
        public void validate(DungeonValidation check) {
            readsAll(check, x, y, z, count);
            if (particle.getDataType() != Void.class) check.warn("particle '" + particle.getKey().getKey() + "' needs extra data and is not supported");
        }

        @Override
        public void run(ScriptContext context) {
            World world = context.feature().world();
            if (world == null || particle.getDataType() != Void.class) return;
            world.spawnParticle(particle, x.get(context), y.get(context), z.get(context), Math.max(0, count.getInt(context)), spread, spread, spread, speed);
        }
    }

    public record StartCountdown(String id, ScriptNumber ticks, ScriptText title, BossBar.Color color) implements DungeonAction {
        private static final ScriptText TIME = ScriptText.compile("{time}").result();

        public static final Codec<StartCountdown> CODEC = Codec.composite(
                Codec.STRING.fieldOf("id").forGetter(StartCountdown::id),
                ScriptNumber.CODEC.fieldOf("ticks").forGetter(StartCountdown::ticks),
                ScriptText.CODEC.optionalFieldOf("title", TIME).forGetter(StartCountdown::title),
                Codec.enumOf(BossBar.Color.class).optionalFieldOf("color", BossBar.Color.RED).forGetter(StartCountdown::color),
                StartCountdown::new
        );

        @Override
        public String getTypeId() {
            return "countdown";
        }

        @Override
        public void validate(DungeonValidation check) {
            if (ticks.isConstant() && ticks.constant() <= 0) check.warn("'ticks' must be positive");
            check.reads(ticks.refs());
            check.reads(title);
            check.emits("countdown_ended", java.util.Set.of());
        }

        @Override
        public void run(ScriptContext context) {
            context.feature().instance().countdowns().start(context, id, ticks.getInt(context), title, color);
        }
    }

    public record StopCountdown(String id) implements DungeonAction {
        public static final Codec<StopCountdown> CODEC = Codec.composite(
                Codec.STRING.fieldOf("id").forGetter(StopCountdown::id),
                StopCountdown::new
        );

        @Override
        public String getTypeId() {
            return "stop_countdown";
        }

        @Override
        public void run(ScriptContext context) {
            context.feature().instance().countdowns().stop(id);
        }
    }

    public record Choose(List<Option> options) implements DungeonAction {
        public record Option(double weight, List<DungeonAction> actions) {
            public static final Codec<Option> CODEC = Codec.composite(
                    Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(Option::weight),
                    DungeonAction.LIST_CODEC.fieldOf("do", "actions").forGetter(Option::actions),
                    Option::new
            );
        }

        public static final Codec<Choose> CODEC = Codec.composite(
                Codec.listOf(Option.CODEC).fieldOf("options").forGetter(Choose::options),
                Choose::new
        );

        @Override
        public String getTypeId() {
            return "choose";
        }

        @Override
        public void validate(DungeonValidation check) {
            if (options.isEmpty()) check.warn("'options' is empty");
            for (int i = 0; i < options.size(); i++) {
                DungeonAction.validateAll(options.get(i).actions(), check.at("options[" + i + "].do"));
            }
        }

        @Override
        public void run(ScriptContext context) {
            double total = options.stream().mapToDouble(Option::weight).sum();
            if (total <= 0) return;
            double roll = ThreadLocalRandom.current().nextDouble(total);
            for (Option option : options) {
                roll -= option.weight();
                if (roll < 0) {
                    DungeonAction.runAll(option.actions(), context);
                    return;
                }
            }
        }
    }

    public record RunCommand(ScriptText command, boolean asPlayer) implements DungeonAction {
        public static final Codec<RunCommand> CODEC = Codec.composite(
                ScriptText.CODEC.fieldOf("command").forGetter(RunCommand::command),
                Codec.BOOLEAN.optionalFieldOf("as_player", false).forGetter(RunCommand::asPlayer),
                RunCommand::new
        );

        @Override
        public String getTypeId() {
            return "command";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.reads(command);
        }

        @Override
        public void run(ScriptContext context) {
            String formatted = command.format(context);
            if (formatted.startsWith("/")) formatted = formatted.substring(1);
            if (asPlayer) {
                if (context.player() != null) context.player().performCommand(formatted);
                return;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
        }
    }

    public record Call(String function, Map<String, ScriptValue> args) implements DungeonAction {
        private static final int MAX_DEPTH = 16;

        public static final Codec<Call> CODEC = Codec.composite(
                Codec.STRING.fieldOf("function").forGetter(Call::function),
                Codec.unboundedMap(ScriptValue.CODEC).optionalFieldOf("args", Map.of()).forGetter(Call::args),
                Call::new
        );

        @Override
        public String getTypeId() {
            return "call";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.checkFunction(function, args.keySet());
            args.forEach((key, value) -> check.at("args." + key).reads(value));
        }

        @Override
        public void run(ScriptContext context) {
            DungeonDefinition definition = context.feature().instance().definition();
            ScriptFunction called = definition == null ? null : definition.functions().get(function);
            if (called == null) {
                RogueSmpCore.LOGGER.warn("call: unknown function {} in dungeon {}", function, context.feature().instance().dungeonId());
                return;
            }
            if (context.depth() >= MAX_DEPTH) {
                RogueSmpCore.LOGGER.warn("call: function {} nested deeper than {} in dungeon {}, stopping", function, MAX_DEPTH, context.feature().instance().dungeonId());
                return;
            }
            DungeonAction.runAll(called.actions(), context.call(called.params().isEmpty() ? null : evaluate(args, context)));
        }
    }

    public static final Codec<Signal> SIGNAL = register("signal", Signal.CODEC);
    public static final Codec<Eval> EVAL = register("eval", Eval.CODEC);
    public static final Codec<Cancel> CANCEL = register("cancel", Cancel.CODEC);
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
    public static final Codec<FillBlock> FILL_BLOCK = register("fill_block", FillBlock.CODEC);
    public static final Codec<ShowTitle> TITLE = register("title", ShowTitle.CODEC);
    public static final Codec<PlaySound> PLAY_SOUND = register("play_sound", PlaySound.CODEC);
    public static final Codec<StartQuest> START_QUEST = register("start_quest", StartQuest.CODEC);
    public static final Codec<CreateRegion> CREATE_REGION = register("create_region", CreateRegion.CODEC);
    public static final Codec<RegionFlag> REGION_FLAG = register("region_flag", RegionFlag.CODEC);
    public static final Codec<RemoveRegion> REMOVE_REGION = register("remove_region", RemoveRegion.CODEC);
    public static final Codec<TakeItem> TAKE_ITEM = register("take_item", TakeItem.CODEC);
    public static final Codec<Effect> EFFECT = register("effect", Effect.CODEC);
    public static final Codec<Particles> PARTICLES = register("particles", Particles.CODEC);
    public static final Codec<StartCountdown> COUNTDOWN = register("countdown", StartCountdown.CODEC);
    public static final Codec<StopCountdown> STOP_COUNTDOWN = register("stop_countdown", StopCountdown.CODEC);
    public static final Codec<Choose> CHOOSE = register("choose", Choose.CODEC);
    public static final Codec<RunCommand> COMMAND = register("command", RunCommand.CODEC);
    public static final Codec<Call> CALL = register("call", Call.CODEC);

    public static void loadClass() {

    }

    private static void readsAll(DungeonValidation check, ScriptNumber... numbers) {
        for (ScriptNumber number : numbers) check.reads(number.refs());
    }

    private static List<Player> targets(ScriptContext context, boolean all) {
        if (all || context.player() == null) return context.feature().onlinePlayers();
        return List.of(context.player());
    }

    private static Map<String, DataValue> evaluate(Map<String, ScriptValue> values, ScriptContext context) {
        Map<String, DataValue> result = new HashMap<>();
        values.forEach((key, value) -> result.put(key, value.get(context)));
        return result;
    }

    private static void placeBlock(Block target, BlockRef block) {
        BlockManager blocks = BlockManager.getInstance();
        if (blocks.get(BlockPos.of(target)) != null) blocks.breakBlock(target, null, false, false);
        if (block instanceof BlockRef.Custom) target.setType(Material.AIR, false);
        block.place(target);
    }

    private static void checkBlock(DungeonValidation check, BlockRef block) {
        if (block instanceof BlockRef.Custom custom && !custom.holder().isBound()) check.warn("unknown block '" + custom.id() + "'");
    }

    private static <T extends Keyed> Codec<T> registryCodec(Registry<T> registry, String kind) {
        return Codec.STRING.comapFlatMap(name -> {
            NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
            T value = key == null ? null : registry.get(key);
            return value != null ? DataResult.success(value) : DataResult.error("Unknown " + kind + " " + name);
        }, value -> value.getKey().asString());
    }

    private static void validateFlag(DungeonValidation check, String flag, String value) {
        String error = ServerRegionManager.getInstance().flagError(flag, value);
        if (error != null) check.warn(error);
    }

    private static void applyFlag(World world, String region, String flag, String value, RegionGroup group) {
        try {
            ServerRegionManager.getInstance().setFlag(world, region, flag, value, group);
        } catch (InvalidFlagFormat e) {
            RogueSmpCore.LOGGER.warn("Invalid value '{}' for flag {} on region {}: {}", value, flag, region, e.getMessage());
        }
    }

    private static <T extends DungeonAction> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_ACTION_CODEC.register(typeName, codec);
    }
}
