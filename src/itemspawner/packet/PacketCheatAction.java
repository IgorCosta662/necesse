package itemspawner.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.pickup.PickupEntity;
import necesse.level.maps.Level;

public class PacketCheatAction extends Packet {

    public enum CheatType {
        HEAL,
        RESTORE_MANA,
        RESTORE_HUNGER,
        TOGGLE_GODMODE,
        TOGGLE_SPEED,
        TOGGLE_INVIS,
        KILL_HOSTILES,
        CLEAR_DROPS,
        SET_DAY,
        SET_NIGHT
    }

    public final CheatType cheatType;

    public PacketCheatAction(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.cheatType = reader.getNextEnum(CheatType.class);
    }

    public PacketCheatAction(CheatType type) {
        this.cheatType = type;
        PacketWriter writer = new PacketWriter(this);
        writer.putNextEnum(type);
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient serverClient) {
        if (serverClient == null || serverClient.playerMob == null) {
            return;
        }

        PlayerMob player = serverClient.playerMob;
        Level level = serverClient.getLevel();

        switch (this.cheatType) {
            case HEAL:
                player.setHealth(player.getMaxHealth());
                player.sendHealthPacket(true);
                break;

            case RESTORE_MANA:
                player.setMana(player.getMaxMana());
                player.sendManaPacket(true);
                break;

            case RESTORE_HUNGER:
                player.addHunger(1.0f);
                player.sendHungerPacket();
                break;

            case TOGGLE_GODMODE:
                player.hasInvincibility = !player.hasInvincibility;
                if (player.hasInvincibility) {
                    player.buffManager.addBuff(new ActiveBuff(BuffRegistry.GODMODE_BUFF, player, 0, null), true);
                } else {
                    player.buffManager.removeBuff(BuffRegistry.GODMODE_BUFF, true);
                }
                break;

            case TOGGLE_SPEED:
                if (player.buffManager.hasBuff(BuffRegistry.DEBUG_SPEED)) {
                    player.buffManager.removeBuff(BuffRegistry.DEBUG_SPEED, true);
                } else {
                    player.buffManager.addBuff(new ActiveBuff(BuffRegistry.DEBUG_SPEED, player, 3600f, null), true);
                }
                break;

            case TOGGLE_INVIS:
                if (player.buffManager.hasBuff(BuffRegistry.DEBUG_INVIS)) {
                    player.buffManager.removeBuff(BuffRegistry.DEBUG_INVIS, true);
                } else {
                    player.buffManager.addBuff(new ActiveBuff(BuffRegistry.DEBUG_INVIS, player, 3600f, null), true);
                }
                break;

            case KILL_HOSTILES:
                if (level != null && level.entityManager != null) {
                    for (Mob mob : level.entityManager.mobs) {
                        if (mob != null && mob.isHostile) {
                            mob.remove();
                        }
                    }
                }
                break;

            case CLEAR_DROPS:
                if (level != null && level.entityManager != null) {
                    for (PickupEntity p : level.entityManager.pickups) {
                        if (p != null) {
                            p.remove();
                        }
                    }
                }
                break;

            case SET_DAY:
                if (server != null && server.world != null) {
                    server.world.setMorning();
                    server.world.simulateWorldTime(1000, true);
                }
                break;

            case SET_NIGHT:
                if (server != null && server.world != null) {
                    server.world.setNight();
                    server.world.simulateWorldTime(1000, true);
                }
                break;
        }
    }
}
