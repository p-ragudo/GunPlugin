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
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.*
import kotlin.random.Random

class GunShootListener(private val plugin: JavaPlugin) : Listener {
    private var shootTask: BukkitTask ?= null

    private val cooldownTracker = mutableMapOf<UUID, Long>()
    private val focusedPlayers = mutableSetOf<UUID>()
    private val shootingPlayersAutomatic = mutableSetOf<UUID>()
    private val playerGunMap = mutableMapOf<UUID, Gun>()

    private val baseDamage = 1.0
    private val damageKey = NamespacedKey(plugin, "bullet_damage")

    private var currentTick = 0L

    init {
        startTask()
    }

    private fun startTask() {
        if(shootTask != null) return

        shootTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            currentTick++

            for((playerId, nextShootTick) in cooldownTracker.toList().iterator()) {
                val player = Bukkit.getPlayer(playerId) ?: continue

                val gun = detectGun(player)
                val storedGun = playerGunMap[playerId]

                if (gun != storedGun || gun == null) {
                    cooldownTracker.remove(playerId)
                    shootingPlayersAutomatic.remove(playerId)
                    playerGunMap.remove(playerId)
                    focusedPlayers.remove(playerId)
                    removePotionEffects(player)
                    continue
                }

                if(!gun.isAutomatic && !gun.needsManualFireDelay) continue
                if(currentTick < nextShootTick) continue
                if(gun.isAutomatic) {
                    shoot(player, gun)
                    cooldownTracker[playerId] = currentTick + gun.shootDelayTicks
                } else if(gun.needsManualFireDelay) {
                    cooldownTracker.remove(playerId) // Cooldown expired
                }
            }
        }, 0L, 1L)
    }

    private fun shoot(player: Player, gun: Gun) {
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

        if(!gun.isAutomatic) {
            val loc = player.location.clone()

            val vertRecoil = if(gun.verticalRecoil == null) loc.pitch
                            else Random.nextDouble(gun.verticalRecoil.first, gun.verticalRecoil.second)
            val horizRecoil = if(gun.horizontalRecoil == null) loc.yaw
                            else Random.nextDouble(gun.horizontalRecoil.first, gun.horizontalRecoil.second)

            loc.pitch += vertRecoil.toFloat()
            loc.yaw += horizRecoil.toFloat()

            player.teleport(loc)
        }
    }

    @EventHandler
    fun onSniperShoot(event: PlayerDropItemEvent) {
        val player = event.player
        val droppedItem = detectGun(event.itemDrop.itemStack)

        if(droppedItem == null) return
        if(droppedItem == gunRegistry["Sniper"]) shoot(player, droppedItem)

        event.isCancelled = true
        focusedPlayers.remove(player.uniqueId)
        removePotionEffects(player)
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
        val player = event.player

        if(event.hand != EquipmentSlot.HAND) return
        val gun = detectGun(event.player) ?: return
        val itemMeta = event.player.inventory.itemInMainHand.itemMeta!!
        if(itemMeta.displayName != gun.name) return
        if(itemMeta.lore != gun.lore) return
        if(event.action != Action.LEFT_CLICK_AIR && event.action != Action.LEFT_CLICK_BLOCK) return

        val playerId = player.uniqueId
        val nextShootTick = cooldownTracker[playerId]

        when {
            // Case 1: Semi-auto without manual delay — shoot instantly
            !gun.isAutomatic && !gun.needsManualFireDelay -> {
                shoot(player, gun)
                playerGunMap[playerId] = gun
            }

            // Case 2: Semi-auto with manual delay — shoot only if cooldown expired
            !gun.isAutomatic && gun.needsManualFireDelay -> {
                if (nextShootTick == null || currentTick >= nextShootTick) {
                    shoot(player, gun)
                    cooldownTracker[playerId] = currentTick + gun.shootDelayTicks
                    playerGunMap[playerId] = gun
                }
            }

            // Case 3: Automatic — start firing
            gun.isAutomatic && !shootingPlayersAutomatic.contains(playerId) -> {
                shootingPlayersAutomatic.add(playerId)
                playerGunMap[playerId] = gun
                cooldownTracker[playerId] = currentTick
            }

            // Case 4: Automatic and already shooting — stop firing
            gun.isAutomatic && shootingPlayersAutomatic.contains(playerId) -> {
                shootingPlayersAutomatic.remove(playerId)
                playerGunMap.remove(playerId)
                cooldownTracker.remove(playerId)
            }
        }
    }

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if(event.hand != EquipmentSlot.HAND) return
        val gun = detectGun(event.player) ?: return
        if(gun == gunRegistry["Sniper"]) return // no potion effects for sniper when using scope

        val itemMeta = event.player.inventory.itemInMainHand.itemMeta!!
        if(itemMeta.displayName != gun.name) return
        if(itemMeta.lore != gun.lore) return

        if(event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        if(event.player.uniqueId in focusedPlayers) {
            removePotionEffects(event.player)
            focusedPlayers.remove(event.player.uniqueId)
        } else { // Turn on focus
            focusedPlayers.add(event.player.uniqueId)
            addPotionEffects(event.player)
        }
    }

    @EventHandler
    fun onItemSwitchFocusControl(event: PlayerItemHeldEvent){
        val player = event.player
        val playerId = player.uniqueId

        if(!focusedPlayers.contains(playerId)) return
        if(!playerGunMap.contains(playerId)) return

        val oldSlot = player.inventory.getItem(event.previousSlot)
        val gunOnHand = detectGun(oldSlot)

        val newItem = player.inventory.getItem(event.newSlot)
        val newItemOnHand = detectGun(newItem)

        if(newItemOnHand == null || newItemOnHand != gunOnHand) {
            focusedPlayers.remove(playerId)
            removePotionEffects(player)
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity.player!!

        shootingPlayersAutomatic.remove(player.uniqueId)
        focusedPlayers.remove(player.uniqueId)
        playerGunMap.remove(player.uniqueId)
        removePotionEffects(player)
    }

    @EventHandler
    fun onPlayerOpenInventory(event: InventoryOpenEvent) {
        if(event.player !is Player) return
        val player = event.player

        shootingPlayersAutomatic.remove(player.uniqueId)
        focusedPlayers.remove(player.uniqueId)
        playerGunMap.remove(player.uniqueId)
        removePotionEffects(player as Player)
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