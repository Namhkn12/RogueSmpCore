package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.constant.PrefixConfig;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.repository.ISchemetaRepository;
import com.roguesmp.dungeon.utils.ConsoleLogger;
import com.roguesmp.dungeon.utils.Log4Craft;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemetaManager {

    private final Map<String, Schemeta> schemetas = new HashMap<>();
    private final ISchemetaRepository repository;

    public SchemetaManager(ISchemetaRepository repository) {
        this.repository = repository;

        load();
    }

    public void load() {
        schemetas.clear();
        repository.loadAll().forEach(s -> schemetas.put(s.getSchemId(), s));
        Log4Craft.success("Loaded schemeta to cache: " + schemetas.size() + " party");
    }

    public void register(Schemeta schemeta) {
        schemetas.put(schemeta.getSchemId(), schemeta);
        repository.save(schemeta);
    }

    public void delete(String id) {
        schemetas.remove(id);
        repository.delete(id);
    }

    public Schemeta get(String id) {
        return schemetas.get(id);
    }

    public List<Schemeta> getSchemetaList() {
        return List.copyOf(schemetas.values());
    }

    public List<String> getSchematicIdList() {
        return List.copyOf(schemetas.keySet());
    }

    public void saveSchem(String name, Clipboard clipboard) {
        repository.saveSchem(name, clipboard);
    }

    public Clipboard loadSchem(String name) {
        return repository.loadSchem(name);
    }
}
