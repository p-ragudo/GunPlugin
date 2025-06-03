package me.icespikes.gunPlugin
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class GunPlugin : JavaPlugin() {

    override fun onEnable() {
        Bukkit.getLogger().info("")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
