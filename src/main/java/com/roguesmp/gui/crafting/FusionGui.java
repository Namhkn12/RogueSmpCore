package com.roguesmp.gui.crafting;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.FusionRecipe;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code /fusiondemo} GUI - a player-fillable {@link FusionRecipe} layout: 8 ingredient pedestals
 * at slots {@link #INGREDIENT_SLOTS} surrounding a single input/output slot at {@link #INPUT_SLOT},
 * with the functional buttons (guide, Start Fusion, close) along the bottom row, backed by the real
 * {@link CraftingManager}.
 * <p>
 * Flow: drop the input item into the center and the ingredients into the 8 pedestals; a colored
 * trail runs from each occupied pedestal toward the center (red = not part of the recipe, yellow =
 * recognized but incomplete, lime = ready). Clicking the (now enabled) Start Fusion button hides the
 * real input behind a same-material "Đang hợp nhất" placeholder and plays a swirl animation - the
 * ingredients visually orbit the ring, then spiral inward along their trails and vanish - after
 * which the center reveals the real result. Every fusion slot stays locked, and a new fusion can't
 * be started, until the player takes the result out of the center slot.
 * <p>
 * This repo has no JUnit suite (see CLAUDE.md) - verification happens by running/attaching to a
 * live server, so this exists as a manual smoke-test tool for that workflow. Not wired into any
 * real gameplay path; the demo recipe it registers is additive and in-memory only (never written
 * to disk), and re-registers every time the GUI is opened.
 */
public class FusionGui extends BaseGui {

    private static final int[] INGREDIENT_SLOTS = {2, 4, 6, 19, 25, 38, 40, 42};
    private static final int INPUT_SLOT = 22;
    private static final int FUSE_BUTTON_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    /** Ring order (clockwise around the octagon) used to animate ingredients swirling before they converge. */
    private static final int[] RING_ORDER = {2, 4, 6, 25, 42, 40, 38, 19};

    /**
     * Decorative background trail from each ingredient pedestal to the center, and also each
     * pedestal's "journey" (itself + its trail) that its icon travels inward along during the
     * converge phase of the animation. Hand-picked for this exact layout.
     */
    private static final Map<Integer, int[]> INGREDIENT_PATHS = Map.of(
            2, new int[]{12},
            4, new int[]{13},
            6, new int[]{14},
            19, new int[]{20, 21},
            25, new int[]{23, 24},
            38, new int[]{30},
            40, new int[]{31},
            42, new int[]{32}
    );

    private static final Map<Integer, int[]> JOURNEYS = buildJourneys();

    private static Map<Integer, int[]> buildJourneys() {
        Map<Integer, int[]> journeys = new HashMap<>();
        for (Map.Entry<Integer, int[]> entry : INGREDIENT_PATHS.entrySet()) {
            int pedestal = entry.getKey();
            int[] trail = entry.getValue();
            int[] journey = new int[1 + trail.length];
            journey[0] = pedestal;
            System.arraycopy(trail, 0, journey, 1, trail.length);
            journeys.put(pedestal, journey);
        }
        return journeys;
    }

    private static final int MAX_JOURNEY_LENGTH = JOURNEYS.values().stream().mapToInt(j -> j.length).max().orElse(0);

    private static final Set<Integer> RESERVED_SLOTS = computeReservedSlots();

    private static Set<Integer> computeReservedSlots() {
        Set<Integer> reserved = new HashSet<>();
        for (int slot : INGREDIENT_SLOTS) reserved.add(slot);
        reserved.add(INPUT_SLOT);
        reserved.add(FUSE_BUTTON_SLOT);
        reserved.add(CLOSE_SLOT);
        for (int[] path : INGREDIENT_PATHS.values()) {
            for (int slot : path) reserved.add(slot);
        }
        return Set.copyOf(reserved);
    }

    private static final int SPIN_FRAMES = 12;
    private static final int CONVERGE_FRAMES = MAX_JOURNEY_LENGTH; // frames 0..CONVERGE_FRAMES, last one finishes clearing the longest trail
    private static final long ANIMATION_FRAME_INTERVAL = 2L;

    private enum State {IDLE, PROCESSING, AWAITING_PICKUP}

    private final Player player;

    private State state = State.IDLE;

    /** The real input item, captured right before animating - lets {@link #onCloseInventory} give back the actual item instead of the "Đang hợp nhất" placeholder if the GUI closes mid-fusion. */
    private @Nullable ItemStack frozenInputForClose;

    public FusionGui(Player player) {
        super(Utils.text("Fusion", NamedTextColor.DARK_GRAY), 6);
        this.player = player;
    }

    @Override
    public void setup() {

        addButton(CLOSE_SLOT, closeButton(), event -> player.closeInventory());
        fillBackground();
        render();
    }

    /** Fills every slot except the ingredient pedestals, their trails, input/output, and the bottom-row buttons. */
    private void fillBackground() {
        Inventory inv = getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (RESERVED_SLOTS.contains(i)) continue;
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
    }

    @Override
    public void onClickTopInventory(InventoryClickEvent event) {
        super.onClickTopInventory(event);
        Utils.runLater(this::render);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        Utils.runLater(this::render);
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        Inventory inv = getInventory();
        for (int slot : INGREDIENT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) PlayerUtils.giveItem(player, item);
        }

        // PROCESSING: the slot shows the placeholder, so give back the real item captured before animating.
        // IDLE/AWAITING_PICKUP: the slot genuinely holds the real input or the real result - read it directly.
        ItemStack input = state == State.PROCESSING ? frozenInputForClose : inv.getItem(INPUT_SLOT);
        if (input != null && !input.getType().isAir()) PlayerUtils.giveItem(player, input);
    }

    private void render() {
        if (state == State.PROCESSING) return; // the animation owns every fusion slot until it finishes

        if (state == State.AWAITING_PICKUP) {
            ItemStack current = getInventory().getItem(INPUT_SLOT);
            if (current == null || current.getType().isAir()) {
                state = State.IDLE; // player collected the result - undo the "finished" green paint-over, then fall through and render the idle state below
                resetToIdleDecor();
            } else {
                addButton(FUSE_BUTTON_SLOT, collectResultFirstItem(), ClickHandler.noAction());
                renderIngredientTrails(false);
                return;
            }
        }

        // Ingredient slots never keep a handler bound between idle renders except this pass-through one,
        // so freezeSlots()'s noAction from a previous fusion always gets overwritten back to normal here.
        for (int slot : INGREDIENT_SLOTS) addAction(slot, event -> {});
        addAction(INPUT_SLOT, event -> {});

        ItemStack[] items = currentItems();
        FusionRecipe matched = CraftingManager.getInstance().match(CraftingRecipes.FUSION, items, 0, 0);

        addButton(FUSE_BUTTON_SLOT, fuseButtonItem(matched), matched != null ? this::onFuseButtonClick : ClickHandler.noAction());
        renderIngredientTrails(matched != null);
    }

    /**
     * Colors each occupied pedestal's trail toward the center: red for an item this recipe doesn't
     * want at all, yellow for a recognized ingredient while the set is still incomplete, lime once
     * everything (including the input) lines up and Start Fusion would actually work.
     */
    private void renderIngredientTrails(boolean ready) {
        Inventory inv = getInventory();
        for (int slot : INGREDIENT_SLOTS) {
            Material color = trailColor(inv.getItem(slot), ready);
            for (int pathSlot : INGREDIENT_PATHS.get(slot)) {
                addButton(pathSlot, createDecoration(color), ClickHandler.noAction());
            }
        }
    }

    /** Undoes {@link #applyFinishedDecor} - restores the plain black background and empties the (still green-covered) pedestals back out. */
    private void resetToIdleDecor() {
        Inventory inv = getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (RESERVED_SLOTS.contains(i)) continue;
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
        for (int slot : INGREDIENT_SLOTS) addItem(slot, null);
    }

    private Material trailColor(@Nullable ItemStack stack, boolean ready) {
        if (stack == null || stack.getType().isAir()) return Material.BLACK_STAINED_GLASS_PANE;
        if (ready) return Material.LIME_STAINED_GLASS_PANE;
        return Material.YELLOW_STAINED_GLASS_PANE;
    }

    /** Locks the layout, hides the real input behind a placeholder, and kicks off {@link #animateFusion}. */
    private void onFuseButtonClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (state != State.IDLE) return;

        ItemStack[] items = currentItems(); // a defensive clone of every slot - the animation is about to overwrite the live ones
        FusionRecipe matched = CraftingManager.getInstance().match(CraftingRecipes.FUSION, items, 0, 0);
        if (matched == null) return;

        ItemStack result = matched.getResultStack();
        if (result == null) return;

        state = State.PROCESSING;
        frozenInputForClose = items[0];
        freezeSlots();
        hideInputBehindPlaceholder(items[0]);
        animateFusion(items, result);
    }

    /** Cancels every fusion-slot handler so nothing can be moved while {@link #animateFusion} plays. */
    private void freezeSlots() {
        for (int slot : INGREDIENT_SLOTS) addAction(slot, ClickHandler.noAction());
        addAction(INPUT_SLOT, ClickHandler.noAction());
        addButton(FUSE_BUTTON_SLOT, processingButtonItem(), ClickHandler.noAction());
    }

    /** Swaps the center slot for a same-material "Đang hợp nhất" stand-in - never touched again until {@link #finishFusion}. */
    private void hideInputBehindPlaceholder(ItemStack realInput) {
        ItemStack placeholder = ItemStack.of(realInput.getType());
        placeholder.setData(DataComponentTypes.ITEM_NAME, Utils.text("Đang hợp nhất", NamedTextColor.GOLD));
        addItem(INPUT_SLOT, placeholder);
    }

    /**
     * The swirl: ingredients orbit the ring for {@link #SPIN_FRAMES} frames, then each spirals
     * inward along its own trail (see {@link #JOURNEYS}) and vanishes, absorbed into the center -
     * which itself is never touched here, staying on the "Đang hợp nhất" placeholder throughout.
     * GUI inventories can't render real particles over a slot, so this is the standard
     * flip-book-of-items approach instead (see {@code GemSocketingGui} for the same technique
     * elsewhere in this plugin).
     */
    private void animateFusion(ItemStack[] items, ItemStack result) {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.4f);

        Map<Integer, ItemStack> iconBySlot = new HashMap<>();
        for (int i = 0; i < INGREDIENT_SLOTS.length; i++) {
            iconBySlot.put(INGREDIENT_SLOTS[i], items[i + 1]); // items[0] is the input slot, ingredients start at 1
        }

        new BukkitRunnable() {
            int frame = 0;
            ItemStack[] convergingIcons; // fixed snapshot of "icon currently at ring position i", taken once spinning stops

            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory().getHolder() != FusionGui.this) {
                    cancel();
                    state = State.IDLE;
                    frozenInputForClose = null;
                    return;
                }

                if (frame < SPIN_FRAMES) {
                    int offset = frame % RING_ORDER.length;
                    for (int i = 0; i < RING_ORDER.length; i++) {
                        int sourceSlot = RING_ORDER[(i + offset) % RING_ORDER.length];
                        addItem(RING_ORDER[i], iconBySlot.get(sourceSlot));
                    }
                    pulseTrails(frame);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, 1.0f + frame * 0.03f);
                    frame++;
                    return;
                }

                if (convergingIcons == null) {
                    int finalOffset = (SPIN_FRAMES - 1) % RING_ORDER.length;
                    convergingIcons = new ItemStack[RING_ORDER.length];
                    for (int i = 0; i < RING_ORDER.length; i++) {
                        convergingIcons[i] = iconBySlot.get(RING_ORDER[(i + finalOffset) % RING_ORDER.length]);
                    }
                }

                int convergeFrame = frame - SPIN_FRAMES;
                if (convergeFrame > CONVERGE_FRAMES) {
                    cancel();
                    finishFusion(result);
                    return;
                }

                for (int i = 0; i < RING_ORDER.length; i++) {
                    int[] journey = JOURNEYS.get(RING_ORDER[i]);

                    if (convergeFrame > 0 && convergeFrame - 1 < journey.length) {
                        addItem(journey[convergeFrame - 1], null);
                    }
                    if (convergeFrame < journey.length) {
                        addItem(journey[convergeFrame], convergingIcons[i]);
                    }
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.4f + convergeFrame * 0.1f);
                frame++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, ANIMATION_FRAME_INTERVAL);
    }

    private void pulseTrails(int frame) {
        Material color = frame % 2 == 0 ? Material.YELLOW_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
        for (int[] path : INGREDIENT_PATHS.values()) {
            for (int slot : path) addItem(slot, createDecoration(color));
        }
    }

    /**
     * Hides the (now-consumed) pedestals, paints the background green, and reveals the real result -
     * locked in place until the player takes it. Doesn't bother computing/writing
     * {@link FusionRecipe#consume}'s leftover-per-pedestal result - {@link #applyFinishedDecor}
     * below covers every pedestal regardless (per design, ingredient slots are fully hidden once a
     * fusion finishes, whatever - if anything - it left behind).
     */
    private void finishFusion(ItemStack result) {
        Inventory inv = getInventory();
        inv.setItem(INPUT_SLOT, result.clone());
        addAction(INPUT_SLOT, event -> {}); // unfreeze just the center slot - normal click takes the result out

        applyFinishedDecor();

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.02);

        frozenInputForClose = null;
        state = State.AWAITING_PICKUP;
        render();
    }

    /** Covers the background, the trails and the (now-empty) ingredient pedestals themselves in green - a "success" screen around the result. */
    private void applyFinishedDecor() {
        Inventory inv = getInventory();
        ItemStack finishedPane = createDecoration(Material.GREEN_STAINED_GLASS_PANE);

        for (int i = 0; i < inv.getSize(); i++) {
            if (RESERVED_SLOTS.contains(i)) continue;
            addButton(i, finishedPane, ClickHandler.noAction());
        }
        for (int[] path : INGREDIENT_PATHS.values()) {
            for (int slot : path) addButton(slot, finishedPane, ClickHandler.noAction());
        }
        for (int slot : INGREDIENT_SLOTS) {
            addButton(slot, finishedPane, ClickHandler.noAction());
        }
    }

    /**
     * A defensive clone of every fusion slot as a flat array - index 0 is the input slot, 1.. are
     * the ingredient pedestals in {@link #INGREDIENT_SLOTS} order (the convention {@link FusionRecipe}
     * expects). Survives the live inventory being overwritten by the animation.
     */
    private ItemStack[] currentItems() {
        Inventory inv = getInventory();
        ItemStack[] items = new ItemStack[INGREDIENT_SLOTS.length + 1];

        ItemStack input = inv.getItem(INPUT_SLOT);
        items[0] = input != null ? input.clone() : null;

        for (int i = 0; i < INGREDIENT_SLOTS.length; i++) {
            ItemStack stack = inv.getItem(INGREDIENT_SLOTS[i]);
            items[i + 1] = stack != null ? stack.clone() : null;
        }
        return items;
    }

    private ItemStack fuseButtonItem(@Nullable FusionRecipe matched) {
        boolean ready = matched != null;
        ItemStack item = ItemStack.of(ready ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.text(ready ? "Start Fusion" : "Not ready", ready ? NamedTextColor.GREEN : NamedTextColor.RED));

        List<Component> lore = ready
                ? List.of(Utils.text("Click to fuse!", NamedTextColor.GRAY), Utils.text("Matched: " + matched.getId(), NamedTextColor.DARK_GRAY))
                : List.of(Utils.text("Place the input + ingredients", NamedTextColor.GRAY), Utils.text("exactly as the recipe needs.", NamedTextColor.GRAY));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack processingButtonItem() {
        ItemStack item = ItemStack.of(Material.GRAY_CONCRETE);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.text("Fusing...", NamedTextColor.GOLD));
        return item;
    }

    private ItemStack collectResultFirstItem() {
        ItemStack item = ItemStack.of(Material.YELLOW_CONCRETE);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.text("Take your result first!", NamedTextColor.YELLOW));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Click the center slot to collect it.", NamedTextColor.GRAY))));
        return item;
    }

    private ItemStack closeButton() {
        ItemStack item = ItemStack.of(Material.BARRIER);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.text("Close", NamedTextColor.RED));
        return item;
    }

    public static void register() {
        new CommandAPICommand("fusiondemo")
                .executesPlayer((player, args) -> {
                    new FusionGui(player).showInventory(player);
                })
                .register(RogueSmpCore.getInstance());
    }

}
