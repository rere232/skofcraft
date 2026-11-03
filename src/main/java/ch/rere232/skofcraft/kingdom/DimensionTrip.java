package ch.rere232.skofcraft.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/** Remembers where a player left from, across death and relogs, and takes them back there. */
final class DimensionTrip {
    private static final String HAS_RETURN = "HasReturn";
    private static final String DIMENSION = "Dimension";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String YAW = "Yaw";
    private static final String PITCH = "Pitch";

    private DimensionTrip() {}

    /** Forge copies this tag across death; the rest of the player's data is lost. */
    static CompoundTag record(Player player, String name) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        CompoundTag record = persisted.getCompound(name);
        persisted.put(name, record);
        return record;
    }

    static void remember(ServerPlayer player, CompoundTag record) {
        record.putBoolean(HAS_RETURN, true);
        record.putString(DIMENSION, player.level().dimension().location().toString());
        record.putDouble(X, player.getX());
        record.putDouble(Y, player.getY());
        record.putDouble(Z, player.getZ());
        record.putFloat(YAW, player.getYRot());
        record.putFloat(PITCH, player.getXRot());
    }

    /** Back where they left, or on the ground above it if something has been built or dropped there since. */
    static void goBack(ServerPlayer player, CompoundTag record) {
        MinecraftServer server = player.server;
        ServerLevel target = server.overworld();
        BlockPos spawn = target.getSharedSpawnPos();
        double x = spawn.getX() + 0.5D;
        double y = spawn.getY();
        double z = spawn.getZ() + 0.5D;
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (record.getBoolean(HAS_RETURN)) {
            ResourceLocation id = ResourceLocation.tryParse(record.getString(DIMENSION));
            ServerLevel saved = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (saved != null) {
                target = saved;
                x = record.getDouble(X);
                y = record.getDouble(Y);
                z = record.getDouble(Z);
                yaw = record.getFloat(YAW);
                pitch = record.getFloat(PITCH);
            }
        }
        record.remove(HAS_RETURN);

        BlockPos spot = BlockPos.containing(x, y, z);
        target.getChunkAt(spot);
        if (!target.noCollision(EntityType.PLAYER.getAABB(x, y, z))) {
            y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.getX(), spot.getZ());
        }
        player.stopRiding();
        player.teleportTo(target, x, y, z, yaw, pitch);
    }
}
