package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.jbukkitlib.command.ErrorExecutor;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.locale.MultiLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentParser;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public class Help extends SubCommand implements ErrorExecutor {
    private final BukkitLocale<MessageConfig> message;
    private final Map<String, SubCommand> commands;

    private final BukkitLocale<Map<String, HelpInfo>> help;

    public Help(BukkitLocale<MessageConfig> message, Map<String, SubCommand> commands) {
        this.message = message;
        this.commands = commands;

        this.help = new MultiLocale<>(
            message.get().locale,
            message.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> buildHelp(e.getValue().help)))
        );
    }

    @Override
    public boolean onError(Info error) {
        CommandSender sender = error.sender;
        switch (error.cause) {
            case ERROR:
                details(sender, error.subArgs);
                break;
            case UNKNOWN_COMMAND:
                list(sender);
                break;
            case DONT_HAVE_PERMISSION:
                message.get(sender).doNotHavePermission.apply().send(sender);
                break;
            case MISSING_ARGUMENT:
                message.get(sender).missingArgument.apply().send(sender);
                details(sender, error.subArgs);
                break;
            case PLAYER_ONLY:
                sender.sendMessage(MessageConfig.PLAYER_ONLY);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Error: " + ChatColor.RESET + error.cause.name());
                break;
        }
        return true;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        // search help info.
        HelpInfo info = null;
        if (!args.isEmpty()) {
            info = help.get(sender).get(args.remove().toLowerCase(Locale.ENGLISH));
        }

        if (info == null) {
            list(sender);
        } else {
            details(sender, info);
        }
        return Result.OK;
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, Deque<String> args) {
        if (args.size() == 1) {
            return commands.keySet().stream()
                .filter(str -> str.startsWith(args.getFirst()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private void list(CommandSender sender) {
        Map<String, HelpInfo> m = help.get(sender);

        sender.sendMessage(MessageConfig.HEADER);

        // commandsに追加された順番(SubExecutorはLinkedHashMapになってる)で使用したいのでこうする
        for (String key : commands.keySet()) {
            HelpInfo info = m.get(key);
            if (info != null) {
                sender.spigot().sendMessage(info.help);
            }
        }
    }

    private void details(CommandSender sender, HelpInfo info) {
        CommandSender.Spigot s = sender.spigot();
        sender.sendMessage(MessageConfig.HEADER);
        for (TextComponent[] details : info.details) {
            s.sendMessage(details);
        }
    }

    private void details(CommandSender sender, String command) {
        HelpInfo info = help.get(sender).get(command);
        if (info != null) {
            details(sender, info);
        }
    }

    private Map<String, HelpInfo> buildHelp(MessageConfig.HelpMessage msg) {
        HashMap<String, HelpInfo> m = new HashMap<>();
        HelpBuilder builder = new HelpBuilder(msg.example, msg.run, msg.details);

        builder.command("public").description(msg.public_).put(m);
        builder.command("remove").description(msg.remove).put(m);
        builder.command("info").description(msg.info).put(m);
        builder.command("reload").description(msg.reload).put(m);
        builder.command("version").description(msg.version).put(m);

        builder.command("private").option("[member]").description(msg.private_).usage("", "member1", "member1 member2").put(m);
        builder.command("transfer").option("<owner>").description(msg.transfer).usage("new_owner").put(m);
        builder.command("cleanup").option("[limit]").description(msg.cleanup).usage("", "100", "cancel").put(m);
        builder.command("help").option("[command]").description(msg.help).usage("", "private").put(m);

        builder.command("member").option("<operator> [value]").description(msg.member)
            .usageSuggest("add", "member1")
            .usageSuggest("remove", "member1")
            .usageSuggest("modify", "member1 -member2")
            .put(m);
        builder.command("persist").option("[true/false]").description(msg.persist)
            .usageSuggest("", "")
            .usageSuggest("true", "")
            .usageSuggest("false", "")
            .put(m);

        TextComponent[] flags = msg.availableFlags.apply("flag", c -> {
            c.setText("");
            List<BaseComponent> extra = new ArrayList<>((Protection.Flag.values().length * 2) - 1);
            boolean f = false;
            for (Protection.Flag flag : Protection.Flag.values()) {
                if (f) {
                    extra.add(new TextComponent(", "));
                } else {
                    f = true;
                }
                String name = flag.name().toLowerCase(Locale.ENGLISH);
                TextComponent component = new TextComponent(name);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, HelpBuilder.BASE_COMMAND + "flag " + name));
                component.setHoverEvent(builder.hoverSuggest);
                extra.add(component);
            }
            c.setExtra(extra);
        }).toTextComponent();
        builder.command("flag").option("<flag> [value]").description(msg.flag)
            .usage("hopper true", "explosion remove", "redstone")
            .usage(flags)
            .put(m);

        return Collections.unmodifiableMap(m);
    }

    private final static class HelpInfo {
        private final TextComponent[] help;
        private final TextComponent[][] details;

        private HelpInfo(TextComponent[] help, TextComponent[][] details) {
            this.help = help;
            this.details = details;
        }
    }


    private final static class HelpBuilder {
        private final static String BASE_COMMAND = "/chestsafe ";
        private final static TextComponent EMPTY = new TextComponent();
        private final static TextComponent SEPARATOR = new TextComponent(" - ");

        private final List<TextComponent[]> details = new ArrayList<>();
        private final TextComponent[] example;
        private final HoverEvent hoverSuggest;
        private final HoverEvent hoverDetails;

        private String command;
        private String option;
        private TextComponent[] description;
        private final List<TextComponent[]> usage = new ArrayList<>();

        private String fullCommand;

        private HelpBuilder(ComponentParser example, ComponentParser suggest, ComponentParser details) {
            this.example = example.apply().toTextComponent();
            this.hoverSuggest = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(suggest.apply().toTextComponent()));
            this.hoverDetails = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(details.apply().toTextComponent()));
        }

        private HelpBuilder command(String command) {
            this.command = command;
            this.fullCommand = BASE_COMMAND + command;
            return this;
        }

        private HelpBuilder option(String option) {
            this.option = option;
            return this;
        }

        private HelpBuilder description(ComponentParser description) {
            this.description = description.apply().toTextComponent();
            return this;
        }

        private HelpBuilder usage(TextComponent... component) {
            this.usage.add(component);
            return this;
        }

        private HelpBuilder usage(String... usage) {
            for (String e : usage) {
                this.usage.add(new TextComponent[]{new TextComponent(e.length() == 0 ? fullCommand : fullCommand + " " + e)});
            }
            return this;
        }

        private HelpBuilder usageSuggest(String suggest, String option) {
            String cmd = fullCommand +" "+ suggest;
            TextComponent c = new TextComponent(option.length() == 0 ? cmd : cmd + " " + option);
            c.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd));
            c.setHoverEvent(hoverSuggest);
            usage.add(new TextComponent[]{c});
            return this;
        }

        private HelpBuilder clear() {
            command = null;
            option = null;
            description = null;
            usage.clear();
            details.clear();

            fullCommand = null;

            return this;
        }

        private HelpInfo build() {
            TextComponent c = new TextComponent(option == null ? fullCommand : fullCommand + " " + option);
            c.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, fullCommand));
            c.setHoverEvent(hoverSuggest);

            TextComponent[] help = new TextComponent[]{c, SEPARATOR, new TextComponent()};
            help[2].setExtra(Arrays.asList(description));
            help[2].setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, BASE_COMMAND + "help " + command));
            help[2].setHoverEvent(hoverDetails);

            details.add(new TextComponent[]{c});
            details.add(description);
            if (!usage.isEmpty()) {
                details.add(new TextComponent[]{EMPTY});
                details.add(example);
                details.addAll(usage);
            }

            return new HelpInfo(help,details.toArray(new TextComponent[0][0]));
        }

        private HelpBuilder put(Map<String, HelpInfo> map) {
            map.put(command, build());
            clear();
            return this;
        }
    }
}
