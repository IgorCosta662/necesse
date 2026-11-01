package itemspawner.ui;

import itemspawner.ItemSpawnerMod;
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
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.GameInterfaceStyle;
import necesse.gfx.ui.HoverStateTextures;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class FormSpawnerItemSlot extends FormComponent implements FormPositionContainer {

    private static final GameBlackboard SHARED_BLACKBOARD = new GameBlackboard();
    private static final FontOptions FAV_FONT = new FontOptions(11).color(new Color(255, 215, 0));

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
        if (event.isUsed() || !this.isMouseOver(event) || !event.state) {
            return;
        }

        int eventId = event.getID();
        // Clique Esquerdo (-100): entrega a quantidade selecionada no seletor
        if (eventId == -100) {
            form.giveItem(this.item.item);
            playTickSound();
            event.use();
        }
        // Clique Direito (-99): entrega exatamente 1 unidade rapidamente
        else if (eventId == -99) {
            form.giveItemCustomAmount(this.item.item, 1);
            playTickSound();
            event.use();
        }
        // Clique do Meio (-98): adiciona/remove dos Favoritos
        else if (eventId == -98) {
            ItemSpawnerMod.toggleFavorite(this.item.item.getStringID());
            form.onFavoriteToggled();
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
        int x = getX();
        int y = getY();

        // Culling de renderização: ignora slots fora da área visível da caixa de rolagem para alto FPS
        if (drawArea != null && !drawArea.intersects(x, y, 32, 32)) {
            return;
        }

        boolean fav = ItemSpawnerMod.isFavorite(this.item.item.getStringID());
        GameInterfaceStyle style = getInterfaceStyle();
        Color color = isHovering
            ? style.highlightElementColor
            : (fav ? new Color(255, 230, 120) : style.activeElementColor);

        HoverStateTextures slotTex = style.inventoryslot_small;
        GameTexture tex = isHovering ? slotTex.highlighted : slotTex.active;
        tex.initDraw().color(color).draw(x, y);

        this.item.draw(player, x, y);

        if (fav) {
            FontManager.bit.drawString(x + 2, y + 1, "★", FAV_FONT);
        }

        if (isHovering) {
            ListGameTooltips tooltips = new ListGameTooltips();
            tooltips.add(this.item.getTooltip(player, SHARED_BLACKBOARD));
            tooltips.add(new StringTooltips(
                "ID: " + this.item.item.getStringID() + " (Stack Max: " + this.item.item.getStackSize() + ")",
                new Color(170, 170, 190)
            ));
            tooltips.add(new StringTooltips(
                "[Esq] Pegar Qtd | [Dir] Pegar x1 | [Meio] " + (fav ? "Remover Favorito" : "Favoritar ★"),
                new Color(120, 220, 140)
            ));
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

