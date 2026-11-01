package ch.rere232.skofcraft.space;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** The satellites in orbit over one dimension, saved with that dimension. */
public class OrbitData extends SavedData {
    private static final String NAME = "skofcraft_orbit";
    /** Ticks a strike satellite needs between two strikes. */
    public static final int STRIKE_COOLDOWN = 20 * 60;

    public static final class Satellite {
        public final SatelliteType type;
        public final UUID owner;
        public final long launched;
        private long lastStrike;

        Satellite(SatelliteType type, UUID owner, long launched, long lastStrike) {
            this.type = type;
            this.owner = owner;
            this.launched = launched;
            this.lastStrike = lastStrike;
        }

        public long readyAt() {
            return lastStrike + STRIKE_COOLDOWN;
        }
    }

    private final List<Satellite> satellites = new ArrayList<>();

    public static OrbitData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OrbitData::load, OrbitData::new, NAME);
    }

    public void add(SatelliteType type, UUID owner, long now) {
        satellites.add(new Satellite(type, owner, now, now - STRIKE_COOLDOWN));
        setDirty();
    }

    public List<Satellite> satellites() {
        return Collections.unmodifiableList(satellites);
    }

    public int count(SatelliteType type) {
        int count = 0;
        for (Satellite satellite : satellites) {
            if (satellite.type == type) {
                count++;
            }
        }
        return count;
    }

    /** Strike satellites this player may fire: their own, or their team-mates'. */
    public List<Satellite> strikeSatellitesFor(ServerPlayer player) {
        List<Satellite> usable = new ArrayList<>();
        for (Satellite satellite : satellites) {
            if (satellite.type != SatelliteType.STRIKE) {
                continue;
            }
            if (satellite.owner.equals(player.getUUID())) {
                usable.add(satellite);
                continue;
            }
            Player owner = player.server.getPlayerList().getPlayer(satellite.owner);
            if (owner != null && owner.isAlliedTo(player)) {
                usable.add(satellite);
            }
        }
        return usable;
    }

    /** The usable strike satellite that recharged first, if any is ready now. */
    @Nullable
    public Satellite readyStrike(ServerPlayer player, long now) {
        Satellite best = null;
        for (Satellite satellite : strikeSatellitesFor(player)) {
            if (satellite.readyAt() <= now && (best == null || satellite.readyAt() < best.readyAt())) {
                best = satellite;
            }
        }
        return best;
    }

    public void fire(Satellite satellite, long now) {
        satellite.lastStrike = now;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Satellite satellite : satellites) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Type", satellite.type.ordinal());
            entry.putUUID("Owner", satellite.owner);
            entry.putLong("Launched", satellite.launched);
            entry.putLong("LastStrike", satellite.lastStrike);
            list.add(entry);
        }
        tag.put("Satellites", list);
        return tag;
    }

    private static OrbitData load(CompoundTag tag) {
        OrbitData data = new OrbitData();
        for (Tag element : tag.getList("Satellites", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            if (entry.hasUUID("Owner")) {
                data.satellites.add(new Satellite(SatelliteType.byId(entry.getInt("Type")), entry.getUUID("Owner"),
                        entry.getLong("Launched"), entry.getLong("LastStrike")));
            }
        }
        return data;
    }
}
