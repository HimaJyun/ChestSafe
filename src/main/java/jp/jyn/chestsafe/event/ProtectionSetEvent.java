package jp.jyn.chestsafe.event;

import jp.jyn.chestsafe.protection.Protection;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ProtectionSetEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private Block block;
    private Protection protection;

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
     * Changing the block does not cause anything (for internal use)
     *
     * @param block new block
     */
    public void setBlock(Block block) {
        this.block = block;
    }

    /**
     * Get protection
     *
     * @return protection
     */
    public Protection getProtection() {
        return protection;
    }

    public void setProtection(Protection protection) {
        this.protection = protection;
    }
}
