package me.icespikes.gunPlugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

class AutoShooter(private val plugin: JavaPlugin) : Listener {
    private var shootTask: BukkitTask ?= null

    private val shootingPlayers = mutableSetOf<UUID>()
    private val shootSpeed = 5L

    private val baseDamage = 1.0
    private val damageMultiplier = 100
    private val damageKey = NamespacedKey(plugin, "bullet_damage")

    private fun startTask() {
        if(shootTask == null) {
            shootTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                for(playerId in shootingPlayers.toList()) {
                    val player = Bukkit.getPlayer(playerId) ?: continue

                    if(player.inventory.itemInMainHand.type != Material.STICK) {
                        shootingPlayers.remove(playerId)
                        continue
                    }

                    player.launchProjectile(Snowball::class.java).apply {
                        velocity = player.eyeLocation.direction.multiply(4.5)

                        val speed = velocity.length()
                        val calculatedDamage = baseDamage + (speed * damageMultiplier)
                        persistentDataContainer.set(damageKey, PersistentDataType.DOUBLE, calculatedDamage)
                    }
                }
            }, 0L, shootSpeed)
        }
    }

    private fun disableTask() {
        shootTask?.cancel()
        shootTask = null
    }

    @EventHandler
    fun onGunClick(event: PlayerInteractEvent) {
        if(event.player.inventory.itemInMainHand.type != Material.STICK) {
            shootingPlayers.remove(event.player.uniqueId)
            disableTask()
            return
        }
        if(event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) {
            shootingPlayers.remove(event.player.uniqueId)
            disableTask()
            return
        }

        shootingPlayers.add(event.player.uniqueId)
        startTask()
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