package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.AircraftCarrier;
import ch.rere232.skofcraft.entity.AnimeCrew;
import ch.rere232.skofcraft.entity.ApacheBullet;
import ch.rere232.skofcraft.entity.ApacheHelicopter;
import ch.rere232.skofcraft.entity.ApacheRocket;
import ch.rere232.skofcraft.entity.DeliveryCrate;
import ch.rere232.skofcraft.entity.F14Tomcat;
import ch.rere232.skofcraft.entity.Femboy;
import ch.rere232.skofcraft.entity.FpvDrone;
import ch.rere232.skofcraft.entity.DeliveryPlane;
import ch.rere232.skofcraft.entity.SpitProjectile;
import ch.rere232.skofcraft.entity.GigaIcbm;
import ch.rere232.skofcraft.entity.HrRepresentative;
import ch.rere232.skofcraft.entity.IcbmWarhead;
import ch.rere232.skofcraft.entity.LaunchRocket;
import ch.rere232.skofcraft.entity.Leopard1Tank;
import ch.rere232.skofcraft.entity.MausTank;
import ch.rere232.skofcraft.entity.TankShell;
import ch.rere232.skofcraft.entity.NuclearSubmarine;
import ch.rere232.skofcraft.entity.Tiger2Tank;
import ch.rere232.skofcraft.entity.Torpedo;
import ch.rere232.skofcraft.entity.OrbitalRod;
import ch.rere232.skofcraft.entity.Postbus;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SkofcraftMod.MODID);

    public static final RegistryObject<EntityType<SpitProjectile>> SPIT = ENTITIES.register("spit",
            () -> EntityType.Builder.<SpitProjectile>of(SpitProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("spit"));

    public static final RegistryObject<EntityType<DeliveryPlane>> DELIVERY_PLANE = ENTITIES.register("delivery_plane",
            () -> EntityType.Builder.<DeliveryPlane>of(DeliveryPlane::new, MobCategory.MISC)
                    .sized(3.0F, 1.0F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("delivery_plane"));

    public static final RegistryObject<EntityType<DeliveryCrate>> DELIVERY_CRATE = ENTITIES.register("delivery_crate",
            () -> EntityType.Builder.<DeliveryCrate>of(DeliveryCrate::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("delivery_crate"));

    public static final RegistryObject<EntityType<ApacheHelicopter>> APACHE = ENTITIES.register("apache",
            () -> EntityType.Builder.<ApacheHelicopter>of(ApacheHelicopter::new, MobCategory.MISC)
                    .sized(4.0F, 3.6F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("apache"));

    public static final RegistryObject<EntityType<F14Tomcat>> F14 = ENTITIES.register("f14",
            () -> EntityType.Builder.<F14Tomcat>of(F14Tomcat::new, MobCategory.MISC)
                    .sized(3.4F, 2.6F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("f14"));

    public static final RegistryObject<EntityType<FpvDrone>> FPV_DRONE = ENTITIES.register("fpv_drone",
            () -> EntityType.Builder.<FpvDrone>of(FpvDrone::new, MobCategory.MISC)
                    .sized(0.7F, 0.45F)
                    // Further than any view distance: the pilot must keep tracking it right up to the edge of the signal
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("fpv_drone"));

    public static final RegistryObject<EntityType<AircraftCarrier>> AIRCRAFT_CARRIER = ENTITIES.register("aircraft_carrier",
            () -> EntityType.Builder.<AircraftCarrier>of(AircraftCarrier::new, MobCategory.MISC)
                    .sized(1.0F, 12.0F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build("aircraft_carrier"));

    public static final RegistryObject<EntityType<ApacheBullet>> APACHE_BULLET = ENTITIES.register("apache_bullet",
            () -> EntityType.Builder.<ApacheBullet>of(ApacheBullet::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(6)
                    .updateInterval(1)
                    .build("apache_bullet"));

    public static final RegistryObject<EntityType<ApacheRocket>> APACHE_ROCKET = ENTITIES.register("apache_rocket",
            () -> EntityType.Builder.<ApacheRocket>of(ApacheRocket::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("apache_rocket"));

    public static final RegistryObject<EntityType<LaunchRocket>> LAUNCH_ROCKET = ENTITIES.register("launch_rocket",
            () -> EntityType.Builder.<LaunchRocket>of(LaunchRocket::new, MobCategory.MISC)
                    .sized(1.0F, 5.5F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .fireImmune()
                    .build("launch_rocket"));

    public static final RegistryObject<EntityType<OrbitalRod>> ORBITAL_ROD = ENTITIES.register("orbital_rod",
            () -> EntityType.Builder.<OrbitalRod>of(OrbitalRod::new, MobCategory.MISC)
                    .sized(0.4F, 3.0F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .fireImmune()
                    .build("orbital_rod"));

    public static final RegistryObject<EntityType<GigaIcbm>> GIGA_ICBM = ENTITIES.register("giga_icbm",
            () -> EntityType.Builder.<GigaIcbm>of(GigaIcbm::new, MobCategory.MISC)
                    .sized(1.6F, 8.8F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .fireImmune()
                    .build("giga_icbm"));

    public static final RegistryObject<EntityType<IcbmWarhead>> ICBM_WARHEAD = ENTITIES.register("icbm_warhead",
            () -> EntityType.Builder.<IcbmWarhead>of(IcbmWarhead::new, MobCategory.MISC)
                    .sized(1.6F, 8.8F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .fireImmune()
                    .build("icbm_warhead"));

    public static final RegistryObject<EntityType<Femboy>> FEMBOY = ENTITIES.register("femboy",
            () -> EntityType.Builder.<Femboy>of(Femboy::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("femboy"));

    public static final RegistryObject<EntityType<HrRepresentative>> HR_REPRESENTATIVE = ENTITIES.register("hr_representative",
            () -> EntityType.Builder.<HrRepresentative>of(HrRepresentative::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("hr_representative"));

    public static final RegistryObject<EntityType<MausTank>> MAUS = ENTITIES.register("maus",
            () -> EntityType.Builder.<MausTank>of(MausTank::new, MobCategory.MISC)
                    .sized(3.6F, 3.2F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .build("maus"));

    public static final RegistryObject<EntityType<Leopard1Tank>> LEOPARD_1 = ENTITIES.register("leopard_1",
            () -> EntityType.Builder.<Leopard1Tank>of(Leopard1Tank::new, MobCategory.MISC)
                    .sized(3.3F, 2.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .build("leopard_1"));

    public static final RegistryObject<EntityType<Tiger2Tank>> TIGER_2 = ENTITIES.register("tiger_2",
            () -> EntityType.Builder.<Tiger2Tank>of(Tiger2Tank::new, MobCategory.MISC)
                    .sized(3.7F, 2.9F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .build("tiger_2"));

    public static final RegistryObject<EntityType<NuclearSubmarine>> NUCLEAR_SUBMARINE = ENTITIES.register("nuclear_submarine",
            () -> EntityType.Builder.<NuclearSubmarine>of(NuclearSubmarine::new, MobCategory.MISC)
                    .sized(4.0F, 4.0F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build("nuclear_submarine"));

    public static final RegistryObject<EntityType<Torpedo>> TORPEDO = ENTITIES.register("torpedo",
            () -> EntityType.Builder.<Torpedo>of(Torpedo::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .build("torpedo"));

    public static final RegistryObject<EntityType<TankShell>> TANK_SHELL = ENTITIES.register("tank_shell",
            () -> EntityType.Builder.<TankShell>of(TankShell::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("tank_shell"));

    public static final RegistryObject<EntityType<AnimeCrew>> ANIME_CREW = ENTITIES.register("anime_crew",
            () -> EntityType.Builder.<AnimeCrew>of(AnimeCrew::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("anime_crew"));

    public static final RegistryObject<EntityType<Postbus>> POSTBUS = ENTITIES.register("postbus",
            () -> EntityType.Builder.<Postbus>of(Postbus::new, MobCategory.MISC)
                    .sized(2.8F, 3.0F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("postbus"));

    private SkofcraftEntities() {}
}
