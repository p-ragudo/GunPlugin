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

    val fireSound: Sound,
    val fireVolume: Float,
    val firePitch: Float,

    val hitSound: Sound,
    val hitVolume: Float,
    val hitPitch: Float,

    val shootDelayTicks: Long,
    val baseDamage: Double,
    val damageMultiplier: Double,
    val velocity: Double, // Standard is 4.5
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

        Sound.ENTITY_FIREWORK_ROCKET_BLAST,
        1.0f,
        1.5f,

        Sound.ENTITY_ARROW_HIT,
        0.8f,
        1.4f,

        5L,
        2.0,
        1.0,
        4.5,
        false,
        true),
    "SMG" to Gun(
        "SMG",
        smgLore,
        Snowball::class.java,

        Sound.BLOCK_NOTE_BLOCK_SNARE,
        1.2f,
        1.2f,

        Sound.ENTITY_GENERIC_HURT,
        1.0f,
        1.0f,

        2L,
        1.0,
        1.0,
        3.0,
        true,
        false),
    "Sniper" to Gun(
        "Sniper",
        sniperLore,
        Snowball::class.java,

        Sound.ENTITY_GENERIC_EXPLODE,
        2.0f,
        1.5f,

        Sound.BLOCK_ANVIL_HIT,
        1.2f,
        1.8f,

        0L,
        50.0,
        20.0,
        50.0,
        false,
        false),
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