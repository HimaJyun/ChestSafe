package jp.jyn.chestsafe;

import jp.jyn.chestsafe.command.CommandRedirection;
import jp.jyn.chestsafe.command.SubExecutor;
import jp.jyn.chestsafe.config.ConfigLoader;
import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.db.DBConnector;
import jp.jyn.chestsafe.listener.BlockListener;
import jp.jyn.chestsafe.listener.PlayerListener;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.chestsafe.util.ProtectionCleaner;
import jp.jyn.chestsafe.uuid.UUIDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;

public class ChestSafe extends JavaPlugin {
    private static ChestSafe instance = null;

    private ConfigLoader config = null;
    private ProtectionRepository repository = null;

    // Stack(LIFO)
    private final Deque<Runnable> destructor = new ArrayDeque<>();

    @Override
    public void onEnable() {
        instance = this;
        destructor.clear();

        // load config.
        if (config == null) {
            config = new ConfigLoader(this);
        }
        config.reloadConfig();
        MainConfig main = config.getMainConfig();
        MessageConfig message = config.getMessageConfig();

        UUIDRegistry registry = new UUIDRegistry(this, main.cache);

        // connect db
        DBConnector dbConnector = new DBConnector(main.database);
        destructor.addFirst(dbConnector::close);

        // init repository
        repository = new ProtectionRepository(main, dbConnector);
        destructor.addFirst(() -> repository = null);

        // cleanup
        if (main.cleanup.enable) {
            int cps = main.cleanup.checkPerSecond;
            long delay = main.cleanup.delay * 20;
            long interval = main.cleanup.interval * 20;
            Runnable runnable = () -> new ProtectionCleaner(main, message, repository, cps, Bukkit.getConsoleSender()).runTaskTimer(this, 0, 20);

            if (interval > 0) {
                getServer().getScheduler().runTaskTimer(this, runnable, delay, interval);
            } else {
                getServer().getScheduler().runTaskLater(this, runnable, delay);
            }
        }
        // Always cancel task (to cancel cleanup command)
        destructor.addFirst(() -> getServer().getScheduler().cancelTasks(this));

        // Player action manager
        PlayerAction action = new PlayerAction();
        destructor.addFirst(action::clear);

        // register events
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new PlayerListener(main, message, this, registry, repository, action), this);
        manager.registerEvents(new BlockListener(main, repository), this);
        destructor.addFirst(() -> HandlerList.unregisterAll(this));

        // register commands
        SubExecutor executor = new SubExecutor(main, message, this, registry, repository, action);
        PluginCommand command = getCommand("chestsafe");
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        destructor.addFirst(() -> command.setExecutor(this));
        destructor.addFirst(() -> command.setTabCompleter(this));

        // redirect commands.
        CommandRedirection redirection = new CommandRedirection(executor);
        redirection.getRedirects().forEach(cmd -> getCommand(cmd).setExecutor(redirection));
        redirection.getRedirects().forEach(cmd -> getCommand(cmd).setTabCompleter(redirection));
        destructor.addFirst(() -> redirection.getRedirects().forEach(cmd -> getCommand(cmd).setExecutor(this)));
        destructor.addFirst(() -> redirection.getRedirects().forEach(cmd -> getCommand(cmd).setTabCompleter(this)));
    }

    @Override
    public void onDisable() {
        repository = null;

        while (!destructor.isEmpty()) {
            destructor.removeFirst().run();
        }
    }

    /**
     * <p>Get ProtectionRepository</p>
     * <p>Note: If the ChestSafe plugin is reloaded, you need to regain repository again</p>
     *
     * @return An instance of ProtectionRepository, or null if not initialized.
     */
    public ProtectionRepository getRepository() {
        return repository;
    }

    public static ChestSafe getInstance() {
        return instance;
    }
}
