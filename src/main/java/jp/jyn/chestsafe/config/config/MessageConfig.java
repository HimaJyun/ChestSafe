package jp.jyn.chestsafe.config.config;

import jp.jyn.chestsafe.config.parser.MessageParser;
import jp.jyn.chestsafe.config.parser.Parser;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.stream.Collectors;

public class MessageConfig {
    private final static String PREFIX = "[ChestSafe] ";
    public final static String HEADER = "========== ChestSafe ==========";
    public final static String PLAYER_ONLY = PREFIX + ChatColor.RED + "This command can only be run by players.";

    public final Parser doNotHavePermission;
    public final Parser missingArgument;
    /**
     * value
     */
    public final Parser invalidArgument;
    /**
     * name
     */
    public final Parser playerNotFound;

    /**
     * block,name,uuid,type
     */
    public final Parser notice;
    /**
     * block,type
     */
    public final Parser denied;
    /**
     * block,type
     */
    public final Parser protected_;
    /**
     * block,type
     */
    public final Parser removed;
    public final Parser ready;

    /**
     * block
     */
    public final Parser notProtected;
    /**
     * block
     */
    public final Parser notProtectable;
    /**
     * block
     */
    public final Parser alreadyProtected;

    public final Parser persistEnabled;
    public final Parser persistDisabled;
    /**
     * flag,value
     */
    public final Parser flagSet;
    public final Parser memberChanged;
    public final Parser transferSuccess;
    public final Parser transferWarning;
    public final Parser reloaded;

    /**
     * type,owner,uuid,members,flags
     */
    public final Iterable<Parser> info;

    public final ActionBar actionbar;
    public final CleanupMessage cleanup;
    public final HelpMessage help;

    public MessageConfig(FileConfiguration config) {
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

        info = config.getStringList("info").stream().map(MessageConfig::parse).collect(Collectors.toList());

        memberChanged = parse(config, "memberChanged");

        transferSuccess = parse(config, "transferSuccess");
        transferWarning = parse(config, "transferWarning");

        reloaded = parse(config, "reloaded");

        actionbar = new ActionBar(config);
        cleanup = new CleanupMessage(config.getConfigurationSection("cleanup"));
        help = new HelpMessage(config.getConfigurationSection("help"));
    }

    public static class ActionBar {
        /**
         * block,name,uuid,type
         */
        public final Parser notice;
        /**
         * block,type
         */
        public final Parser denied;
        /**
         * block,type
         */
        public final Parser protected_;
        /**
         * block,type
         */
        public final Parser removed;

        public ActionBar(ConfigurationSection config) {
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
        public final Parser start;
        /**
         * checked,removed
         */
        public final Parser progress;
        /**
         * world,x,y,z
         */
        public final Parser removed;
        public final Parser end;
        public final Parser already;
        public final Parser cancelled;

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
        public final Parser private_;
        public final Parser public_;
        public final Parser flag;
        public final Parser remove;
        public final Parser info;
        public final Parser member;
        public final Parser transfer;
        public final Parser persist;
        public final Parser cleanup;
        public final Parser reload;
        public final Parser version;
        public final Parser help;
        public final Parser example;
        /**
         * flags
         */
        public final Parser availableFlags;

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
            availableFlags = parse(config.getString("availableFlags"));
        }
    }

    private static Parser parse(ConfigurationSection config, String key) {
        return parse(PREFIX + config.getString(key));
    }

    private static Parser parse(String value) {
        return MessageParser.parse(value);
    }
}
