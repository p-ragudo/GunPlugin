package me.icespikes.gunPlugin

import org.bukkit.entity.Player

data class Gun (
    val name: String,
    val lore: List<String>,
    val shootDelayTicks: Long,
    val baseDamage: Double,
    val damageMultiplier: Double,
    val velocity: Double // Standard is 4.5
)

val pistolLore = listOf("Standard issue firearm", "Deals normal damage")
val smgLore = listOf("Rapid fire", "Low damage")
val sniperLore = listOf("Long-range precision", "High damage")

val gunRegistry = mapOf(
    "Pistol" to Gun("Pistol", pistolLore, 10L, 2.0, 1.0, 4.5),
    "SMG" to Gun("SMG", smgLore, 2L, 1.0, 1.0, 4.5),
    "Sniper" to Gun("Sniper", sniperLore, 20L, 8.0, 4.0, 12.0)
)

fun detectGun(player: Player) : Gun? {
    val item = player.inventory.itemInMainHand

    val itemMeta = item.itemMeta ?: return null

    if(gunRegistry[itemMeta.displayName] == null) return null
    if(gunRegistry[itemMeta.displayName]?.lore != itemMeta.lore) return null

    return gunRegistry[itemMeta.displayName]
}