package ch.rere232.skofcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * One class for every block in {@link ch.rere232.skofcraft.registry.SkofcraftGagBlocks}: what a gag does is described
 * by its {@link Spec}, so a new one is a registry entry and not another class. Hooks only ever run on the server.
 */
public class GagBlock extends Block {
    private static final float MAX_BOUNCE = 2.0F;

    private final String name;
    private final Spec spec;

    public GagBlock(String name, Spec spec) {
        super(spec.properties);
        this.name = name;
        this.spec = spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft." + name));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return spec.shape;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (spec.use == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            fire(spec.use, spec.useCooldown, serverLevel, pos, state, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (spec.step != null && level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
            fire(spec.step, spec.stepCooldown, serverLevel, pos, state, player);
        }
        super.stepOn(level, pos, state, entity);
    }

    /** Blocks under about 4 pixels tall never get {@link #stepOn}, because it looks 0.2 below the feet. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (spec.inside != null && level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
            fire(spec.inside, spec.insideCooldown, serverLevel, pos, state, player);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (spec.broken != null && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            spec.broken.run(new Ctx(serverLevel, pos, state, serverPlayer));
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (spec.firstTick > 0 && !oldState.is(this)) {
            level.scheduleTick(pos, this, spec.firstTick);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (spec.tick != null) {
            int next = spec.tick.run(level, pos, state, random);
            if (next > 0) {
                level.scheduleTick(pos, this, next);
            }
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return spec.ambient != null;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (spec.ambient != null) {
            spec.ambient.run(level, pos, state, random);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (spec.bounce > 0.0F) {
            // Sneaking stops the bounce, not the softness
            entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if (spec.bounce > 0.0F && !entity.isSuppressingBounce() && motion.y < 0.0D) {
            entity.setDeltaMovement(motion.x, Math.min(-motion.y * spec.bounce, MAX_BOUNCE), motion.z);
        } else {
            super.updateEntityAfterFallOn(level, entity);
        }
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return spec.flammability;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return spec.fireSpread;
    }

    /** The cooldown lives on the block's own item, so it shows on the hotbar and is kept per player. */
    private void fire(Action action, int cooldown, ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (cooldown > 0) {
            if (player.getCooldowns().isOnCooldown(asItem())) {
                return;
            }
            player.getCooldowns().addCooldown(asItem(), cooldown);
        }
        action.run(new Ctx(level, pos, state, player));
    }

    /** Sends a random one of {@code message.skofcraft.gag.<key>.0} up to {@code count - 1} to the action bar. */
    public static void say(ServerPlayer player, String key, int count) {
        player.displayClientMessage(Component.translatable(
                "message.skofcraft.gag." + key + "." + player.getRandom().nextInt(count)), true);
    }

    @FunctionalInterface
    public interface Action {
        void run(Ctx context);
    }

    /** Returns the delay until the next tick, or 0 to stop. */
    @FunctionalInterface
    public interface Tick {
        int run(ServerLevel level, BlockPos pos, BlockState state, RandomSource random);
    }

    @FunctionalInterface
    public interface Ambient {
        void run(ServerLevel level, BlockPos pos, BlockState state, RandomSource random);
    }

    /** A player doing something to a gag block. */
    public record Ctx(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        public RandomSource random() {
            return player.getRandom();
        }

        public void say(String key, int count) {
            GagBlock.say(player, key, count);
        }

        public void sound(SoundEvent sound, float volume, float pitch) {
            level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
        }

        public void effect(MobEffect effect, int ticks, int amplifier) {
            player.addEffect(new MobEffectInstance(effect, ticks, amplifier));
        }

        public void give(ItemStack stack) {
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }

        /** A player's own client decides where they are, so it has to be told about the push. */
        public void push(Vec3 motion) {
            player.setDeltaMovement(motion);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
        }
    }

    public static final class Spec {
        private final Properties properties;
        private VoxelShape shape = Shapes.block();
        private Action use;
        private int useCooldown;
        private Action step;
        private int stepCooldown;
        private Action inside;
        private int insideCooldown;
        private Action broken;
        private Tick tick;
        private int firstTick;
        private Ambient ambient;
        private float bounce;
        private int flammability;
        private int fireSpread;

        public Spec(Properties properties) {
            this.properties = properties;
        }

        /** Selection and collision box, in pixels, for a model smaller than a full cube. */
        public Spec box(double x1, double y1, double z1, double x2, double y2, double z2) {
            shape = Block.box(x1, y1, z1, x2, y2, z2);
            properties.noOcclusion();
            return this;
        }

        public Spec use(int cooldown, Action action) {
            useCooldown = cooldown;
            use = action;
            return this;
        }

        public Spec step(int cooldown, Action action) {
            stepCooldown = cooldown;
            step = action;
            return this;
        }

        public Spec inside(int cooldown, Action action) {
            insideCooldown = cooldown;
            inside = action;
            return this;
        }

        public Spec broken(Action action) {
            broken = action;
            return this;
        }

        /** Ticks by itself from {@code first} ticks after placement; 0 waits for something else to schedule one. */
        public Spec tick(int first, Tick action) {
            firstTick = first;
            tick = action;
            return this;
        }

        public Spec ambient(Ambient action) {
            ambient = action;
            return this;
        }

        /** Speed kept when landing: 1 bounces back as high, more gains height every bounce. Sneak to stop. */
        public Spec bounce(float factor) {
            bounce = factor;
            return this;
        }

        public Spec flammable(int burnChance, int spreadChance) {
            flammability = burnChance;
            fireSpread = spreadChance;
            return this;
        }
    }
}
