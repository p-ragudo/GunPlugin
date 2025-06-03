package me.icespikes.gunPlugin

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class LaunchArrow(plugin: JavaPlugin) : Listener {

    private val baseDamage = 1.0
    private val damageMultiplier = 0.1
    private val damageKey = NamespacedKey(plugin, "arrow_damage")

    @EventHandler
    fun onPlayerShoot(event: PlayerInteractEvent) {
        val player = event.player

        if (player.inventory.itemInMainHand.type != Material.STICK) return
        if (event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) return

        val arrow = player.launchProjectile(Arrow::class.java).apply {
            velocity = player.eyeLocation.direction.multiply(10)

            val speed = velocity.length()
            val calculatedDamage = baseDamage + (speed * damageMultiplier)
            persistentDataContainer.set(damageKey, PersistentDataType.DOUBLE, calculatedDamage)

            world.spawnParticle(Particle.SMOKE, location, 5)
        }

        player.world.playSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f)
    }

    @EventHandler
    fun onArrowHit(event: ProjectileHitEvent) {
        if (event.entity !is Arrow) return
        val arrow = event.entity as Arrow
        val hitEntity = event.hitEntity as? LivingEntity ?: return

        val damage = arrow.persistentDataContainer.get(damageKey, PersistentDataType.DOUBLE) ?: baseDamage
        hitEntity.damage(damage)

        arrow.world.spawnParticle(Particle.SMOKE, arrow.location, 3)
        arrow.remove()
    }
}