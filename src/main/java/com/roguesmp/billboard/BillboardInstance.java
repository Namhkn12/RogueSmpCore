package com.roguesmp.billboard;

import com.roguesmp.action.ActionContext;
import com.roguesmp.action.ActionEntry;
import com.roguesmp.billboard.line.BillboardLine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class BillboardInstance {

    private static final int RANGE_CHECK_INTERVAL = 10;
    private static final double LINE_HEIGHT = 0.25;
    private static final float DEFAULT_HITBOX_WIDTH = 2f;

    private record Segment(List<BillboardLine> lines, DisplaySettings settings, double yOffset) {
    }

    private final Billboard billboard;
    private final Location anchor;
    private final List<Segment> segments;
    private final List<PacketEntity<TextDisplay>> displays = new ArrayList<>();
    private final @Nullable PacketEntity<Interaction> hitbox;
    private final @Nullable BiConsumer<Player, Boolean> clickHandler;
    private final Map<UUID, Component[]> viewers = new HashMap<>();
    private final double viewRangeSquared;
    private long tick;

    BillboardInstance(Billboard billboard, Location anchor, @Nullable BiConsumer<Player, Boolean> clickHandler) {
        this.billboard = billboard;
        this.anchor = anchor;
        this.clickHandler = clickHandler;
        this.segments = layout(billboard);
        var level = ((CraftWorld) anchor.getWorld()).getHandle();
        for (Segment segment : segments) {
            displays.add(new PacketEntity<>(new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level),
                    anchor.clone().add(0, segment.yOffset(), 0), segment.settings()::apply));
        }
        if (billboard.isClickable() || clickHandler != null) {
            Billboard.Hitbox size = billboard.hitbox() != null ? billboard.hitbox() : estimateHitbox(segments);
            this.hitbox = new PacketEntity<>(new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION, level), anchor, interaction -> {
                interaction.setInteractionWidth(size.width());
                interaction.setInteractionHeight(size.height());
                interaction.setResponsive(true);
            });
        } else {
            this.hitbox = null;
        }
        this.viewRangeSquared = billboard.viewRange() * billboard.viewRange();
    }

    private static List<Segment> layout(Billboard billboard) {
        DisplaySettings base = billboard.display().merge(DisplaySettings.DEFAULT);

        List<List<BillboardLine>> groups = new ArrayList<>();
        List<DisplaySettings> groupSettings = new ArrayList<>();
        for (BillboardLine line : billboard.lines()) {
            DisplaySettings resolved = line.display().merge(base);
            int last = groups.size() - 1;
            if (last >= 0 && groupSettings.get(last).equals(resolved)) {
                groups.get(last).add(line);
            } else {
                groups.add(new ArrayList<>(List.of(line)));
                groupSettings.add(resolved);
            }
        }

        List<Segment> segments = new ArrayList<>();
        double offset = 0;
        for (int i = groups.size() - 1; i >= 0; i--) {
            List<BillboardLine> lines = groups.get(i);
            DisplaySettings settings = groupSettings.get(i);
            segments.add(new Segment(List.copyOf(lines), settings, offset));
            offset += lines.size() * LINE_HEIGHT * settings.scaleOrDefault() + billboard.lineSpacing();
        }
        Collections.reverse(segments);
        return segments;
    }

    private static Billboard.Hitbox estimateHitbox(List<Segment> segments) {
        if (segments.isEmpty()) return new Billboard.Hitbox(1f, (float) LINE_HEIGHT);
        Segment top = segments.getFirst();
        float height = (float) (top.yOffset() + top.lines().size() * LINE_HEIGHT * top.settings().scaleOrDefault());
        float maxScale = (float) segments.stream().mapToDouble(segment -> segment.settings().scaleOrDefault()).max().orElse(1);
        return new Billboard.Hitbox(DEFAULT_HITBOX_WIDTH * maxScale, Math.max(height, (float) LINE_HEIGHT));
    }

    public Billboard getBillboard() {
        return billboard;
    }

    public Location getAnchor() {
        return anchor.clone();
    }

    int getHitboxEntityId() {
        return hitbox == null ? -1 : hitbox.getEntityId();
    }

    void click(Player player, boolean rightClick) {
        if (!viewers.containsKey(player.getUniqueId())) return;
        ActionEntry.runAll(billboard.actions(), new ActionContext(player, rightClick, null, null, anchor.clone(), null));
        if (clickHandler != null) clickHandler.accept(player, rightClick);
    }

    void tick() {
        tick++;
        if (tick % RANGE_CHECK_INTERVAL == 0) updateViewers();
        if (tick % billboard.refresh() == 0) viewers.keySet().forEach(this::refresh);
    }

    private void updateViewers() {
        Iterator<Map.Entry<UUID, Component[]>> iterator = viewers.entrySet().iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next().getKey());
            if (player == null) {
                iterator.remove();
            } else if (!inRange(player)) {
                despawnAll(player);
                iterator.remove();
            }
        }

        for (Player player : anchor.getWorld().getPlayers()) {
            if (!viewers.containsKey(player.getUniqueId()) && inRange(player)) show(player);
        }
    }

    private boolean inRange(Player player) {
        return player.getWorld().equals(anchor.getWorld()) && player.getLocation().distanceSquared(anchor) <= viewRangeSquared;
    }

    private void show(Player player) {
        Component[] rendered = new Component[displays.size()];
        for (int i = 0; i < rendered.length; i++) {
            rendered[i] = render(segments.get(i), player);
            PacketEntity<TextDisplay> display = displays.get(i);
            display.bukkit().text(rendered[i]);
            display.spawn(player);
        }
        if (hitbox != null) hitbox.spawn(player);
        viewers.put(player.getUniqueId(), rendered);
    }

    private void refresh(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        Component[] lastSent = viewers.get(uuid);
        for (int i = 0; i < lastSent.length; i++) {
            Component rendered = render(segments.get(i), player);
            if (Objects.equals(rendered, lastSent[i])) continue;
            lastSent[i] = rendered;
            PacketEntity<TextDisplay> display = displays.get(i);
            display.bukkit().text(rendered);
            display.sync(player);
        }
    }

    private Component render(Segment segment, Player viewer) {
        List<Component> lines = new ArrayList<>(segment.lines().size());
        for (BillboardLine line : segment.lines()) {
            if (line.visibleTo(viewer)) lines.add(line.render(viewer, tick));
        }
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private void despawnAll(Player player) {
        displays.forEach(display -> display.despawn(player));
        if (hitbox != null) hitbox.despawn(player);
    }

    void forget(UUID uuid) {
        viewers.remove(uuid);
    }

    void destroy() {
        for (UUID uuid : viewers.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) despawnAll(player);
        }
        viewers.clear();
    }
}
