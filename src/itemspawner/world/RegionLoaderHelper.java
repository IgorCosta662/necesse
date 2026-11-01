package itemspawner.world;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.network.Packet;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;
import necesse.level.maps.mapData.ClientDiscoveredMap;

/**
 * Utilitário avançado para o sistema de Regiões (16x16 tiles) do Necesse:
 * 1. Força o Servidor a gerar regiões ainda não exploradas (ungenerated chunks/regions).
 * 2. Sincroniza as regiões geradas do Servidor para o Cliente via PacketRegionData + pacotes de tiles.
 * 3. Garante que o ClientDiscoveredMap carregue e desenhe todas as regiões no Mapa e Minimapa.
 */
public class RegionLoaderHelper {

    public static final int REGION_SIZE = 16;

    private static boolean reflectionInitialized = false;
    private static Field regionManagerField;
    private static final List<Method> regionByTileMethods3 = new ArrayList<>();
    private static final List<Method> regionByTileMethods2 = new ArrayList<>();
    private static final List<Method> regionByCoordMethods3 = new ArrayList<>();
    private static final List<Method> regionByCoordMethods2 = new ArrayList<>();
    private static final List<Method> ensureLoadedMethods = new ArrayList<>();
    private static Constructor<?> packetRegionDataCtorRegion;
    private static Constructor<?> packetRegionDataCtorLevelCoords;

    // Agendamento de revelação progressiva no cliente conforme os pacotes de região chegam da rede
    private static int pendingRevealTicks = 0;
    private static int pendingStartX = 0;
    private static int pendingStartY = 0;
    private static int pendingEndX = 0;
    private static int pendingEndY = 0;

    private static synchronized void initReflection(Level level) {
        if (reflectionInitialized || level == null) {
            return;
        }
        reflectionInitialized = true;

        try {
            Class<?> curr = level.getClass();
            while (curr != null && curr != Object.class) {
                for (Field f : curr.getDeclaredFields()) {
                    if (f.getName().equals("regionManager") || f.getType().getSimpleName().toLowerCase().contains("regionmanager")) {
                        f.setAccessible(true);
                        regionManagerField = f;
                        break;
                    }
                }
                if (regionManagerField != null) {
                    break;
                }
                curr = curr.getSuperclass();
            }

            if (regionManagerField != null) {
                Object rmInstance = regionManagerField.get(level);
                Class<?> rmClass = rmInstance != null ? rmInstance.getClass() : regionManagerField.getType();
                Class<?> c = rmClass;
                while (c != null && c != Object.class) {
                    for (Method m : c.getDeclaredMethods()) {
                        m.setAccessible(true);
                        String name = m.getName().toLowerCase();
                        Class<?>[] params = m.getParameterTypes();

                        if (name.contains("regionbytile") && params.length == 3
                                && params[0] == int.class && params[1] == int.class && params[2] == boolean.class) {
                            regionByTileMethods3.add(m);
                        } else if (name.contains("regionbytile") && params.length == 2
                                && params[0] == int.class && params[1] == int.class) {
                            regionByTileMethods2.add(m);
                        } else if (name.contains("region") && params.length == 3
                                && params[0] == int.class && params[1] == int.class && params[2] == boolean.class) {
                            regionByCoordMethods3.add(m);
                        } else if (name.contains("region") && params.length == 2
                                && params[0] == int.class && params[1] == int.class
                                && m.getReturnType() != void.class && m.getReturnType() != boolean.class && m.getReturnType() != int.class) {
                            regionByCoordMethods2.add(m);
                        } else if ((name.contains("ensure") || name.contains("load")) && params.length == 2
                                && params[0] == int.class && params[1] == int.class) {
                            ensureLoadedMethods.add(m);
                        }
                    }
                    c = c.getSuperclass();
                }
            }
        } catch (Exception ignored) {
        }

        String[] packetCandidates = new String[] {
            "necesse.engine.network.packet.PacketRegionData",
            "necesse.engine.network.packet.PacketLevelRegionData"
        };
        for (String className : packetCandidates) {
            try {
                Class<?> pktClass = Class.forName(className);
                for (Constructor<?> ctor : pktClass.getDeclaredConstructors()) {
                    ctor.setAccessible(true);
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 1 && !params[0].isArray() && params[0] != byte[].class) {
                        packetRegionDataCtorRegion = ctor;
                    } else if (params.length == 3 && Level.class.isAssignableFrom(params[0])
                            && params[1] == int.class && params[2] == int.class) {
                        packetRegionDataCtorLevelCoords = ctor;
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Força a geração ou carregamento de uma Região (16x16) na coordenada de tile especificada.
     */
    public static Object ensureRegionGeneratedByTile(Level level, int tileX, int tileY) {
        if (level == null) {
            return null;
        }
        initReflection(level);

        int regionX = Math.floorDiv(tileX, REGION_SIZE);
        int regionY = Math.floorDiv(tileY, REGION_SIZE);
        Object regionObj = null;

        try {
            if (regionManagerField != null) {
                Object rm = regionManagerField.get(level);
                if (rm != null) {
                    for (Method m : ensureLoadedMethods) {
                        try {
                            m.invoke(rm, tileX, tileY);
                        } catch (Exception ignored) {
                        }
                    }
                    for (Method m : regionByTileMethods3) {
                        try {
                            regionObj = m.invoke(rm, tileX, tileY, true);
                            if (regionObj != null) break;
                        } catch (Exception ignored) {
                        }
                    }
                    if (regionObj == null) {
                        for (Method m : regionByCoordMethods3) {
                            try {
                                regionObj = m.invoke(rm, regionX, regionY, true);
                                if (regionObj != null) break;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    if (regionObj == null) {
                        for (Method m : regionByTileMethods2) {
                            try {
                                regionObj = m.invoke(rm, tileX, tileY);
                                if (regionObj != null) break;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    if (regionObj == null) {
                        for (Method m : regionByCoordMethods2) {
                            try {
                                regionObj = m.invoke(rm, regionX, regionY);
                                if (regionObj != null) break;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Aciona leitura direta de tile e objeto para forçar inicialização lazy do Level
        try {
            level.getTileID(tileX, tileY);
            level.getObjectID(tileX, tileY);
        } catch (Exception ignored) {
        }

        return regionObj;
    }

    /**
     * Garante que todas as regiões dentro do retângulo de tiles estejam carregadas/geradas.
     */
    public static void ensureAreaRegionsLoaded(Level level, int startTileX, int startTileY, int endTileX, int endTileY) {
        if (level == null) {
            return;
        }
        int minX = Math.min(startTileX, endTileX);
        int maxX = Math.max(startTileX, endTileX);
        int minY = Math.min(startTileY, endTileY);
        int maxY = Math.max(startTileY, endTileY);

        if (level.tileWidth > 0) {
            minX = Math.max(0, minX);
            maxX = Math.min(level.tileWidth - 1, maxX);
        }
        if (level.tileHeight > 0) {
            minY = Math.max(0, minY);
            maxY = Math.min(level.tileHeight - 1, maxY);
        }

        int minRx = Math.floorDiv(minX, REGION_SIZE);
        int maxRx = Math.floorDiv(maxX, REGION_SIZE);
        int minRy = Math.floorDiv(minY, REGION_SIZE);
        int maxRy = Math.floorDiv(maxY, REGION_SIZE);

        for (int rx = minRx; rx <= maxRx; rx++) {
            for (int ry = minRy; ry <= maxRy; ry++) {
                ensureRegionGeneratedByTile(level, rx * REGION_SIZE + 8, ry * REGION_SIZE + 8);
            }
        }
    }

    /**
     * No Servidor: Gera todas as regiões não exploradas na área e envia PacketRegionData nativo para o cliente.
     */
    public static void ensureAreaGeneratedAndSyncedOnServer(
            Server server,
            ServerClient serverClient,
            Level level,
            int startTileX,
            int startTileY,
            int endTileX,
            int endTileY
    ) {
        if (level == null) {
            return;
        }
        initReflection(level);

        int minX = Math.min(startTileX, endTileX);
        int maxX = Math.max(startTileX, endTileX);
        int minY = Math.min(startTileY, endTileY);
        int maxY = Math.max(startTileY, endTileY);

        if (level.tileWidth > 0) {
            minX = Math.max(0, minX);
            maxX = Math.min(level.tileWidth - 1, maxX);
        }
        if (level.tileHeight > 0) {
            minY = Math.max(0, minY);
            maxY = Math.min(level.tileHeight - 1, maxY);
        }

        int minRegionX = Math.floorDiv(minX, REGION_SIZE);
        int maxRegionX = Math.floorDiv(maxX, REGION_SIZE);
        int minRegionY = Math.floorDiv(minY, REGION_SIZE);
        int maxRegionY = Math.floorDiv(maxY, REGION_SIZE);

        for (int rx = minRegionX; rx <= maxRegionX; rx++) {
            for (int ry = minRegionY; ry <= maxRegionY; ry++) {
                int sampleTileX = rx * REGION_SIZE + 8;
                int sampleTileY = ry * REGION_SIZE + 8;

                Object regionObj = ensureRegionGeneratedByTile(level, sampleTileX, sampleTileY);

                if (serverClient != null) {
                    sendRegionPacketToClient(serverClient, level, regionObj, rx, ry);
                }
            }
        }
    }

    public static void sendRegionPacketToClient(ServerClient serverClient, Level level, Object regionObj, int regionX, int regionY) {
        try {
            Packet pkt = null;
            if (packetRegionDataCtorRegion != null && regionObj != null) {
                pkt = (Packet) packetRegionDataCtorRegion.newInstance(regionObj);
            } else if (packetRegionDataCtorLevelCoords != null) {
                pkt = (Packet) packetRegionDataCtorLevelCoords.newInstance(level, regionX, regionY);
            }
            if (pkt != null) {
                serverClient.sendPacket(pkt);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Agenda a descoberta do mapa no cliente imediatamente e em múltiplas passagens nos frames seguintes.
     */
    public static void scheduleClientMapReveal(Client client, int startTileX, int startTileY, int endTileX, int endTileY) {
        pendingStartX = Math.min(startTileX, endTileX);
        pendingEndX = Math.max(startTileX, endTileX);
        pendingStartY = Math.min(startTileY, endTileY);
        pendingEndY = Math.max(startTileY, endTileY);
        pendingRevealTicks = 60;

        revealAreaOnClientMapNow(client, pendingStartX, pendingStartY, pendingEndX, pendingEndY);
    }

    public static void tickClientMapReveal(Client client) {
        if (pendingRevealTicks <= 0 || client == null) {
            return;
        }
        pendingRevealTicks--;
        if (pendingRevealTicks == 45 || pendingRevealTicks == 30 || pendingRevealTicks == 15 || pendingRevealTicks == 1) {
            revealAreaOnClientMapNow(client, pendingStartX, pendingStartY, pendingEndX, pendingEndY);
        }
    }

    public static void revealAreaOnClientMapNow(Client client, int startTileX, int startTileY, int endTileX, int endTileY) {
        if (client == null || client.levelManager == null) {
            return;
        }
        Level level = client.getLevel();
        if (level == null) {
            return;
        }

        try {
            ClientDiscoveredMap map = client.levelManager.getMap();
            if (map == null) {
                return;
            }

            int minX = Math.min(startTileX, endTileX);
            int maxX = Math.max(startTileX, endTileX);
            int minY = Math.min(startTileY, endTileY);
            int maxY = Math.max(startTileY, endTileY);

            if (level.tileWidth > 0) {
                minX = Math.max(0, minX);
                maxX = Math.min(level.tileWidth - 1, maxX);
            }
            if (level.tileHeight > 0) {
                minY = Math.max(0, minY);
                maxY = Math.min(level.tileHeight - 1, maxY);
            }

            int step = 8;
            for (int tx = minX; tx <= maxX; tx += step) {
                for (int ty = minY; ty <= maxY; ty += step) {
                    ensureRegionGeneratedByTile(level, tx, ty);
                    map.tickDiscovery(level, tx, ty);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
