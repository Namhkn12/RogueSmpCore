package com.roguesmp.dungeon.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.IOException;

public class LocationAdapter extends TypeAdapter<Location> {

    @Override
    public void write(JsonWriter out, Location value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        out.name("world").value(value.getWorld() != null ? value.getWorld().getName() : "");
        out.name("x").value(value.getX());
        out.name("y").value(value.getY());
        out.name("z").value(value.getZ());
        out.name("yaw").value(value.getYaw());
        out.name("pitch").value(value.getPitch());
        out.endObject();
    }

    @Override
    public Location read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String world = "";
        double x = 0, y = 0, z = 0;
        float yaw = 0, pitch = 0;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "world" -> world = in.nextString();
                case "x"    -> x = in.nextDouble();
                case "y"    -> y = in.nextDouble();
                case "z"    -> z = in.nextDouble();
                case "yaw"  -> yaw = (float) in.nextDouble();
                case "pitch"-> pitch = (float) in.nextDouble();
                default     -> in.skipValue();
            }
        }
        in.endObject();

        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}