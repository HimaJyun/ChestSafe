package jp.jyn.chestsafe.command;

import jp.jyn.chestsafe.ChestSafe;
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
import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubExecutor;
import jp.jyn.jbukkitlib.uuid.UUIDRegistry;
import org.bukkit.command.PluginCommand;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class CommandLoader {
    private final static String COMMAND = "chestsafe";
    private final static String DEFAULT = "help";

    private final Deque<String> registered = new ArrayDeque<>();

    public CommandLoader(MessageConfig message, MainConfig config, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        ChestSafe plugin = ChestSafe.getInstance();

        SubExecutor.Builder builder = SubExecutor.Builder.init()
            .setDefaultCommand(DEFAULT)
            .putCommand("private", new Private(message, registry, repository, action))
            .putCommand("public", new Public(message, repository, action))
            .putCommand("flag", new Flag(message, config, repository, action))
            .putCommand("remove", new Remove(message, repository, action))
            .putCommand("info", new Info(message, registry, repository, action))
            .putCommand("member", new Member(message, registry, repository, action))
            .putCommand("transfer", new Transfer(message, registry, repository, action))
            .putCommand("persist", new Persist(message, action))
            .putCommand("cleanup", new Cleanup(message, config, repository))
            .putCommand("reload", new Reload(message))
            .putCommand("version", new Version(message));

        Help help = new Help(message, builder.getSubCommands());
        SubExecutor subExecutor = builder.setErrorExecutor(help).putCommand(DEFAULT, help).build();

        PluginCommand cmd = plugin.getCommand(COMMAND);
        registered.addFirst(COMMAND);
        cmd.setExecutor(subExecutor);
        cmd.setTabCompleter(subExecutor);

        // register Redirects
        CommandRedirection redirection = new CommandRedirection(subExecutor);
        for (String redirect : redirection.getRedirects()) {
            cmd = plugin.getCommand(redirect);
            registered.addFirst(redirect);
            cmd.setExecutor(redirection);
            cmd.setTabCompleter(redirection);
        }
    }

    public void unloadCommand() {
        ChestSafe plugin = ChestSafe.getInstance();
        while (!registered.isEmpty()) {
            PluginCommand cmd = plugin.getCommand(registered.removeFirst());
            cmd.setTabCompleter(plugin);
            cmd.setExecutor(plugin);
        }
    }

    public static boolean str2Bool(String str) throws IllegalArgumentException {
        switch (str.toLowerCase(Locale.ENGLISH)) {
            case "on":
            case "1":
            case "true":
            case "yes":
            case "enable":
                return true;
            case "off":
            case "0":
            case "false":
            case "no":
            case "disable":
                return false;
            default:
                throw new IllegalArgumentException("Argument is not interpretable as boolean.");
        }
    }
}
