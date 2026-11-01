package itemspawner.ui;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.GameInterfaceStyle;
import necesse.gfx.ui.HoverStateTextures;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class FormSpawnerItemSlot extends FormComponent implements FormPositionContainer {
    private FormPosition position;
    public final InventoryItem item;
    private final ItemSpawnerForm form;
    private boolean isHovering;

    public FormSpawnerItemSlot(ItemSpawnerForm form, Item item, int x, int y) {
        this.form = form;
        this.item = new InventoryItem(item, 1);
        this.position = new FormFixedPosition(x, y);
    }

    @Override
    public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob player) {
        if (event.isMouseMoveEvent()) {
            this.isHovering = this.isMouseOver(event);
            if (this.isHovering) {
                event.useMove();
            }
        }
        if (event.isUsed()) {
            return;
        }
        if (!this.isMouseOver(event)) {
            return;
        }

        if (event.state && (event.getID() == -100 || event.getID() == -99)) {
            form.giveItem(this.item.item);
            playTickSound();
            event.use();
        }
    }

    @Override
    public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob player) {
        // Suporte opcional a controle
    }

    @Override
    public void draw(TickManager tickManager, PlayerMob player, Rectangle drawArea) {
        GameInterfaceStyle style = getInterfaceStyle();
        Color color = isHovering ? style.highlightElementColor : style.activeElementColor;
        HoverStateTextures slotTex = style.inventoryslot_small;
        GameTexture tex = isHovering ? slotTex.highlighted : slotTex.active;
        tex.initDraw().color(color).draw(getX(), getY());

        this.item.draw(player, getX(), getY());

        if (isHovering) {
            ListGameTooltips tooltips = new ListGameTooltips();
            tooltips.add(this.item.getTooltip(player, new GameBlackboard()));
            GameTooltipManager.addTooltip(tooltips, GameBackground.getItemTooltipBackground(), TooltipLocation.FORM_FOCUS);
        }
    }

    @Override
    public void addNextControllerFocus(List<ControllerFocus> list, int i, int j, ControllerNavigationHandler handler, Rectangle rect, boolean b) {
        // Suporte opcional a controle
    }

    @Override
    public List<Rectangle> getHitboxes() {
        return Collections.singletonList(new Rectangle(getX(), getY(), 32, 32));
    }

    @Override
    public FormPosition getPosition() {
        return this.position;
    }

    @Override
    public void setPosition(FormPosition position) {
        this.position = position;
    }

    public boolean isHovering() {
        return this.isHovering;
    }
}

