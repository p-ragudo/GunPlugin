package me.icespikes.gunPlugin

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
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
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.*

class GunShootListener(private val plugin: JavaPlugin) : Listener {
    private var shootTask: BukkitTask ?= null

    private val cooldownTracker = mutableMapOf<UUID, Long>()
    private val shootingPlayersAutomatic = mutableSetOf<UUID>()
    private val playerGunMap = mutableMapOf<UUID, Gun>()

    private val baseDamage = 1.0
    private val damageKey = NamespacedKey(plugin, "bullet_damage")

    private var currentTick = 0L

    init { startTask() }

    private fun startTask() {
        if(shootTask != null) return

        shootTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            currentTick++

            for((playerId, nextShootTick) in cooldownTracker.toList().iterator()) {
                val player = Bukkit.getPlayer(playerId) ?: continue
                if(!hasBullets(player)) {
                    shootingPlayersAutomatic.remove(playerId)
                    continue
                }

                val gun = detectGun(player)
                val storedGun = playerGunMap[playerId]

                if (gun != storedGun || gun == null) {
                    cooldownTracker.remove(playerId)
                    shootingPlayersAutomatic.remove(playerId)
                    playerGunMap.remove(playerId)
                    continue
                }

                if(!gun.isAutomatic && !gun.needsManualFireDelay) continue
                if(currentTick < nextShootTick) continue
                if(gun.isAutomatic) {
                    shoot(player, gun, true)
                    cooldownTracker[playerId] = currentTick + gun.shootDelayTicks
                } else if(gun.needsManualFireDelay) {
                    cooldownTracker.remove(playerId) // Cooldown expired
                }
            }
        }, 0L, 1L)
    }

    private fun shoot(player: Player, gun: Gun, subtractBullet: Boolean) {
        val direction = player.eyeLocation.direction.normalize()
        val spawnOffset = direction.clone().multiply(0.8).add(Vector(0.0, -0.2,0.0))
        val spawnLocation = player.eyeLocation.clone().add(spawnOffset)

        val bullet = player.world.spawn(spawnLocation, gun.bulletEntity)

        bullet.apply{
            shooter = player
            velocity = player.eyeLocation.direction.multiply(gun.velocity)
            val speed = velocity.length()
            val calculatedDamage = gun.baseDamage + (speed * gun.damageMultiplier)
            persistentDataContainer.set(damageKey, PersistentDataType.DOUBLE, calculatedDamage)
            setGravity(false)
        }

        player.world.playSound(
            player.location,
            gun.fireSound,
            gun.fireVolume,
            gun.firePitch
        )

        val bulletInventory = getBulletFirstInstance(player)
        if(bulletInventory == null) {
            shootingPlayersAutomatic.remove(player.uniqueId)
        } else {
            if(subtractBullet) bulletInventory.amount -= 1
        }

        sendBulletCountActionBarMessage(player)
    }

    @EventHandler
    fun onSniperShoot(event: PlayerDropItemEvent) {
        val player = event.player

        if(!hasBullets(player)) {
            sendBulletCountActionBarMessage(player)
            return
        }

        val droppedItem = event.itemDrop.itemStack
        val sniper = detectGun(droppedItem) ?: return
        if(sniper != gunRegistry["Sniper"]) return

        if(sniper.rounds == 0) {
            return
        }

        event.isCancelled = true
        event.itemDrop.remove()
        shoot(player, sniper, false)
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
        if(!hasBullets(player)) {
            sendBulletCountActionBarMessage(player)
            return
        }
        if(gun.rounds <= 0) {
            // reload
            return
        }

        val playerId = player.uniqueId
        val nextShootTick = cooldownTracker[playerId]

        when {
            // Case 1: Semi-auto without manual delay — shoot instantly
            !gun.isAutomatic && !gun.needsManualFireDelay -> {
                shoot(player, gun, true)
                playerGunMap[playerId] = gun
            }

            // Case 2: Semi-auto with manual delay — shoot only if cooldown expired
            !gun.isAutomatic && gun.needsManualFireDelay -> {
                if (nextShootTick == null || currentTick >= nextShootTick) {
                    shoot(player, gun, true)
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

    private fun getBulletFirstInstance(player: Player): ItemStack? {
        if(!player.inventory.contains(Material.SNOWBALL)) return null

        var snowball: ItemStack? = null
        for(item in player.inventory) {
            if(item != null && item.type == Material.SNOWBALL) {
                snowball = item
                break
            }
            else continue
        }

        val snowballMeta = snowball!!.itemMeta
        if(snowballMeta!!.displayName != "${ChatColor.RED}Bullet") return null
        val snowballLore = mutableListOf("${net.md_5.bungee.api.ChatColor.DARK_PURPLE}Generic all-around ammo")
        if(snowballMeta.lore != snowballLore) return null

        return snowball
    }

    private fun hasBullets(player: Player): Boolean {
        val bullet = getBulletFirstInstance(player)
        return bullet != null
    }

    private fun getBulletCount(player: Player): Int {
        if(!hasBullets(player)) return 0

        var bulletCount = 0;
        for(item in player.inventory) {
            if(item != null && item.type == Material.SNOWBALL) {
                bulletCount += item.amount
            }
        }

        return bulletCount
    }

    @EventHandler
    fun onHoldGunOnHand(event: PlayerItemHeldEvent) {
        val item = event.player.inventory.getItem(event.newSlot) ?: return
        detectGun(item) ?: return

        sendBulletCountActionBarMessage(event.player)
    }

    private fun sendBulletCountActionBarMessage(player: Player) {
        val bulletCountMessage: String?
        val bulletCount = getBulletCount(player)

        if(bulletCount > 20) {
            bulletCountMessage = "${ChatColor.AQUA}Bullets left: ${ChatColor.GREEN}${bulletCount}"
        } else if (bulletCount <= 20 && bulletCount != 0) {
            bulletCountMessage = "${ChatColor.AQUA}Bullets left: ${ChatColor.RED}${bulletCount}"
        } else {
            bulletCountMessage = "${ChatColor.RED}No more ammo!"
        }

        val message = "${ChatColor.GOLD}<< $bulletCountMessage ${ChatColor.GOLD}>>"
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message))
    }

    @EventHandler
    fun onSnowballHit(event: ProjectileHitEvent) {
        if(event.entity !is Snowball) return
        val bullet = event.entity as Snowball

        val hitEntity = event.hitEntity as? LivingEntity?
        if(hitEntity != null) {
            val damage = bullet.persistentDataContainer.get(damageKey, PersistentDataType.DOUBLE) ?: baseDamage

            val player = bullet.shooter as? Player?
            if(player != null) {
                val gun = detectGun(player)
                val knockbackMultiplier = bullet.velocity.clone().normalize().multiply(gun!!.knockbackFactor)
                hitEntity.velocity = hitEntity.velocity.add(knockbackMultiplier)
            } else {
                val knockbackMultiplier = bullet.velocity.clone().normalize().multiply(0.2)
                hitEntity.velocity = hitEntity.velocity.add(knockbackMultiplier)
            }

            hitEntity.damage(damage)
            hitEntity.world.playSound(
                hitEntity.location,
                Sound.ENTITY_GENERIC_HURT,
                1.0f,
                1.0f
            )
        }

        val hitBlock = event.hitBlock
        if(hitBlock != null) {
            val hitBlockType = hitBlock.type
            if(hitBlockType.name.lowercase().contains("glass")) {
                hitBlock.breakNaturally()
                hitBlock.world.playSound(
                    hitBlock.location,
                    Sound.BLOCK_GLASS_BREAK,
                    1f,
                    1f
                )
            }
        }

        bullet.world.spawnParticle(Particle.SMOKE, bullet.location, 3)
        bullet.remove()
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity.player!!

        shootingPlayersAutomatic.remove(player.uniqueId)
        playerGunMap.remove(player.uniqueId)
    }

    @EventHandler
    fun onPlayerOpenInventory(event: InventoryOpenEvent) {
        if(event.player !is Player) return
        val player = event.player

        shootingPlayersAutomatic.remove(player.uniqueId)
        playerGunMap.remove(player.uniqueId)
    }
}