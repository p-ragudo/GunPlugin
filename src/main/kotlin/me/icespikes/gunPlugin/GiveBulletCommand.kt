package me.icespikes.gunPlugin

import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class GiveBulletCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) return true
        val player = sender.player

        if(args.size < 2 || args.size > 3) {
            player!!.sendMessage("${ChatColor.RED}[GunPlugin] Usage: /gp give bullet <quantity>")
            return true
        } else if(args[0] != "give" || args[1] != "bullet") {
            player!!.sendMessage("${ChatColor.RED}[GunPlugin] Usage: /gp give bullet <quantity>")
            return true
        }

        val quantity = args[2].toInt()

        val bullet = ItemStack(Material.SNOWBALL, quantity)
        val bulletMeta = bullet.itemMeta
        bulletMeta!!.setDisplayName("${ChatColor.RED}Bullet")
        bulletMeta.lore = mutableListOf("${ChatColor.DARK_PURPLE}Generic all-around ammo")
        bullet.setItemMeta(bulletMeta)

        player!!.inventory.addItem(bullet)
        player.sendMessage("${ChatColor.GOLD}[GunPlugin]${ChatColor.GREEN} Added ${bullet.amount} bullets to ${player.displayName}")

        return true
    }

}