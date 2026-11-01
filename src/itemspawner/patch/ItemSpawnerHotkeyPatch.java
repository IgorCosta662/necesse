package itemspawner.patch;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.forms.MainGameFormManager;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(target = MainGameFormManager.class, name = "frameTick", arguments = {TickManager.class})
public class ItemSpawnerHotkeyPatch {

    @Advice.OnMethodExit
    static void onExit() {
        ItemSpawnerHotkeyPatchLogic.apply();
    }
}
