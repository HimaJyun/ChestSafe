package jp.jyn.chestsafe.util;

import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.config.parser.Parser;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Set;

public class ProtectionCleaner extends BukkitRunnable {
    private final Set<Material> protectable = EnumSet.noneOf(Material.class);
    private final MessageConfig.CleanupMessage message;
    private final ProtectionRepository repository;

    private final int speed;
    private final CommandSender[] sender;

    private final Parser.Variable variable = new Parser.SupplierVariable();
    private int offset = 0, checked = 0;
    private int protection = 0, removed = 0;

    public ProtectionCleaner(MainConfig config, MessageConfig message, ProtectionRepository repository, int speed, CommandSender... sender) {
        this.protectable.addAll(config.protectable.keySet());
        this.message = message.cleanup;
        this.repository = repository;

        this.speed = speed;
        this.sender = sender;

        variable.put("speed", speed)
            .put("checked", () -> String.valueOf(protection))
            .put("removed", () -> String.valueOf(removed));
        sendMessage(this.message.start.toString(variable));
    }

    @Override
    public void run() {
        checked = 0;
        offset = repository.cleanup(speed, offset, this::checker);

        sendMessage(message.progress.toString(variable));

        if (checked < speed) {
            this.cancel();
        }
    }

    private boolean checker(String name, int x, int y, int z) {
        variable.put("world", name).put("x", x).put("y", y).put("z", z);

        // block exists check
        boolean exists = false;
        World world = Bukkit.getWorld(name);
        if (world != null) {
            Block block = world.getBlockAt(x, y, z);
            exists = protectable.contains(block.getType());
        }

        checked += 1;
        protection += 1;
        if (!exists) {
            removed += 1;
            sendMessage(message.removed.toString(variable));
        }
        return !exists;
    }

    private void sendMessage(String message) {
        for (CommandSender commandSender : sender) {
            commandSender.sendMessage(message);
        }

    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        sendMessage(message.end.toString(variable));
    }
}