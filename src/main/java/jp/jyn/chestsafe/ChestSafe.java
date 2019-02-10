package jp.jyn.chestsafe;

import jp.jyn.chestsafe.command.CommandRedirection;
import jp.jyn.chestsafe.command.sub.Cleanup;
import jp.jyn.chestsafe.command.sub.Flag;
import jp.jyn.chestsafe.command.sub.Help;
import jp.jyn.chestsafe.command.sub.Info;
import jp.jyn.chestsafe.command.sub.Member;
import jp.jyn.chestsafe.command.sub.Persist;
import jp.jyn.chestsafe.command.sub.Private;
import jp.jyn.chestsafe.command.sub.Public;
import jp.jyn.chestsafe.command.sub.Reload;
import jp.jyn.chestsafe.command.sub.Remove;
import jp.jyn.chestsafe.command.sub.Transfer;
import jp.jyn.chestsafe.command.sub.Version;
import jp.jyn.chestsafe.config.ConfigLoader;
import jp.jyn.chestsafe.config.MainConfig;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.db.DBConnector;
import jp.jyn.chestsafe.listener.BlockListener;
import jp.jyn.chestsafe.listener.PlayerListener;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.chestsafe.util.ProtectionCleaner;
import jp.jyn.chestsafe.util.VersionChecker;
import jp.jyn.jbukkitlib.command.SubExecutor;
import jp.jyn.jbukkitlib.uuid.UUIDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

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
            config = new ConfigLoader();
        }
        config.reloadConfig();
        MainConfig main = config.getMainConfig();
        MessageConfig message = config.getMessageConfig();

        UUIDRegistry registry = UUIDRegistry.getSharedCacheRegistry(this);

        VersionChecker checker = new VersionChecker(main.versionCheck, message);
        BukkitTask task = getServer().getScheduler().runTaskLater(this, () -> checker.check(Bukkit.getConsoleSender()), 20 * 30);
        destructor.addFirst(task::cancel);

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
        manager.registerEvents(new PlayerListener(main, message, registry, checker, repository, action), this);
        manager.registerEvents(new BlockListener(main, repository), this);
        destructor.addFirst(() -> HandlerList.unregisterAll(this));

        // register commands
        SubExecutor.Builder builder = SubExecutor.Builder.init()
            .setDefaultCommand("help")
            .putCommand("private", new Private(message, registry, repository, action))
            .putCommand("public", new Public(message, repository, action))
            .putCommand("flag", new Flag(main, message, repository, action))
            .putCommand("remove", new Remove(message, repository, action))
            .putCommand("info", new Info(message, registry, repository, action))
            .putCommand("member", new Member(message, registry, repository, action))
            .putCommand("transfer", new Transfer(message, registry, repository, action))
            .putCommand("persist", new Persist(message, action))
            .putCommand("cleanup", new Cleanup(main, message, repository))
            .putCommand("reload", new Reload(message))
            .putCommand("version", new Version(message, checker));
        Help help = new Help(message, builder.getSubCommands());
        builder.setErrorExecutor(help).putCommand("help", help);

        PluginCommand cmd = getCommand("chestsafe");
        SubExecutor subExecutor = builder.register(cmd);
        destructor.addFirst(() -> {
            cmd.setTabCompleter(this);
            cmd.setExecutor(this);
        });

        // register redirects
        CommandRedirection redirection = new CommandRedirection(subExecutor);
        for (String redirect : redirection.getRedirects()) {
            PluginCommand c = getCommand(redirect);
            cmd.setExecutor(redirection);
            cmd.setTabCompleter(redirection);
            destructor.addFirst(() -> {
                c.setTabCompleter(this);
                c.setExecutor(this);
            });
        }
    }

    @Override
    public void onDisable() {
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

    /**
     * <p>Get ChestSafe instance</p>
     *
     * @return ChestSafe instance
     */
    public static ChestSafe getInstance() {
        return instance;
    }
}
