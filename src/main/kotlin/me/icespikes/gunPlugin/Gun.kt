package me.icespikes.gunPlugin

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class Gun (
    val name: String,
    val lore: List<String>,
    val shootDelayTicks: Long,
    val baseDamage: Double,
    val damageMultiplier: Double,
    val velocity: Double, // Standard is 4.5
    val isAutomatic: Boolean,
    val needsManualFireDelay: Boolean,
    val verticalRecoil: Pair<Double, Double>?,
    val horizontalRecoil: Pair<Double, Double>?,
)

val pistolLore = listOf("Standard issue firearm", "Deals normal damage")
val smgLore = listOf("Rapid fire", "Low damage")
val sniperLore = listOf("Long-range precision", "High damage")

val gunRegistry = mapOf(
    "Pistol" to Gun("Pistol", pistolLore, 5L, 2.0,
                1.0, 4.5, false,
                true, Pair(-5.0, -10.0), Pair(-2.0, 2.0)),
    "SMG" to Gun("SMG", smgLore, 2L, 1.0,
                1.0, 4.5, true,
                false, null, null),
    "Sniper" to Gun("Sniper", sniperLore, 0L, 50.0,
                20.0, 50.0, false,
                false, Pair(-20.0, -35.0), null)
)

fun detectGun(player: Player) : Gun? {
    val item = player.inventory.itemInMainHand

    val itemMeta = item.itemMeta ?: return null

    if(gunRegistry[itemMeta.displayName] == null) return null
    if(gunRegistry[itemMeta.displayName]?.lore != itemMeta.lore) return null

    return gunRegistry[itemMeta.displayName]
}

fun detectGun(item: ItemStack?): Gun? {
    if (item == null || !item.hasItemMeta()) return null
    val meta = item.itemMeta!!
    val gun = gunRegistry[meta.displayName] ?: return null
    if (gun.lore != meta.lore) return null
    return gun
}