package itemspawner.ui;

import itemspawner.packet.PacketCheatAction;
import itemspawner.packet.PacketGiveItem;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.GlobalData;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.state.MainGame;
import necesse.engine.state.State;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormComponentList;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;
import necesse.level.maps.mapData.ClientDiscoveredMap;

public class ItemSpawnerForm extends Form {

    private static ItemSpawnerForm activeInstance;

    private final MainGameFormManager formManager;

    // Botões de navegação por Abas
    private FormTextButton tabItemsBtn;
    private FormTextButton tabPlayerBtn;
    private FormTextButton tabWorldBtn;

    // Listas de componentes para cada aba
    private final FormComponentList itemsTab = new FormComponentList();
    private final FormComponentList playerTab = new FormComponentList();
    private final FormComponentList worldTab = new FormComponentList();

    // Componentes da Aba de Itens
    private FormTextInput searchInput;
    private FormContentBox contentBox;
    private FormLabel countLabel;
    private final List<Item> allItems = new ArrayList<>();
    private int spawnAmount = 1;
    private boolean maxStack = false;
    private final List<FormTextButton> amountButtons = new ArrayList<>();

    // Status atual dos cheats
    private boolean godModeActive = false;
    private boolean speedActive = false;
    private boolean invisActive = false;
    private FormTextButton godModeBtn;
    private FormTextButton speedBtn;
    private FormTextButton invisBtn;

    public ItemSpawnerForm(MainGameFormManager formManager) {
        super("cheatmenu", 580, 500);
        this.formManager = formManager;
        activeInstance = this;

        // Permite arrastar a janela pela barra de título
        setDraggingBox(new Rectangle(0, 0, 540, 32));

        // Título da janela
        addComponent(new FormLabel("Cheat Menu & Spawner", new FontOptions(16), FormLabel.ALIGN_LEFT, 12, 8));

        // Botão Fechar [X]
        FormTextButton closeBtn = new FormTextButton("X", 544, 6, 26, FormInputSize.SIZE_24, ButtonColor.BASE);
        closeBtn.onClicked(e -> setHidden(true));
        addComponent(closeBtn);

        // Abas no topo
        setupTabs();

        // Adiciona as abas no formulário principal
        addComponent(itemsTab);
        addComponent(playerTab);
        addComponent(worldTab);

        // Conteúdo de cada aba
        setupItemsTab();
        setupPlayerTab();
        setupWorldTab();

        // Inicia na aba de itens
        selectTab(0);

        // Posição inicial centralizada
        centerOnScreen();
    }

    private void setupTabs() {
        tabItemsBtn = new FormTextButton("📦 Itens", 12, 36, 175, FormInputSize.SIZE_24, ButtonColor.BASE);
        tabItemsBtn.onClicked(e -> selectTab(0));
        addComponent(tabItemsBtn);

        tabPlayerBtn = new FormTextButton("⚡ Jogador", 195, 36, 175, FormInputSize.SIZE_24, ButtonColor.BASE);
        tabPlayerBtn.onClicked(e -> selectTab(1));
        addComponent(tabPlayerBtn);

        tabWorldBtn = new FormTextButton("🌍 Mundo / Mapa", 378, 36, 175, FormInputSize.SIZE_24, ButtonColor.BASE);
        tabWorldBtn.onClicked(e -> selectTab(2));
        addComponent(tabWorldBtn);
    }

    private void selectTab(int index) {
        tabItemsBtn.color = (index == 0) ? ButtonColor.GREEN : ButtonColor.BASE;
        tabPlayerBtn.color = (index == 1) ? ButtonColor.GREEN : ButtonColor.BASE;
        tabWorldBtn.color = (index == 2) ? ButtonColor.GREEN : ButtonColor.BASE;

        itemsTab.setHidden(index != 0);
        playerTab.setHidden(index != 1);
        worldTab.setHidden(index != 2);

        if (index == 0) {
            filterItems(searchInput.getText());
        }
    }

    // =========================================================================
    // ABA 1: ITENS (ITEM SPAWNER)
    // =========================================================================
    private void setupItemsTab() {
        // Campo de Pesquisa
        searchInput = new FormTextInput(12, 68, FormInputSize.SIZE_24, 250, 100);
        searchInput.placeHolder = new StaticMessage("Buscar por nome ou ID...");
        searchInput.rightClickToClear = true;
        searchInput.onChange(e -> filterItems(searchInput.getText()));
        itemsTab.addComponent(searchInput);

        // Botões de Quantidade: [x1] [x5] [x10] [x50] [Max]
        int[] amounts = new int[] { 1, 5, 10, 50 };
        int btnX = 270;
        for (int amt : amounts) {
            final int fixedAmt = amt;
            FormTextButton btn = new FormTextButton("x" + fixedAmt, btnX, 68, 48, FormInputSize.SIZE_24, ButtonColor.BASE);
            btn.onClicked(e -> {
                this.spawnAmount = fixedAmt;
                this.maxStack = false;
                updateAmountButtons(btn);
            });
            itemsTab.addComponent(btn);
            amountButtons.add(btn);
            btnX += 50;
        }

        FormTextButton maxBtn = new FormTextButton("Max", btnX, 68, 56, FormInputSize.SIZE_24, ButtonColor.BASE);
        maxBtn.onClicked(e -> {
            this.maxStack = true;
            updateAmountButtons(maxBtn);
        });
        itemsTab.addComponent(maxBtn);
        amountButtons.add(maxBtn);

        if (!amountButtons.isEmpty()) {
            amountButtons.get(0).color = ButtonColor.GREEN;
        }

        // Contador de itens
        countLabel = new FormLabel("Carregando itens...", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, 98);
        itemsTab.addComponent(countLabel);

        // Caixa de rolagem com os slots
        contentBox = new FormContentBox(12, 118, 556, 368);
        itemsTab.addComponent(contentBox);

        loadAllItems();
    }

    private void updateAmountButtons(FormTextButton selectedBtn) {
        for (FormTextButton b : amountButtons) {
            b.color = (b == selectedBtn) ? ButtonColor.GREEN : ButtonColor.BASE;
        }
    }

    private void loadAllItems() {
        allItems.clear();
        for (Item item : ItemRegistry.getItems()) {
            if (item != null && item.getStringID() != null && !item.getStringID().isEmpty()) {
                allItems.add(item);
            }
        }
    }

    public void filterItems(String query) {
        contentBox.getComponentList().clearComponents();

        String q = (query == null) ? "" : query.trim().toLowerCase();
        int index = 0;
        int matched = 0;

        for (Item item : allItems) {
            boolean matches = q.isEmpty();
            if (!matches) {
                String id = item.getStringID().toLowerCase();
                String name = "";
                try {
                    name = ItemRegistry.getDisplayName(item.getID()).toLowerCase();
                } catch (Exception ignored) {}

                if (id.contains(q) || name.contains(q)) {
                    matches = true;
                }
            }

            if (matches) {
                int col = index % 14;
                int row = index / 14;
                int x = col * 36;
                int y = row * 36;

                contentBox.addComponent(new FormSpawnerItemSlot(this, item, x, y));
                index++;
                matched++;
            }
        }

        contentBox.fitContentBoxToComponents();
        countLabel.setText(new StaticMessage("Itens encontrados: " + matched + " (Clique para receber no inventario)"));
    }

    public void giveItem(Item item) {
        if (item == null) return;

        int amount = this.maxStack ? item.getStackSize() : this.spawnAmount;
        if (amount <= 0) amount = 1;

        Client client = getClient();
        if (client != null && client.network != null) {
            client.network.sendPacket(new PacketGiveItem(item.getStringID(), amount));
        }
    }

    // =========================================================================
    // ABA 2: JOGADOR (PLAYER CHEATS)
    // =========================================================================
    private void setupPlayerTab() {
        int startY = 80;
        int col1X = 20;
        int col2X = 300;
        int btnW = 260;

        // Seção Modificadores
        FormLabel lblMods = new FormLabel("=== Modificadores do Personagem ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY);
        playerTab.addComponent(lblMods);

        // God Mode Toggle
        godModeBtn = new FormTextButton("🛡️ God Mode (Invencivel): OFF", col1X, startY + 28, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        godModeBtn.onClicked(e -> {
            godModeActive = !godModeActive;
            godModeBtn.color = godModeActive ? ButtonColor.GREEN : ButtonColor.BASE;
            godModeBtn.setText("🛡️ God Mode (Invencivel): " + (godModeActive ? "ON" : "OFF"));
            sendCheat(PacketCheatAction.CheatType.TOGGLE_GODMODE);
        });
        playerTab.addComponent(godModeBtn);

        // Speed Boost Toggle
        speedBtn = new FormTextButton("⚡ Super Velocidade: OFF", col2X, startY + 28, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        speedBtn.onClicked(e -> {
            speedActive = !speedActive;
            speedBtn.color = speedActive ? ButtonColor.GREEN : ButtonColor.BASE;
            speedBtn.setText("⚡ Super Velocidade: " + (speedActive ? "ON" : "OFF"));
            sendCheat(PacketCheatAction.CheatType.TOGGLE_SPEED);
        });
        playerTab.addComponent(speedBtn);

        // Invisibilidade Toggle
        invisBtn = new FormTextButton("👻 Invisibilidade: OFF", col1X, startY + 70, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        invisBtn.onClicked(e -> {
            invisActive = !invisActive;
            invisBtn.color = invisActive ? ButtonColor.GREEN : ButtonColor.BASE;
            invisBtn.setText("👻 Invisibilidade: " + (invisActive ? "ON" : "OFF"));
            sendCheat(PacketCheatAction.CheatType.TOGGLE_INVIS);
        });
        playerTab.addComponent(invisBtn);

        // Seção Restauração
        FormLabel lblRest = new FormLabel("=== Restauracao Instantanea ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 120);
        playerTab.addComponent(lblRest);

        // Curar Vida 100%
        FormTextButton healBtn = new FormTextButton("❤️ Curar Vida 100%", col1X, startY + 148, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        healBtn.onClicked(e -> {
            healBtn.startCooldown(500);
            sendCheat(PacketCheatAction.CheatType.HEAL);
        });
        playerTab.addComponent(healBtn);

        // Restaurar Mana 100%
        FormTextButton manaBtn = new FormTextButton("🔵 Restaurar Mana 100%", col2X, startY + 148, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        manaBtn.onClicked(e -> {
            manaBtn.startCooldown(500);
            sendCheat(PacketCheatAction.CheatType.RESTORE_MANA);
        });
        playerTab.addComponent(manaBtn);

        // Saciar Fome 100%
        FormTextButton hungerBtn = new FormTextButton("🍖 Saciar Fome 100%", col1X, startY + 190, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        hungerBtn.onClicked(e -> {
            hungerBtn.startCooldown(500);
            sendCheat(PacketCheatAction.CheatType.RESTORE_HUNGER);
        });
        playerTab.addComponent(hungerBtn);

        // Seção Economia
        FormLabel lblEco = new FormLabel("=== Moedas de Ouro ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 240);
        playerTab.addComponent(lblEco);

        // +1.000 Moedas
        FormTextButton coins1kBtn = new FormTextButton("💰 Dar +1.000 Moedas", col1X, startY + 268, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        coins1kBtn.onClicked(e -> {
            coins1kBtn.startCooldown(300);
            sendGiveItem("coin", 1000);
        });
        playerTab.addComponent(coins1kBtn);

        // +50.000 Moedas
        FormTextButton coins50kBtn = new FormTextButton("👑 Dar +50.000 Moedas", col2X, startY + 268, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        coins50kBtn.onClicked(e -> {
            coins50kBtn.startCooldown(300);
            sendGiveItem("coin", 50000);
        });
        playerTab.addComponent(coins50kBtn);
    }

    // =========================================================================
    // ABA 3: MUNDO E MAPA (WORLD & MAP CHEATS)
    // =========================================================================
    private void setupWorldTab() {
        int startY = 80;
        int col1X = 20;
        int col2X = 300;
        int btnW = 260;

        // Seção Horário
        FormLabel lblTime = new FormLabel("=== Controle do Tempo e Horario ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY);
        worldTab.addComponent(lblTime);

        // Definir Dia
        FormTextButton dayBtn = new FormTextButton("☀️ Mudar Horario para DIA", col1X, startY + 28, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        dayBtn.onClicked(e -> {
            dayBtn.startCooldown(500);
            sendCheat(PacketCheatAction.CheatType.SET_DAY);
        });
        worldTab.addComponent(dayBtn);

        // Definir Noite
        FormTextButton nightBtn = new FormTextButton("🌙 Mudar Horario para NOITE", col2X, startY + 28, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        nightBtn.onClicked(e -> {
            nightBtn.startCooldown(500);
            sendCheat(PacketCheatAction.CheatType.SET_NIGHT);
        });
        worldTab.addComponent(nightBtn);

        // Seção Limpeza
        FormLabel lblClean = new FormLabel("=== Limpeza e Entidades ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 80);
        worldTab.addComponent(lblClean);

        // Eliminar Monstros Hostis
        FormTextButton killHostilesBtn = new FormTextButton("💀 Eliminar Monstros Hostis", col1X, startY + 108, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        killHostilesBtn.onClicked(e -> {
            killHostilesBtn.startCooldown(500);
            sendCheat(PacketCheatAction.CheatType.KILL_HOSTILES);
        });
        worldTab.addComponent(killHostilesBtn);

        // Limpar Itens do Chão
        FormTextButton clearDropsBtn = new FormTextButton("🧹 Limpar Itens do Chao", col2X, startY + 108, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        clearDropsBtn.onClicked(e -> {
            clearDropsBtn.startCooldown(500);
            sendCheat(PacketCheatAction.CheatType.CLEAR_DROPS);
        });
        worldTab.addComponent(clearDropsBtn);

        // Seção Mapa
        FormLabel lblMap = new FormLabel("=== Exploracao do Mapa ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 160);
        worldTab.addComponent(lblMap);

        // Revelar Todo o Mapa
        FormTextButton revealMapBtn = new FormTextButton("🗺️ Revelar Mapa da Ilha Inteira", col1X, startY + 188, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        revealMapBtn.onClicked(e -> {
            revealMapBtn.startCooldown(1000);
            revealCurrentLevelMap();
        });
        worldTab.addComponent(revealMapBtn);
    }

    private void revealCurrentLevelMap() {
        Client client = getClient();
        if (client == null) return;
        Level level = client.getLevel();
        if (level == null) return;

        try {
            ClientDiscoveredMap map = client.levelManager.getMap();
            if (map != null) {
                int w = level.tileWidth;
                int h = level.tileHeight;
                for (int tx = 0; tx < w; tx += 25) {
                    for (int ty = 0; ty < h; ty += 25) {
                        map.tickDiscovery(level, tx, ty);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // UTILITÁRIOS DE REDE E CONTROLE
    // =========================================================================
    private void sendCheat(PacketCheatAction.CheatType type) {
        Client client = getClient();
        if (client != null && client.network != null) {
            client.network.sendPacket(new PacketCheatAction(type));
        }
    }

    private void sendGiveItem(String itemStringID, int amount) {
        Client client = getClient();
        if (client != null && client.network != null) {
            client.network.sendPacket(new PacketGiveItem(itemStringID, amount));
        }
    }

    private Client getClient() {
        State state = GlobalData.getCurrentState();
        if (state instanceof MainGame) {
            return ((MainGame) state).getClient();
        }
        return null;
    }

    public void centerOnScreen() {
        GameWindow win = WindowManager.getWindow();
        if (win != null) {
            setPosMiddle(win.getHudWidth() / 2, win.getHudHeight() / 2);
        }
    }

    @Override
    public void onWindowResized(GameWindow window) {
        super.onWindowResized(window);
        centerOnScreen();
    }

    public static void init(MainGameFormManager formManager) {
        if (activeInstance != null) {
            activeInstance.dispose();
        }
        ItemSpawnerForm form = new ItemSpawnerForm(formManager);
        form.setHidden(true);
        formManager.addComponent(form);
    }

    public static void toggle() {
        if (activeInstance != null) {
            boolean hidden = activeInstance.isHidden();
            activeInstance.setHidden(!hidden);
            if (hidden) {
                activeInstance.centerOnScreen();
            }
        }
    }

    public static void close() {
        if (activeInstance != null) {
            activeInstance.setHidden(true);
        }
    }

    public static boolean isOpen() {
        return activeInstance != null && !activeInstance.isHidden();
    }

    public static ItemSpawnerForm getActiveInstance() {
        return activeInstance;
    }
}
