package it.pureorigins.purespawn

import it.pureorigins.common.file
import it.pureorigins.common.json
import it.pureorigins.common.readFileAs
import it.pureorigins.common.registerEvents
import kotlinx.serialization.Serializable
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace.DOWN
import org.bukkit.block.BlockFace.UP
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.math.roundToInt


class PureSpawn : JavaPlugin(), Listener {

    private lateinit var spawnBuffer: SpawnBuffer

    override fun onEnable() {
        val config = json.readFileAs(file("purespawn.json"), Config())
        server.consoleSender.sendMessage("PureSpawn successfully loaded!")
        spawnBuffer =
            SpawnBuffer(server.worlds[0], config.range, config.centerX, config.centerZ, config.spawnBufferSize)
        registerEvents(this)
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        if (!p.hasPlayedBefore()) {
            val pos = spawnBuffer.pop()
            val l = Location(p.world, pos.x + 0.5, pos.y, pos.z + 0.5)
            p.teleport(l)
            p.respawnLocation = l
            spawnBuffer.addSpawnPoint()
        }
    }

    @Serializable
    data class Config(
        val range: Int = 25000,
        val centerX: Int = 0,
        val centerZ: Int = 0,
        val spawnBufferSize: Int = 10
    )

    class SpawnBuffer(
        private val overworld: World,
        private val range: Int,
        private val centerX: Int,
        private val centerZ: Int,
        size: Int
    ) : Stack<Location>() {
        init {
            for (i in 0..size) this.push(findSpawnPoint())
        }

        override fun pop(): Location = if (empty())
            findSpawnPoint()
        else super.pop()

        fun addSpawnPoint() {
            push(findSpawnPoint())
        }

        private fun findSpawnPoint(): Location {
            var pos: Location
            do {
                pos =
                    overworld.getHighestBlockAt(randomPos(overworld, range, centerX, centerZ)).getRelative(UP).location
            } while (!isSafe(pos))
            return pos
        }

        private fun randomPos(world: World, r: Int, centerX: Int, centerZ: Int): Location {
            var x: Double
            var z: Double
            do {
                x = (2 * Math.random() - 1)
                z = (2 * Math.random() - 1)
            } while (x * x + z * z > 1)
            return Location(
                world, (x * r + centerX).roundToInt().toDouble(), 64.0, (z * r + centerZ).roundToInt().toDouble()
            )
        }

        private fun isSafe(location: Location): Boolean {
            val feet: Block = location.block
            val head: Block = feet.getRelative(UP)
            // not transparent (will suffocate)
            if (feet.type.isOccluding || head.type.isOccluding) return false
            val ground: Block = feet.getRelative(DOWN)
            return ground.type.isSolid
        }
    }
}
