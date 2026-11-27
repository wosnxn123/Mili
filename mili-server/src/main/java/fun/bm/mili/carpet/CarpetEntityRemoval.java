/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Derived from Carpet TIS Addition, Fallen_Breath and contributors
 * (62c3cb6fff26cd9c4e12aa616b403c184a4a6ca0), via Lophine 0134/0135
 * by Bacteriawa (7a51f49a512f1b5465f15a905d3704754e4bed02).
 */
package fun.bm.mili.carpet;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;

public final class CarpetEntityRemoval {
    private CarpetEntityRemoval() {}

    public static boolean canRemove(Entity entity) {
        return !(entity instanceof Player) && !entity.isSpectator() && !entity.isRemoved();
    }

    private static boolean remove(ServerPlayer player, Entity entity) {
        // 只处理当前世界、当前区域的实体。目标跨区后不延迟执行一次过期的攻击。
        if (!TickThread.isTickThreadFor(player) || player.isRemoved() || !player.isCreative()
            || !TickThread.isTickThreadFor(entity) || entity.level() != player.level() || !canRemove(entity)) return false;
        if (entity instanceof EnderDragonPart part) entity = part.parentMob;
        if (entity instanceof EnderDragon dragon) {
            if (!TickThread.isTickThreadFor(dragon) || dragon.isRemoved()) return false;
            for (Entity part : dragon.getSubEntities()) {
                if (!TickThread.isTickThreadFor(part)) return false;
            }
            for (Entity part : dragon.getSubEntities()) {
                if (!part.isRemoved()) part.mili$discardCleanly();
            }
        }
        entity.mili$discardCleanly();
        return true;
    }

    public static void onHit(ServerPlayer player, Entity victim) {
        if (!TickThread.isTickThreadFor(player) || !TickThread.isTickThreadFor(victim)
            || player.level() != victim.level() || !canRemove(victim)) return;
        final var area = victim.getBoundingBox().inflate(1.0, 0.25, 1.0);
        final boolean sweep = player.getMainHandItem().is(ItemTags.SWORDS) && player.onGround()
            && !player.isSprinting() && TickThread.isTickThreadFor(player.level(), area);
        if (sweep) {
            for (Entity nearby : player.level().getEntitiesOfClass(Entity.class, area)) {
                // 主目标已经通过原有 Paper 事件；额外目标也允许插件单独拒绝。
                if (nearby != victim && TickThread.isTickThreadFor(nearby) && canRemove(nearby)
                    && !(nearby instanceof EnderDragonPart) && player.distanceToSqr(nearby) < 9.0
                    && new PrePlayerAttackEntityEvent(player.getBukkitEntity(), nearby.getBukkitEntity(), true).callEvent()) {
                    remove(player, nearby);
                }
            }
        }
        if (remove(player, victim)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sweep ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_STRONG, player.getSoundSource(), 0.7F, 0.9F);
        }
    }
}
