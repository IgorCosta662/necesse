package itemspawner.patch;

import itemspawner.ui.ItemSpawnerForm;
import java.awt.Color;
import necesse.engine.localization.message.StaticMessage;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonTexture;

public class ItemSpawnerHudPatchLogic {

    // Cache da textura para evitar recarregar do disco sempre que a janela for redimensionada
    private static ButtonTexture cachedButtonTexture;

    private static ButtonTexture getButtonTexture() {
        if (cachedButtonTexture == null) {
            GameTexture rawTex = GameTexture.fromFile("itemspawner/icon.png");
            cachedButtonTexture = new ButtonTexture(rawTex, Color.WHITE);
        }
        return cachedButtonTexture;
    }

    public static void apply(MainGameFormManager formManager) {
        if (formManager == null) {
            return;
        }

        // Inicializa (ou restaura o estado anterior após redimensionamento) a janela do menu
        ItemSpawnerForm.init(formManager);

        // Cria o botão para a barra rápida (HUD)
        try {
            ButtonTexture texture = getButtonTexture();

            FormContentIconButton button = new FormContentIconButton(
                0, 0,
                FormInputSize.SIZE_32,
                ButtonColor.BASE,
                texture,
                new StaticMessage("Cheat Menu & Spawner v2.1\nAtalho: [F6]")
            );

            button.onClicked(e -> {
                button.startCooldown(150);
                ItemSpawnerForm.toggle();
            });

            if (formManager.rightQuickbar != null) {
                formManager.rightQuickbar.addButton(button);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

