package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Room;
import com.roguesmp.dungeon.service.IDungeonService;

import java.util.List;
import java.util.Optional;

public class TemplateController {

    private final IDungeonService dungeonService;

    public TemplateController(IDungeonService dungeonService) {
        this.dungeonService = dungeonService;
    }

    public ActionResult<Dungeon> createTemplate(String name, List<Room> rooms) {
        try {
            Dungeon dungeon = dungeonService.createDungeon(name);
            return ActionResult.ok("Dungeon template created: " + dungeon.getDgId(), dungeon);
        } catch (IllegalArgumentException e) {
            return ActionResult.failed(e.getMessage());
        }
    }

    public ActionResult<Dungeon> loadTemplate(String dgId) {
        Optional<Dungeon> dungeon = dungeonService.getDungeonById(dgId);

        return dungeon
                .map(d -> ActionResult.<Dungeon>ok("Dungeon template found.", d))
                .orElseGet(() -> ActionResult.failed("No dungeon template found with id: " + dgId));
    }

    public ActionResult<List<String>> loadTemplateIdList() {
        List<String> ids = dungeonService.getDungeonIdList();

        if (ids.isEmpty()) return ActionResult.failed("No dungeon templates found.");

        return ActionResult.ok("Found " + ids.size() + " dungeon template(s).", ids);
    }

    public ActionResult<Void> deleteTemplate(String dgId) {
        boolean deleted = dungeonService.deleteDungeon(dgId);

        return deleted
                ? ActionResult.ok("Dungeon template deleted: " + dgId)
                : ActionResult.failed("No dungeon template found with id: " + dgId);
    }
}