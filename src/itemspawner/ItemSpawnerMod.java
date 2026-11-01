package itemspawner;

import itemspawner.packet.PacketBuildStructure;
import itemspawner.packet.PacketCheatAction;
import itemspawner.packet.PacketGiveItem;
import itemspawner.packet.PacketRevealMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import necesse.engine.input.Control;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.PacketRegistry;
import org.lwjgl.glfw.GLFW;

@ModEntry
public class ItemSpawnerMod {

    public static final String MOD_ID = "itemspawner";
    public static final String MOD_VERSION = "2.3.0";

    public static Control OPEN_MENU;

    // Mantém os itens favoritados durante a sessão mesmo se a UI for recriada ao redimensionar a janela
    private static final Set<String> FAVORITE_ITEMS = Collections.synchronizedSet(new HashSet<>());

    public void init() {
        OPEN_MENU = Control.addModControl(new Control(
            GLFW.GLFW_KEY_F6,
            "cheatmenu_open",
            new StaticMessage("Abrir Cheat Menu & Spawner")
        ));

        PacketRegistry.registerPacket(PacketGiveItem.class);
        PacketRegistry.registerPacket(PacketCheatAction.class);
        PacketRegistry.registerPacket(PacketBuildStructure.class);
        PacketRegistry.registerPacket(PacketRevealMap.class);
    }

    public static boolean isFavorite(String itemStringID) {
        return itemStringID != null && FAVORITE_ITEMS.contains(itemStringID);
    }

    public static boolean toggleFavorite(String itemStringID) {
        if (itemStringID == null || itemStringID.isEmpty()) {
            return false;
        }
        if (FAVORITE_ITEMS.contains(itemStringID)) {
            FAVORITE_ITEMS.remove(itemStringID);
            return false;
        } else {
            FAVORITE_ITEMS.add(itemStringID);
            return true;
        }
    }

    public static Set<String> getFavorites() {
        return Collections.unmodifiableSet(FAVORITE_ITEMS);
    }
}

