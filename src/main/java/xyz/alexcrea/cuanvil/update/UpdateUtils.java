package xyz.alexcrea.cuanvil.update;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jetbrains.annotations.Nullable;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NotNullByDefault
public class UpdateUtils {
    public static final String MINECRAFT_VERSION_PATH = "lowMinecraftVersion";

    private UpdateUtils() {}

    public static Version currentMinecraftVersion() {
        Server server = Bukkit.getServer();
        String minecraftVersion = null;
        try {
            // Paper/Leaf expose the game version separately from the build identifier.
            // Reflection retains the upstream Spigot API minimum.
            minecraftVersion = (String) Server.class.getMethod("getMinecraftVersion").invoke(server);
        } catch (NoSuchMethodException ignored) {
            // Older Spigot exposes only the Bukkit version and the server banner.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read the native Minecraft version", e);
        }
        return resolveMinecraftVersion(minecraftVersion, server.getBukkitVersion(), server.getVersion());
    }

    private static final Pattern NUMERIC_VERSION =
            Pattern.compile("^(\\d+)(?:\\.\\d+){0,2}(?:[-+].*)?$");
    private static final Pattern BANNER_VERSION =
            Pattern.compile("\\(MC: ([0-9]+(?:\\.[0-9]+){0,2}(?:[-+][^)]*)?)\\)");

    static Version resolveMinecraftVersion(@Nullable String nativeVersion,
                                           String bukkitVersion, String serverVersion) {
        if (nativeVersion != null && NUMERIC_VERSION.matcher(nativeVersion).matches()) {
            return Version.fromString(nativeVersion.split("[-+]")[0]);
        }
        if (NUMERIC_VERSION.matcher(bukkitVersion).matches()) {
            return Version.fromString(bukkitVersion.split("[-+]")[0]);
        }
        var banner = BANNER_VERSION.matcher(serverVersion);
        if (banner.find()) {
            return Version.fromString(banner.group(1).split("[-+]")[0]);
        }
        throw new IllegalStateException("Cannot determine Minecraft version from server metadata");
    }

    @SuppressWarnings("unused")
    public static void addToStringList(FileConfiguration config, String path, String... toAdd) {
        List<String> groups = new ArrayList<>(config.getStringList(path));
        groups.addAll(Arrays.asList(toAdd));
        config.set(path, groups);

    }

    public static void addAbsentToList(FileConfiguration config, String path, String... toAdd) {
        List<String> groups = new ArrayList<>(config.getStringList(path));
        for (String val : toAdd) {
            if (groups.contains(val)) continue;

            groups.add(val);
        }
        config.set(path, groups);

    }

    public static boolean removeFromList(FileConfiguration config, String path, String toRemove) {
        List<String> groups = new ArrayList<>(config.getStringList(path));
        if (!groups.remove(toRemove)) return false;

        config.set(path, groups);
        return true;
    }

    public static void mergeSections(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            if(target.contains(key)) continue;
            target.set(key, source.get(key));
        }
    }


}
