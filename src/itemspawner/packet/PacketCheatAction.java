package itemspawner.packet;

import java.util.ArrayList;
import java.util.List;
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
import necesse.inventory.PlayerInventory;
import necesse.level.maps.Level;

public class PacketCheatAction extends Packet {

    public enum CheatType {
        HEAL,
        RESTORE_MANA,
        RESTORE_HUNGER,
        RESTORE_ALL,
        CLEAR_DEBUFFS,
        TOGGLE_GODMODE,
        TOGGLE_SPEED,
        TOGGLE_INVIS,
        KILL_HOSTILES,
        CLEAR_DROPS,
        CLEAR_INVENTORY,
        SET_DAY,
        SET_NOON,
        SET_NIGHT
    }

    // Duração longa (24 horas em segundos) para buffs de toggle não expirarem no meio da sessão
    private static final float PERSISTENT_BUFF_DURATION = 86400f;

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
        if (serverClient == null || serverClient.playerMob == null || this.cheatType == null) {
            return;
        }

        PlayerMob player = serverClient.playerMob;
        Level level = serverClient.getLevel();

        switch (this.cheatType) {
            case HEAL:
                healPlayer(player);
                break;

            case RESTORE_MANA:
                restoreMana(player);
                break;

            case RESTORE_HUNGER:
                restoreHunger(player);
                break;

            case RESTORE_ALL:
                healPlayer(player);
                restoreMana(player);
                restoreHunger(player);
                clearNegativeBuffs(player);
                break;

            case CLEAR_DEBUFFS:
                clearNegativeBuffs(player);
                break;

            case TOGGLE_GODMODE:
                boolean enableGod = !player.buffManager.hasBuff(BuffRegistry.GODMODE_BUFF) && !player.hasInvincibility;
                player.hasInvincibility = enableGod;
                if (enableGod) {
                    player.buffManager.addBuff(new ActiveBuff(BuffRegistry.GODMODE_BUFF, player, PERSISTENT_BUFF_DURATION, null), true);
                    healPlayer(player);
                } else {
                    player.buffManager.removeBuff(BuffRegistry.GODMODE_BUFF, true);
                }
                break;

            case TOGGLE_SPEED:
                if (player.buffManager.hasBuff(BuffRegistry.DEBUG_SPEED)) {
                    player.buffManager.removeBuff(BuffRegistry.DEBUG_SPEED, true);
                } else {
                    player.buffManager.addBuff(new ActiveBuff(BuffRegistry.DEBUG_SPEED, player, PERSISTENT_BUFF_DURATION, null), true);
                }
                break;

            case TOGGLE_INVIS:
                if (player.buffManager.hasBuff(BuffRegistry.DEBUG_INVIS)) {
                    player.buffManager.removeBuff(BuffRegistry.DEBUG_INVIS, true);
                } else {
                    player.buffManager.addBuff(new ActiveBuff(BuffRegistry.DEBUG_INVIS, player, PERSISTENT_BUFF_DURATION, null), true);
                }
                break;

            case KILL_HOSTILES:
                if (level != null && level.entityManager != null && level.entityManager.mobs != null) {
                    // Cópia defensiva para evitar ConcurrentModificationException durante a remoção
                    List<Mob> toRemove = new ArrayList<>();
                    for (Mob mob : level.entityManager.mobs) {
                        if (mob != null && !mob.removed() && mob.isHostile && !mob.isPlayer) {
                            toRemove.add(mob);
                        }
                    }
                    for (Mob mob : toRemove) {
                        mob.remove(0, 0, null, true);
                    }
                }
                break;

            case CLEAR_DROPS:
                if (level != null && level.entityManager != null && level.entityManager.pickups != null) {
                    // Cópia defensiva para evitar ConcurrentModificationException durante a remoção
                    List<PickupEntity> toRemove = new ArrayList<>();
                    for (PickupEntity pickup : level.entityManager.pickups) {
                        if (pickup != null && !pickup.removed()) {
                            toRemove.add(pickup);
                        }
                    }
                    for (PickupEntity pickup : toRemove) {
                        pickup.remove();
                    }
                }
                break;

            case CLEAR_INVENTORY:
                if (player.getInv() != null && player.getInv().main != null) {
                    PlayerInventory mainInv = player.getInv().main;
                    // Limpa apenas os slots da mochila principal (a partir do slot 10, preservando a hotbar 0-9 e slots trancados)
                    for (int slot = 10; slot < mainInv.getSize(); slot++) {
                        if (!mainInv.isItemLocked(slot)) {
                            mainInv.clearSlot(slot);
                        }
                    }
                    player.getInv().markFullDirty();
                }
                break;

            case SET_DAY:
                if (server != null && server.world != null) {
                    server.world.setMorning();
                    server.world.simulateWorldTime(1000, true);
                }
                break;

            case SET_NOON:
                if (server != null && server.world != null) {
                    server.world.setMidday();
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

    private static void healPlayer(PlayerMob player) {
        player.setHealth(player.getMaxHealth());
        player.sendHealthPacket(true);
    }

    private static void restoreMana(PlayerMob player) {
        player.setMana(player.getMaxMana());
        player.sendManaPacket(true);
    }

    private static void restoreHunger(PlayerMob player) {
        player.addHunger(1.0f);
        player.sendHungerPacket();
    }

    private static void clearNegativeBuffs(PlayerMob player) {
        if (player.buffManager == null) {
            return;
        }
        List<ActiveBuff> buffsToRemove = new ArrayList<>();
        for (ActiveBuff ab : player.buffManager.getBuffs().values()) {
            if (ab != null && ab.buff != null && ab.buff.isImportant && ab.buff.canCancel == false) {
                buffsToRemove.add(ab);
            }
        }
        for (ActiveBuff ab : buffsToRemove) {
            player.buffManager.removeBuff(ab.buff, true);
        }
    }
}

