package me.icespikes.gunPlugin

import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Snowball
import org.bukkit.inventory.ItemStack

data class Gun (
    val name: String,
    val lore: List<String>,
    val bulletEntity: Class<out Projectile>,
    val magSize: Int,
    var currentRounds: Int,

    val fireSound: Sound,
    val fireVolume: Float,
    val firePitch: Float,

    val shootDelayTicks: Long,
    val reloadTicks: Long,
    val baseDamage: Float,
    val damageMultiplier: Float,
    val velocity: Float, // Standard is 4.5
    val knockbackFactor: Float, // Pistol / Standard is 0.6 (but need to lessen for automatic guns)
    val isAutomatic: Boolean,
    val needsManualFireDelay: Boolean,
)

val pistolLore = listOf("Standard issue firearm", "Deals normal damage")
val smgLore = listOf("Rapid fire", "Low damage")
val sniperLore = listOf("Long-range precision", "High damage")

val gunRegistry = mapOf(
    "Pistol" to Gun(
        "Pistol",
        pistolLore,
        Snowball::class.java,
        17,
        17,

        Sound.ENTITY_FIREWORK_ROCKET_BLAST,
        1.0f,
        1.5f,

        5L,
        40L,
        2.0f,
        1.0f,
        4.5f,
        0.6f,
        false,
        true),
    "SMG" to Gun(
        "SMG",
        smgLore,
        Snowball::class.java,
        50,
        50,

        Sound.ENTITY_FIREWORK_ROCKET_SHOOT,
        1.2f,
        1.2f,

        2L,
        45L,
        1.0f,
        1.0f,
        3.0f,
        0.13f,
        true,
        false),
    "Sniper" to Gun(
        "Sniper",
        sniperLore,
        Snowball::class.java,
        4,
        4,

        Sound.ENTITY_GENERIC_EXPLODE,
        2.0f,
        1.5f,

        30L,
        80L,
        50.0f,
        20.0f,
        50.0f,
        5f,
        false,
        true),
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