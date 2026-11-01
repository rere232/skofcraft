package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.network.ScreenFlipS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * A lever like any other, except flipping it turns the flipper's screen upside down. Flipping an Evil Lever again
 * turns it back. It sticks across relogs.
 */
public class EvilLeverBlock extends LeverBlock {
    private static final String FLIPPED_KEY = "SkofcraftScreenFlipped";

    public EvilLeverBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
            boolean flipped = !isFlipped(serverPlayer);
            serverPlayer.getPersistentData().putBoolean(FLIPPED_KEY, flipped);
            sync(serverPlayer);
            level.playSound(null, pos, SoundEvents.WITCH_CELEBRATE, SoundSource.BLOCKS, 0.8F, flipped ? 0.8F : 1.3F);
            serverPlayer.displayClientMessage(Component.translatable(flipped
                    ? "message.skofcraft.evil_lever_flipped" : "message.skofcraft.evil_lever_restored"), true);
            if (flipped) {
                SkofcraftAdvancements.award(serverPlayer, SkofcraftAdvancements.UPSIDE_DOWN);
            }
        }
        return result;
    }

    private static boolean isFlipped(ServerPlayer player) {
        return player.getPersistentData().getBoolean(FLIPPED_KEY);
    }

    private static void sync(ServerPlayer player) {
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScreenFlipS2CPacket(isFlipped(player)));
    }

    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {
        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player && isFlipped(player)) {
                sync(player);
            }
        }

        /** Persistent data is lost on death unless copied over. */
        @SubscribeEvent
        public static void onClone(PlayerEvent.Clone event) {
            if (event.isWasDeath() && event.getOriginal().getPersistentData().getBoolean(FLIPPED_KEY)) {
                event.getEntity().getPersistentData().putBoolean(FLIPPED_KEY, true);
            }
        }
    }
}
