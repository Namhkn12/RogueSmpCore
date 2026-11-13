package com.roguesmp.billboard.line;

import com.roguesmp.billboard.DisplaySettings;
import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface BillboardLine {

    Codec<BillboardLine> CODEC = Codec.withAlternative(
            Codec.dispatch(BillboardLine::getTypeId, Registries.BILLBOARD_LINE_CODEC::getOrThrow),
            Codec.STRING.xmap(TextLine::of, line -> ((TextLine) line).text())
    );

    String getTypeId();

    Component render(Player viewer, long tick);

    default DisplaySettings display() {
        return DisplaySettings.EMPTY;
    }

    default @Nullable Requirement condition() {
        return null;
    }

    default boolean visibleTo(Player viewer) {
        Requirement condition = condition();
        if (condition == null) return true;
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(viewer);
        return smpPlayer != null && condition.test(smpPlayer);
    }

    static BillboardLine dynamic(Function<Player, Component> renderer) {
        return new Dynamic(renderer, DisplaySettings.EMPTY);
    }

    static BillboardLine dynamic(Function<Player, Component> renderer, DisplaySettings display) {
        return new Dynamic(renderer, display);
    }

    record Dynamic(Function<Player, Component> renderer, DisplaySettings display) implements BillboardLine {

        @Override
        public String getTypeId() {
            return "dynamic";
        }

        @Override
        public Component render(Player viewer, long tick) {
            return renderer.apply(viewer);
        }
    }
}
