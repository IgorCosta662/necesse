package itemspawner.patch;

import itemspawner.ItemSpawnerMod;
import itemspawner.ui.ItemSpawnerForm;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.forms.components.FormTypingComponent;
import org.lwjgl.glfw.GLFW;

public class ItemSpawnerHotkeyPatchLogic {

    private static boolean wasEscapeDown = false;

    public static void apply() {
        // Evita abrir/fechar acidentalmente caso o jogador esteja digitando em um campo de texto
        boolean isTyping = FormTypingComponent.isCurrentlyTyping();

        if (ItemSpawnerMod.OPEN_MENU != null && ItemSpawnerMod.OPEN_MENU.isPressed()) {
            int key = ItemSpawnerMod.OPEN_MENU.getKey();
            boolean isFunctionKey = (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25);
            if (!isTyping || isFunctionKey) {
                ItemSpawnerForm.toggle();
            }
        }

        GameWindow window = WindowManager.getWindow();
        if (window != null) {
            boolean escapeDown = window.isKeyDown(GLFW.GLFW_KEY_ESCAPE);
            if (escapeDown && !wasEscapeDown && ItemSpawnerForm.isOpen()) {
                ItemSpawnerForm.close();
            }
            wasEscapeDown = escapeDown;
        }
    }
}

