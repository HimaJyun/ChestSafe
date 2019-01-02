package jp.jyn.chestsafe.util;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerAction {
    private final Map<UUID, Consumer<Block>> actions = new HashMap<>();
    private final Set<UUID> persist = new HashSet<>();

    public PlayerAction() { }

    public boolean execAction(Player player, Block block) {
        Consumer<Block> action = actions.get(player.getUniqueId());
        if (action == null) {
            return false;
        }

        // call action
        action.accept(block);
        // remove action
        if (!getPersist(player)) {
            actions.remove(player.getUniqueId());
        }
        return true;
    }

    public void setAction(Player player, Consumer<Block> action) {
        actions.put(player.getUniqueId(), Objects.requireNonNull(action));
    }

    public void setPersist(Player player, boolean value) {
        if (value) {
            persist.add(player.getUniqueId());
        } else {
            persist.remove(player.getUniqueId());
            actions.remove(player.getUniqueId());
        }
    }

    public boolean getPersist(Player player) {
        return persist.contains(player.getUniqueId());
    }

    public void clear() {
        actions.clear();
        persist.clear();
    }
}
