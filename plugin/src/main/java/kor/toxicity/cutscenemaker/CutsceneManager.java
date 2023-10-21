package kor.toxicity.cutscenemaker;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.slikey.effectlib.EffectManager;
import kor.toxicity.cutscenemaker.entity.EntityManager;
import kor.toxicity.cutscenemaker.event.UserDataLoadEvent;
import kor.toxicity.cutscenemaker.shaded.com.mewin.WGRegionEvents.WGRegionEventsListener;
import kor.toxicity.cutscenemaker.skript.SkManager;
import kor.toxicity.cutscenemaker.util.DataContainer;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.LocationStudio;
import kor.toxicity.cutscenemaker.util.blockanims.BlockAnimation;
import kor.toxicity.cutscenemaker.util.databases.CutsceneDB;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import kor.toxicity.cutscenemaker.util.vars.VarsContainer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CutsceneManager {

    public CutsceneMaker getPlugin() {
        return plugin;
    }

    private final CutsceneMaker plugin;
    private final CutsceneUser user;
    @Getter
    private final EffectManager effectLib;
    @Getter
    private final ProtocolManager protocolLib;

    private final DataContainer<Location> locations = new DataContainer<>();

    public DataContainer<Location> getLocations() {
        return locations;
    }

    @Getter
    private final Map<String, LocationStudio> studioMap = new HashMap<>();

    public Map<String, BlockAnimation> getAnimationMap() {
        return animationMap;
    }

    private final Map<String, BlockAnimation> animationMap = new HashMap<>();

    private static final List<Player> delays = new ArrayList<>(1 << 8);
    private static Function<Player,Boolean> applyDelay;
    @Getter
    private final EntityManager entityManager;

    CutsceneManager(CutsceneMaker plugin) {
        this.plugin = plugin;

        this.user = new CutsceneUser();
        Bukkit.getOnlinePlayers().forEach(this.user::load);
        EvtUtil.register(plugin,user);

        applyDelay = p -> {
            if (!delays.contains(p)) {
                delays.add(p);
                runTaskLaterAsynchronously(() -> delays.remove(p), 4);
                return true;
            } else return false;
        };
        entityManager = new EntityManager(this);

        protocolLib = ProtocolLibrary.getProtocolManager();
        effectLib = new EffectManager(plugin);

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            EvtUtil.register(plugin,new WGRegionEventsListener(plugin));
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Skript")) {
            SkManager.registerAddon();
        }
        new Metrics(plugin,CutsceneMaker.BSTATS_ID);
    }
    public static boolean onDelay(Player player) {
        return applyDelay == null || applyDelay.apply(player);
    }

    public BukkitTask runTask(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin,task);
    }
    public BukkitTask runTaskAsynchronously(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin,task);
    }
    public BukkitTask runTaskTimer(Runnable task, long delay, long time) {
        return Bukkit.getScheduler().runTaskTimer(plugin,task,delay,time);
    }
    public BukkitTask runTaskLater(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin,task,delay);
    }
    public BukkitTask runTaskLaterAsynchronously(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,task,delay);
    }
    public BukkitTask runTaskTimerAsynchronously(Runnable task, long delay, long time) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,task,delay,time);
    }

    public void registerEvent(Listener listener) {
        EvtUtil.register(plugin,listener);
    }
    public ListenerManager register(Listener... listener) {
        return new ListenerManager(plugin,listener);
    }
    public void addLateCheck(Runnable runnable) {
        plugin.addLateCheck(runnable);
    }
    public VarsContainer getVars(Player player) {
        return user.container.get(player);
    }
    public Vars getVars(Player player, String name) {
        VarsContainer container;
        return ((container = getVars(player)) != null) ? container.get(name) : null;
    }
    public boolean isSet(Player player, String name) {
        VarsContainer container;
        return (container = getVars(player)) != null && container.contains(name);
    }

    public MetadataValue createMetaData(Object value) {
        return new FixedMetadataValue(plugin,value);
    }
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private class CutsceneUser implements Listener {

        private final Map<Player, VarsContainer> container = new HashMap<>();

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onJoin(PlayerJoinEvent e) {
            Player player = e.getPlayer();
            if (CutsceneConfig.getInstance().isChangeGameMode()) {
                GameMode mode = CutsceneConfig.getInstance().getDefaultGameMode();
                if (!player.isOp() && player.getGameMode() != mode) {
                    player.setGameMode(mode);
                    player.setHealth(0);
                }
            }
            load(player);
        }
        private void load(Player player) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
                VarsContainer container1 = CutsceneDB.load(player,plugin);
                container1.addTask(runTaskTimer(() -> {
                    if (container1.getTempStorage().size() > 0) {
                        String str = CutsceneConfig.getInstance().getTempStorageMessage().print(player);
                        if (!"".equals(str)) player.sendMessage(str);
                    }
                },60,600 * 20));
                container.put(player, container1);
                Bukkit.getScheduler().runTask(plugin,() -> EvtUtil.call(new UserDataLoadEvent(player)));
            });
        }
        @EventHandler(priority = EventPriority.MONITOR)
        public void onQuit(PlayerQuitEvent e) {
            VarsContainer c = container.get(e.getPlayer());
            if (c != null) {
                CutsceneDB.stop(e.getPlayer(),plugin,c);
                container.remove(e.getPlayer());
            }
        }
    }

    public void openTempStorage(Player player) {
        plugin.tempStorage(player);
    }
}