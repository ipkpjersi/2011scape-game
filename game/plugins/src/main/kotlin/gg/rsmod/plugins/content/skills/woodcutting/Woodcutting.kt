package gg.rsmod.plugins.content.skills.woodcutting

import gg.rsmod.game.fs.def.ItemDef
import gg.rsmod.game.model.entity.DynamicObject
import gg.rsmod.game.model.entity.GameObject
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.model.queue.QueueTask
import gg.rsmod.plugins.api.ChatMessageType
import gg.rsmod.plugins.api.Skills
import gg.rsmod.plugins.api.cfg.Items
import gg.rsmod.plugins.api.ext.*

/**
 * @author Tom <rspsmods@gmail.com>
 */
object Woodcutting {

    data class Tree(val type: TreeType, val obj: Int)

    val treeStumps: MutableMap<Int, Int> = HashMap()

    suspend fun chopDownTree(it: QueueTask, obj: GameObject, tree: TreeType) {
        val p = it.player

        if (!canChop(p, obj, tree)) {
            return
        }

        val logName = p.world.definitions.get(ItemDef::class.java, tree.log).name
        val axe = AxeType.values.reversed().firstOrNull { p.getSkills().getMaxLevel(Skills.WOODCUTTING) >= it.level && (p.equipment.contains(it.item) || p.inventory.contains(it.item)) }!!

        val infernoAdze = axe.item == Items.INFERNO_ADZE
        p.message("You swing your hatchet at the tree.", type = ChatMessageType.GAME_MESSAGE)
        while (true) {
            p.animate(axe.animation)
            it.wait(2)

            if (!canChop(p, obj, tree)) {
                p.animate(-1)
                break
            }

            val level = p.getSkills().getCurrentLevel(Skills.WOODCUTTING)
            if (interpolate((tree.lowChance * axe.ratio).toInt(), (tree.highChance * axe.ratio).toInt(), level) > RANDOM.nextInt(255)) {
                // TODO: find the game filter id
                p.message("You get some ${logName.pluralSuffix(2).toLowerCase()}.", type = ChatMessageType.GAME_MESSAGE)
                p.playSound(3600)
                p.inventory.add(tree.log)
                p.addXp(Skills.WOODCUTTING, tree.xp)

                if (p.world.random(tree.depleteChance) == 0) {
                    p.animate(-1)

                    if (treeStumps[obj.id] != -1) {
                        val world = p.world
                        world.queue {
                            val trunk = DynamicObject(obj, treeStumps[obj.id]!!)
                            world.remove(obj)
                            world.spawn(trunk)
                            wait(tree.respawnTime.random())
                            world.remove(trunk)
                            world.spawn(DynamicObject(obj))
                        }
                    }
                    break
                }
            }
            it.wait(2)
        }
    }

    private fun canChop(p: Player, obj: GameObject, tree: TreeType): Boolean {
        if (!p.world.isSpawned(obj)) {
            return false
        }

        val axe = AxeType.values.reversed().firstOrNull { p.getSkills().getMaxLevel(Skills.WOODCUTTING) >= it.level && (p.equipment.contains(it.item) || p.inventory.contains(it.item)) }
        if (axe == null) {
            p.message("You need a hatchet to chop down this tree.")
            p.message("You do not have an axe which you have the woodcutting level to use.")
            return false
        }

        if (p.getSkills().getMaxLevel(Skills.WOODCUTTING) < tree.level) {
            p.message("You need a Woodcutting level of ${tree.level} to chop down this tree.")
            return false
        }

        if (p.inventory.isFull) {
            p.message("Your inventory is too full to hold any more logs.")
            return false
        }

        return true
    }
}