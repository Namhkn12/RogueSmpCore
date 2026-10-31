package com.roguesmp.dungeon.feature;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.BlockRef;
import com.roguesmp.block.BlockTypes;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.PaletteMarkerBlock;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.dungeon.FeatureContext;
import org.bukkit.World;
import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonDefinition;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.NoState;
import com.roguesmp.dungeon.script.DungeonValidation;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public record BlockPalettesFeature(Map<String, Palette> palettes) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "block_palettes";

    public record Replacement(BlockRef block, double weight) {
        public static final Codec<Replacement> CODEC = Codec.composite(
                BlockRef.CODEC.fieldOf("block").forGetter(Replacement::block),
                Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(Replacement::weight),
                Replacement::new
        );
    }

    public record Palette(List<Replacement> entries, double chance, Optional<BlockRef> fallback) {
        public static final Codec<Palette> CODEC = Codec.composite(
                Codec.listOf(Replacement.CODEC).fieldOf("entries").forGetter(Palette::entries),
                Codec.DOUBLE.optionalFieldOf("chance", 1.0).forGetter(Palette::chance),
                BlockRef.CODEC.optionalFieldOf("fallback").forGetter(Palette::fallback),
                Palette::new
        );

        public @Nullable BlockRef roll(RandomGenerator random) {
            if (entries.isEmpty() || random.nextDouble() >= chance) return fallback.orElse(null);

            double roll = random.nextDouble() * entries.stream().mapToDouble(Replacement::weight).sum();
            for (Replacement entry : entries) {
                roll -= entry.weight();
                if (roll < 0) return entry.block();
            }
            return entries.getLast().block();
        }
    }

    public static final Codec<BlockPalettesFeature> CODEC = Codec.composite(
            Codec.unboundedMap(Palette.CODEC).fieldOf("palettes").forGetter(BlockPalettesFeature::palettes),
            BlockPalettesFeature::new
    );

    public static void replace(DungeonInstance instance, String paletteId, Block target) {
        DungeonDefinition definition = instance.definition();
        Palette palette = null;
        if (definition != null) {
            for (DungeonFeature<?> feature : definition.features().values()) {
                if (feature instanceof BlockPalettesFeature palettes && (palette = palettes.palettes.get(paletteId)) != null) break;
            }
        }
        if (palette == null) {
            RogueSmpCore.LOGGER.warn("Dungeon {} has no block palette '{}', removing marker at {}", instance.dungeonId(), paletteId, target.getLocation());
            return;
        }

        long seed = instance.id().getMostSignificantBits() ^ instance.id().getLeastSignificantBits() ^ target.getBlockKey();
        BlockRef result = palette.roll(new SplittableRandom(seed));
        if (result != null) result.place(target);
    }

    @Override
    public void validate(String featureId, DungeonValidation check) {
        palettes.forEach((id, palette) -> {
            DungeonValidation at = check.at("palettes." + id);
            for (int i = 0; i < palette.entries.size(); i++) checkBlock(at.at("entries[" + i + "]"), palette.entries.get(i).block());
            palette.fallback.ifPresent(fallback -> checkBlock(at.at("fallback"), fallback));
        });
    }

    private static void checkBlock(DungeonValidation check, BlockRef block) {
        if (!(block instanceof BlockRef.Custom custom)) return;
        if (!custom.holder().isBound()) check.warn("unknown block '" + custom.id() + "'");
        else if (custom.holder().value().type() == BlockTypes.PALETTE) check.warn("palette marker '" + custom.id() + "' cannot be a replacement");
    }

    @Override
    public void onStart(FeatureContext<NoState> context) {
        replaceLoadedMarkers(context);
    }

    @Override
    public void onRestore(FeatureContext<NoState> context) {
        replaceLoadedMarkers(context);
    }

    private static void replaceLoadedMarkers(FeatureContext<NoState> context) {
        World world = context.world();
        if (world == null) return;
        for (SmpBlock block : BlockManager.getInstance().blocksIn(world.getKey())) {
            if (block instanceof PaletteMarkerBlock marker) marker.replace(context.instance());
        }
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public NoState createState() {
        return NoState.INSTANCE;
    }

    @Override
    public Codec<NoState> stateCodec() {
        return NoState.CODEC;
    }
}
