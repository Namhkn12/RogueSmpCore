package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.utils.Log4Craft;
import com.roguesmp.dungeon.data.definition.Schemeta;
import com.roguesmp.dungeon.repository.ISchematicRepository;
import com.roguesmp.dungeon.repository.ISchemetaRepository;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.util.*;

public class SchemetaManager {

    // Eager cache — metadata nhẹ, load hết khi khởi động
    private final Map<String, Schemeta> schemetaCache = new HashMap<>();

    // Lazy cache — Clipboard nặng, chỉ load khi lần đầu paste
    private final Map<String, Clipboard> clipboardCache = new HashMap<>();

    private final ISchemetaRepository schemetaRepo;
    private final ISchematicRepository schematicRepo;

    public SchemetaManager(ISchemetaRepository schemetaRepo,
                           ISchematicRepository schematicRepo) {
        this.schemetaRepo = schemetaRepo;
        this.schematicRepo = schematicRepo;
        loadAll();
    }

    // ── Lifecycle ────────────────────────────────────────────

    public void loadAll() {
        schemetaCache.clear();
        clipboardCache.clear(); // invalidate clipboard cache cùng lúc
        schemetaRepo.loadAll().forEach(s -> schemetaCache.put(s.getId(), s));
        Log4Craft.success("Loaded " + schemetaCache.size() + " schemeta(s) into cache.");
    }

    // ── Schemeta CRUD ────────────────────────────────────────

    public void register(Schemeta schemeta, Clipboard clipboard) {
        schemetaRepo.save(schemeta);
        schematicRepo.save(schemeta.getId(), clipboard);

        schemetaCache.put(schemeta.getId(), schemeta);
        clipboardCache.put(schemeta.getId(), clipboard); // cache luôn vì vừa có trong tay
    }

    public void delete(String id) {
        schemetaRepo.delete(id);
        schematicRepo.delete(id);

        schemetaCache.remove(id);
        clipboardCache.remove(id);
    }

    public Optional<Schemeta> get(String id) {
        return Optional.ofNullable(schemetaCache.get(id));
    }

    public List<Schemeta> getAll() {
        return List.copyOf(schemetaCache.values());
    }

    public boolean exists(String id) {
        return schemetaCache.containsKey(id);
    }

    // ── Clipboard (lazy) ─────────────────────────────────────

    /**
     * Lấy Clipboard để paste. Load từ file nếu chưa có trong cache.
     * Trả về Optional.empty() nếu file không tồn tại.
     */
    public Optional<Clipboard> getClipboard(String id) {
        if (clipboardCache.containsKey(id)) {
            return Optional.of(clipboardCache.get(id));
        }

        if (!schematicRepo.exists(id)) {
            Log4Craft.fire("Schematic file not found for id: " + id, null);
            return Optional.empty();
        }

        Clipboard clipboard = schematicRepo.load(id);
        clipboardCache.put(id, clipboard);
        return Optional.of(clipboard);
    }

    /**
     * Xóa 1 clipboard khỏi cache (giải phóng RAM nếu cần).
     */
    public void evictClipboard(String id) {
        clipboardCache.remove(id);
    }

    /**
     * Xóa toàn bộ clipboard cache — dùng khi reload hoặc bộ nhớ thấp.
     */
    public void evictAllClipboards() {
        clipboardCache.clear();
        Log4Craft.success("Clipboard cache cleared.");
    }
}