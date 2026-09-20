package com.roguesmp.server;

import com.roguesmp.RogueSmpCore;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;

import java.net.URI;

public class ResourcePackManager {

    public static void sendResourcePack(final Audience target) {
        String url = RogueSmpCore.getGlobalConfig().getResourcePackUrl();
        String hash = RogueSmpCore.getGlobalConfig().getResourcePackHash();
        ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create(url))
                .hash(hash)
                .build();

        final ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .prompt(Component.text("Server bắt buộc cần Resource pack, không chấp nhận là bị đá!"))
                .required(true)
                .build();

        target.sendResourcePacks(request);
    }
}
