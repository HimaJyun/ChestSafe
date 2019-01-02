package jp.jyn.chestsafe.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public class ActionBarSender {
    private Object enumActionBar;
    private Constructor<?> constructorActionBar;
    private Method methodChatSerializer, methodHandle, methodSendpacket;
    private Field fieldConnection;

    public ActionBarSender() {
        String[] tmpVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        final String version = tmpVersion[tmpVersion.length - 1];

        final Function<String, Class<?>> getNMS = name -> {
            try {
                return Class.forName("net.minecraft.server." + version + "." + name);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        };

        try {
            Class<?> tmpIChatBase = getNMS.apply("IChatBaseComponent"),
                tmpChatMessageType = getNMS.apply("ChatMessageType");

            constructorActionBar = getNMS.apply("PacketPlayOutChat").getConstructor(tmpIChatBase, tmpChatMessageType);
            enumActionBar = tmpChatMessageType.getField("GAME_INFO").get(null);
            fieldConnection = getNMS.apply("EntityPlayer").getField("playerConnection");

            methodChatSerializer = getNMS.apply("IChatBaseComponent$ChatSerializer").getMethod("a", String.class);
            methodSendpacket = getNMS.apply("PlayerConnection").getMethod("sendPacket", getNMS.apply("Packet"));
            methodHandle = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer").getMethod("getHandle");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reset(Player player) {
        send(player, "");
    }

    public void send(Player player, String message) {
        try {
            Object chat = chatSerialize("{\"text\":\"" + message + "\"}");

            sendPacket(
                player,
                constructorActionBar.newInstance(chat, enumActionBar)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object chatSerialize(String json) throws InvocationTargetException, IllegalAccessException {
        return methodChatSerializer.invoke(null, json);
    }

    private void sendPacket(Player player, Object packet) throws InvocationTargetException, IllegalAccessException {
        methodSendpacket.invoke(fieldConnection.get(methodHandle.invoke(player)), packet);
    }
}
