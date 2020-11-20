package jp.jyn.chestsafe.event;

import jp.jyn.chestsafe.protection.Protection;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ProtectionSetEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Block block;
    private final Protection protection;

    private boolean cancelled = false;

    public ProtectionSetEvent(Block block, Protection protection) {
        this.block = block;
        this.protection = protection;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    /**
     * Get protect target block
     *
     * @return block
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Get protection
     *
     * @return protection
     */
    public Protection getProtection() {
        return protection;
    }
}
