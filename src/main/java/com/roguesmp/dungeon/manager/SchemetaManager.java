package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.constraint.PrefixConfig;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.repository.ISchemetaRepository;
import com.roguesmp.dungeon.ultis.ConsoleLogger;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemetaManager {

    private final Map<String, Schemeta> schemetas = new HashMap<>();
    private final ISchemetaRepository repository;

    public SchemetaManager(ISchemetaRepository repository) {
        this.repository = repository;

        //init data
        load();
    }

    public void load() {
        schemetas.clear();
        repository.loadAll().forEach(s -> schemetas.put(s.getSchemId(), s));
        ConsoleLogger.info(PrefixConfig.SCHEMETA, "Load " + schemetas.size() + " schemeta file");
    }

    public void register(Schemeta schemeta) throws IOException {
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

    public void saveSchem(String name, Clipboard clipboard) throws IOException {
        repository.saveSchem(name, clipboard);
    }

    public Clipboard loadSchem(String name) throws IOException {
        return repository.loadSchem(name);
    }
}