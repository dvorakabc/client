package me.zeroeightsix.kami.module.modules.player

import me.zeroeightsix.kami.NecronClient
import me.zeroeightsix.kami.event.events.SafeTickEvent
import me.zeroeightsix.kami.manager.managers.PlayerPacketManager
import me.zeroeightsix.kami.mixin.client.entity.MixinEntity
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.module.modules.combat.Surround
import me.zeroeightsix.kami.setting.Settings
import me.zeroeightsix.kami.util.BlockUtils
import me.zeroeightsix.kami.util.EntityUtils
import me.zeroeightsix.kami.util.InventoryUtils
import org.kamiblue.event.listener.listener
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.BlockFalling
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.math.BlockPos
import net.minecraftforge.client.event.InputUpdateEvent
import me.zeroeightsix.kami.util.InventoryUtils.swapSlot
import me.zeroeightsix.kami.util.text.MessageSendHelper
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3i
import net.minecraft.util.math.Vec3d
import kotlin.math.absoluteValue
import kotlin.math.round

/**
 * @author TopiasL
 */
@Module.Info(
    name = "HighwayBuilder",
    category = Module.Category.PLAYER,
    description = "Builds obsidian highways."
)
object HighwayBuilder : Module() {
    val safeWalk = register(Settings.b("SafeWalk", true))
    private val width = register(Settings.integerBuilder("Width").withValue(4).withRange(1, 6).withStep(1))
    private val doSides = register(Settings.booleanBuilder("DoSides").withValue(true))
    private val swapMode = register(Settings.e<SwapMode>("HotbarSwapMode", SwapMode.SPOOF))
    private val ticks = register(Settings.integerBuilder("Ticks").withValue(5).withRange(0, 30).withStep(1))

    private var holding = false
    private var oldSlot = 0
    private var direction = EnumFacing.NORTH
    private var startPos = Vec3d(0.0, 0.0, 0.0)
    private var forward = Vec3i(0.0, 0.0, -1.0)
    private var axis = Vec3d(0.0, 0.0, 1.0)

    private enum class SwapMode {
        NO, SWAP, SPOOF
    }

    override fun onEnable() {
        super.onEnable()
        if (mc.player == null) { disable(); return }
        val dir = mc.player.lookVec
        direction = EnumFacing.getFacingFromVector(dir.x.toFloat(), dir.y.toFloat(), dir.z.toFloat())
        startPos = EntityUtils.getInterpolatedPos(mc.player, ticks.value.toFloat())
        if (direction == EnumFacing.UP) { disable(); MessageSendHelper.sendChatMessage("$chatName Do not look up when enabling!"); return }
        if (direction == EnumFacing.DOWN) { disable(); MessageSendHelper.sendChatMessage("$chatName Do not look down when enabling!"); return }
        forward = direction.directionVec
        axis = Vec3d(forward.x.absoluteValue.toDouble(), 0.0, forward.z.absoluteValue.toDouble())
    }

    init {
        listener<InputUpdateEvent> {
        }

        listener<SafeTickEvent> {
            if (!isEnabled) return@listener
            if (direction == EnumFacing.UP) { disable(); return@listener }
            if (direction == EnumFacing.DOWN) { disable(); return@listener }
            var vec3d = EntityUtils.getInterpolatedPos(mc.player, ticks.value.toFloat())
            vec3d = Vec3d(vec3d.x * axis.x, startPos.y, vec3d.z * axis.z)
            if (axis.x == 0.0) vec3d = Vec3d(startPos.x, vec3d.y, vec3d.z)
            else if (axis.z == 0.0) vec3d = Vec3d(vec3d.x, vec3d.y, startPos.z)

            val blockPos = BlockPos(vec3d).down()
            val blockPos2 = CorrectPos(vec3d).down()

            //if (!mc.world.getBlockState(blockPos).material.isReplaceable) {
            //    return@listener
            //}

            if (swapMode.value == SwapMode.SWAP || swapMode.value == SwapMode.SPOOF ) {
                if (!spoofHotbar()) return@listener
            }
            val halfWidth = width.value / 2
            for (i in -halfWidth..halfWidth) {
                if (width.value % 2 == 0 && i != halfWidth) {
                    /* check if we don't have a block adjacent to the blockPos */
                    val neighbor = BlockUtils.getNeighbour(blockPos2.offset(direction.rotateY(), i), attempts = 1)
                        ?: continue

                    /* place the block */
                    PlaceBlock(neighbor.second, neighbor.first)
                } else {
                    /* check if we don't have a block adjacent to the blockPos */
                    val neighbor = BlockUtils.getNeighbour(blockPos.offset(direction.rotateY(), i), attempts = 1)
                        ?: continue

                    /* place the block */
                    PlaceBlock(neighbor.second, neighbor.first)
                }
            }
            if (doSides.value) {
                if (width.value % 2 == 0) {
                    val neighbor = BlockUtils.getNeighbour(blockPos2.offset(direction.rotateY(), halfWidth).offset(EnumFacing.UP), attempts = 2)
                        ?: return@listener

                    /* place the block */
                    PlaceBlock(neighbor.second, neighbor.first)

                    val neighbor2 = BlockUtils.getNeighbour(blockPos2.offset(direction.rotateY(), -halfWidth - 1).offset(EnumFacing.UP), attempts = 2)
                        ?: return@listener

                    /* place the block */
                    PlaceBlock(neighbor2.second, neighbor2.first)
                }
                else {
                    val neighbor = BlockUtils.getNeighbour(blockPos.offset(direction.rotateY(), halfWidth + 1).offset(EnumFacing.UP), attempts = 2)
                        ?: return@listener

                    /* place the block */
                    PlaceBlock(neighbor.second, neighbor.first)

                    val neighbor2 = BlockUtils.getNeighbour(blockPos.offset(direction.rotateY(), -halfWidth - 1).offset(EnumFacing.UP), attempts = 2)
                        ?: return@listener

                    /* place the block */
                    PlaceBlock(neighbor2.second, neighbor2.first)
                }
            }
            /* Reset the slot */
            if (!holding) { swapSlot(oldSlot); PlayerPacketManager.resetHotbar() }

        }
    }
    private fun PlaceBlock(pos: BlockPos, facing: EnumFacing) {
        if (mc.world.getBlockState(pos.offset(facing)).material.isReplaceable) {
            BlockUtils.placeBlock(pos, facing)
        }
    }
    private fun CorrectPos(pos: Vec3d): BlockPos {
        if (GetDecimalPart(pos.x) > 0.5f) return BlockPos(pos.add(1.0, 0.0, 0.0))
        if (GetDecimalPart(pos.z) > 0.5f) return BlockPos(pos.add(0.0, 0.0, 1.0))
        return BlockPos(pos)
    }
    private fun GetDecimalPart(a: Double): Float {
        NecronClient.LOG.info(a.toString().split(".")[1])
        return a.toString().split(".")[1].toFloat()
    }
    private fun TranslateFacing(pos: BlockPos, facing: EnumFacing, dist: Int): BlockPos {
        return when (facing) {
            EnumFacing.NORTH -> pos.east(dist)
            EnumFacing.EAST -> pos.south(dist)
            EnumFacing.SOUTH -> pos.west(dist)
            EnumFacing.WEST -> pos.north(dist)
            else -> pos
        }
    }
    private fun spoofHotbar(): Boolean {
        val slot = getObby()
        if (slot == -1) return false
        if (slot != mc.player.inventory.currentItem) oldSlot = mc.player.inventory.currentItem
        if (swapMode.value == SwapMode.SWAP) {
            if (slot != -1) {
                swapSlot(slot)
            }
        }
        else {
            if (slot != -1) PlayerPacketManager.spoofHotbar(getObby())
        }
        return true
    }

    private fun getObby(): Int {
        val slots = InventoryUtils.getSlotsHotbar(49)
        if (slots == null) { // Obsidian check
            if (isEnabled) {
                MessageSendHelper.sendChatMessage("$chatName No obsidian in hotbar, disabling!")
                disable()
            }
            return -1
        }
        return slots[0]
    }

}