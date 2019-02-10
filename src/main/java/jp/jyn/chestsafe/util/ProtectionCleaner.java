package jp.jyn.chestsafe.util;

import jp.jyn.chestsafe.config.MainConfig;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.jbukkitlib.config.parser.template.variable.SupplierVariable;
import jp.jyn.jbukkitlib.config.parser.template.variable.TemplateVariable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

public class ProtectionCleaner extends BukkitRunnable {
    private final Set<Material> protectable = EnumSet.noneOf(Material.class);
    private final MessageConfig.CleanupMessage message;
    private final ProtectionRepository repository;

    private final int speed;
    private final CommandSender[] sender;

    private final TemplateVariable variable = SupplierVariable.init();
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
        offset = repository.checkAll(speed, offset, this::check);

        sendMessage(message.progress.toString(variable));

        if (checked < speed) {
            this.cancel();
        }
    }

    private ProtectionRepository.Checker.Do check(String name, int x, int y, int z, Supplier<Protection> ignore) {
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
        if (exists) {
            return ProtectionRepository.Checker.Do.NOTHING;
        }

        removed += 1;
        sendMessage(message.removed.toString(variable));
        return ProtectionRepository.Checker.Do.REMOVE;
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
