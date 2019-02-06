package jp.jyn.chestsafe.config.config;

import jp.jyn.jbukkitlib.config.parser.template.StringParser;
import jp.jyn.jbukkitlib.config.parser.template.TemplateParser;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.stream.Collectors;

public class MessageConfig {
    private final static String PREFIX = "[ChestSafe] ";
    public final static String HEADER = "========== ChestSafe ==========";
    public final static String PLAYER_ONLY = PREFIX + ChatColor.RED + "This command can only be run by players.";

    public final TemplateParser doNotHavePermission;
    public final TemplateParser missingArgument;
    /**
     * value
     */
    public final TemplateParser invalidArgument;
    /**
     * name
     */
    public final TemplateParser playerNotFound;

    /**
     * block,name,uuid,type
     */
    public final TemplateParser notice;
    /**
     * block,type
     */
    public final TemplateParser denied;
    /**
     * block,type
     */
    public final TemplateParser protected_;
    /**
     * block,type
     */
    public final TemplateParser removed;
    public final TemplateParser ready;

    /**
     * block
     */
    public final TemplateParser notProtected;
    /**
     * block
     */
    public final TemplateParser notProtectable;
    /**
     * block
     */
    public final TemplateParser alreadyProtected;

    public final TemplateParser persistEnabled;
    public final TemplateParser persistDisabled;
    /**
     * flag,value
     */
    public final TemplateParser flagSet;
    public final TemplateParser memberChanged;
    public final TemplateParser transferSuccess;
    public final TemplateParser transferWarning;
    public final TemplateParser reloaded;

    /**
     * type,owner,uuid,members,flags
     */
    public final Iterable<TemplateParser> info;

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
        public final TemplateParser notice;
        /**
         * block,type
         */
        public final TemplateParser denied;
        /**
         * block,type
         */
        public final TemplateParser protected_;
        /**
         * block,type
         */
        public final TemplateParser removed;

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
        public final TemplateParser start;
        /**
         * checked,removed
         */
        public final TemplateParser progress;
        /**
         * world,x,y,z
         */
        public final TemplateParser removed;
        public final TemplateParser end;
        public final TemplateParser already;
        public final TemplateParser cancelled;

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
        public final TemplateParser private_;
        public final TemplateParser public_;
        public final TemplateParser flag;
        public final TemplateParser remove;
        public final TemplateParser info;
        public final TemplateParser member;
        public final TemplateParser transfer;
        public final TemplateParser persist;
        public final TemplateParser cleanup;
        public final TemplateParser reload;
        public final TemplateParser version;
        public final TemplateParser help;
        public final TemplateParser example;
        /**
         * flags
         */
        public final TemplateParser availableFlags;

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

    private static TemplateParser parse(ConfigurationSection config, String key) {
        return parse(PREFIX + config.getString(key));
    }

    private static TemplateParser parse(String value) {
        return StringParser.parse(value);
    }
}
