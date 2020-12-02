package jp.jyn.chestsafe.config;

import jp.jyn.jbukkitlib.config.parser.component.ComponentParser;
import jp.jyn.jbukkitlib.util.PackagePrivate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MessageConfig {
    private final static String PREFIX = "[ChestSafe] ";
    public final static String HEADER = "========== ChestSafe =========="; // TODO: TextComponentで良いかも
    public final static String PLAYER_ONLY = PREFIX + ChatColor.RED + "This command can only be run by players.";

    public final String locale;

    public final ComponentParser doNotHavePermission;
    public final ComponentParser missingArgument;
    /**
     * value
     */
    public final ComponentParser invalidArgument;
    /**
     * name
     */
    public final ComponentParser playerNotFound;

    /**
     * block,name,uuid,type
     */
    public final ComponentParser notice;
    /**
     * block,type
     */
    public final ComponentParser denied;
    /**
     * block,type
     */
    public final ComponentParser protected_;
    /**
     * block,type
     */
    public final ComponentParser removed;
    public final ComponentParser ready;

    /**
     * block
     */
    public final ComponentParser notProtected;
    /**
     * block
     */
    public final ComponentParser notProtectable;
    /**
     * block
     */
    public final ComponentParser alreadyProtected;

    public final ComponentParser persistEnabled;
    public final ComponentParser persistDisabled;
    /**
     * flag,value
     */
    public final ComponentParser flagSet;
    public final ComponentParser memberChanged;
    public final ComponentParser transferSuccess;
    public final ComponentParser transferWarning;
    public final ComponentParser reloaded;

    /**
     * type,owner,uuid,members,flags
     */
    public final List<ComponentParser> info;

    /**
     * old,new,url
     */
    public final List<ComponentParser> newVersion;

    public final ActionBar actionbar;
    public final CleanupMessage cleanup;
    public final HelpMessage help;

    @PackagePrivate
    MessageConfig(String locale, FileConfiguration config) {
        this.locale = locale;

        doNotHavePermission = parse(config, "doNotHavePermission");
        missingArgument = parse(config, "missingArgument");
        invalidArgument = parse(config, "invalidArgument");
        playerNotFound = parse(config, "playerNotFound");

        notice = parse(config, "notice");
        denied = parse(config, "denied");
        protected_ = parse(config, "protected");
        removed = parse(config, "removed");
        ready = parse(config, "ready");

        notProtected = parse(config, "notProtected");
        notProtectable = parse(config, "notProtectable");
        alreadyProtected = parse(config, "alreadyProtected");

        persistEnabled = parse(config, "persistEnabled");
        persistDisabled = parse(config, "persistDisabled");
        flagSet = parse(config, "flagSet");
        memberChanged = parse(config, "memberChanged");
        transferSuccess = parse(config, "transferSuccess");
        transferWarning = parse(config, "transferWarning");
        reloaded = parse(config, "reloaded");

        info = config.getStringList("info")
            .stream()
            .map(MessageConfig::parse)
            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        newVersion = config.getStringList("newVersion")
            .stream()
            .map(MessageConfig::parse)
            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

        actionbar = new MessageConfig.ActionBar(config);
        cleanup = new MessageConfig.CleanupMessage(config.getConfigurationSection("cleanup"));
        help = new MessageConfig.HelpMessage(config.getConfigurationSection("help"));
    }

    public static class ActionBar {
        /**
         * block,name,uuid,type
         */
        public final ComponentParser notice;
        /**
         * block,type
         */
        public final ComponentParser denied;
        /**
         * block,type
         */
        public final ComponentParser protected_;
        /**
         * block,type
         */
        public final ComponentParser removed;

        private ActionBar(ConfigurationSection config) {
            notice = parse(config.getString("notice"));
            denied = parse(config.getString("denied"));
            protected_ = parse(config.getString("protected"));
            removed = parse(config.getString("removed"));
        }
    }

    public static class CleanupMessage {
        /**
         * speed
         */
        public final ComponentParser start;
        /**
         * checked,removed
         */
        public final ComponentParser progress;
        /**
         * world,x,y,z
         */
        public final ComponentParser removed;
        public final ComponentParser end;
        public final ComponentParser already;
        public final ComponentParser cancelled;

        private CleanupMessage(ConfigurationSection config) {
            start = parse(config, "start");
            progress = parse(config, "progress");
            removed = parse(config, "removed");
            end = parse(config, "end");
            already = parse(config, "already");
            cancelled = parse(config, "cancelled");
        }
    }

    public static class HelpMessage {
        public final ComponentParser private_;
        public final ComponentParser public_;
        public final ComponentParser flag;
        public final ComponentParser remove;
        public final ComponentParser info;
        public final ComponentParser member;
        public final ComponentParser transfer;
        public final ComponentParser persist;
        public final ComponentParser cleanup;
        public final ComponentParser reload;
        public final ComponentParser version;
        public final ComponentParser help;

        public final ComponentParser example;
        public final ComponentParser run;
        public final ComponentParser details;

        /**
         * flags
         */
        public final ComponentParser availableFlags;

        private HelpMessage(ConfigurationSection config) {
            private_ = parse(config.getString("private"));
            public_ = parse(config.getString("public"));
            flag = parse(config.getString("flag"));
            remove = parse(config.getString("remove"));
            info = parse(config.getString("info"));
            member = parse(config.getString("member"));
            transfer = parse(config.getString("transfer"));
            persist = parse(config.getString("persist"));
            cleanup = parse(config.getString("cleanup"));
            reload = parse(config.getString("reload"));
            version = parse(config.getString("version"));
            help = parse(config.getString("help"));

            example = parse(config.getString("example"));
            run = parse(config.getString("run"));
            details = parse(config.getString("details"));

            availableFlags = parse(config.getString("availableFlags"));
        }
    }

    private static ComponentParser parse(ConfigurationSection config, String key) {
        return parse(PREFIX + config.getString(key));
    }

    private static ComponentParser parse(String value) {
        return ComponentParser.parse(value);
    }
}
