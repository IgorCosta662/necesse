package itemspawner.packet;

import itemspawner.world.RegionLoaderHelper;
import itemspawner.world.VillageGenerator;
import itemspawner.world.VillageGenerator.StructureType;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;

public class PacketBuildStructure extends Packet {

    public final StructureType structureType;
    public final int centerX;
    public final int centerY;

    public PacketBuildStructure(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.structureType = reader.getNextEnum(StructureType.class);
        this.centerX = reader.getNextInt();
        this.centerY = reader.getNextInt();
    }

    public PacketBuildStructure(StructureType structureType, int centerX, int centerY) {
        this.structureType = structureType;
        this.centerX = centerX;
        this.centerY = centerY;
        PacketWriter writer = new PacketWriter(this);
        writer.putNextEnum(structureType);
        writer.putNextInt(centerX);
        writer.putNextInt(centerY);
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient serverClient) {
        if (serverClient == null || serverClient.playerMob == null || this.structureType == null) {
            return;
        }

        PlayerMob player = serverClient.playerMob;
        Level level = serverClient.getLevel();
        if (level == null) {
            return;
        }

        int targetTileX = (this.centerX != 0 || this.centerY != 0) ? this.centerX : player.getTileX();
        int targetTileY = (this.centerX != 0 || this.centerY != 0) ? this.centerY : player.getTileY();

        int minX = targetTileX - 32;
        int minY = targetTileY - 36;
        int maxX = targetTileX + 32;
        int maxY = targetTileY + 24;

        // 1. Garante que todas as regiões 16x16 da área (mesmo as ainda não exploradas) sejam geradas antes da construção
        RegionLoaderHelper.ensureAreaGeneratedAndSyncedOnServer(server, serverClient, level, minX, minY, maxX, maxY);

        // 2. Executa a construção na instância do servidor (salva no mundo + spawna NPCs)
        VillageGenerator.generate(level, this.structureType, targetTileX, targetTileY, true);

        // 3. Reenvia os dados das regiões modificadas para o cliente e retransmite PacketBuildStructure
        RegionLoaderHelper.ensureAreaGeneratedAndSyncedOnServer(server, serverClient, level, minX, minY, maxX, maxY);

        if (server != null && server.network != null) {
            server.network.sendToAllClients(new PacketBuildStructure(this.structureType, targetTileX, targetTileY));
        }

        // 4. Também envia os blocos das regiões via PacketRevealMap para atualizar imediatamente áreas antes não geradas no mapa
        int minRx = Math.floorDiv(minX, RegionLoaderHelper.REGION_SIZE);
        int maxRx = Math.floorDiv(maxX, RegionLoaderHelper.REGION_SIZE);
        int minRy = Math.floorDiv(minY, RegionLoaderHelper.REGION_SIZE);
        int maxRy = Math.floorDiv(maxY, RegionLoaderHelper.REGION_SIZE);

        for (int rx = minRx; rx <= maxRx; rx++) {
            for (int ry = minRy; ry <= maxRy; ry++) {
                int baseTx = rx * RegionLoaderHelper.REGION_SIZE;
                int baseTy = ry * RegionLoaderHelper.REGION_SIZE;
                short[] regionTiles = new short[256];
                short[] regionObjects = new short[256];
                int idx = 0;
                for (int dy = 0; dy < 16; dy++) {
                    for (int dx = 0; dx < 16; dx++) {
                        regionTiles[idx] = (short) level.getTileID(baseTx + dx, baseTy + dy);
                        regionObjects[idx] = (short) level.getObjectID(baseTx + dx, baseTy + dy);
                        idx++;
                    }
                }
                serverClient.sendPacket(new PacketRevealMap(rx, ry, regionTiles, regionObjects));
            }
        }
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
        if (client == null || this.structureType == null) {
            return;
        }
        Level level = client.getLevel();
        if (level == null) {
            return;
        }

        int minX = this.centerX - 32;
        int minY = this.centerY - 36;
        int maxX = this.centerX + 32;
        int maxY = this.centerY + 24;

        // 1. Garante que as regiões existem no Client Level
        RegionLoaderHelper.ensureAreaRegionsLoaded(level, minX, minY, maxX, maxY);

        // 2. Atualiza os tiles e objetos no cliente imediatamente
        VillageGenerator.generate(level, this.structureType, this.centerX, this.centerY, false);

        // 3. Revela toda a área construída no Mapa e Minimapa (com passagens extras para regiões recém-carregadas)
        RegionLoaderHelper.scheduleClientMapReveal(client, minX, minY, maxX, maxY);
    }
}
