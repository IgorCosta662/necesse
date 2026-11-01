package itemspawner.world;

import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;

/**
 * Gerador procedural de estruturas para o Cheat Menu:
 * - Vilarejo Completo (Castelo Real + 6 Casas de NPC + 2 Salas de Baús + Muralha + Praça)
 * - Castelo Real individual
 * - Sala de Baús (Armazém) individual
 * - Casa de NPC mobiliada individual
 * - Limpeza/Nivelamento de terreno e Povoamento de NPCs
 */
public class VillageGenerator {

    public enum StructureType {
        FULL_VILLAGE,
        CASTLE_ONLY,
        CHEST_ROOM_ONLY,
        NPC_HOUSE_ONLY,
        SPAWN_NPCS,
        FLATTEN_AREA
    }

    public static void generate(Level level, StructureType type, int centerX, int centerY, boolean isServer) {
        if (level == null || type == null) {
            return;
        }

        // Garante que todas as regiões 16x16 ao redor do ponto de construção estejam geradas/carregadas
        RegionLoaderHelper.ensureAreaRegionsLoaded(level, centerX - 32, centerY - 36, centerX + 32, centerY + 24);

        switch (type) {
            case FULL_VILLAGE:
                buildFullVillage(level, centerX, centerY, isServer);
                break;
            case CASTLE_ONLY:
                buildCastle(level, centerX - 9, centerY - 16);
                break;
            case CHEST_ROOM_ONLY:
                buildChestRoom(level, centerX - 6, centerY - 10, 13, 9, 0);
                break;
            case NPC_HOUSE_ONLY:
                buildNpcHouse(level, centerX - 3, centerY - 7, true);
                break;
            case SPAWN_NPCS:
                if (isServer) {
                    spawnVillageNpcs(level, centerX, centerY);
                }
                break;
            case FLATTEN_AREA:
                flattenArea(level, centerX - 17, centerY - 17, 35, 35);
                break;
        }
    }

    // =========================================================================
    // 1. VILAREJO COMPLETO (49x46 tiles)
    // =========================================================================
    private static void buildFullVillage(Level level, int cx, int cy, boolean isServer) {
        int startX = cx - 24;
        int startY = cy - 30;
        int width = 49;
        int height = 46;

        int grassTile = findTile("grasstile", "grass", "plainsgrass");
        int roadTile = findTile("stonepathtile", "stonepath", "stonefloor", "stonebrickfloor");
        int plazaTile = findTile("stonebrickfloor", "stonefloor", "sandstonefloor");

        int perimeterWall = findObject("stonewall", "deepstonewall", "woodwall");
        int torchObj = findObject("torch", "walltorch", "goldlamp");

        // 1. Limpa toda a área do vilarejo e aplica grama base
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                setTileSafe(level, x, y, grassTile);
                setObjectSafe(level, x, y, 0, 0);
            }
        }

        // 2. Muralha Externa do Vilarejo com 4 Portões (Norte, Sul, Leste, Oeste)
        for (int x = startX; x < startX + width; x++) {
            boolean isGateX = Math.abs(x - cx) <= 2;
            if (!isGateX) {
                setObjectSafe(level, x, startY, perimeterWall, 0);
                setObjectSafe(level, x, startY + height - 1, perimeterWall, 0);
            }
        }
        for (int y = startY; y < startY + height; y++) {
            boolean isGateY = Math.abs(y - cy) <= 2;
            if (!isGateY) {
                setObjectSafe(level, startX, y, perimeterWall, 0);
                setObjectSafe(level, startX + width - 1, y, perimeterWall, 0);
            }
        }

        // 3. Avenidas Principais (Cruz Central) e Praça Central
        for (int y = startY + 1; y < startY + height - 1; y++) {
            for (int x = cx - 2; x <= cx + 2; x++) {
                setTileSafe(level, x, y, roadTile);
            }
        }
        for (int x = startX + 1; x < startX + width - 1; x++) {
            for (int y = cy - 1; y <= cy + 1; y++) {
                setTileSafe(level, x, y, roadTile);
            }
        }
        // Praça Central (7x7 ao redor do jogador)
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int y = cy - 3; y <= cy + 3; y++) {
                setTileSafe(level, x, y, plazaTile);
            }
        }
        setObjectSafe(level, cx - 4, cy - 3, torchObj, 0);
        setObjectSafe(level, cx + 4, cy - 3, torchObj, 0);
        setObjectSafe(level, cx - 4, cy + 3, torchObj, 0);
        setObjectSafe(level, cx + 4, cy + 3, torchObj, 0);

        // 4. Castelo Real ao Norte (19x14 tiles)
        buildCastle(level, cx - 9, startY + 2);

        // 5. 6 Casas Mobiliadas para NPCs (3 na Ala Oeste, 3 na Ala Leste)
        int[] houseOffsetsY = new int[] { startY + 17, startY + 24, startY + 31 };
        for (int hy : houseOffsetsY) {
            // Casa Oeste (porta voltada para a avenida à direita)
            buildNpcHouseSide(level, cx - 11, hy, true);
            // Casa Leste (porta voltada para a avenida à esquerda)
            buildNpcHouseSide(level, cx + 4, hy, false);
        }

        // 6. 2 Salas de Baús (Armazéns Reais no setor Sul-Oeste e Sul-Leste)
        buildChestRoom(level, startX + 2, startY + 35, 13, 9, 2);
        buildChestRoom(level, startX + width - 15, startY + 35, 13, 9, 2);

        // 7. Iluminação ao longo das avenidas e portões
        for (int y = startY + 4; y < startY + height - 3; y += 6) {
            if (level.getObjectID(cx - 3, y) == 0) {
                setObjectSafe(level, cx - 3, y, torchObj, 0);
            }
            if (level.getObjectID(cx + 3, y) == 0) {
                setObjectSafe(level, cx + 3, y, torchObj, 0);
            }
        }

        // 8. Spawna os NPCs automaticamente no servidor
        if (isServer) {
            spawnVillageNpcs(level, cx, cy);
        }
    }

    // =========================================================================
    // 2. CASTELO REAL COM TORRES, SALÃO DO TRONO E OFICINAS (19x14)
    // =========================================================================
    private static void buildCastle(Level level, int startX, int startY) {
        int width = 19;
        int height = 14;

        int castleFloor = findTile("stonebrickfloor", "deepstonebrickfloor", "stonefloor");
        int carpetFloor = findTile("woodfloor", "pinefloor", "sandstonebrickfloor", "stonebrickfloor");

        int castleWall = findObject("stonewall", "deepstonewall", "obsidianwall");
        int castleDoor = findObject("stonedoor", "deepstonedoor", "wooddoor");
        int torchObj = findObject("goldcandelabra", "candelabra", "torch");
        int throneChair = findObject("goldchair", "woodchair", "oakchair");
        int royalTable = findObject("goldtable", "woodtable", "oaktable");
        int bedObj = findObject("goldbed", "woodbed", "oakbed");
        int bookshelfObj = findObject("woodbookshelf", "oakbookshelf", "bookshelf");
        int chestObj = findObject("storagebox", "oakchest", "woodchest");

        // Estações de criação reais
        int workstationObj = findObject("demonicworkstation", "workstation");
        int forgeObj = findObject("forge");
        int anvilObj = findObject("ironanvil");
        int alchemyObj = findObject("alchemybench");
        int carpenterObj = findObject("carpentersbench");

        // Piso e limpeza interna
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                setTileSafe(level, x, y, castleFloor);
                setObjectSafe(level, x, y, 0, 0);
            }
        }

        // Tapete central do Salão do Trono
        int midX = startX + width / 2;
        for (int y = startY + 2; y < startY + height - 1; y++) {
            for (int x = midX - 1; x <= midX + 1; x++) {
                setTileSafe(level, x, y, carpetFloor);
            }
        }

        // Paredes externas do Castelo
        for (int x = startX; x < startX + width; x++) {
            setObjectSafe(level, x, startY, castleWall, 0);
            setObjectSafe(level, x, startY + height - 1, castleWall, 0);
        }
        for (int y = startY; y < startY + height; y++) {
            setObjectSafe(level, startX, y, castleWall, 0);
            setObjectSafe(level, startX + width - 1, y, castleWall, 0);
        }

        // Torres de Vigia nos 4 cantos (reforço 3x3)
        int[][] corners = new int[][] {
            { startX, startY },
            { startX + width - 3, startY },
            { startX, startY + height - 3 },
            { startX + width - 3, startY + height - 3 }
        };
        for (int[] c : corners) {
            for (int tx = c[0]; tx < c[0] + 3; tx++) {
                for (int ty = c[1]; ty < c[1] + 3; ty++) {
                    if (tx == c[0] || tx == c[0] + 2 || ty == c[1] || ty == c[1] + 2) {
                        setObjectSafe(level, tx, ty, castleWall, 0);
                    }
                }
            }
            setObjectSafe(level, c[0] + 1, c[1] + 1, torchObj, 0);
        }

        // Divisória interna para Ala Real (Oeste) e Ala de Oficinas (Leste)
        for (int y = startY + 1; y < startY + height - 1; y++) {
            if (y != startY + 6 && y != startY + 7) {
                setObjectSafe(level, startX + 5, y, castleWall, 0);
                setObjectSafe(level, startX + width - 6, y, castleWall, 0);
            } else {
                setObjectSafe(level, startX + 5, y, castleDoor, 1);
                setObjectSafe(level, startX + width - 6, y, castleDoor, 1);
            }
        }

        // Entrada Principal Dupla ao Sul do Castelo
        setObjectSafe(level, midX, startY + height - 1, castleDoor, 0);
        setObjectSafe(level, midX - 1, startY + height - 1, castleDoor, 0);
        setObjectSafe(level, midX + 1, startY + height - 1, castleDoor, 0);

        // Trono Real e decoração no Norte do Salão Central
        setObjectSafe(level, midX, startY + 2, throneChair, 2);
        setObjectSafe(level, midX - 2, startY + 2, torchObj, 0);
        setObjectSafe(level, midX + 2, startY + 2, torchObj, 0);
        setObjectSafe(level, midX - 2, startY + 5, royalTable, 0);
        setObjectSafe(level, midX + 2, startY + 5, royalTable, 0);

        // Ala Oeste: Aposentos Reais (Cama, Estantes, Baús Reais)
        setObjectSafe(level, startX + 2, startY + 4, bedObj, 1);
        setObjectSafe(level, startX + 2, startY + 2, bookshelfObj, 2);
        setObjectSafe(level, startX + 3, startY + 2, bookshelfObj, 2);
        setObjectSafe(level, startX + 2, startY + 9, chestObj, 2);
        setObjectSafe(level, startX + 3, startY + 9, chestObj, 2);
        setObjectSafe(level, startX + 4, startY + 4, torchObj, 0);

        // Ala Leste: Sala de Oficinas Completas (Crafting Room)
        setObjectSafe(level, startX + width - 3, startY + 4, workstationObj, 0);
        setObjectSafe(level, startX + width - 3, startY + 6, forgeObj, 0);
        setObjectSafe(level, startX + width - 3, startY + 8, anvilObj, 0);
        setObjectSafe(level, startX + width - 4, startY + 4, alchemyObj, 0);
        setObjectSafe(level, startX + width - 4, startY + 8, carpenterObj, 0);
        setObjectSafe(level, startX + width - 5, startY + 4, torchObj, 0);
    }

    // =========================================================================
    // 3. CASA DE NPC MOBILIADA (8x6 tiles)
    // =========================================================================
    private static void buildNpcHouse(Level level, int startX, int startY, boolean doorAtBottom) {
        int width = 8;
        int height = 6;

        int woodFloor = findTile("woodfloor", "pinefloor", "palmfloor");
        int woodWall = findObject("woodwall", "oakwall", "pinewall");
        int woodDoor = findObject("wooddoor", "oakdoor", "pinedoor");
        int bedObj = findObject("woodbed", "oakbed", "pinebed");
        int tableObj = findObject("woodtable", "oaktable", "pinetable");
        int chairObj = findObject("woodchair", "oakchair", "pinechair");
        int chestObj = findObject("storagebox", "oakchest", "woodchest");
        int torchObj = findObject("torch", "walltorch", "goldlamp");

        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                setTileSafe(level, x, y, woodFloor);
                setObjectSafe(level, x, y, 0, 0);
            }
        }

        for (int x = startX; x < startX + width; x++) {
            setObjectSafe(level, x, startY, woodWall, 0);
            setObjectSafe(level, x, startY + height - 1, woodWall, 0);
        }
        for (int y = startY; y < startY + height; y++) {
            setObjectSafe(level, startX, y, woodWall, 0);
            setObjectSafe(level, startX + width - 1, y, woodWall, 0);
        }

        // Porta
        int doorX = startX + width / 2;
        int doorY = doorAtBottom ? (startY + height - 1) : startY;
        setObjectSafe(level, doorX, doorY, woodDoor, 0);

        // Mobília válida para quarto de NPC no Necesse (Cama + Mesa + Cadeira + Baú + Luz)
        setObjectSafe(level, startX + 1, startY + 1, bedObj, 2);
        setObjectSafe(level, startX + width - 3, startY + 1, tableObj, 0);
        setObjectSafe(level, startX + width - 4, startY + 1, chairObj, 1);
        setObjectSafe(level, startX + width - 2, startY + height - 2, chestObj, 0);
        setObjectSafe(level, startX + 1, startY + height - 2, torchObj, 0);
    }

    private static void buildNpcHouseSide(Level level, int startX, int startY, boolean doorOnRight) {
        int width = 8;
        int height = 6;

        int woodFloor = findTile("woodfloor", "pinefloor", "palmfloor");
        int woodWall = findObject("woodwall", "oakwall", "pinewall");
        int woodDoor = findObject("wooddoor", "oakdoor", "pinedoor");
        int bedObj = findObject("woodbed", "oakbed", "pinebed");
        int tableObj = findObject("woodtable", "oaktable", "pinetable");
        int chairObj = findObject("woodchair", "oakchair", "pinechair");
        int chestObj = findObject("storagebox", "oakchest", "woodchest");
        int torchObj = findObject("torch", "walltorch", "goldlamp");

        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                setTileSafe(level, x, y, woodFloor);
                setObjectSafe(level, x, y, 0, 0);
            }
        }

        for (int x = startX; x < startX + width; x++) {
            setObjectSafe(level, x, startY, woodWall, 0);
            setObjectSafe(level, x, startY + height - 1, woodWall, 0);
        }
        for (int y = startY; y < startY + height; y++) {
            setObjectSafe(level, startX, y, woodWall, 0);
            setObjectSafe(level, startX + width - 1, y, woodWall, 0);
        }

        // Porta lateral voltada para a avenida central
        int doorX = doorOnRight ? (startX + width - 1) : startX;
        int doorY = startY + height / 2;
        setObjectSafe(level, doorX, doorY, woodDoor, 1);

        // Mobília interna
        int innerCol = doorOnRight ? (startX + 1) : (startX + width - 3);
        setObjectSafe(level, innerCol, startY + 1, bedObj, 2);
        setObjectSafe(level, startX + 3, startY + 1, tableObj, 0);
        setObjectSafe(level, startX + 4, startY + 1, chairObj, 3);
        setObjectSafe(level, startX + 3, startY + height - 2, chestObj, 0);
        setObjectSafe(level, startX + 5, startY + height - 2, torchObj, 0);
    }

    // =========================================================================
    // 4. SALA DE BAÚS / ARMAZÉM REAL (13x9 com fileiras de Baús)
    // =========================================================================
    private static void buildChestRoom(Level level, int startX, int startY, int width, int height, int doorSide) {
        int stoneFloor = findTile("stonebrickfloor", "woodfloor", "stonefloor");
        int aisleFloor = findTile("woodfloor", "stonepathtile", "stonebrickfloor");
        int wallObj = findObject("stonewall", "woodwall", "oakwall");
        int doorObj = findObject("stonedoor", "wooddoor", "oakdoor");
        int chestObj = findObject("storagebox", "oakchest", "woodchest");
        int torchObj = findObject("torch", "walltorch", "goldlamp");

        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                setTileSafe(level, x, y, stoneFloor);
                setObjectSafe(level, x, y, 0, 0);
            }
        }

        // Corredor central
        int midY = startY + height / 2;
        for (int x = startX + 1; x < startX + width - 1; x++) {
            setTileSafe(level, x, midY, aisleFloor);
        }

        // Paredes
        for (int x = startX; x < startX + width; x++) {
            setObjectSafe(level, x, startY, wallObj, 0);
            setObjectSafe(level, x, startY + height - 1, wallObj, 0);
        }
        for (int y = startY; y < startY + height; y++) {
            setObjectSafe(level, startX, y, wallObj, 0);
            setObjectSafe(level, startX + width - 1, y, wallObj, 0);
        }

        // Porta de entrada
        int midX = startX + width / 2;
        if (doorSide == 2) {
            // Porta no topo (Norte)
            setObjectSafe(level, midX, startY, doorObj, 0);
            setObjectSafe(level, midX - 1, startY, doorObj, 0);
        } else {
            // Porta embaixo (Sul)
            setObjectSafe(level, midX, startY + height - 1, doorObj, 0);
            setObjectSafe(level, midX - 1, startY + height - 1, doorObj, 0);
        }

        // Fileiras organizadas de Baús (parede Norte, parede Sul e bancadas internas)
        int[] chestRowsY = new int[] { startY + 1, startY + 3, startY + height - 4, startY + height - 2 };
        for (int ry : chestRowsY) {
            for (int cx = startX + 2; cx < startX + width - 2; cx++) {
                // Deixa o corredor vertical central livre para caminhar
                if (Math.abs(cx - midX) <= 1) {
                    continue;
                }
                setObjectSafe(level, cx, ry, chestObj, 2);
            }
        }

        // Iluminação nos 4 cantos internos da Sala de Baús
        setObjectSafe(level, startX + 1, startY + 1, torchObj, 0);
        setObjectSafe(level, startX + width - 2, startY + 1, torchObj, 0);
        setObjectSafe(level, startX + 1, startY + height - 2, torchObj, 0);
        setObjectSafe(level, startX + width - 2, startY + height - 2, torchObj, 0);
    }

    // =========================================================================
    // 5. NIVELAR / LIMPAR TERRENO (35x35)
    // =========================================================================
    private static void flattenArea(Level level, int startX, int startY, int width, int height) {
        int grassTile = findTile("grasstile", "grass", "stonefloor");
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                setTileSafe(level, x, y, grassTile);
                setObjectSafe(level, x, y, 0, 0);
            }
        }
    }

    // =========================================================================
    // 6. POVOAMENTO DE NPCs NO VILAREJO
    // =========================================================================
    private static void spawnVillageNpcs(Level level, int cx, int cy) {
        String[] npcIds = new String[] {
            "elder",
            "blacksmith",
            "mage",
            "alchemist",
            "hunter",
            "farmer",
            "guard",
            "miner"
        };

        int[][] offsets = new int[][] {
            { 0, -18 },   // Castelo Real
            { -7, -10 },  // Casa Oeste 1
            { -7, -3 },   // Casa Oeste 2
            { -7, 4 },    // Casa Oeste 3
            { 7, -10 },   // Casa Leste 1
            { 7, -3 },    // Casa Leste 2
            { -2, 0 },    // Praça Central (Guarda)
            { 7, 4 }      // Casa Leste 3
        };

        for (int i = 0; i < npcIds.length && i < offsets.length; i++) {
            spawnMobSafe(level, npcIds[i], (cx + offsets[i][0]) * 32 + 16, (cy + offsets[i][1]) * 32 + 16);
        }
    }

    private static void spawnMobSafe(Level level, String mobStringId, float worldX, float worldY) {
        try {
            Mob mob = MobRegistry.getMob(mobStringId, level);
            if (mob == null) {
                mob = MobRegistry.getMob("humanmob", level);
            }
            if (mob != null && level.entityManager != null) {
                level.entityManager.addMob(mob, worldX, worldY);
            }
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // UTILITÁRIOS SEGUROS DE REGISTRO E COORDENADAS
    // =========================================================================
    private static boolean isInsideLevel(Level level, int x, int y) {
        // Nos mundos infinitos do Necesse 1.0+ (tileWidth <= 0), coordenadas negativas e positivas são válidas!
        if (level.tileWidth > 0 && (x < 2 || x >= level.tileWidth - 2)) {
            return false;
        }
        if (level.tileHeight > 0 && (y < 2 || y >= level.tileHeight - 2)) {
            return false;
        }
        return true;
    }

    private static void setTileSafe(Level level, int x, int y, int tileId) {
        if (tileId < 0 || !isInsideLevel(level, x, y)) {
            return;
        }
        try {
            level.setTile(x, y, tileId);
        } catch (Exception ignored) {
        }
    }

    private static void setObjectSafe(Level level, int x, int y, int objectId, int rotation) {
        if (objectId < 0 || !isInsideLevel(level, x, y)) {
            return;
        }
        try {
            level.setObject(x, y, objectId, rotation);
        } catch (Exception ignored) {
            try {
                level.setObject(x, y, objectId);
            } catch (Exception ignored2) {
            }
        }
    }

    private static int findTile(String... candidates) {
        for (String name : candidates) {
            try {
                int id = TileRegistry.getTileID(name);
                if (id >= 0) {
                    return id;
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private static int findObject(String... candidates) {
        for (String name : candidates) {
            try {
                int id = ObjectRegistry.getObjectID(name);
                if (id > 0) {
                    return id;
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
}
