package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.quests.QuestUtil;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class CutsceneConfig {
    @Getter
    private static final CutsceneConfig instance = new CutsceneConfig();
    @Getter
    private GameMode defaultGameMode;
    @Getter
    private boolean changeGameMode, debug, enableSkip;

    @Getter
    private FunctionPrinter tempStorageMessage, tempStorageName, timeLimit;
    @Getter
    private int autoSaveTime;
    @Getter
    private int autoBackupTime;
    @Getter
    private Material dialogReader;
    @Getter
    private Consumer<Player> defaultTypingSound, questCompleteSound;
    @Getter
    private int defaultDialogRows;
    @Getter
    private int defaultDialogCenter;
    @Getter
    private int defaultTypingDelay;
    @Getter
    private ItemStack defaultQuestIcon;
    @Getter
    private boolean enableTagging;

    @Getter
    private String openAPIKey;

    void load(CutsceneMaker pl) {
        try {
            if (!new File(pl.getDataFolder().getAbsolutePath() + "\\config.yml").exists()) pl.saveResource("config.yml",false);
            ConfigLoad load = new ConfigLoad(pl,"config.yml","");

            String mode = load.getString("default-game-mode","SURVIVAL");
            safeSet(() -> defaultGameMode = GameMode.valueOf(mode.toUpperCase()),() -> {
                CutsceneMaker.warn("unable to find the game mode named \"" + mode + "\"");
                defaultGameMode = GameMode.SURVIVAL;
            });

            dialogReader = getMaterial(load.getString("default-dialog-reader","BOOK"));

            tempStorageMessage = new FunctionPrinter(load.getString("temp-storage-message",""));
            tempStorageName = new FunctionPrinter(load.getString("temp-storage-name",""));
            timeLimit = new FunctionPrinter(load.getString("quest-time-limit",""));

            changeGameMode = load.getBoolean("change-game-mode",true);
            debug = load.getBoolean("debug",false);
            autoSaveTime = load.getInt("auto-save-time",300);
            autoBackupTime = load.getInt("auto-backup-time", 200);

            defaultTypingSound = QuestUtil.getSoundPlay(load.getString("default-typing-sound","block.stone_button.click_on 0.2 0.7"));
            questCompleteSound = QuestUtil.getSoundPlay(load.getString("quest-complete-sound","ui.toast.challenge_complete 1 1"));

            defaultTypingDelay = getValue(load.getInt("default-typing-delay",2),1,4);
            defaultDialogRows = getValue(load.getInt("default-dialog-rows",5),1,6);
            defaultDialogCenter = getValue(load.getInt("default-dialog-center", 22),0,defaultDialogRows*9-1);

            defaultQuestIcon = getItemStack(load.getString("default-quest-icon", "BOOK"),(short) load.getInt("default-quest-durability",0));

            enableTagging = load.getBoolean("enable-tagging",false);
            enableSkip = load.getBoolean("enable-skip",true);

            openAPIKey = load.getString("open-api-key",null);

        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    private int getValue(int target, int min, int max){
        return Math.max(Math.min(target,max),min);
    }
    private ItemStack getItemStack(String material, short durability) {
        ItemStack item = new ItemStack(getMaterial(material));
        item.setDurability(durability);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
    private Material getMaterial(String material) {
        try {
            return Material.valueOf(material.toUpperCase());
        } catch (Exception e) {
            CutsceneMaker.warn("unable to find the material named \"" + material + "\"");
            return Material.BOOK;
        }
    }
    private void safeSet(Runnable tries, Runnable ifFailed) {
        try {
            tries.run();
        } catch (Exception e) {
            ifFailed.run();
        }
    }
}
