package itemspawner.patch;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.forms.MainGameFormManager;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(target = MainGameFormManager.class, name = "setup", arguments = {})
public class ItemSpawnerHudPatch {

    @Advice.OnMethodExit
    static void onExit(@Advice.This MainGameFormManager formManager) {
        ItemSpawnerHudPatchLogic.apply(formManager);
    }
}
