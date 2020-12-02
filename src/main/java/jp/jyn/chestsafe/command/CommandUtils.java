package jp.jyn.chestsafe.command;

import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentVariable;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Waltz of brains far from object-oriented
public class CommandUtils {
    private CommandUtils() {}

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

    public static List<String> tabCompletePlayer(Deque<String> args) {
        String last = args.removeLast();
        Set<String> exclude = args.stream().map(str -> str.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());

        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(str -> str.startsWith(last))
            .filter(str -> !exclude.contains(str.toLowerCase(Locale.ENGLISH)))
            .collect(Collectors.toList());
    }

    public static void setProtection(BukkitLocale<MessageConfig> message, ProtectionRepository repository,
                                     Player player, Block block, Protection protection) {
        ProtectionRepository.Result result = repository.set(protection, block);

        ComponentVariable variable = ComponentVariable.init().put("block", block.getType());
        switch (result) {
            case NOT_PROTECTABLE:
                message.get(player).notProtectable.apply("block", block.getType()).send(player);
                break;
            case ALREADY_PROTECTED:
                message.get(player).alreadyProtected.apply("block", block.getType()).send(player);
                break;
            case SUCCESS:
                variable.put("type", protection.getType());
                message.get(player).protected_.apply(variable).send(player);
                break;
        }
    }

    public static Optional<Protection> checkProtection(BukkitLocale<MessageConfig> message, ProtectionRepository repository,
                                                       Player player, Block block, ComponentVariable variable) {
        variable.put("block", block.getType());

        Protection protection = repository.get(block).orElse(null);
        if (protection == null) {
            message.get(player).notProtected.apply(variable).send(player);
            return Optional.empty();
        }
        variable.put("type", protection.getType());

        if (!protection.isOwner(player) &&
            !player.hasPermission("chestsafe.passthrough")) {
            message.get(player).denied.apply(variable).send(player);
            return Optional.empty();
        }
        return Optional.of(protection);
    }
}
