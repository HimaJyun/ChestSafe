package jp.jyn.chestsafe.command;

import jp.jyn.chestsafe.util.PlayerAction;
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
import jp.jyn.chestsafe.uuid.UUIDRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SubExecutor implements CommandExecutor, TabCompleter {
    private final static String NO_ARGS = "help";

    private final Map<String, SubCommand> commands = new LinkedHashMap<>();
    private final Help help;

    public SubExecutor(MainConfig config, MessageConfig message, Plugin plugin, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        commands.put("private", new Private(message, registry, repository, action));
        commands.put("public", new Public(message, repository, action));
        commands.put("flag", new Flag(message, config, repository, action));
        commands.put("remove", new Remove(message, repository, action));
        commands.put("info", new Info(message, registry, repository, action));
        commands.put("member", new Member(message, registry, repository, action));
        commands.put("transfer", new Transfer(message, registry, repository, action));
        commands.put("persist", new Persist(message, action));
        commands.put("cleanup", new Cleanup(message, config, plugin, repository));
        commands.put("reload", new Reload(message, plugin));
        commands.put("version", new Version(message, plugin));

        help = new Help(message, commands);
        commands.put(NO_ARGS, help);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // get args
        String sub = NO_ARGS;
        if (args.length != 0) {
            sub = args[0].toLowerCase(Locale.ENGLISH);
        }

        // get sub command
        SubCommand cmd = commands.get(sub);
        if (cmd == null) {
            // If there is no sub command, display help.
            help.sendSubCommands(sender);
            return true;
        }

        // call command
        if (!cmd.onCommand(sender, command, label, args)) {
            help.sendSubDetails(sender, cmd);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            // It is not actually processed (args is always 1 or more)
            return new ArrayList<>(commands.keySet());
        }

        if (args.length == 1) {
            return commands.keySet().stream()
                .filter(str -> str.startsWith(args[0]))
                .collect(Collectors.toList());
            // Note: "string".startsWith("") -> true
        }

        SubCommand cmd = commands.get(args[0].toLowerCase(Locale.ENGLISH));
        if (cmd == null) {
            return Collections.emptyList();
        }

        return cmd.onTabComplete(sender, command, alias, args);
    }
}
