package com.dranant.entity.ai;

import com.dranant.entity.MutantBloodBeastEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** Base for the beast's timed special attacks (shared cooldowns, action poses, interruption by stun/handcuffs). */
public abstract class BossAbilityGoal extends Goal {
    protected final MutantBloodBeastEntity beast;
    private final String key;
    private final int duration;
    private final int cooldown;
    private final int enragedCooldown;
    private final int action;
    private final double minRange;
    private final double maxRange;
    protected int timer;
    @Nullable
    protected LivingEntity target;

    protected BossAbilityGoal(MutantBloodBeastEntity beast, String key, int duration, int cooldown, int enragedCooldown,
                              int action, double minRange, double maxRange, EnumSet<Flag> flags) {
        this.beast = beast;
        this.key = key;
        this.duration = duration;
        this.cooldown = cooldown;
        this.enragedCooldown = enragedCooldown;
        this.action = action;
        this.minRange = minRange;
        this.maxRange = maxRange;
        setFlags(flags);
    }

    protected boolean extraCondition(LivingEntity target) {
        return true;
    }

    @Override
    public boolean canUse() {
        LivingEntity t = beast.getTarget();
        if (t == null || !t.isAlive() || !beast.canUseSpecial(key)) return false;
        double d = beast.distanceTo(t);
        if (d < minRange || d > maxRange || !extraCondition(t)) return false;
        target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return timer < duration && beast.isAlive() && !beast.isHandcuffed() && !beast.isBeingCured() && !beast.isStunned()
                && target != null && target.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        timer = 0;
        beast.getNavigation().stop();
        beast.setActionState(action);
        onStart();
    }

    @Override
    public void stop() {
        if (beast.getActionState() == action) beast.setActionState(MutantBloodBeastEntity.ACTION_NONE);
        beast.markSpecialUsed(key, beast.isEnraged() ? enragedCooldown : cooldown);
        onStop();
        target = null;
    }

    @Override
    public void tick() {
        timer++;
        if (target != null) beast.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (beast.level() instanceof ServerLevel level && target != null) onTick(level, target, timer);
    }

    protected void onStart() {
    }

    protected void onStop() {
    }

    protected abstract void onTick(ServerLevel level, LivingEntity target, int t);
}
