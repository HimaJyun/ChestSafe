package jp.jyn.chestsafe.util;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.MainConfig;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.protection.ProtectionRepository.CheckElement;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentVariable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ProtectionCleaner implements Runnable {
    private final static int DEFAULT_CHECK_SIZE = 100;
    private final static AtomicReference<ProtectionCleaner> RUNNING = new AtomicReference<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Set<Material> protectable = EnumSet.noneOf(Material.class);
    private final ComponentVariable variable = ComponentVariable.init();

    private final Plugin plugin;
    private final MessageConfig.CleanupMessage message;
    private final ProtectionRepository repository;
    private final long limit;
    private final boolean unloaded;
    private final Set<CommandSender> senders = new CopyOnWriteArraySet<>();

    private final AtomicInteger count = new AtomicInteger();
    private volatile BigDecimal average = BigDecimal.ZERO;

    // もしかするとvolatile intで十分かもしれない……けど、大した負荷じゃないし念のため
    private final AtomicInteger offset = new AtomicInteger();
    private final AtomicInteger checked = new AtomicInteger();
    private final AtomicInteger removed = new AtomicInteger();
    private final AtomicInteger checkPerCycle = new AtomicInteger(DEFAULT_CHECK_SIZE);
    private final Queue<CheckElement> queue = new ArrayDeque<>(DEFAULT_CHECK_SIZE);

    public ProtectionCleaner(Plugin plugin, MainConfig config, BukkitLocale<MessageConfig> message, ProtectionRepository repository,
                             long limit, TimeUnit unit, boolean unloaded, CommandSender... senders) {
        this.plugin = plugin;
        this.message = message.get().cleanup;
        this.repository = repository;
        this.unloaded = unloaded;
        this.senders.addAll(Arrays.asList(senders));
        this.protectable.addAll(config.protectable.keySet());

        if (unit.toSeconds(limit) >= 1) {
            throw new IllegalArgumentException("limit over 1000ms");
        }
        this.limit = unit.toNanos(limit);

        variable.put("limit", String.valueOf(limit));
        // こうすればいちいちputし直さなくても値が変化すると勝手に変わる
        variable.put("checked", c -> c.setText(checked.toString()));
        variable.put("removed", c -> c.setText(removed.toString()));
        variable.put("speed", c -> c.setText(checkPerCycle.toString()));
        variable.put("average", c -> c.setText(average.setScale(1, RoundingMode.HALF_UP).toPlainString()));
        // デバッグ用隠し変数
        variable.put("__offset", c -> c.setText(offset.toString()));
        variable.put("__count", c -> c.setText(count.toString()));
        variable.put("__size", c -> c.setText(String.valueOf(queue.size())));

        if (!RUNNING.compareAndSet(null, this)) {
            throw new IllegalStateException("Already running.");
        }
        this.message.start.apply(variable).send(this.senders);
        executor.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        if (!isRunning() || finished()) {
            return; // 既に終わってたら何もしない
        }

        if (offset.get() != -1 && // オフセットが-1ではない == 終端まで行ってなくて
            queue.size() < checkPerCycle.get()) { // キュー残数がサイクルあたり処理数より少ない == 処理速度が間に合ってる時だけ問い合わせ
            offset.set(repository.checkExists(queue, offset.get(), checkPerCycle.get()));
        }

        int size = queue.size();

        Future<Long> f = Bukkit.getScheduler().callSyncMethod(plugin, this::check);
        long time;
        try {
            time = f.get();
        } catch (InterruptedException | ExecutionException e) {
            ChestSafe.getInstance().getLogger().severe("Unknown error!");
            e.printStackTrace();
            cancel();
            return;
        }

        int check = size - queue.size();
        int ced = this.checked.addAndGet(check);
        // 平均値
        int co = count.incrementAndGet();
        BigDecimal avg = average = BigDecimal.valueOf(ced).divide(BigDecimal.valueOf(co), MathContext.DECIMAL64);
        // 途中経過
        message.progress.apply(variable).send(senders);

        // チェック数の自動調整 (許容時間/(処理時間/処理数=1個辺りの所要時間)=許容時間内に処理できる数)
        BigDecimal timePerCheck = BigDecimal.valueOf(time).divide(BigDecimal.valueOf(check), MathContext.DECIMAL64);
        BigDecimal newSpeed = BigDecimal.valueOf(this.limit/*キャッシュできる*/).divide(timePerCheck, MathContext.DECIMAL64);
        if (newSpeed.compareTo(BigDecimal.ZERO) == 0 ||// 新しい速度が0 (確認処理がが遅すぎる) か
            newSpeed.compareTo(avg.scaleByPowerOfTen(1)) >= 0) { // 平均値の10倍を超えている (異常値) なら
            newSpeed = avg; // 次の周期には平均値でやる
        }
        checkPerCycle.set(newSpeed.intValue());

        if (finished()) {
            cancel();
        }
    }

    private long check() {
        long start = System.nanoTime();
        int remove = 0;

        // 変数には先にputして値だけすり替える
        AtomicReference<String> w = new AtomicReference<>();
        AtomicInteger x = new AtomicInteger();
        AtomicInteger y = new AtomicInteger();
        AtomicInteger z = new AtomicInteger();
        variable.put("world", w::get).put("x", x::toString).put("y", y::toString).put("z", z::toString);

        while (!queue.isEmpty()) {
            CheckElement e = queue.remove();

            String name = e.world.get();
            World world = Bukkit.getWorld(name);
            if (world == null
                ? unloaded // ロードされていない世界ならunloadedの値をそのまま = trueなら削除
                : !protectable.contains(world.getBlockAt(e.x, e.y, e.z).getType())) { // ロードされているのなら保護可能タイプか確認
                // 削除
                e.remove();
                remove++;
                w.set(name);
                x.set(e.x);
                y.set(e.y);
                z.set(e.z);
                message.removed.apply(variable).send(senders);
            }

            long elapsed = System.nanoTime() - start;
            if (elapsed >= limit) {
                removed.getAndAdd(remove);
                return elapsed;
            }
        }

        removed.getAndAdd(remove);
        return System.nanoTime() - start;
    }

    private boolean finished() {
        return offset.get() == -1 && queue.isEmpty();
    }

    public static boolean isRunning() {
        return RUNNING.get() != null;
    }

    public static boolean cancel() {
        ProtectionCleaner old = RUNNING.getAndUpdate(ignore -> null);
        if (old == null) {
            return false;
        }

        old.executor.shutdown();

        if (old.finished()) {
            old.message.end.apply(old.variable).send(old.senders);
        } else {
            old.message.cancelled.apply(old.variable).send(old.senders);
        }
        return true;
    }

    public static boolean addSender(CommandSender sender) {
        ProtectionCleaner old = RUNNING.get();
        if (old != null) {
            return old.senders.add(sender);
        }
        return false;
    }
}
