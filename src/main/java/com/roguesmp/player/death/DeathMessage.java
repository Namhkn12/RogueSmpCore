package com.roguesmp.player.death;

import com.roguesmp.codec.Codec;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;

public record DeathMessage(List<String> messages) {
    public static final Codec<DeathMessage> CODEC = Codec.composite(
            Codec.listOf(Codec.STRING).fieldOf("messages").forGetter(DeathMessage::messages),
            DeathMessage::new
    );

    public Component render(Component victim, Component killer) {
        String template = messages.get(Utils.RANDOM.nextInt(messages.size()));
        return Utils.fromString(template,
                Placeholder.component("victim", victim),
                Placeholder.component("killer", killer));
    }
}
