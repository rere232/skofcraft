package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SkofcraftMod.MODID);

    /** Blocks away the horn can be heard. The real one is 120 dB and made for mountain roads. */
    private static final float HORN_RANGE = 128.0F;

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, name)));
    }

    public static final RegistryObject<SoundEvent> POSTBUS_HORN = SOUNDS.register("postbus_horn",
            () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "postbus_horn"), HORN_RANGE));

    public static final RegistryObject<SoundEvent> ITEM_SCANNER_BEEP = SOUNDS.register("item_scanner_beep",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "item_scanner_beep")));

    /** Placeholder event for the DJ mixer's streamed songs; the audio itself comes from yt-dlp, never from this file. */
    public static final RegistryObject<SoundEvent> DJ_MIXER_STREAM = sound("dj_mixer_stream");

    public static final RegistryObject<SoundEvent> ANCHOR_CHAIN = sound("anchor_chain");
    public static final RegistryObject<SoundEvent> ARMOR_EQUIP = sound("armor_equip");
    public static final RegistryObject<SoundEvent> ARRESTING_WIRE = sound("arresting_wire");
    public static final RegistryObject<SoundEvent> BEANS_GRIND = sound("beans_grind");
    public static final RegistryObject<SoundEvent> BEER_GULP = sound("beer_gulp");
    public static final RegistryObject<SoundEvent> BLAHAJ_SQUEAK = sound("blahaj_squeak");
    public static final RegistryObject<SoundEvent> BUTTON_PRESS = sound("button_press");
    public static final RegistryObject<SoundEvent> BUTTON_RELEASE = sound("button_release");
    public static final RegistryObject<SoundEvent> CARGO_PLANE = sound("cargo_plane");
    public static final RegistryObject<SoundEvent> CATAPULT = sound("catapult");
    public static final RegistryObject<SoundEvent> CHAIN_GUN = sound("chain_gun");
    public static final RegistryObject<SoundEvent> CIWS = sound("ciws");
    public static final RegistryObject<SoundEvent> CLOTHES_RUSTLE = sound("clothes_rustle");
    public static final RegistryObject<SoundEvent> COFFEE_BREW = sound("coffee_brew");
    public static final RegistryObject<SoundEvent> COFFEE_DING = sound("coffee_ding");
    public static final RegistryObject<SoundEvent> COFFEE_FAIL = sound("coffee_fail");
    public static final RegistryObject<SoundEvent> COIN = sound("coin");
    public static final RegistryObject<SoundEvent> COUNTDOWN_BEEP = sound("countdown_beep");
    public static final RegistryObject<SoundEvent> CRATE_DROP = sound("crate_drop");
    public static final RegistryObject<SoundEvent> CRATE_LAND = sound("crate_land");
    public static final RegistryObject<SoundEvent> CRATE_OPEN = sound("crate_open");
    public static final RegistryObject<SoundEvent> DESIGNATOR_FIRE = sound("designator_fire");
    public static final RegistryObject<SoundEvent> DESIGNATOR_MARK = sound("designator_mark");
    public static final RegistryObject<SoundEvent> ENGINE_IGNITE = sound("engine_ignite");
    public static final RegistryObject<SoundEvent> EVIL_CACKLE = sound("evil_cackle");
    public static final RegistryObject<SoundEvent> EVIL_WARP = sound("evil_warp");
    public static final RegistryObject<SoundEvent> EXHALE = sound("exhale");
    public static final RegistryObject<SoundEvent> EXPLOSION = sound("explosion");
    public static final RegistryObject<SoundEvent> FEMBOY_HUM = sound("femboy_hum");
    public static final RegistryObject<SoundEvent> FUEL_PUMP = sound("fuel_pump");
    public static final RegistryObject<SoundEvent> GULP = sound("gulp");
    public static final RegistryObject<SoundEvent> HEADPAT = sound("headpat");
    public static final RegistryObject<SoundEvent> HOOKAH_BUBBLE = sound("hookah_bubble");
    public static final RegistryObject<SoundEvent> HR_CHIME = sound("hr_chime");
    public static final RegistryObject<SoundEvent> TANK_CANNON = sound("tank_cannon");
    public static final RegistryObject<SoundEvent> TANK_ENGINE = sound("tank_engine");
    public static final RegistryObject<SoundEvent> HULL_CLANG = sound("hull_clang");
    public static final RegistryObject<SoundEvent> ICBM_ARM = sound("icbm_arm");
    public static final RegistryObject<SoundEvent> INHALE = sound("inhale");
    public static final RegistryObject<SoundEvent> JACKPOT = sound("jackpot");
    public static final RegistryObject<SoundEvent> JET_ENGINE = sound("jet_engine");
    public static final RegistryObject<SoundEvent> LAUNCH_ARM = sound("launch_arm");
    public static final RegistryObject<SoundEvent> LIGHTER = sound("lighter");
    public static final RegistryObject<SoundEvent> MEDKIT = sound("medkit");
    public static final RegistryObject<SoundEvent> MELTDOWN = sound("meltdown");
    public static final RegistryObject<SoundEvent> MISSILE_ROAR = sound("missile_roar");
    public static final RegistryObject<SoundEvent> NUKE_BOOM = sound("nuke_boom");
    public static final RegistryObject<SoundEvent> NUKE_RUMBLE = sound("nuke_rumble");
    public static final RegistryObject<SoundEvent> NUKE_WARNING = sound("nuke_warning");
    public static final RegistryObject<SoundEvent> PAYLOAD_LOAD = sound("payload_load");
    public static final RegistryObject<SoundEvent> PHONE_NOTIFY = sound("phone_notify");
    public static final RegistryObject<SoundEvent> PILL_CHIME = sound("pill_chime");
    public static final RegistryObject<SoundEvent> PLUSH_SOFT = sound("plush_soft");
    public static final RegistryObject<SoundEvent> RADAR_PING = sound("radar_ping");
    public static final RegistryObject<SoundEvent> SONAR_PING = sound("sonar_ping");
    public static final RegistryObject<SoundEvent> DIVE_KLAXON = sound("dive_klaxon");
    public static final RegistryObject<SoundEvent> SUB_HUM = sound("sub_hum");
    public static final RegistryObject<SoundEvent> REACTOR_ALARM = sound("reactor_alarm");
    public static final RegistryObject<SoundEvent> REPAIR_WRENCH = sound("repair_wrench");
    public static final RegistryObject<SoundEvent> ROCKET_FIRE = sound("rocket_fire");
    public static final RegistryObject<SoundEvent> ROD_CHARGE = sound("rod_charge");
    public static final RegistryObject<SoundEvent> FPV_MOTOR = sound("fpv_motor");
    public static final RegistryObject<SoundEvent> ROTOR = sound("rotor");
    public static final RegistryObject<SoundEvent> SHIP_HORN = sound("ship_horn");
    public static final RegistryObject<SoundEvent> SHOCKWAVE = sound("shockwave");
    public static final RegistryObject<SoundEvent> SIREN = sound("siren");
    public static final RegistryObject<SoundEvent> SNIFF = sound("sniff");
    public static final RegistryObject<SoundEvent> SPIT = sound("spit");
    public static final RegistryObject<SoundEvent> STEAM_HISS = sound("steam_hiss");
    public static final RegistryObject<SoundEvent> SWITCH_CLICK = sound("switch_click");
    public static final RegistryObject<SoundEvent> SWORD_CLANG = sound("sword_clang");
    public static final RegistryObject<SoundEvent> TASER_ZAP = sound("taser_zap");
    public static final RegistryObject<SoundEvent> TRIP_THUD = sound("trip_thud");
    public static final RegistryObject<SoundEvent> UI_CLICK = sound("ui_click");
    public static final RegistryObject<SoundEvent> VAPE_PUFF = sound("vape_puff");
    public static final RegistryObject<SoundEvent> VULCAN = sound("vulcan");
    public static final RegistryObject<SoundEvent> WARHEAD_FALL = sound("warhead_fall");
    public static final RegistryObject<SoundEvent> WATER_POUR = sound("water_pour");

    private SkofcraftSounds() {}
}
