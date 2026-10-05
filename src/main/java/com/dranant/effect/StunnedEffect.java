package com.dranant.effect;

import com.dranant.DoctorAnantMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Inflicted by the beast's Stun Slam: you cannot walk or jump for a moment (stars spin around your head). */
public class StunnedEffect extends MobEffect {
    public StunnedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFEE55);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, DoctorAnantMod.id("stunned_speed"), -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, DoctorAnantMod.id("stunned_jump"), -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 4 == 0;
    }

    /** Spinning yellow "stars" above the head of the stunned entity. */
    @Override
    public boolean applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
            double a = entity.tickCount * 0.5D;
            for (int i = 0; i < 3; i++) {
                double ang = a + i * Math.PI * 2 / 3;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON, entity.getX() + Math.cos(ang) * 0.45D,
                        entity.getY() + entity.getBbHeight() + 0.25D, entity.getZ() + Math.sin(ang) * 0.45D, 1, 0, 0, 0, 0);
            }
            if (entity.isSprinting()) entity.setSprinting(false);
        }
        return true;
    }
}
