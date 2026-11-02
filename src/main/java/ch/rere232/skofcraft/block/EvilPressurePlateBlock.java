package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/** Looks like a pressure plate. Whoever steps on it is sent straight to the Nether roof. */
public class EvilPressurePlateBlock extends PressurePlateBlock {
    public EvilPressurePlateBlock(Properties properties) {
        super(Sensitivity.MOBS, properties, BlockSetType.POLISHED_BLACKSTONE);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof LivingEntity) || entity.isPassenger()
                || entity.isVehicle() || entity.isOnPortalCooldown() || !entity.canChangeDimensions()) {
            return;
        }
        ServerLevel nether = serverLevel.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            return;
        }
        serverLevel.playSound(null, pos, SkofcraftSounds.EVIL_CACKLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.2D, entity.getZ(), 20, 0.3D, 0.1D, 0.3D, 0.05D);
        sendToRoof(entity, serverLevel, nether);
    }

    private static void sendToRoof(Entity entity, ServerLevel from, ServerLevel nether) {
        double scale = DimensionType.getTeleportationScale(from.dimensionType(), nether.dimensionType());
        double x = Mth.floor(Mth.clamp(entity.getX() * scale, -29999000.0D, 29999000.0D)) + 0.5D;
        double z = Mth.floor(Mth.clamp(entity.getZ() * scale, -29999000.0D, 29999000.0D)) + 0.5D;
        // Load the chunk, then stand on top of the bedrock ceiling
        nether.getChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
        double y = nether.getLogicalHeight();
        while (y < nether.getMaxBuildHeight() - 2 && !nether.noCollision(entity, entity.getDimensions(entity.getPose()).makeBoundingBox(x, y, z))) {
            y++;
        }
        Vec3 target = new Vec3(x, y, z);

        entity.setPortalCooldown();
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(nether, x, y, z, player.getYRot(), player.getXRot());
            player.displayClientMessage(Component.translatable("message.skofcraft.evil_pressure_plate"), true);
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.ROOF_ACCESS);
        } else if (from == nether) {
            entity.teleportTo(x, y, z);
        } else {
            entity.changeDimension(nether, new ITeleporter() {
                @Override
                public PortalInfo getPortalInfo(Entity e, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                    return new PortalInfo(target, Vec3.ZERO, e.getYRot(), e.getXRot());
                }

                @Override
                public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
                    return false;
                }
            });
        }
        nether.playSound(null, x, y, z, SkofcraftSounds.EVIL_WARP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}
