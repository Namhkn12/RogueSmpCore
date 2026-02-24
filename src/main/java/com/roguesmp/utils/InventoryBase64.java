package com.roguesmp.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class InventoryBase64 {

    public static String itemMapToBase64(Map<Integer, ItemStack> items){
        if(items == null || items.isEmpty()) return "";
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(items.size());
            for(Map.Entry<Integer, ItemStack> entry: items.entrySet()){
                dos.writeInt(entry.getKey());

                byte[] itemBytes = entry.getValue().serializeAsBytes();

                dos.writeInt(itemBytes.length);
                dos.write(itemBytes);
            }

            dos.close();

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
        catch(Exception e){
            Bukkit.getLogger().severe("Cannot convert item maps into base64" );
            Bukkit.getLogger().severe(e.getMessage());
            return "";
        }
    }

    public static Map<Integer, ItemStack> itemMapFromBase64(String data){
        Map<Integer, ItemStack> map = new HashMap<>();
        if(data==null || data.isEmpty()) return map;

        try{
            byte[] decodedBytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes);
            DataInputStream dis = new DataInputStream(bais);

            int size = dis.readInt();

            for(int i=0; i<size; i++){
                int slot = dis.readInt();
                int length = dis.readInt();       // Đọc độ dài dữ liệu item

                byte[] itemBytes = new byte[length];
                dis.readFully(itemBytes);         // Đọc đủ dữ liệu byte của item đó

                ItemStack item = ItemStack.deserializeBytes(itemBytes);
                map.put(slot, item);
            }

            dis.close();
        }
        catch(Exception e){
            Bukkit.getLogger().severe("Cannot convert base64 string into item map" );
            Bukkit.getLogger().severe(e.getMessage());
        }

        return map;
    }
}
