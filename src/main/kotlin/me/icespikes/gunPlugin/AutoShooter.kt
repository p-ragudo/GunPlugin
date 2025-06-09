package me.icespikes.gunPlugin

import org.bukkit.Bukkit
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
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.*

class AutoShooter(private val plugin: JavaPlugin) : Listener {
    private var shootTask: BukkitTask ?= null

    private val shootingPlayers = mutableMapOf<UUID, Long>()
    private val focusedPlayers = mutableSetOf<UUID>()

    private val baseDamage = 1.0
    private val damageKey = NamespacedKey(plugin, "bullet_damage")

    private var currentTick = 0L

    private fun startTask() {
        if(shootTask != null) return

        shootTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if(shootingPlayers.isEmpty()) {
                currentTick = 0L
                disableTask()
                return@Runnable
            }

            currentTick++

            for((playerId, nextShootTick) in shootingPlayers.toList().iterator()) {
                val player = Bukkit.getPlayer(playerId) ?: continue

                val gun = detectGun(player)
                if(gun == null) {
                    shootingPlayers.remove(playerId)
                    focusedPlayers.remove(playerId)
                    removePotionEffects(player)
                    continue
                }

                if(currentTick < nextShootTick) continue

                val direction = player.eyeLocation.direction.normalize()
                val spawnOffset = direction.clone().multiply(0.8).add(Vector(0.0, -0.2,0.0))
                val spawnLocation = player.eyeLocation.clone().add(spawnOffset)

                val snowBall = player.world.spawn(spawnLocation, Snowball::class.java)

                snowBall.apply{
                    velocity = player.eyeLocation.direction.multiply(gun.velocity)
                    val speed = velocity.length()
                    val calculatedDamage = gun.baseDamage + (speed * gun.damageMultiplier)
                    persistentDataContainer.set(damageKey, PersistentDataType.DOUBLE, calculatedDamage)
                }

                if(gun.name == "Sniper") {
                    shootingPlayers.remove(playerId)
                    continue
                }

                addPotionEffects(player)
                shootingPlayers[playerId] = currentTick + gun.shootDelayTicks
            }
        }, 0L, 1L)
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
        if(event.hand != EquipmentSlot.HAND) return

        val gun = detectGun(event.player) ?: run {
            Bukkit.getLogger().info("detectGun() returned null")
            return
        }
        val item = event.player.inventory.itemInMainHand
        if(item.itemMeta!!.displayName != gun.name) {
            Bukkit.getLogger().info("Gun name not equals to gun.name")
            return
        }
        if(item.itemMeta!!.lore != gun.lore) {
            Bukkit.getLogger().info("Gun lore not equals to gun.lore")
            return
        }

        if(event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) return

        if(event.player.uniqueId in shootingPlayers) {
            shootingPlayers.remove(event.player.uniqueId)
            return
        } else {
            shootingPlayers[event.player.uniqueId] = currentTick + gun.shootDelayTicks
            focusedPlayers.add(event.player.uniqueId)
            startTask()
        }
    }

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if(event.hand != EquipmentSlot.HAND) return

        val gun = detectGun(event.player) ?: run {
            Bukkit.getLogger().info("detectGun() returned null")
            return
        }
        val item = event.player.inventory.itemInMainHand
        if(item.itemMeta!!.displayName != gun.name) {
            Bukkit.getLogger().info("Gun name not equals to gun.name")
            return
        }
        if(item.itemMeta!!.lore != gun.lore) {
            Bukkit.getLogger().info("Gun lore not equals to gun.lore")
            return
        }

        if(event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        if(event.player.uniqueId in shootingPlayers) {
            removePotionEffects(event.player)
            shootingPlayers.remove(event.player.uniqueId)
            focusedPlayers.remove(event.player.uniqueId)
            return
        }
        else if(event.player.uniqueId in focusedPlayers) {
            focusedPlayers.remove(event.player.uniqueId)
            removePotionEffects(event.player)
            return
        } else {
            focusedPlayers.add(event.player.uniqueId)
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