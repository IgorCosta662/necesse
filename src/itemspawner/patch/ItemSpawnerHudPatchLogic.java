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

    public static void apply(MainGameFormManager formManager) {
        if (formManager == null) {
            return;
        }

        // Inicializa a janela do menu
        ItemSpawnerForm.init(formManager);

        // Cria o botão para a barra rápida (HUD)
        try {
            ButtonTexture texture = new ButtonTexture(
                GameTexture.fromFile("itemspawner/icon.png"),
                Color.WHITE
            );

            FormContentIconButton button = new FormContentIconButton(
                0, 0,
                FormInputSize.SIZE_32,
                ButtonColor.BASE,
                texture,
                new StaticMessage("Cheat Menu & Spawner\nAtalho: [F6]")
            );

            button.onClicked(e -> {
                button.startCooldown(200);
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

