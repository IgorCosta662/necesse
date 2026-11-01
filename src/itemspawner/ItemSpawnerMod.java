package itemspawner;

import itemspawner.packet.PacketCheatAction;
import itemspawner.packet.PacketGiveItem;
import necesse.engine.input.Control;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.PacketRegistry;
import org.lwjgl.glfw.GLFW;

@ModEntry
public class ItemSpawnerMod {

    public static Control OPEN_MENU;

    public void init() {
        OPEN_MENU = Control.addModControl(new Control(
            GLFW.GLFW_KEY_F6,
            "cheatmenu_open",
            new StaticMessage("Abrir Cheat Menu")
        ));

        PacketRegistry.registerPacket(PacketGiveItem.class);
        PacketRegistry.registerPacket(PacketCheatAction.class);
    }
}
