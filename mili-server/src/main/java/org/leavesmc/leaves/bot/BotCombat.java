package org.leavesmc.leaves.bot;

import ca.spottedleaf.moonrise.common.util.TickThread;
import fun.bm.mili.carpet.WorldGameplayRules;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.player.PlayerMoveEvent;
import org.leavesmc.leaves.bot.agent.Configs;
import org.leavesmc.leaves.bot.agent.configs.CombatModeConfig.CombatMode;

final class BotCombat {
    private ResourceKey<Level> lureDimension;
    private Vec3 lureAnchor;

    void tick(ServerBot bot) {
        final CombatMode mode = bot.getConfigValue(Configs.COMBAT_MODE);
        if (mode != CombatMode.LURE) {
            this.lureAnchor = null;
            this.lureDimension = null;
        }
        if (mode == CombatMode.NONE || !TickThread.isTickThreadFor(bot) || !canAct(bot)) return;

        final ServerLevel level = bot.level();
        final var region = level.getCurrentWorldData();
        if (region == null || !ownsLoadedArea(level, bot.getBoundingBox().inflate(2.0))) return;
        if (mode == CombatMode.LURE && (this.lureAnchor == null || !level.dimension().equals(this.lureDimension))) {
            this.lureDimension = level.dimension();
            this.lureAnchor = bot.position();
        }
        final double attackRange = bot.getConfigValue(Configs.COMBAT_RANGE);
        final double lureRange = bot.getConfigValue(Configs.COMBAT_LURE_RANGE);
        final double searchRange = mode == CombatMode.LURE ? Math.max(attackRange, lureRange) : attackRange;
        final AABB search = bot.getBoundingBox().inflate(searchRange, searchRange * 0.5, searchRange);
        Mob target = null;
        double closest = searchRange * searchRange;
        // Never query Moonrise slices belonging to another region or keep a target across ticks.
        for (Entity entity : region.getLocalEntities()) {
            if (!(entity instanceof Mob mob) || !(mob instanceof Enemy) || !TickThread.isTickThreadFor(mob) || mob.level() != level) continue;
            final double distance = bot.distanceToSqr(mob);
            if (distance <= closest && search.intersects(mob.getBoundingBox()) && canSee(bot, level, mob)) {
                closest = distance;
                target = mob;
            }
        }

        if (target != null) {
            bot.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            if (canReach(bot, target, attackRange)) {
                if (bot.getAttackStrengthScale(0.5F) > 0.9F) {
                    bot.attackCombatTarget(target, candidate -> mayAttack(bot, level, candidate));
                }
                return;
            }
        }
        if (mode == CombatMode.LURE && this.lureAnchor != null) {
            final Vec3 destination = target != null && target.position().distanceToSqr(this.lureAnchor) <= lureRange * lureRange
                ? target.position() : this.lureAnchor;
            stepTowards(bot, level, destination, this.lureAnchor, lureRange);
        }
    }

    private static boolean canAct(ServerBot bot) {
        return bot.isAlive() && !bot.isRemoved() && !bot.isCreative() && !bot.isSpectator() && !bot.handsBusy
            && !bot.isUsingItem() && !bot.isSleeping() && !bot.isPassenger() && !bot.isVehicle() && !bot.isAutoSpinAttack();
    }

    private static boolean canSee(ServerBot bot, ServerLevel level, Mob target) {
        return ownsLoadedArea(level, bot.getBoundingBox().minmax(target.getBoundingBox()).inflate(1.0))
            && level.getWorldBorder().isWithinBounds(target.getBoundingBox()) && target.isAlive() && !target.isRemoved()
            && target.isAttackable() && !target.skipAttackInteraction(bot) && !bot.isAlliedTo(target) && bot.hasLineOfSight(target);
    }

    private static boolean canReach(ServerBot bot, Mob target, double range) {
        return bot.distanceToSqr(target) <= range * range && bot.isWithinAttackRange(bot.getMainHandItem(), target.getBoundingBox(), 0.0);
    }

    // The native attack calls this again after PrePlayerAttackEntityEvent, which may move or equip either entity.
    private static boolean mayAttack(ServerBot bot, ServerLevel level, Entity candidate) {
        if (!(candidate instanceof Mob target) || !(target instanceof Enemy)
            || !TickThread.isTickThreadFor(bot) || !TickThread.isTickThreadFor(target)
            || bot.level() != level || target.level() != level || !canAct(bot)
            || bot.getConfigValue(Configs.COMBAT_MODE) == CombatMode.NONE
            || !canReach(bot, target, bot.getConfigValue(Configs.COMBAT_RANGE)) || !canSee(bot, level, target)) return false;
        if (bot.getMainHandItem().is(ItemTags.SWORDS)) {
            final AABB sweep = target.getBoundingBox().inflate(1.0, 0.25, 1.0);
            if (!ownsLoadedArea(level, sweep.inflate(1.0))) return false;
            for (ServerPlayer bystander : level.getLocalPlayers()) {
                if (bystander != bot && (!TickThread.isTickThreadFor(bystander)
                    || (bot.distanceToSqr(bystander) < 9.0 && sweep.intersects(bystander.getBoundingBox())))) return false;
            }
        }
        return true;
    }

    static void stepTowards(ServerBot bot, ServerLevel level, Vec3 destination, Vec3 anchor, double range) {
        if (!bot.onGround() || bot.getAbilities().flying || bot.isInWater() || bot.isInLava() || bot.noPhysics) return;
        final Vec3 position = bot.position();
        final Vec3 step = lureStep(position, destination, anchor, range);
        if (step == Vec3.ZERO) return;
        final Vec3 next = position.add(step);
        final AABB nextBox = bot.getBoundingBox().move(step);
        if (!ownsLoadedArea(level, bot.getBoundingBox().minmax(nextBox).inflate(2.0))
            || !level.getWorldBorder().isWithinBounds(nextBox)) return;
        // Require ground under the new centre; no jumps, pathfinding, or walking off a ledge.
        final AABB support = new AABB(next.x - 0.01, nextBox.minY - 0.0625, next.z - 0.01,
            next.x + 0.01, nextBox.minY, next.z + 0.01);
        if (!level.getBlockCollisions(bot, support).iterator().hasNext()) return;

        final var player = bot.getBukkitEntity();
        final var from = player.getLocation();
        final var to = from.clone().add(step.x, 0.0, step.z);
        final var event = new PlayerMoveEvent(player, from, to.clone());
        // Cancellation or a redirected destination must not turn this bounded step into a teleport.
        if (!event.callEvent() || !to.equals(event.getTo()) || !TickThread.isTickThreadFor(bot)
            || bot.level() != level || !position.equals(bot.position()) || !canAct(bot) || bot.noPhysics
            || bot.getConfigValue(Configs.COMBAT_MODE) != CombatMode.LURE
            || !ownsLoadedArea(level, bot.getBoundingBox().minmax(nextBox).inflate(2.0))
            || !level.getWorldBorder().isWithinBounds(nextBox)
            || !level.getBlockCollisions(bot, support).iterator().hasNext()) return;
        bot.move(MoverType.SELF, step.add(0.0, -0.01, 0.0));
    }

    static Vec3 lureStep(Vec3 position, Vec3 destination, Vec3 anchor, double range) {
        final Vec3 horizontal = new Vec3(destination.x - position.x, 0.0, destination.z - position.z);
        if (horizontal.lengthSqr() < 0.01) return Vec3.ZERO;
        final Vec3 step = horizontal.normalize().scale(0.1);
        final double nextDistance = position.add(step).distanceToSqr(anchor);
        return nextDistance > range * range && nextDistance >= position.distanceToSqr(anchor) ? Vec3.ZERO : step;
    }

    private static boolean ownsLoadedArea(ServerLevel level, AABB area) {
        return WorldGameplayRules.ownsLoadedBox(level, Mth.floor(area.minX), Mth.floor(area.minZ),
            Mth.floor(area.maxX), Mth.floor(area.maxZ));
    }
}
