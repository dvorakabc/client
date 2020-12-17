package me.zeroeightsix.kami.module.modules.combat

import baritone.behavior.InventoryBehavior
import jdk.nashorn.internal.ir.Block
import me.zeroeightsix.kami.NecronClient
import me.zeroeightsix.kami.event.events.SafeTickEvent
import me.zeroeightsix.kami.manager.managers.CombatManager
import me.zeroeightsix.kami.manager.managers.PlayerInventoryManager.addInventoryTask
import me.zeroeightsix.kami.manager.managers.PlayerPacketManager
import me.zeroeightsix.kami.mixin.extension.rightClickMouse
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.module.modules.client.InventoryViewer
import me.zeroeightsix.kami.module.modules.player.InventoryManager
import me.zeroeightsix.kami.module.modules.render.Search
import me.zeroeightsix.kami.setting.Settings
import me.zeroeightsix.kami.util.Bind
import me.zeroeightsix.kami.util.BlockUtils
import me.zeroeightsix.kami.util.EntityUtils
import me.zeroeightsix.kami.util.InventoryUtils
import me.zeroeightsix.kami.util.combat.SurroundUtils
import me.zeroeightsix.kami.util.math.RotationUtils
import me.zeroeightsix.kami.util.math.VectorUtils.toBlockPos
import me.zeroeightsix.kami.util.text.MessageSendHelper
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.util.math.BlockPos
import org.kamiblue.event.listener.listener
import org.lwjgl.input.Keyboard
import net.minecraft.util.math.RayTraceResult

import net.minecraft.client.Minecraft
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.InventoryBasic
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.kamiblue.commons.utils.ClassUtils.getInstance
import java.util.concurrent.Executors
import java.util.concurrent.Future


//@CombatManager.CombatModule
@Module.Info(
    name = "Auto32k",
    category = Module.Category.COMBAT,
    description = "Automatically builds a 32k machine and hits players with 32ks",
    //modulePriority = 90
)
object Auto32k : Module() {
    private val bindBuild = register(Settings.custom("BindBuild", Bind.none(), BindConverter()))
    private val speed = register(Settings.floatBuilder("Speed").withValue(2f).withRange(0.25f, 5f).withStep(0.25f))
    private val bindKill = register(Settings.custom("BindKill", Bind.none(), BindConverter()))
    private var killOn = false
    private val threadPool = Executors.newSingleThreadExecutor()
    private val placeThread = Thread { Auto32k.place() }.apply { name = "Auto32k" }
    private val moveThread = Thread { Auto32k.move() }.apply { name = "Auto32k" }
    private var future: Future<*>? = null
    private var future2: Future<*>? = null
    private lateinit var lookingAt: RayTraceResult
    private var oldMode: KillAura.WaitMode = KillAura.WaitMode.SPAM
    private var oldDelay: Float = 2f

    val offset = arrayOf(
        BlockPos(0, 0, 0), // down
    )
    override fun getHudInfo(): String {
        return if (killOn) "killon" else if (future != null && !future!!.isDone) "build" else ""
    }
    init {
        listener<InputEvent.KeyInputEvent> {
            if (bindBuild.value.isDown(Keyboard.getEventKey())) {
                if (future == null || future!!.isDone || future!!.isCancelled) {
                    if (!killOn) {
                        lookingAt = Minecraft.getMinecraft().objectMouseOver
                        future = threadPool.submit(placeThread)
                    }
                }
            }
            else if (bindKill.value.isDown(Keyboard.getEventKey())) {
                killOn = !killOn
                if (killOn) {
                    oldMode = KillAura.delayMode.value
                    oldDelay = KillAura.waitTick.value
                    KillAura.delayMode.value = KillAura.WaitMode.SPAM
                    KillAura.waitTick.value = 0f
                    KillAura.enable()
                    future2 = threadPool.submit(moveThread)
                }
                else {
                    KillAura.delayMode.value = oldMode
                    KillAura.waitTick.value = oldDelay
                    KillAura.disable()
                    future2!!.cancel(true)
                }
            }
        }
    }
    private fun move() {
        while(true) {
            if (killOn && mc.currentScreen is net.minecraft.client.gui.inventory.GuiContainer) {
                val item = mc.player.openContainer.inventory[32]
                if (Item.getIdFromItem(item.item) == 276) {
                    val ench = EnchantmentHelper.getEnchantments(item)[Enchantment.getEnchantmentByID(16)]
                    if (ench == null || ench < 10) {
                        InventoryUtils.throwAllInSlot(mc.player.openContainer.windowId, 32)
                        Thread.sleep(100)
                        mc.playerController.updateController()
                        InventoryUtils.inventoryClick(mc.player.openContainer.windowId, 1, 0, ClickType.SWAP)

                        Thread.sleep(100)
                        InventoryUtils.inventoryClick(mc.player.openContainer.windowId, 32, 0, ClickType.SWAP)
                        //Thread.sleep(500)
                        //InventoryUtils.inventoryClick(mc.player.inventoryContainer.windowId, 1, 0, ClickType.SWAP)
                        mc.playerController.updateController()
                    }
                } else {
                    InventoryUtils.throwAllInSlot(mc.player.openContainer.windowId, 32)
                    Thread.sleep(100)
                    mc.playerController.updateController()
                    InventoryUtils.inventoryClick(mc.player.openContainer.windowId, 1, 0, ClickType.SWAP)
                    Thread.sleep(100)
                    InventoryUtils.inventoryClick(mc.player.openContainer.windowId, 32, 0, ClickType.SWAP)
                    //Thread.sleep(500)
                    //InventoryUtils.inventoryClick(mc.player.inventoryContainer.windowId, 1, 0, ClickType.SWAP)
                    mc.playerController.updateController()
                }
            }
            Thread.sleep(100)
        }
    }
    private fun place() {
        if (lookingAt != null && lookingAt.typeOfHit === RayTraceResult.Type.BLOCK) {
            val x: Double = lookingAt.hitVec.x
            val y: Double = lookingAt.hitVec.y
            val z: Double = lookingAt.hitVec.z
            selectItem(154)
            Thread.sleep((100 / speed.value).toLong())
            NecronClient.LOG.info("$x, $y, $z")
            mc.player.movementInput.sneak = true
            mc.player.connection.sendPacket(CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING))
            BlockUtils.buildStructure(5f) {
                if (isEnabled) {
                    BlockUtils.getPlaceInfo(BlockPos(x, y, z), offset, it, 2)
                } else {
                    null
                }
            }
            selectItem(23)
            Thread.sleep((100 / speed.value).toLong())
            BlockUtils.buildStructure(1f) {
                if (isEnabled) {
                    BlockUtils.getPlaceInfo(BlockPos(x + 1, y, z), offset, it, 2)
                } else {
                    null
                }
            }
            BlockUtils.buildStructure(1f) {
                if (isEnabled) {
                    BlockUtils.getPlaceInfo(BlockPos(x + 1, y + 1, z), offset, it, 2)
                } else {
                    null
                }
            }
            mc.player.isSneaking = false
            mc.player.connection.sendPacket(CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
            Thread.sleep((100 / speed.value).toLong())
            selectItem(0)
            Thread.sleep((100 / speed.value).toLong())
            while (mc.currentScreen !is net.minecraft.client.gui.inventory.GuiDispenser) {
                if (mc.world.getBlockState(BlockPos(x + 1, y + 1, z)).block == Blocks.DISPENSER) {
                    BlockUtils.placeBlock(BlockPos(x + 1, y + 1, z), EnumFacing.EAST)
                    Thread.sleep((100 / speed.value).toLong())
                }
                else {
                    MessageSendHelper.sendChatMessage("$chatName Error, Can't find dispenser")
                    return
                }
            }

            if (!getShulker()) return
            Thread.sleep((100 / speed.value).toLong())
            mc.player.inventory.closeInventory(mc.player)
            Thread.sleep((100 / speed.value).toLong())
            mc.player.movementInput.sneak = true
            mc.player.connection.sendPacket(CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING))
            Thread.sleep((10 / speed.value).toLong())
            selectItem(152)
            Thread.sleep((100 / speed.value).toLong())
            BlockUtils.buildStructure(1f) {
                if (isEnabled) {
                    BlockUtils.getPlaceInfo(BlockPos(x + 1, y + 2, z), offset, it, 1)
                } else {
                    null
                }
            }
            mc.player.isSneaking = false
            mc.player.connection.sendPacket(CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        }
        return
    }
    private fun getFilter(itemId: Int): (ItemStack) -> Boolean = { Item.getIdFromItem(it.getItem()) == itemId }
    private fun getShulker() : Boolean {
        for (i in 219..234){
            val item = InventoryUtils.getSlotsHotbar(i)
            if (item != null){
                val slot = item[0]
                InventoryUtils.inventoryClick(0, slot, 0, ClickType.PICKUP)
                Thread.sleep(50)
                InventoryUtils.inventoryClick(mc.player.openContainer.windowId, 1, 0, ClickType.SWAP)
                mc.playerController.updateController()
                return true
            }
        }
        return false
    }
    private fun moveItem()
    {
        val filter = getFilter(218)
        val sublist = mc.player.inventoryContainer.inventory.subList(9, 35)
        var crystalInventorySlot = sublist.indexOfFirst(filter)
        if (crystalInventorySlot == -1)
            return
        crystalInventorySlot += 9
        InventoryUtils.quickMoveSlot(0, crystalInventorySlot)
        mc.playerController.updateController()
    }
    private fun selectItem(id: Int) : Boolean {
        val slots = InventoryUtils.getSlotsHotbar(id)
        if (slots != null) {
            InventoryUtils.swapSlot(slots[0])
            return true
        }
        MessageSendHelper.sendChatMessage("$chatName Error, Can't find item: \"$id\" in hotbar");
        return false
    }
}