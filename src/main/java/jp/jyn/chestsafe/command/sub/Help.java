package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MessageConfig;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

public class Help extends SubCommand {
    private final Map<String, SubCommand> commands;

    public Help(MessageConfig message, Map<String, SubCommand> commands) {
        super(message);
        this.commands = commands;
    }

    @Override
    protected boolean execCommand(CommandSender sender, Queue<String> args) {
        // search detail help.
        SubCommand cmd = null;
        if (!args.isEmpty()) {
            String sub = args.remove().toLowerCase(Locale.ENGLISH);
            cmd = commands.get(sub);
        }

        if (cmd == null) {
            sendSubCommands(sender);
        } else {
            sendSubDetails(sender, cmd);
        }
        return true;
    }

    @Override
    protected List<String> execTabComplete(CommandSender sender, Deque<String> args) {
        if (args.size() == 1) {
            return commands.keySet().stream()
                .filter(str -> str.startsWith(args.getFirst()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe help [command]",
            message.help.help.toString(),
            "/chestsafe help",
            "/chestsafe help private");
    }

    public void sendSubCommands(CommandSender sender) {
        String[] messages = commands.values()
            .stream()
            .map(SubCommand::getHelp)
            .filter(Objects::nonNull)
            .map(this::simpleHelp)
            .toArray(String[]::new);

        sender.sendMessage(MessageConfig.HEADER);
        sender.sendMessage(messages);
    }

    public void sendSubDetails(CommandSender sender, SubCommand cmd) {
        CommandHelp help = cmd.getHelp();
        if (help != null) {
            sender.sendMessage(MessageConfig.HEADER);
            sender.sendMessage(detailsHelp(help));
        }
    }

    private String simpleHelp(CommandHelp help) {
        return help.usage + " - " + help.description;
    }

    private String[] detailsHelp(CommandHelp help) {
        List<String> tmp = new ArrayList<>();
        tmp.add(help.usage);
        tmp.add(help.description);
        if (help.example.length != 0) {
            tmp.add("");
            tmp.add(message.help.example.toString());
            tmp.addAll(Arrays.asList(help.example));
        }

        return tmp.toArray(new String[0]);
    }
}
