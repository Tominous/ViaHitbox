package eu.kennytv.viahitbox;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ViaHitboxPlugin extends JavaPlugin implements Listener {
    private Method getHandle;
    private Method setSize;
    private boolean fix1_9;
    private boolean fix1_14;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        if (ProtocolRegistry.SERVER_PROTOCOL >= 441) {
            getLogger().warning("Are you shure you read the plugin description correctly?");
            getServer().getPluginManager().disablePlugin(this);
        }

        if (config.getBoolean("change-1_9-hitbox")) {
            if (ProtocolRegistry.SERVER_PROTOCOL >= 107) {
                getLogger().warning("Disabling 1.9 hitbox fix, as the server doesn't need it.");
            } else {
                getLogger().info("Enabled 1.9-1.13 hitbox fix.");
                fix1_9 = true;
            }
        }
        if (config.getBoolean("change-1_14-hitbox")) {
            getLogger().info("Enabled 1.14+ hitbox fix. Be aware that this means players running 1.14+ can sneak and thus go under hights of 1.5 blocks!");
            fix1_14 = true;
        }

        if (!fix1_9 && !fix1_14) {
            getLogger().warning("No fix has been enabled, disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            getHandle = Class.forName(getServer().getClass().getPackage().getName() + ".entity.CraftPlayer").getMethod("getHandle");
            setSize = Class.forName(getServer().getClass().getPackage().getName()
                    .replace("org.bukkit.craftbukkit", "net.minecraft.server") + ".EntityPlayer").getMethod("setSize", Float.TYPE, Float.TYPE);
        } catch (final ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void playerToggleSneak(final PlayerToggleSneakEvent event) {
        setHitbox(event.getPlayer(), event.isSneaking());
    }

    private void setHitbox(final Player player, final boolean sneaking) {
        final int protocolVersion = Via.getAPI().getPlayerVersion(player.getUniqueId());
        if (fix1_14 && protocolVersion >= 441) {
            setHight(player, sneaking ? 1.5F : 1.8F);
        } else if (fix1_9 && protocolVersion >= 107) {
            setHight(player, sneaking ? 1.6F : 1.8F);
        }
    }

    private void setHight(final Player player, final float hight) {
        try {
            setSize.invoke(getHandle.invoke(player), 0.6F, hight);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
