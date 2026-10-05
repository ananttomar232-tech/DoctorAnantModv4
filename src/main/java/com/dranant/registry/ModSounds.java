package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom sound events. Each one is defined in assets/dranant/sounds.json (layered on vanilla sound events, so no .ogg files are required). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DoctorAnantMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HANDCUFF_RATTLE = register("handcuff_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYRINGE_INJECTION = register("syringe_injection");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROUND_SLAM = register("ground_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> CASH_REGISTER_CHIME = register("cash_register_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIAMOND_POP = register("diamond_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIKE_ENGINE = register("bike_engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIKE_HORN = register("bike_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIDEKICK_SHOT = register("sidekick_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIDEKICK_WHISTLE = register("sidekick_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_SHOT = register("plasma_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> KATANA_DASH = register("katana_dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEFIB_SHOCK = register("defib_shock");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAR_ENGINE = register("car_engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAR_HORN = register("car_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAR_DOOR = register("car_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAR_CRASH = register("car_crash");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAR_NITRO = register("car_nitro");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(DoctorAnantMod.id(name)));
    }

    private ModSounds() {}
}
