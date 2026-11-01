package itemspawner.packet;

import itemspawner.world.RegionLoaderHelper;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;

/**
 * Pacote bidirecional para gerar e revelar partes ainda não geradas do mundo no Mapa:
 * - Modo Request (Cliente -> Servidor): Pede ao servidor para gerar as regiões (16x16) na área.
 * - Modo RegionSync (Servidor -> Cliente): Transmite os tiles e objetos de cada região 16x16
 *   gerada pelo servidor diretamente para o cliente e aciona ClientDiscoveredMap.tickDiscovery!
 */
public class PacketRevealMap extends Packet {

    public final boolean isRegionSync;
    public final int coordX;
    public final int coordY;
    public final int radiusTiles;
    public final short[] tiles;
    public final short[] objects;

    public PacketRevealMap(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.isRegionSync = reader.getNextBoolean();
        this.coordX = reader.getNextInt();
        this.coordY = reader.getNextInt();
        this.radiusTiles = reader.getNextInt();

        if (this.isRegionSync) {
            this.tiles = new short[256];
            this.objects = new short[256];
            for (int i = 0; i < 256; i++) {
                this.tiles[i] = reader.getNextShort();
                this.objects[i] = reader.getNextShort();
            }
        } else {
            this.tiles = null;
            this.objects = null;
        }
    }

    /**
     * Construtor usado pelo Cliente para solicitar a geração e revelação de uma área ao Servidor.
     */
    public PacketRevealMap(int centerX, int centerY, int radiusTiles) {
        this.isRegionSync = false;
        this.coordX = centerX;
        this.coordY = centerY;
        this.radiusTiles = Math.max(32, Math.min(radiusTiles, 320));
        this.tiles = null;
        this.objects = null;

        PacketWriter writer = new PacketWriter(this);
        writer.putNextBoolean(false);
        writer.putNextInt(this.coordX);
        writer.putNextInt(this.coordY);
        writer.putNextInt(this.radiusTiles);
    }

    /**
     * Construtor usado pelo Servidor para enviar os dados reais de uma região 16x16 recém-gerada ao Cliente.
     */
    public PacketRevealMap(int regionX, int regionY, short[] tiles, short[] objects) {
        this.isRegionSync = true;
        this.coordX = regionX;
        this.coordY = regionY;
        this.radiusTiles = 16;
        this.tiles = tiles;
        this.objects = objects;

        PacketWriter writer = new PacketWriter(this);
        writer.putNextBoolean(true);
        writer.putNextInt(regionX);
        writer.putNextInt(regionY);
        writer.putNextInt(16);
        for (int i = 0; i < 256; i++) {
            writer.putNextShort(tiles != null && i < tiles.length ? tiles[i] : 0);
            writer.putNextShort(objects != null && i < objects.length ? objects[i] : 0);
        }
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient serverClient) {
        if (this.isRegionSync || serverClient == null || serverClient.playerMob == null) {
            return;
        }

        PlayerMob player = serverClient.playerMob;
        Level level = serverClient.getLevel();
        if (level == null) {
            return;
        }

        int cx = (this.coordX != 0 || this.coordY != 0) ? this.coordX : player.getTileX();
        int cy = (this.coordX != 0 || this.coordY != 0) ? this.coordY : player.getTileY();

        int startX = cx - this.radiusTiles;
        int startY = cy - this.radiusTiles;
        int endX = cx + this.radiusTiles;
        int endY = cy + this.radiusTiles;

        if (level.tileWidth > 0) {
            startX = Math.max(0, startX);
            endX = Math.min(level.tileWidth - 1, endX);
        }
        if (level.tileHeight > 0) {
            startY = Math.max(0, startY);
            endY = Math.min(level.tileHeight - 1, endY);
        }

        int minRx = Math.floorDiv(startX, RegionLoaderHelper.REGION_SIZE);
        int maxRx = Math.floorDiv(endX, RegionLoaderHelper.REGION_SIZE);
        int minRy = Math.floorDiv(startY, RegionLoaderHelper.REGION_SIZE);
        int maxRy = Math.floorDiv(endY, RegionLoaderHelper.REGION_SIZE);

        for (int rx = minRx; rx <= maxRx; rx++) {
            for (int ry = minRy; ry <= maxRy; ry++) {
                int baseTx = rx * RegionLoaderHelper.REGION_SIZE;
                int baseTy = ry * RegionLoaderHelper.REGION_SIZE;

                // 1. Força o servidor a gerar a região 16x16 caso ainda não exista
                Object regionObj = RegionLoaderHelper.ensureRegionGeneratedByTile(level, baseTx + 8, baseTy + 8);

                // 2. Envia o PacketRegionData nativo (se disponível)
                RegionLoaderHelper.sendRegionPacketToClient(serverClient, level, regionObj, rx, ry);

                // 3. Coleta os 16x16 tiles e objetos gerados no Servidor e envia ao Cliente para garantir 100% de carregamento no mapa
                short[] regionTiles = new short[256];
                short[] regionObjects = new short[256];
                int idx = 0;
                for (int dy = 0; dy < 16; dy++) {
                    for (int dx = 0; dx < 16; dx++) {
                        int tx = baseTx + dx;
                        int ty = baseTy + dy;
                        regionTiles[idx] = (short) level.getTileID(tx, ty);
                        regionObjects[idx] = (short) level.getObjectID(tx, ty);
                        idx++;
                    }
                }
                serverClient.sendPacket(new PacketRevealMap(rx, ry, regionTiles, regionObjects));
            }
        }
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
        if (client == null || !this.isRegionSync || this.tiles == null || this.objects == null) {
            return;
        }
        Level level = client.getLevel();
        if (level == null) {
            return;
        }

        int baseTx = this.coordX * RegionLoaderHelper.REGION_SIZE;
        int baseTy = this.coordY * RegionLoaderHelper.REGION_SIZE;

        // 1. Garante que a região 16x16 está alocada no Client Level
        RegionLoaderHelper.ensureRegionGeneratedByTile(level, baseTx + 8, baseTy + 8);

        // 2. Preenche os tiles e objetos recebidos do Servidor caso a região estivesse vazia no Cliente
        int idx = 0;
        for (int dy = 0; dy < 16; dy++) {
            for (int dx = 0; dx < 16; dx++) {
                int tx = baseTx + dx;
                int ty = baseTy + dy;
                short tileId = this.tiles[idx];
                short objId = this.objects[idx];
                idx++;

                try {
                    if (tileId > 0 && level.getTileID(tx, ty) <= 0) {
                        level.setTile(tx, ty, tileId);
                    }
                    if (objId > 0 && level.getObjectID(tx, ty) <= 0) {
                        level.setObject(tx, ty, objId);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 3. Revela imediatamente os 4 quadrantes da região 16x16 no Mapa e Minimapa do Cliente
        try {
            if (client.levelManager != null && client.levelManager.getMap() != null) {
                ClientDiscoveredMap map = client.levelManager.getMap();
                map.tickDiscovery(level, baseTx + 4, baseTy + 4);
                map.tickDiscovery(level, baseTx + 12, baseTy + 4);
                map.tickDiscovery(level, baseTx + 4, baseTy + 12);
                map.tickDiscovery(level, baseTx + 12, baseTy + 12);
            }
        } catch (Exception ignored) {
        }
    }
}
