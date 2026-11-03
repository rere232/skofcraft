package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.block.GagBlock;
import ch.rere232.skofcraft.block.GagBlock.Ctx;
import ch.rere232.skofcraft.block.GagBlock.Spec;
import ch.rere232.skofcraft.hr.MandatoryHr;
import ch.rere232.skofcraft.phone.PhoneAddiction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Blocks that exist because nobody stopped us. Each one is a {@link Spec} for {@link GagBlock}. */
public final class SkofcraftGagBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SkofcraftMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SkofcraftMod.MODID);

    private static final int COLLAPSE_RADIUS = 1;
    private static final int COLLAPSE_HEIGHT = 16;
    private static final int POSTBUS_DELAY_TICKS = 200;

    static {
        // Swiss institutions

        gag("swiss_bureaucracy_block", () -> new Spec(paper(MapColor.TERRACOTTA_WHITE).strength(2.5F))
                .use(40, c -> {
                    c.sound(SkofcraftSounds.UI_CLICK.get(), 0.8F, 0.7F);
                    c.say("swiss_bureaucracy_block", 5);
                }));

        gag("swiss_neutrality_block", () -> new Spec(soft(MapColor.COLOR_RED))
                .use(200, c -> {
                    AABB area = new AABB(c.pos()).inflate(16.0D);
                    for (Monster monster : c.level().getEntitiesOfClass(Monster.class, area)) {
                        monster.setTarget(null);
                        monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
                    }
                    for (Player player : c.level().getEntitiesOfClass(Player.class, area)) {
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
                    }
                    c.sound(SkofcraftSounds.HR_CHIME.get(), 1.0F, 1.0F);
                    c.say("swiss_neutrality_block", 3);
                }));

        gag("health_insurance_block", () -> new Spec(stone(MapColor.COLOR_LIGHT_BLUE))
                .use(60, c -> {
                    if (!takeDiamond(c)) {
                        c.sound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
                        c.say("health_insurance_block.uninsured", 3);
                        return;
                    }
                    c.effect(MobEffects.REGENERATION, 60, 0);
                    c.sound(SkofcraftSounds.COIN.get(), 1.0F, 0.7F);
                    c.say("health_insurance_block.paid", 4);
                    SkofcraftAdvancements.award(c.player(), SkofcraftAdvancements.DEDUCTIBLE);
                }));

        gag("postbus_stop", () -> new Spec(metal(MapColor.COLOR_YELLOW)).box(3.0D, 0.0D, 5.0D, 13.0D, 16.0D, 11.0D)
                .use(40, c -> {
                    if (c.level().getBlockTicks().hasScheduledTick(c.pos(), c.state().getBlock())) {
                        c.say("postbus_stop.waiting", 3);
                        return;
                    }
                    c.level().scheduleTick(c.pos(), c.state().getBlock(), POSTBUS_DELAY_TICKS);
                    c.say("postbus_stop.next", 3);
                })
                .tick(0, (level, pos, state, random) -> {
                    level.playSound(null, pos, SkofcraftSounds.POSTBUS_HORN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.getPlayers(player -> player.blockPosition().closerThan(pos, 48.0D))
                            .forEach(player -> GagBlock.say(player, "postbus_stop.gone", 3));
                    return 0;
                }));

        // The tobacco industry

        gag("load_bearing_snus", () -> new Spec(metal(MapColor.COLOR_LIGHT_BLUE)).box(3.0D, 0.0D, 3.0D, 13.0D, 5.0D, 13.0D)
                .broken(c -> {
                    if (collapse(c.level(), c.pos())) {
                        c.sound(SkofcraftSounds.HULL_CLANG.get(), 1.5F, 0.6F);
                        c.say("load_bearing_snus", 2);
                        SkofcraftAdvancements.award(c.player(), SkofcraftAdvancements.LOAD_BEARING);
                    }
                }));

        gag("strategic_snus_reserve", () -> new Spec(stone(MapColor.COLOR_BROWN))
                .use(600, c -> {
                    c.effect(MobEffects.MOVEMENT_SPEED, 300, 1);
                    c.effect(MobEffects.DIG_SPEED, 300, 1);
                    c.effect(MobEffects.CONFUSION, 100, 0);
                    c.sound(SkofcraftSounds.SIREN.get(), 0.6F, 1.2F);
                    c.say("strategic_snus_reserve", 3);
                }));

        // The office

        gag("hr_complaint_box", () -> new Spec(wood(MapColor.WOOD))
                .use(100, c -> {
                    c.sound(SkofcraftSounds.HR_CHIME.get(), 0.8F, 1.0F);
                    if (c.random().nextInt(10) == 0) {
                        c.say("hr_complaint_box.backfired", 2);
                        MandatoryHr.recordCrime(c.player(), "hr.skofcraft.crime.complaint");
                    } else {
                        c.say("hr_complaint_box.filed", 4);
                    }
                }));

        gag("office_microwave", () -> new Spec(metal(MapColor.METAL))
                .use(100, SkofcraftGagBlocks::microwave));

        gag("swivel_chair", () -> new Spec(wood(MapColor.COLOR_BLACK)).box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D)
                .use(5, c -> {
                    Player player = c.player();
                    c.player().teleportTo(c.level(), player.getX(), player.getY(), player.getZ(),
                            player.getYRot() + 180.0F, player.getXRot());
                    c.sound(SkofcraftSounds.SWITCH_CLICK.get(), 0.6F, 0.6F);
                }));

        gag("thought_leader_block", () -> new Spec(wood(MapColor.COLOR_BLUE))
                .use(100, c -> {
                    c.effect(MobEffects.DIG_SPEED, 200, 0);
                    c.sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
                    c.say("thought_leader_block", 8);
                }));

        gag("unpaid_intern_box", () -> new Spec(wood(MapColor.WOOD))
                .use(1200, c -> {
                    Item[] errands = {Items.STICK, Items.DIRT, Items.PAPER, Items.ROTTEN_FLESH, Items.POISONOUS_POTATO};
                    boolean coffee = c.random().nextInt(20) == 0;
                    c.give(new ItemStack(coffee ? SkofcraftItems.SKOFEE.get() : errands[c.random().nextInt(errands.length)]));
                    c.sound(SoundEvents.VILLAGER_YES, 0.7F, 1.4F);
                    c.say(coffee ? "unpaid_intern_box.coffee" : "unpaid_intern_box.errand", coffee ? 1 : 4);
                }));

        gag("compressed_monday", () -> new Spec(stone(MapColor.COLOR_GRAY))
                .step(100, c -> {
                    DayOfWeek today = LocalDate.now().getDayOfWeek();
                    if (today == DayOfWeek.FRIDAY) {
                        c.say("compressed_monday.friday", 1);
                        return;
                    }
                    int weight = today == DayOfWeek.MONDAY ? 2 : 0;
                    c.effect(MobEffects.MOVEMENT_SLOWDOWN, 200, weight);
                    c.effect(MobEffects.DIG_SLOWDOWN, 200, weight);
                    c.say(today == DayOfWeek.MONDAY ? "compressed_monday.monday" : "compressed_monday.other", 2);
                }));

        gag("passive_aggressive_note", () -> new Spec(paper(MapColor.TERRACOTTA_WHITE)).box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D)
                .use(40, c -> {
                    c.sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.2F);
                    c.say("passive_aggressive_note", 8);
                }));

        gag("server_lag_block", () -> new Spec(stone(MapColor.COLOR_BLACK))
                .step(60, SkofcraftGagBlocks::rubberband));

        gag("missing_texture_block", () -> new Spec(stone(MapColor.COLOR_MAGENTA))
                .use(40, c -> c.say("missing_texture_block", 4)));

        // Things that are lying to you

        gag("definitely_stone", () -> new Spec(stone(MapColor.STONE))
                .step(200, c -> {
                    c.sound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
                    if (c.random().nextInt(4) == 0) {
                        c.say("definitely_stone", 3);
                    }
                }));

        gag("fake_diamond_ore", () -> new Spec(stone(MapColor.STONE))
                .broken(c -> {
                    c.sound(SkofcraftSounds.EVIL_CACKLE.get(), 0.8F, 1.2F);
                    c.say("fake_diamond_ore", 3);
                    SkofcraftAdvancements.award(c.player(), SkofcraftAdvancements.SCAMMED);
                }));

        gag("indoor_grass", () -> new Spec(soft(MapColor.GRASS))
                .use(200, c -> {
                    PhoneAddiction.reduce(c.player(), 2);
                    c.sound(SoundEvents.GRASS_STEP, 1.0F, 1.4F);
                    c.say("indoor_grass", 4);
                }));

        gag("certified_fireproof_block", () -> new Spec(stone(MapColor.COLOR_ORANGE))
                .flammable(100, 100)
                .use(40, c -> {
                    BlockPos above = c.pos().above();
                    if (c.level().isEmptyBlock(above)) {
                        c.level().setBlockAndUpdate(above, BaseFireBlock.getState(c.level(), above));
                    }
                    c.sound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F);
                    c.say("certified_fireproof_block", 3);
                })
                .step(40, c -> c.player().setSecondsOnFire(3)));

        gag("knockoff_excalibur", () -> new Spec(stone(MapColor.STONE)).box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D)
                .use(0, c -> {
                    ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
                    sword.setHoverName(Component.translatable("item.skofcraft.knockoff_excalibur"));
                    sword.setDamageValue(sword.getMaxDamage() - 1);
                    c.level().removeBlock(c.pos(), false);
                    c.give(sword);
                    c.sound(SkofcraftSounds.SWORD_CLANG.get(), 0.8F, 1.6F);
                    c.say("knockoff_excalibur", 3);
                }));

        gag("amazooon_package", () -> new Spec(paper(MapColor.WOOD))
                .use(0, c -> {
                    c.level().removeBlock(c.pos(), false);
                    c.give(new ItemStack(Items.PAPER, 2));
                    c.sound(SkofcraftSounds.CRATE_OPEN.get(), 1.0F, 1.2F);
                    int roll = c.random().nextInt(50);
                    if (roll == 0) {
                        c.give(new ItemStack(Items.DIAMOND));
                        c.say("amazooon_package.diamond", 1);
                    } else if (roll < 38) {
                        c.give(new ItemStack(c.state().getBlock()));
                        c.say("amazooon_package.box", 3);
                    } else {
                        c.say("amazooon_package.empty", 2);
                    }
                }));

        // Things that hurt you, usually on purpose

        gag("ejection_seat", () -> new Spec(metal(MapColor.COLOR_GRAY)).box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D)
                .use(60, c -> {
                    c.push(new Vec3(0.0D, 3.0D, 0.0D));
                    c.sound(SkofcraftSounds.ENGINE_IGNITE.get(), 1.5F, 1.2F);
                    if (c.random().nextInt(10) == 0) {
                        c.say("ejection_seat.no_chute", 1);
                    } else {
                        c.effect(MobEffects.SLOW_FALLING, 300, 0);
                        c.say("ejection_seat.chute", 2);
                    }
                }));

        gag("banana_peel", () -> new Spec(paper(MapColor.COLOR_YELLOW).noCollission().instabreak().pushReaction(PushReaction.DESTROY))
                .box(2.0D, 0.0D, 2.0D, 14.0D, 1.0D, 14.0D)
                .inside(40, c -> {
                    Vec3 look = c.player().getLookAngle();
                    c.push(new Vec3(look.x * 1.6D, 0.4D, look.z * 1.6D));
                    c.effect(MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                    c.sound(SkofcraftSounds.TRIP_THUD.get(), 1.0F, 1.3F);
                    c.say("banana_peel", 3);
                }));

        gag("wet_floor", () -> new Spec(stone(MapColor.COLOR_YELLOW).friction(0.989F))
                .step(100, c -> {
                    if (c.random().nextInt(3) == 0) {
                        c.say("wet_floor", 3);
                    }
                }));

        gag("whoopee_cushion", () -> new Spec(soft(MapColor.COLOR_PINK).strength(0.3F)).box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D)
                .inside(20, c -> {
                    c.sound(SoundEvents.PUFFER_FISH_BLOW_OUT, 1.5F, 0.5F + c.random().nextFloat() * 0.2F);
                    if (c.random().nextInt(4) == 0) {
                        c.say("whoopee_cushion", 3);
                    }
                }));

        gag("bouncy_castle", () -> new Spec(soft(MapColor.COLOR_RED)).bounce(1.15F));

        gag("emergency_exit", () -> new Spec(wood(MapColor.COLOR_GREEN))
                .use(40, c -> {
                    BlockPos top = c.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, c.pos());
                    if (top.getY() <= c.pos().getY() + 1) {
                        c.say("emergency_exit.outside", 2);
                        return;
                    }
                    c.player().teleportTo(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D);
                    c.level().playSound(null, top, SkofcraftSounds.EVIL_WARP.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
                    c.say("emergency_exit.left", 2);
                }));

        gag("slightly_radioactive_barrel", () -> new Spec(metal(MapColor.COLOR_LIGHT_GREEN).lightLevel(state -> 9))
                .box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D)
                .use(100, c -> {
                    c.effect(MobEffects.DIG_SPEED, 300, 1);
                    c.effect(MobEffects.POISON, 60, 0);
                    c.sound(SkofcraftSounds.GULP.get(), 1.0F, 0.8F);
                    c.say("slightly_radioactive_barrel", 3);
                })
                .ambient((level, pos, state, random) -> {
                    level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                            8, 0.3D, 0.3D, 0.3D, 0.0D);
                    for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(4.0D))) {
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200));
                        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200));
                    }
                }));

        // Things that want your diamonds or your attention

        gag("loot_box", () -> new Spec(metal(MapColor.GOLD))
                .use(40, SkofcraftGagBlocks::openLootBox));

        gag("router", () -> new Spec(metal(MapColor.COLOR_BLACK).lightLevel(state -> 4)).box(3.0D, 0.0D, 4.0D, 13.0D, 9.0D, 12.0D)
                .use(40, c -> {
                    c.sound(SkofcraftSounds.UI_CLICK.get(), 1.0F, 1.0F);
                    c.say("router", 5);
                })
                .ambient((level, pos, state, random) ->
                        level.playSound(null, pos, SkofcraftSounds.PHONE_NOTIFY.get(), SoundSource.BLOCKS, 0.7F, 1.0F)));

        gag("smoke_detector", () -> new Spec(metal(MapColor.TERRACOTTA_WHITE)).box(4.0D, 13.0D, 4.0D, 12.0D, 16.0D, 12.0D)
                .use(40, c -> {
                    chirp(c.level(), c.pos());
                    c.say("smoke_detector", 3);
                })
                .tick(200, (level, pos, state, random) -> {
                    chirp(level, pos);
                    return 400 + random.nextInt(1200);
                }));

        gag("launch_code_note", () -> new Spec(paper(MapColor.COLOR_YELLOW)).box(4.0D, 0.0D, 4.0D, 12.0D, 1.0D, 12.0D)
                .use(40, c -> {
                    c.sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.5F);
                    c.say("launch_code_note", 3);
                }));

        // Companions

        gag("emotional_support_anvil", () -> new Spec(metal(MapColor.COLOR_PINK)).box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D)
                .use(60, c -> {
                    PhoneAddiction.reduce(c.player(), 3);
                    if (c.random().nextInt(20) == 0) {
                        c.player().hurt(c.level().damageSources().generic(), 2.0F);
                        c.sound(SoundEvents.ANVIL_LAND, 0.5F, 1.0F);
                        c.say("emotional_support_anvil.ouch", 1);
                        return;
                    }
                    c.effect(MobEffects.REGENERATION, 60, 0);
                    c.sound(SoundEvents.ANVIL_PLACE, 0.2F, 1.6F);
                    c.say("emotional_support_anvil.hug", 4);
                }));

        gag("debugging_duck", () -> new Spec(soft(MapColor.COLOR_YELLOW)).box(4.0D, 0.0D, 3.0D, 12.0D, 11.0D, 14.0D)
                .use(40, c -> {
                    c.sound(SoundEvents.PARROT_AMBIENT, 1.0F, 1.6F);
                    c.say("debugging_duck", 8);
                }));

        gag("cat_loaf", () -> new Spec(soft(MapColor.COLOR_ORANGE)).box(3.0D, 0.0D, 2.0D, 13.0D, 9.0D, 14.0D)
                .use(60, c -> {
                    ItemStack held = c.player().getMainHandItem();
                    if (!held.isEmpty() && c.random().nextInt(5) == 0) {
                        c.player().drop(held.split(1), true);
                        c.sound(SoundEvents.CAT_HISS, 1.0F, 1.0F);
                        c.say("cat_loaf.knocked", 2);
                        return;
                    }
                    c.effect(MobEffects.REGENERATION, 40, 0);
                    c.sound(SoundEvents.CAT_PURR, 1.0F, 1.0F);
                    c.say("cat_loaf.purr", 3);
                }));

        gag("judgmental_block", () -> new Spec(stone(MapColor.STONE))
                .use(60, c -> {
                    List<String> verdicts = new ArrayList<>();
                    verdicts.add("general");
                    if (c.player().getHealth() <= 4.0F) {
                        verdicts.add("low_health");
                    }
                    if (c.player().getArmorValue() == 0) {
                        verdicts.add("no_armor");
                    }
                    if (c.player().getMainHandItem().is(Items.DIRT)) {
                        verdicts.add("dirt");
                    }
                    if (c.player().getMainHandItem().isEmpty()) {
                        verdicts.add("empty_hand");
                    }
                    if (c.player().getY() < 0.0D) {
                        verdicts.add("deep");
                    }
                    if (PhoneAddiction.level(c.player()) >= 50) {
                        verdicts.add("phone");
                    }
                    String verdict = verdicts.get(c.random().nextInt(verdicts.size()));
                    c.sound(SoundEvents.VILLAGER_NO, 0.8F, 0.6F);
                    c.say("judgmental_block." + verdict, verdict.equals("general") ? 4 : 1);
                }));

        gag("reverse_sand", () -> new Spec(soft(MapColor.SAND).sound(SoundType.SAND).strength(0.5F))
                .tick(2, (level, pos, state, random) -> {
                    BlockPos above = pos.above();
                    if (level.isOutsideBuildHeight(above) || !level.isEmptyBlock(above)) {
                        return 10;
                    }
                    // Placing it above schedules that block's own tick, so this one is done
                    level.setBlockAndUpdate(above, state);
                    level.removeBlock(pos, false);
                    return 0;
                }));
    }

    private SkofcraftGagBlocks() {}

    private static void gag(String name, Supplier<Spec> spec) {
        var block = BLOCKS.register(name, () -> new GagBlock(name, spec.get()));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static BlockBehaviour.Properties soft(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.8F).sound(SoundType.WOOL);
    }

    private static BlockBehaviour.Properties paper(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.3F).sound(SoundType.GRASS);
    }

    private static BlockBehaviour.Properties wood(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.0F).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties stone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(1.5F, 6.0F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metal(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(3.0F, 6.0F).sound(SoundType.METAL);
    }

    private static boolean takeDiamond(Ctx c) {
        Container inventory = c.player().getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.DIAMOND)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Drops the ceiling above the block, a few columns wide. Stops at air, fluids, containers and bedrock. */
    private static boolean collapse(ServerLevel level, BlockPos pos) {
        boolean fell = false;
        for (int x = -COLLAPSE_RADIUS; x <= COLLAPSE_RADIUS; x++) {
            for (int z = -COLLAPSE_RADIUS; z <= COLLAPSE_RADIUS; z++) {
                BlockPos.MutableBlockPos cursor = pos.offset(x, 1, z).mutable();
                for (int y = 0; y < COLLAPSE_HEIGHT; y++) {
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()
                            || state.getDestroySpeed(level, cursor) < 0.0F) {
                        break;
                    }
                    FallingBlockEntity.fall(level, cursor.immutable(), state);
                    fell = true;
                    cursor.move(0, 1, 0);
                }
            }
        }
        return fell;
    }

    private static void microwave(Ctx c) {
        ItemStack held = c.player().getMainHandItem();
        if (held.is(Tags.Items.INGOTS) || held.is(Tags.Items.NUGGETS)) {
            held.shrink(1);
            c.level().explode(null, c.pos().getX() + 0.5D, c.pos().getY() + 1.0D, c.pos().getZ() + 0.5D,
                    1.5F, Level.ExplosionInteraction.NONE);
            c.say("office_microwave.metal", 2);
            return;
        }
        ItemStack cooked = !held.isEdible() ? ItemStack.EMPTY
                : c.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(held), c.level())
                        .map(recipe -> recipe.getResultItem(c.level().registryAccess()).copy())
                        .orElse(ItemStack.EMPTY);
        c.sound(SkofcraftSounds.COFFEE_DING.get(), 1.0F, 1.0F);
        if (cooked.isEmpty()) {
            c.say("office_microwave.empty", 3);
            return;
        }
        held.shrink(1);
        boolean burnt = c.random().nextInt(10) == 0;
        c.give(burnt ? new ItemStack(Items.CHARCOAL) : cooked);
        c.say(burnt ? "office_microwave.burnt" : "office_microwave.cooked", burnt ? 1 : 2);
    }

    private static void rubberband(Ctx c) {
        Player player = c.player();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D || player.getRandom().nextBoolean()) {
            return;
        }
        Vec3 back = flat.normalize().scale(-3.0D);
        if (c.level().noCollision(player, player.getBoundingBox().move(back))) {
            player.teleportTo(player.getX() + back.x, player.getY(), player.getZ() + back.z);
            c.say("server_lag_block", 4);
        }
    }

    /** Costs a diamond. On average it pays back about a fifth of one. */
    private static void openLootBox(Ctx c) {
        if (!takeDiamond(c)) {
            c.sound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            c.say("loot_box.broke", 2);
            return;
        }
        int roll = c.random().nextInt(100);
        if (roll == 0) {
            c.give(new ItemStack(Items.NETHERITE_SCRAP));
            c.sound(SkofcraftSounds.JACKPOT.get(), 1.0F, 1.0F);
            c.say("loot_box.jackpot", 1);
        } else if (roll < 10) {
            c.give(new ItemStack(Items.DIAMOND, 2));
            c.sound(SkofcraftSounds.COIN.get(), 1.0F, 1.2F);
            c.say("loot_box.win", 1);
        } else {
            c.give(new ItemStack(roll < 40 ? Items.STICK : Items.DIRT));
            c.sound(SkofcraftSounds.COFFEE_FAIL.get(), 1.0F, 1.0F);
            c.say("loot_box.common", 4);
        }
    }

    private static void chirp(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.get(), SoundSource.BLOCKS, 1.5F, 2.0F);
    }
}
