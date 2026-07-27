package com.roguesmp.player.ability;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * The JSON-loadable "tunable data" half of an ability (description/scaling/trigger bindings/upgrade
 * costs), decoded per {@code ability_info/<id>.json} file and applied onto the matching,
 * code-registered {@link AbilityInfo} via {@link AbilityInfo#populate}.
 */
public record AbilityConfig(
        List<String> description,
        Map<String, List<Double>> scaling,
        String displayName,
        Material icon,
        AbilityType type,
        Map<String, AbilityTrigger> triggers, // action key -> trigger, as authored in JSON (inverted by the loader)
        Map<Integer, List<UpgradeRequirement>> upgrades
) {

    /** Map keys must encode as strings, but upgrade levels are authored as JSON object keys like "1", "2". */
    private static final Codec<Integer> LEVEL_KEY_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    return DataResult.error("Not a valid level number: " + s);
                }
            },
            String::valueOf
    );

    public static final Codec<AbilityConfig> CODEC = Codec.composite(
            Codec.listOf(Codec.STRING).optionalFieldOf("description", List.of()).forGetter(AbilityConfig::description),
            Codec.unboundedMap(Codec.listOf(Codec.DOUBLE)).optionalFieldOf("scaling", Map.of()).forGetter(AbilityConfig::scaling),
            Codec.STRING.optionalFieldOf("display_name", "").forGetter(AbilityConfig::displayName),
            Codec.MATERIAL.optionalFieldOf("icon", Material.BARRIER).forGetter(AbilityConfig::icon),
            Codec.enumOf(AbilityType.class).optionalFieldOf("type", AbilityType.PASSIVE).forGetter(AbilityConfig::type),
            Codec.unboundedMap(AbilityTrigger.CODEC).optionalFieldOf("trigger", Map.of()).forGetter(AbilityConfig::triggers),
            Codec.unboundedMap(LEVEL_KEY_CODEC, Codec.lenientListOf(UpgradeRequirement.CODEC)).optionalFieldOf("upgrades", Map.of()).forGetter(AbilityConfig::upgrades),
            AbilityConfig::new
    );
}
