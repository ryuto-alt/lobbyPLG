package lobby.lobby.data;

import lobby.lobby.Lobby;
import lobby.lobby.commands.BwEggCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages Bedwars NPC data persistence
 * Saves NPC locations to file and restores them on server restart
 */
public class GameNpcDataManager {

    private final Lobby plugin;
    private File npcFile;
    private FileConfiguration npcConfig;

    public GameNpcDataManager(Lobby plugin) {
        this.plugin = plugin;
        setupNpcFile();
    }

    private void setupNpcFile() {
        npcFile = new File(plugin.getDataFolder(), "npcs.yml");

        if (!npcFile.exists()) {
            npcFile.getParentFile().mkdirs();
            try {
                npcFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create npcs.yml file!");
                e.printStackTrace();
            }
        }

        npcConfig = YamlConfiguration.loadConfiguration(npcFile);
    }

    /**
     * Add a new NPC location to the data file
     */
    public void addNpc(Location location) {
        List<NpcData> npcs = loadNpcData();
        npcs.add(new NpcData(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw()
        ));
        saveNpcData(npcs);
    }

    /**
     * Remove an NPC location from the data file
     */
    public void removeNpc(Location location) {
        List<NpcData> npcs = loadNpcData();
        npcs.removeIf(npc ->
                npc.world.equals(location.getWorld().getName()) &&
                        Math.abs(npc.x - location.getX()) < 1.0 &&
                        Math.abs(npc.y - location.getY()) < 1.0 &&
                        Math.abs(npc.z - location.getZ()) < 1.0
        );
        saveNpcData(npcs);
    }

    /**
     * Spawn all saved NPCs
     */
    public void spawnAllNpcs() {
        List<NpcData> npcs = loadNpcData();
        int spawned = 0;

        for (NpcData npc : npcs) {
            World world = Bukkit.getWorld(npc.world);
            if (world == null) {
                plugin.getLogger().warning("Could not find world: " + npc.world + " for NPC spawn");
                continue;
            }

            Location location = new Location(world, npc.x, npc.y, npc.z, npc.yaw, 0);
            spawnNpcAt(location);
            spawned++;
        }

        plugin.getLogger().info("Spawned " + spawned + " Bedwars NPC(s)");
    }

    /**
     * Spawn a single NPC at the specified location
     */
    private void spawnNpcAt(Location location) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);

        villager.setCustomName(BwEggCommand.GAME_NPC_NAME);
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setProfession(Villager.Profession.NITWIT);
        villager.setVillagerLevel(5);
        villager.addScoreboardTag(BwEggCommand.GAME_NPC_IDENTIFIER);

        // Set rotation
        villager.setRotation(location.getYaw(), 0);
    }

    private List<NpcData> loadNpcData() {
        List<NpcData> npcs = new ArrayList<>();

        if (!npcConfig.contains("npcs")) {
            return npcs;
        }

        List<?> npcList = npcConfig.getList("npcs");
        if (npcList == null) {
            return npcs;
        }

        for (int i = 0; i < npcList.size(); i++) {
            ConfigurationSection section = npcConfig.getConfigurationSection("npcs." + i);
            if (section == null) continue;

            String world = section.getString("world", "");
            double x = section.getDouble("x", 0);
            double y = section.getDouble("y", 0);
            double z = section.getDouble("z", 0);
            float yaw = (float) section.getDouble("yaw", 0);

            if (!world.isEmpty()) {
                npcs.add(new NpcData(world, x, y, z, yaw));
            }
        }

        return npcs;
    }

    private void saveNpcData(List<NpcData> npcs) {
        npcConfig.set("npcs", null);

        for (int i = 0; i < npcs.size(); i++) {
            NpcData npc = npcs.get(i);
            String path = "npcs." + i;
            npcConfig.set(path + ".world", npc.world);
            npcConfig.set(path + ".x", npc.x);
            npcConfig.set(path + ".y", npc.y);
            npcConfig.set(path + ".z", npc.z);
            npcConfig.set(path + ".yaw", npc.yaw);
        }

        try {
            npcConfig.save(npcFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save npcs.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Internal class to hold NPC data
     */
    private static class NpcData {
        final String world;
        final double x;
        final double y;
        final double z;
        final float yaw;

        NpcData(String world, double x, double y, double z, float yaw) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }
}
