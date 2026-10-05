package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import com.dranant.entity.AssistantPharmacistEntity;
import com.dranant.entity.AssistantScientistEntity;
import com.dranant.entity.MutantBloodBeastEntity;
import com.dranant.entity.PatientVillagerEntity;
import com.dranant.entity.ShopkeeperEntity;
import com.dranant.entity.SidekickEntity;
import com.dranant.entity.SidekickBulletEntity;
import com.dranant.entity.BikeEntity;
import com.dranant.entity.BloodBoulderEntity;
import com.dranant.entity.BloodOrbEntity;
import com.dranant.entity.SupercarEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, DoctorAnantMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AssistantPharmacistEntity>> ASSISTANT_PHARMACIST = ENTITIES.register("assistant_pharmacist",
            () -> EntityType.Builder.of(AssistantPharmacistEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("assistant_pharmacist"));

    public static final DeferredHolder<EntityType<?>, EntityType<AssistantScientistEntity>> ASSISTANT_SCIENTIST = ENTITIES.register("assistant_scientist",
            () -> EntityType.Builder.of(AssistantScientistEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("assistant_scientist"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShopkeeperEntity>> SHOPKEEPER = ENTITIES.register("shopkeeper",
            () -> EntityType.Builder.of(ShopkeeperEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("shopkeeper"));

    /** Clinic customer: looks like a villager, fully controlled by the clinic queue AI. */
    public static final DeferredHolder<EntityType<?>, EntityType<PatientVillagerEntity>> PATIENT_VILLAGER = ENTITIES.register("patient_villager",
            () -> EntityType.Builder.of(PatientVillagerEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("patient_villager"));

    public static final DeferredHolder<EntityType<?>, EntityType<SidekickEntity>> SIDEKICK = ENTITIES.register("sidekick",
            () -> EntityType.Builder.of(SidekickEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(10).build("sidekick"));

    public static final DeferredHolder<EntityType<?>, EntityType<SidekickBulletEntity>> SIDEKICK_BULLET = ENTITIES.register("sidekick_bullet",
            () -> EntityType.Builder.<SidekickBulletEntity>of(SidekickBulletEntity::new, MobCategory.MISC).sized(0.2F, 0.2F).clientTrackingRange(8).updateInterval(1).build("sidekick_bullet"));

    public static final DeferredHolder<EntityType<?>, EntityType<BikeEntity>> DR_ANANT_BIKE = ENTITIES.register("dr_anant_bike",
            () -> EntityType.Builder.<BikeEntity>of(BikeEntity::new, MobCategory.MISC).sized(1.0F, 1.1F).clientTrackingRange(10).build("dr_anant_bike"));

    public static final DeferredHolder<EntityType<?>, EntityType<BloodBoulderEntity>> BLOOD_BOULDER = ENTITIES.register("blood_boulder",
            () -> EntityType.Builder.<BloodBoulderEntity>of(BloodBoulderEntity::new, MobCategory.MISC).sized(1.2F, 1.2F).clientTrackingRange(8).updateInterval(1).build("blood_boulder"));

    public static final DeferredHolder<EntityType<?>, EntityType<BloodOrbEntity>> BLOOD_ORB = ENTITIES.register("blood_orb",
            () -> EntityType.Builder.<BloodOrbEntity>of(BloodOrbEntity::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(1).build("blood_orb"));

    /** Showroom supercars (three models share one entity type, the variant is synced data). */
    public static final DeferredHolder<EntityType<?>, EntityType<SupercarEntity>> SUPERCAR = ENTITIES.register("supercar",
            () -> EntityType.Builder.<SupercarEntity>of(SupercarEntity::new, MobCategory.MISC).sized(2.1F, 1.25F).clientTrackingRange(10).updateInterval(1).build("supercar"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.dranant.entity.SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<com.dranant.entity.SeatEntity>of(com.dranant.entity.SeatEntity::new, MobCategory.MISC).sized(0.01F, 0.01F).noSummon().clientTrackingRange(8).build("seat"));

    /** 3.5 blocks tall boss. Model converted 1:1 from minecraft_mutant_zombie_download_free.zip. */
    public static final DeferredHolder<EntityType<?>, EntityType<MutantBloodBeastEntity>> MUTANT_BLOOD_BEAST = ENTITIES.register("mutant_blood_beast",
            () -> EntityType.Builder.of(MutantBloodBeastEntity::new, MobCategory.MONSTER).sized(1.9F, 3.5F).fireImmune().clientTrackingRange(12).build("mutant_blood_beast"));

    private ModEntities() {}
}
