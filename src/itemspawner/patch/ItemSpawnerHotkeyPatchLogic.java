package itemspawner.patch;

import itemspawner.ItemSpawnerMod;
import itemspawner.ui.ItemSpawnerForm;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import org.lwjgl.glfw.GLFW;

public class ItemSpawnerHotkeyPatchLogic {

    public static void apply() {
        if (ItemSpawnerMod.OPEN_MENU != null && ItemSpawnerMod.OPEN_MENU.isPressed()) {
            ItemSpawnerForm.toggle();
        }

        if (ItemSpawnerForm.isOpen()) {
            GameWindow window = WindowManager.getWindow();
            if (window != null && window.isKeyDown(GLFW.GLFW_KEY_ESCAPE)) {
                ItemSpawnerForm.close();
            }
        }
    }
}

