package me.icespikes.gunPlugin

import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class GiveGunCommand : CommandExecutor{
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) return true
        val player = sender.player

        if(args.size != 1) {
            player!!.sendMessage("${ChatColor.RED}[GunPlugin] Usage: /give_gun <gun>")
            return true
        }

        val gunArg = args.first()
        val gun = gunRegistry[gunArg] ?: run {
            player!!.sendMessage("${ChatColor.RED}[GunPlugin] Unable to process the request")
            return true
        }

        val gunItem = giveGunItem(gunArg, gun) ?: run {
            player!!.sendMessage("${ChatColor.RED}[GunPlugin] Invalid or unavailable gun type")
            return true
        }

        player!!.inventory.addItem(gunItem)
        player.sendMessage("${ChatColor.GREEN}[GunPlugin] Successfully added ${gun.name}")

        return true
    }

    private fun giveGunItem(gunArg: String, gun: Gun) : ItemStack? {
        if(gunArg == "Pistol") {
            val pistol = createGunItem(gun, Material.STICK)
            return pistol
        } else if(gunArg == "SMG") {
            val smg = createGunItem(gun, Material.IRON_HOE)
            return smg
        } else if(gunArg == "Sniper") {
            val sniper = createGunItem(gun, Material.NETHERITE_SWORD)
            return sniper
        } else {
            return null
        }
    }

    private fun createGunItem(gun: Gun, itemType: Material) : ItemStack {
        val gunItem = ItemStack(itemType)
        val itemMeta = gunItem.itemMeta

        itemMeta!!.setDisplayName(gun.name)
        itemMeta.lore = gun.lore

        gunItem.setItemMeta(itemMeta)
        return gunItem
    }
}