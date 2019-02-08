package jp.jyn.chestsafe.command;

import jp.jyn.jbukkitlib.command.SubExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandRedirection implements CommandExecutor, TabCompleter {
    private final static String[] NO_ARGS = new String[0];

    private final Map<String, String[]> alias = new HashMap<>();
    private final SubExecutor executor;

    @SuppressWarnings("SpellCheckingInspection")
    public CommandRedirection(SubExecutor executor) {
        this.executor = executor;
        alias.put("lock", array("private"));
        alias.put("unlock", array("remove"));
        alias.put("cprivate", array("private"));
        alias.put("cpublic", array("public"));
        //alias.put("cpassword", array("password"));
        //alias.put("cdonation", array("donation"));
        //alias.put("cunlock", array("unlock"));
        alias.put("cremove", array("remove"));
        alias.put("cinfo", array("info"));
        alias.put("cmodify", array("member", "modify"));
        alias.put("cpersist", array("persist"));
        alias.put("chopper", array("flag", "hopper"));
        alias.put("cexplosion", array("flag", "explosion"));
        alias.put("callowexplosions", array("flag", "explosion"));
        alias.put("ctnt", array("flag", "explosion"));
        alias.put("cfire", array("flag", "fire"));
        alias.put("credstone", array("flag", "redstone"));
        alias.put("cmob", array("flag", "mob"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        executor.onCommand(sender, command, label, getRedirectArgs(command, args));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return executor.onTabComplete(sender, command, alias, getRedirectArgs(command, args));
    }

    private static String[] array(String... args) {
        return args;
    }

    private String[] getRedirectArgs(Command command, String[] args) {
        String[] redirect = alias.getOrDefault(command.getName().toLowerCase(Locale.ENGLISH), NO_ARGS);
        return argsConcat(redirect, args);
    }

    /**
     * concat string array
     *
     * @param arr1 array1
     * @param arr2 array2
     * @return array
     */
    private String[] argsConcat(String[] arr1, String[] arr2) {
        if (arr2.length == 0) {
            return arr1;
        }
        if (arr1.length == 0) {
            return arr2;
        }

        final String[] result = new String[arr1.length + arr2.length];
        // copy
        System.arraycopy(arr1, 0, result, 0, arr1.length);
        System.arraycopy(arr2, 0, result, arr1.length, arr2.length);

        return result;
    }

    public Collection<String> getRedirects() {
        return alias.keySet();
    }
}
