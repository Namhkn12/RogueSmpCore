package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.controller.response.ControllerResponse;
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

    public ControllerResponse<Dungeon> createTemplate(String name, List<Room> rooms) {
        try {
            Dungeon dungeon = dungeonService.createDungeon(name);
            return ControllerResponse.success("Dungeon template created: " + dungeon.getDgId(), dungeon);
        } catch (IllegalArgumentException e) {
            return ControllerResponse.failure(e.getMessage());
        }
    }

    public ControllerResponse<Dungeon> loadTemplate(String dgId) {
        Optional<Dungeon> dungeon = dungeonService.getDungeonById(dgId);

        return dungeon
                .map(d -> ControllerResponse.<Dungeon>success("Dungeon template found.", d))
                .orElseGet(() -> ControllerResponse.failure("No dungeon template found with id: " + dgId));
    }

    public ControllerResponse<List<String>> loadTemplateIdList() {
        List<String> ids = dungeonService.getDungeonIdList();

        if (ids.isEmpty()) return ControllerResponse.failure("No dungeon templates found.");

        return ControllerResponse.success("Found " + ids.size() + " dungeon template(s).", ids);
    }

//    public ControllerResponse<Dungeon> editTemplate(Dungeon updated) {
//        if (updated == null || updated.getDgId() == null)
//            return ControllerResponse.failure("Invalid dungeon data.");
//
//        try {
//            Dungeon dungeon = dungeonService.updateDungeon(updated);
//            return ControllerResponse.success("Dungeon template updated: " + dungeon.getDgId(), dungeon);
//        } catch (IllegalArgumentException e) {
//            return ControllerResponse.failure(e.getMessage());
//        }
//    }

    public ControllerResponse<Void> deleteTemplate(String dgId) {
        boolean deleted = dungeonService.deleteDungeon(dgId);

        return deleted
                ? ControllerResponse.success("Dungeon template deleted: " + dgId)
                : ControllerResponse.failure("No dungeon template found with id: " + dgId);
    }
}