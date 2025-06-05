package me.icespikes.gunPlugin

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class LaunchArrow(plugin: JavaPlugin) : Listener {

    private val baseDamage = 1.0
    private val damageMultiplier = 100
    private val damageKey = NamespacedKey(plugin, "bullet_damage")
    private val bulletId = NamespacedKey(plugin, "bullet_id")

    @EventHandler
    fun onPlayerShoot(event: PlayerInteractEvent) {
        val player = event.player

        if (player.inventory.itemInMainHand.type != Material.STICK) return
        if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) return

        val bullet = player.launchProjectile(Snowball::class.java).apply {
            velocity = player.eyeLocation.direction.multiply(4.5)

            val speed = velocity.length()
            val calculatedDamage = baseDamage + (speed * damageMultiplier)
            persistentDataContainer.set(damageKey, PersistentDataType.DOUBLE, calculatedDamage)
        }

        player.world.playSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f)
    }

    @EventHandler
    fun onSnowballHit(event: ProjectileHitEvent) {
        if(event.entity !is Snowball) return
        val bullet = event.entity as Snowball
        val hitEntity = event.hitEntity as? LivingEntity ?: return

        val damage = bullet.persistentDataContainer.get(damageKey, PersistentDataType.DOUBLE) ?: baseDamage
        hitEntity.damage(damage)

        bullet.world.spawnParticle(Particle.SMOKE, bullet.location, 3)
        bullet.remove()
    }
}