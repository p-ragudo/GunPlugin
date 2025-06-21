package me.icespikes.gunPlugin

data class GunDelay(
    var nextShootTick: Long,
    var reloadTick: Long
)