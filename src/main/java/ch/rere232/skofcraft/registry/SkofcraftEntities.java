package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.AircraftCarrier;
import ch.rere232.skofcraft.entity.ApacheBullet;
import ch.rere232.skofcraft.entity.ApacheHelicopter;
import ch.rere232.skofcraft.entity.ApacheRocket;
import ch.rere232.skofcraft.entity.DeliveryCrate;
import ch.rere232.skofcraft.entity.F14Tomcat;
import ch.rere232.skofcraft.entity.DeliveryPlane;
import ch.rere232.skofcraft.entity.SpitProjectile;
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

    private SkofcraftEntities() {}
}
