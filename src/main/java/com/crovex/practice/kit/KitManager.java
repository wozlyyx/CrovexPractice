package com.crovex.practice.kit;

import com.crovex.practice.CrovexPractice;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class KitManager {

    private final CrovexPractice plugin;
    private final Map<String, Kit> kits = new HashMap<>();
    private File configFile;
    private YamlConfiguration config;

    public KitManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public void loadKits() {
        kits.clear();
        configFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!configFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);
                createDefaultKits();
                plugin.getLogger().info(kits.size() + " adet varsayilan kit olusturuldu.");
                return;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "kits.yml dosyasi olusturulamadi!", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section != null) {
                Kit kit = new Kit(key);
                kit.setDisplayName(section.getString("displayName", key));
                kit.setIcon(section.getItemStack("icon", new ItemStack(Material.DIAMOND_SWORD)));
                kit.setType(KitType.valueOf(section.getString("type", "NORMAL")));
                kit.setAllowBlockPlace(section.getBoolean("allowBlockPlace", false));
                kit.setAllowBlockBreak(section.getBoolean("allowBlockBreak", false));
                kit.setAllowExplosions(section.getBoolean("allowExplosions", false));

                // Load inventory
                if (section.contains("inventory")) {
                    List<?> list = section.getList("inventory");
                    kit.setInventoryContents(list.toArray(new ItemStack[0]));
                }
                // Load armor
                if (section.contains("armor")) {
                    List<?> list = section.getList("armor");
                    kit.setArmorContents(list.toArray(new ItemStack[0]));
                }
                // Load effects
                if (section.contains("effects")) {
                    List<PotionEffect> effects = new ArrayList<>();
                    for (Map<?, ?> effectMap : section.getMapList("effects")) {
                        effects.add(new PotionEffect((Map<String, Object>) effectMap));
                    }
                    kit.setActiveEffects(effects);
                }

                kits.put(key.toLowerCase(), kit);
            }
        }
        plugin.getLogger().info(kits.size() + " adet kit yuklendi.");
    }

    public void saveKits() {
        if (config == null || configFile == null) return;

        // Clear config
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        for (Kit kit : kits.values()) {
            String path = kit.getName();
            config.set(path + ".displayName", kit.getDisplayName());
            config.set(path + ".icon", kit.getIcon());
            config.set(path + ".type", kit.getType().name());
            config.set(path + ".allowBlockPlace", kit.isAllowBlockPlace());
            config.set(path + ".allowBlockBreak", kit.isAllowBlockBreak());
            config.set(path + ".allowExplosions", kit.isAllowExplosions());
            config.set(path + ".inventory", Arrays.asList(kit.getInventoryContents()));
            config.set(path + ".armor", Arrays.asList(kit.getArmorContents()));
            
            List<Map<String, Object>> serializedEffects = new ArrayList<>();
            for (PotionEffect effect : kit.getActiveEffects()) {
                serializedEffects.add(effect.serialize());
            }
            config.set(path + ".effects", serializedEffects);
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "kits.yml dosyasi kaydedilemedi!", e);
        }
    }

    public void createKit(String name, KitType type) {
        Kit kit = new Kit(name);
        kit.setType(type);
        kits.put(name.toLowerCase(), kit);
        saveKits();
    }

    public void deleteKit(String name) {
        kits.remove(name.toLowerCase());
        saveKits();
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getKits() {
        return kits.values();
    }

    private void createDefaultKits() {
        // We will create 4 default kits: NoDebuff, Sumo, Boxing, BuildUHC
        
        // 1. Sumo
        Kit sumo = new Kit("Sumo");
        sumo.setDisplayName("Sumo");
        sumo.setType(KitType.SUMO);
        sumo.setIcon(new ItemStack(Material.LEAD));
        kits.put("sumo", sumo);

        // 2. Boxing
        Kit boxing = new Kit("Boxing");
        boxing.setDisplayName("Boxing");
        boxing.setType(KitType.BOXING);
        boxing.setIcon(new ItemStack(Material.RAW_GOLD));
        ItemStack bSword = new ItemStack(Material.DIAMOND_SWORD);
        enchant(bSword, "sharpness", 1);
        ItemStack[] bInv = new ItemStack[36];
        bInv[0] = bSword;
        boxing.setInventoryContents(bInv);
        List<PotionEffect> bEffects = new ArrayList<>();
        bEffects.add(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
        boxing.setActiveEffects(bEffects);
        kits.put("boxing", boxing);

        // 3. NoDebuff
        Kit noDebuff = new Kit("NoDebuff");
        noDebuff.setDisplayName("NoDebuff");
        noDebuff.setType(KitType.NORMAL);
        noDebuff.setIcon(new ItemStack(Material.SPLASH_POTION));
        
        ItemStack ndSword = new ItemStack(Material.DIAMOND_SWORD);
        enchant(ndSword, "sharpness", 1);
        enchant(ndSword, "unbreaking", 3);
        
        ItemStack ndPearl = new ItemStack(Material.ENDER_PEARL, 16);
        ItemStack ndSpeedPot = new ItemStack(Material.POTION); // speed II drink pot
        
        ItemStack healingPot = new ItemStack(Material.SPLASH_POTION);
        ItemStack[] ndInv = new ItemStack[36];
        ndInv[0] = ndSword;
        ndInv[1] = ndPearl;
        ndInv[2] = new ItemStack(Material.GOLDEN_CARROT, 64);
        for (int i = 3; i < 36; i++) {
            ndInv[i] = healingPot;
        }
        noDebuff.setInventoryContents(ndInv);

        ItemStack[] ndArmor = new ItemStack[4];
        ndArmor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), "protection", 2);
        ndArmor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), "protection", 2);
        ndArmor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), "protection", 2);
        ndArmor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), "protection", 2);
        noDebuff.setArmorContents(ndArmor);
        
        List<PotionEffect> ndEffects = new ArrayList<>();
        ndEffects.add(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
        noDebuff.setActiveEffects(ndEffects);
        kits.put("nodebuff", noDebuff);

        // 4. BuildUHC
        Kit buildUhc = new Kit("BuildUHC");
        buildUhc.setDisplayName("BuildUHC");
        buildUhc.setType(KitType.BUILDUHC);
        buildUhc.setIcon(new ItemStack(Material.GOLDEN_APPLE));

        ItemStack uhcSword = enchant(new ItemStack(Material.DIAMOND_SWORD), "sharpness", 2);
        ItemStack uhcBow = enchant(new ItemStack(Material.BOW), "power", 2);
        ItemStack uhcRod = new ItemStack(Material.FISHING_ROD);
        ItemStack uhcPlanks = new ItemStack(Material.OAK_PLANKS, 64);
        ItemStack uhcCobble = new ItemStack(Material.COBBLESTONE, 64);
        ItemStack gApple = new ItemStack(Material.GOLDEN_APPLE, 6);
        ItemStack gHead = new ItemStack(Material.GOLDEN_APPLE, 3);
        ItemMeta headMeta = gHead.getItemMeta();
        headMeta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#ffaa00:#ffff55>Golden Head</gradient>"));
        gHead.setItemMeta(headMeta);
        
        ItemStack[] uhcInv = new ItemStack[36];
        uhcInv[0] = uhcSword;
        uhcInv[1] = uhcRod;
        uhcInv[2] = uhcBow;
        uhcInv[3] = gApple;
        uhcInv[4] = gHead;
        uhcInv[5] = uhcPlanks;
        uhcInv[6] = uhcCobble;
        uhcInv[7] = new ItemStack(Material.LAVA_BUCKET);
        uhcInv[8] = new ItemStack(Material.WATER_BUCKET);
        uhcInv[9] = new ItemStack(Material.DIAMOND_PICKAXE);
        uhcInv[10] = new ItemStack(Material.DIAMOND_AXE);
        uhcInv[35] = new ItemStack(Material.ARROW, 32);
        buildUhc.setInventoryContents(uhcInv);

        ItemStack[] uhcArmor = new ItemStack[4];
        uhcArmor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), "protection", 2);
        uhcArmor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), "protection", 2);
        uhcArmor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), "protection", 2);
        uhcArmor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), "protection", 2);
        buildUhc.setArmorContents(uhcArmor);
        kits.put("builduhc", buildUhc);

        // Save immediately
        config = YamlConfiguration.loadConfiguration(configFile);
        saveKits();
    }

    private ItemStack enchant(ItemStack item, String enchantKey, int lvl) {
        Enchantment enchant = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantKey));
        if (enchant != null) {
            item.addUnsafeEnchantment(enchant, lvl);
        }
        return item;
    }
}

