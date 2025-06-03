package me.icespikes.gunPlugin
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class GunPlugin : JavaPlugin() {

    override fun onEnable() {
        Bukkit.getLogger().info("[GunPlugin] Successfully loaded!")

        Bukkit.getPluginManager().registerEvents(LaunchArrow(this), this)
    }
}
