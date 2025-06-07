package me.icespikes.gunPlugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.*

class AutoShooter(private val plugin: JavaPlugin) : Listener {
    private var shootTask: BukkitTask ?= null

    private val shootingPlayers = mutableSetOf<UUID>()
    private val shootSpeed = 3L

    private val baseDamage = 1.0
    private val damageMultiplier = 1
    private val damageKey = NamespacedKey(plugin, "bullet_damage")

    private var isShooting = false
    private var isFocused = false

    private fun startTask() {
        if(shootTask != null) return

        Bukkit.getLogger().info("Shoot Task is not null. Starting task")
        shootTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            for(playerId in shootingPlayers.toList()) {
                val player = Bukkit.getPlayer(playerId) ?: continue

                if(player.inventory.itemInMainHand.type != Material.STICK) {
                    shootingPlayers.remove(playerId)
                    continue
                }

                val direction = player.eyeLocation.direction.normalize()
                val spawnOffset = direction.clone().multiply(0.8).add(Vector(0.0, -0.2,0.0))
                val spawnLocation = player.eyeLocation.clone().add(spawnOffset)

                val snowBall = player.world.spawn(spawnLocation, Snowball::class.java)

                snowBall.apply{
                    velocity = player.eyeLocation.direction.multiply(4.5)
                    val speed = velocity.length()
                    val calculatedDamage = baseDamage + (speed * damageMultiplier)
                    persistentDataContainer.set(damageKey, PersistentDataType.DOUBLE, calculatedDamage)
                }

                addPotionEffects(player)
            }
        }, 0L, shootSpeed)
    }

    private fun disableTask() {
        shootTask?.cancel()
        shootTask = null
    }

    private fun addPotionEffects(player: Player) {
        if(!player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, Int.MAX_VALUE, 2, false, false))
        }
        if(!player.hasPotionEffect(PotionEffectType.SPEED)) {
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 1, false, false))
        }
    }

    private fun removePotionEffects(player: Player) {
        if(player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            player.removePotionEffect(PotionEffectType.SLOWNESS)
        }
        if(player.hasPotionEffect(PotionEffectType.SPEED)) {
            player.removePotionEffect(PotionEffectType.SPEED)
        }
    }

    @EventHandler
    fun onLeftClick(event: PlayerInteractEvent) {
        if(event.player.inventory.itemInMainHand.type != Material.STICK) {
            removePotionEffects(event.player)
            shootingPlayers.remove(event.player.uniqueId)
            disableTask()
            return
        }
        if(event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) {
            removePotionEffects(event.player)
            shootingPlayers.remove(event.player.uniqueId)
            disableTask()
            return
        }

        shootingPlayers.add(event.player.uniqueId)
        println("${event.player.displayName} added to shooting players! with uniqueID ${event.player.uniqueId}")
        startTask()

//        if(isShooting) {
//            Bukkit.getLogger().info("Left Click. Turn off shooting")
//            isShooting = false
//            shootingPlayers.remove(event.player.uniqueId)
//            disableTask()
//        } else {
//            Bukkit.getLogger().info("Left Click. Turn on shooting")
//            isShooting = true
//            isFocused = true
//            shootingPlayers.add(event.player.uniqueId)
//            println("${event.player.displayName} added to shooting players! with uniqueID ${event.player.uniqueId}")
//            startTask()
//        }
    }

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if(isShooting) {
            isShooting = false
            isFocused = false
            removePotionEffects(event.player)
            shootingPlayers.remove(event.player.uniqueId)
            disableTask()
        }
        else if(isFocused) {
            isFocused = false
        } else {
            isFocused = true
            addPotionEffects(event.player)
        }
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