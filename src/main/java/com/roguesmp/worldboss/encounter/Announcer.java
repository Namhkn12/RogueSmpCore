package com.roguesmp.worldboss.encounter;

import com.roguesmp.gate.GateDisplay;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import com.roguesmp.worldboss.runtime.WorldBossInstance;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Announcer {

    private final WorldBossInstance fight;
    private final Collection<? extends Player> audience;
    private final TagResolver placeholders;
    private boolean requirementsHover;

    Announcer(WorldBossInstance fight, Collection<? extends Player> audience) {
        this.fight = fight;
        this.audience = audience;
        this.placeholders = placeholders(fight);
    }

    public Announcer withRequirementsHover() {
        requirementsHover = !fight.getDefinition().requirements().isEmpty();
        return this;
    }

    public Announcer chat(String... lines) {
        for (String line : lines) {
            Component message = Utils.fromString(line, placeholders);
            audience.forEach(player -> player.sendMessage(requirementsHover
                    ? message.hoverEvent(HoverEvent.showText(requirementsFor(player)))
                    : message));
        }
        return this;
    }

    private Component requirementsFor(Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        List<Component> lines = new ArrayList<>();
        lines.add(Utils.fromString("<gold>Yêu cầu tham gia"));
        if (smpPlayer != null) {
            for (Requirement requirement : fight.getDefinition().requirements()) {
                lines.add(GateDisplay.check(requirement.getDisplay(smpPlayer), requirement.test(smpPlayer)));
            }
        }
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    public Announcer title(String title, String subtitle) {
        Title shown = Title.title(Utils.fromString(title, placeholders), Utils.fromString(subtitle, placeholders));
        audience.forEach(player -> player.showTitle(shown));
        return this;
    }

    public Announcer actionBar(String text) {
        Component message = Utils.fromString(text, placeholders);
        audience.forEach(player -> player.sendActionBar(message));
        return this;
    }

    public Announcer sound(Key sound, float volume, float pitch) {
        Sound played = Sound.sound(sound, Sound.Source.HOSTILE, volume, pitch);
        audience.forEach(player -> player.playSound(played, Sound.Emitter.self()));
        return this;
    }

    private static TagResolver placeholders(WorldBossInstance fight) {
        Location center = fight.getCenter();
        String survivors = fight.getAlivePlayers().stream().map(Player::getName).collect(Collectors.joining(", "));
        return TagResolver.resolver(
                Placeholder.parsed("boss", fight.getDefinition().displayName()),
                Placeholder.unparsed("place", fight.getSiteName()),
                Placeholder.unparsed("x", String.valueOf(center.getBlockX())),
                Placeholder.unparsed("y", String.valueOf(center.getBlockY())),
                Placeholder.unparsed("z", String.valueOf(center.getBlockZ())),
                Placeholder.unparsed("seconds", String.valueOf(fight.getSecondsLeft())),
                Placeholder.unparsed("participants", String.valueOf(fight.getParticipants().size())),
                Placeholder.unparsed("survivors", survivors),
                Placeholder.unparsed("summoner", summonerName(fight.getSummoner()))
        );
    }

    private static String summonerName(UUID summoner) {
        if (summoner == null) return "";
        OfflinePlayer player = Bukkit.getOfflinePlayer(summoner);
        return player.getName() == null ? "" : player.getName();
    }
}
