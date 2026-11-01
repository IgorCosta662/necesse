package itemspawner.ui;

import itemspawner.ItemSpawnerMod;
import itemspawner.packet.PacketBuildStructure;
import itemspawner.packet.PacketCheatAction;
import itemspawner.packet.PacketGiveItem;
import itemspawner.packet.PacketRevealMap;
import itemspawner.world.RegionLoaderHelper;
import itemspawner.world.VillageGenerator.StructureType;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.GlobalData;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.state.MainGame;
import necesse.engine.state.State;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
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

    public enum ItemFilterCategory {
        ALL("Todos"),
        FAVORITES("★ Fav"),
        COMBAT("Combate"),
        TOOLS("Ferram."),
        CONSUMABLES("Consum."),
        BLOCKS("Blocos");

        public final String label;

        ItemFilterCategory(String label) {
            this.label = label;
        }
    }

    private static class CachedItemEntry {
        final Item item;
        final String lowerId;
        final String lowerName;
        final ItemFilterCategory category;

        CachedItemEntry(Item item, String lowerId, String lowerName, ItemFilterCategory category) {
            this.item = item;
            this.lowerId = lowerId;
            this.lowerName = lowerName;
            this.category = category;
        }
    }

    private static ItemSpawnerForm activeInstance;

    // Preserva estado da janela quando o jogo recria o MainGameFormManager ao redimensionar a tela
    private static boolean lastHiddenState = true;
    private static int lastSelectedTab = 0;
    private static String lastSearchQuery = "";
    private static int lastSpawnAmount = 1;
    private static boolean lastMaxStack = false;
    private static ItemFilterCategory lastCategory = ItemFilterCategory.ALL;

    private final MainGameFormManager formManager;

    // Botões de navegação por Abas (4 Abas)
    private FormTextButton tabItemsBtn;
    private FormTextButton tabPlayerBtn;
    private FormTextButton tabWorldBtn;
    private FormTextButton tabVillageBtn;
    private int currentTabIndex = 0;

    // Listas de componentes para cada aba
    private final FormComponentList itemsTab = new FormComponentList();
    private final FormComponentList playerTab = new FormComponentList();
    private final FormComponentList worldTab = new FormComponentList();
    private final FormComponentList villageTab = new FormComponentList();

    // Componentes da Aba de Itens
    private FormTextInput searchInput;
    private FormContentBox contentBox;
    private FormLabel countLabel;
    private final List<CachedItemEntry> cachedItems = new ArrayList<>();
    private int spawnAmount = 1;
    private boolean maxStack = false;
    private ItemFilterCategory activeCategory = ItemFilterCategory.ALL;
    private final List<FormTextButton> amountButtons = new ArrayList<>();
    private final List<FormTextButton> categoryButtons = new ArrayList<>();

    // Status atual dos cheats (sincronizado em tempo real com os buffs do jogador)
    private boolean godModeActive = false;
    private boolean speedActive = false;
    private boolean invisActive = false;
    private FormTextButton godModeBtn;
    private FormTextButton speedBtn;
    private FormTextButton invisBtn;

    public ItemSpawnerForm(MainGameFormManager formManager) {
        super("cheatmenu", 600, 520);
        this.formManager = formManager;
        activeInstance = this;

        this.spawnAmount = lastSpawnAmount;
        this.maxStack = lastMaxStack;
        this.activeCategory = lastCategory;

        // Permite arrastar a janela pela barra de título
        setDraggingBox(new Rectangle(0, 0, 560, 32));

        // Título da janela
        addComponent(new FormLabel("Cheat Menu & Item Spawner v" + ItemSpawnerMod.MOD_VERSION, new FontOptions(16), FormLabel.ALIGN_LEFT, 12, 8));

        // Botão Fechar [X]
        FormTextButton closeBtn = new FormTextButton("X", 564, 6, 26, FormInputSize.SIZE_24, ButtonColor.BASE);
        closeBtn.onClicked(e -> close());
        addComponent(closeBtn);

        // Abas no topo
        setupTabs();

        // Adiciona as abas no formulário principal
        addComponent(itemsTab);
        addComponent(playerTab);
        addComponent(worldTab);
        addComponent(villageTab);

        // Conteúdo de cada aba
        setupItemsTab();
        setupPlayerTab();
        setupWorldTab();
        setupVillageTab();

        // Restaura a última aba selecionada
        selectTab(lastSelectedTab);

        // Posição inicial centralizada
        centerOnScreen();
    }

    private void setupTabs() {
        tabItemsBtn = new FormTextButton("📦 Itens", 12, 36, 138, FormInputSize.SIZE_24, ButtonColor.BASE);
        tabItemsBtn.onClicked(e -> selectTab(0));
        addComponent(tabItemsBtn);

        tabPlayerBtn = new FormTextButton("⚡ Jogador", 156, 36, 138, FormInputSize.SIZE_24, ButtonColor.BASE);
        tabPlayerBtn.onClicked(e -> selectTab(1));
        addComponent(tabPlayerBtn);

        tabWorldBtn = new FormTextButton("🌍 Mundo", 300, 36, 138, FormInputSize.SIZE_24, ButtonColor.BASE);
        tabWorldBtn.onClicked(e -> selectTab(2));
        addComponent(tabWorldBtn);

        tabVillageBtn = new FormTextButton("🏰 Vilarejo", 444, 36, 144, FormInputSize.SIZE_24, ButtonColor.BASE);
        tabVillageBtn.onClicked(e -> selectTab(3));
        addComponent(tabVillageBtn);
    }

    private void selectTab(int index) {
        this.currentTabIndex = index;
        lastSelectedTab = index;

        tabItemsBtn.color = (index == 0) ? ButtonColor.GREEN : ButtonColor.BASE;
        tabPlayerBtn.color = (index == 1) ? ButtonColor.GREEN : ButtonColor.BASE;
        tabWorldBtn.color = (index == 2) ? ButtonColor.GREEN : ButtonColor.BASE;
        tabVillageBtn.color = (index == 3) ? ButtonColor.GREEN : ButtonColor.BASE;

        itemsTab.setHidden(index != 0);
        playerTab.setHidden(index != 1);
        worldTab.setHidden(index != 2);
        villageTab.setHidden(index != 3);

        if (index == 0) {
            filterItems(searchInput != null ? searchInput.getText() : "");
        } else if (index == 1) {
            syncPlayerBuffButtons();
        }
    }

    // =========================================================================
    // ABA 1: ITENS (ITEM SPAWNER)
    // =========================================================================
    private void setupItemsTab() {
        // Campo de Pesquisa
        searchInput = new FormTextInput(12, 66, FormInputSize.SIZE_24, 260, 100);
        searchInput.placeHolder = new StaticMessage("Buscar por nome ou ID...");
        searchInput.rightClickToClear = true;
        if (lastSearchQuery != null && !lastSearchQuery.isEmpty()) {
            searchInput.setText(lastSearchQuery);
        }
        searchInput.onChange(e -> {
            lastSearchQuery = searchInput.getText();
            filterItems(lastSearchQuery);
        });
        itemsTab.addComponent(searchInput);

        // Botões de Quantidade: [x1] [x5] [x10] [x50] [Max]
        int[] amounts = new int[] { 1, 5, 10, 50 };
        int btnX = 280;
        for (int amt : amounts) {
            final int fixedAmt = amt;
            FormTextButton btn = new FormTextButton("x" + fixedAmt, btnX, 66, 50, FormInputSize.SIZE_24, ButtonColor.BASE);
            btn.onClicked(e -> {
                this.spawnAmount = fixedAmt;
                this.maxStack = false;
                lastSpawnAmount = fixedAmt;
                lastMaxStack = false;
                updateAmountButtons(btn);
            });
            itemsTab.addComponent(btn);
            amountButtons.add(btn);
            if (!this.maxStack && this.spawnAmount == fixedAmt) {
                btn.color = ButtonColor.GREEN;
            }
            btnX += 54;
        }

        FormTextButton maxBtn = new FormTextButton("Max", btnX, 66, 62, FormInputSize.SIZE_24, ButtonColor.BASE);
        maxBtn.onClicked(e -> {
            this.maxStack = true;
            lastMaxStack = true;
            updateAmountButtons(maxBtn);
        });
        if (this.maxStack) {
            maxBtn.color = ButtonColor.GREEN;
        }
        itemsTab.addComponent(maxBtn);
        amountButtons.add(maxBtn);

        // Barra de Categorias (Todos, ★ Fav, Combate, Ferram., Consum., Blocos)
        ItemFilterCategory[] categories = ItemFilterCategory.values();
        int catX = 12;
        int catW = 92;
        for (ItemFilterCategory cat : categories) {
            final ItemFilterCategory targetCat = cat;
            FormTextButton catBtn = new FormTextButton(cat.label, catX, 96, catW, FormInputSize.SIZE_20, ButtonColor.BASE);
            catBtn.onClicked(e -> {
                this.activeCategory = targetCat;
                lastCategory = targetCat;
                updateCategoryButtons(catBtn);
                filterItems(searchInput.getText());
            });
            if (this.activeCategory == targetCat) {
                catBtn.color = ButtonColor.GREEN;
            }
            itemsTab.addComponent(catBtn);
            categoryButtons.add(catBtn);
            catX += 96;
        }

        // Contador de itens
        countLabel = new FormLabel("Carregando itens...", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, 122);
        itemsTab.addComponent(countLabel);

        // Caixa de rolagem com os slots (15 colunas x 36px)
        contentBox = new FormContentBox(12, 140, 576, 368);
        itemsTab.addComponent(contentBox);

        loadAndCacheAllItems();
    }

    private void updateAmountButtons(FormTextButton selectedBtn) {
        for (FormTextButton b : amountButtons) {
            b.color = (b == selectedBtn) ? ButtonColor.GREEN : ButtonColor.BASE;
        }
    }

    private void updateCategoryButtons(FormTextButton selectedBtn) {
        for (FormTextButton b : categoryButtons) {
            b.color = (b == selectedBtn) ? ButtonColor.GREEN : ButtonColor.BASE;
        }
    }

    private void loadAndCacheAllItems() {
        cachedItems.clear();
        for (Item item : ItemRegistry.getItems()) {
            if (item == null || item.getStringID() == null || item.getStringID().isEmpty()) {
                continue;
            }
            String lowerId = item.getStringID().toLowerCase();
            String lowerName = lowerId;
            try {
                String display = ItemRegistry.getDisplayName(item.getID());
                if (display != null && !display.isEmpty()) {
                    lowerName = display.toLowerCase();
                }
            } catch (Exception ignored) {
            }

            ItemFilterCategory category = classifyItem(item, lowerId);
            cachedItems.add(new CachedItemEntry(item, lowerId, lowerName, category));
        }
    }

    private static ItemFilterCategory classifyItem(Item item, String lowerId) {
        String className = item.getClass().getSimpleName().toLowerCase();
        if (className.contains("weapon") || className.contains("sword") || className.contains("bow")
                || className.contains("gun") || className.contains("magic") || className.contains("summon")
                || className.contains("armor") || className.contains("trinket") || className.contains("ammo")
                || className.contains("arrow") || className.contains("bullet")) {
            return ItemFilterCategory.COMBAT;
        }
        if (className.contains("tool") || className.contains("pickaxe") || className.contains("axe")
                || className.contains("shovel") || className.contains("fishing") || className.contains("net")) {
            return ItemFilterCategory.TOOLS;
        }
        if (className.contains("food") || className.contains("potion") || className.contains("consumable")
                || lowerId.contains("potion") || lowerId.contains("elixir")) {
            return ItemFilterCategory.CONSUMABLES;
        }
        if (className.contains("tile") || className.contains("object") || className.contains("wall")
                || className.contains("floor") || className.contains("seed") || className.contains("placeable")) {
            return ItemFilterCategory.BLOCKS;
        }
        return ItemFilterCategory.ALL;
    }

    public void onFavoriteToggled() {
        if (this.activeCategory == ItemFilterCategory.FAVORITES) {
            filterItems(searchInput != null ? searchInput.getText() : "");
        }
    }

    public void filterItems(String query) {
        if (contentBox == null) {
            return;
        }
        contentBox.getComponentList().clearComponents();

        String q = (query == null) ? "" : query.trim().toLowerCase();
        int index = 0;
        int matched = 0;
        int columns = 15;

        for (CachedItemEntry entry : cachedItems) {
            // Filtro de categoria
            if (this.activeCategory == ItemFilterCategory.FAVORITES) {
                if (!ItemSpawnerMod.isFavorite(entry.item.getStringID())) {
                    continue;
                }
            } else if (this.activeCategory != ItemFilterCategory.ALL && entry.category != this.activeCategory) {
                continue;
            }

            // Filtro de busca por texto
            if (!q.isEmpty() && !entry.lowerId.contains(q) && !entry.lowerName.contains(q)) {
                continue;
            }

            int col = index % columns;
            int row = index / columns;
            int x = col * 36;
            int y = row * 36;

            contentBox.addComponent(new FormSpawnerItemSlot(this, entry.item, x, y));
            index++;
            matched++;
        }

        contentBox.fitContentBoxToComponents();
        countLabel.setText(new StaticMessage(
            "Itens: " + matched + " / " + cachedItems.size() + " | [Esq] Pegar Qtd  [Dir] Pegar x1  [Meio] Favoritar ★"
        ));
    }

    public void giveItem(Item item) {
        if (item == null) {
            return;
        }
        int amount = this.maxStack ? item.getStackSize() : this.spawnAmount;
        giveItemCustomAmount(item, amount);
    }

    public void giveItemCustomAmount(Item item, int amount) {
        if (item == null) {
            return;
        }
        int validAmount = Math.max(1, amount);
        sendGiveItem(item.getStringID(), validAmount);
    }

    // =========================================================================
    // ABA 2: JOGADOR (PLAYER CHEATS)
    // =========================================================================
    private void setupPlayerTab() {
        int startY = 74;
        int col1X = 20;
        int col2X = 310;
        int btnW = 270;

        // Seção Modificadores
        FormLabel lblMods = new FormLabel("=== Modificadores do Personagem ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY);
        playerTab.addComponent(lblMods);

        // God Mode Toggle
        godModeBtn = new FormTextButton("🛡️ God Mode (Invencivel): OFF", col1X, startY + 24, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        godModeBtn.onClicked(e -> {
            godModeActive = !godModeActive;
            updateToggleBtn(godModeBtn, "🛡️ God Mode (Invencivel)", godModeActive);
            sendCheat(PacketCheatAction.CheatType.TOGGLE_GODMODE);
        });
        playerTab.addComponent(godModeBtn);

        // Speed Boost Toggle
        speedBtn = new FormTextButton("⚡ Super Velocidade: OFF", col2X, startY + 24, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        speedBtn.onClicked(e -> {
            speedActive = !speedActive;
            updateToggleBtn(speedBtn, "⚡ Super Velocidade", speedActive);
            sendCheat(PacketCheatAction.CheatType.TOGGLE_SPEED);
        });
        playerTab.addComponent(speedBtn);

        // Invisibilidade Toggle
        invisBtn = new FormTextButton("👻 Invisibilidade: OFF", col1X, startY + 64, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        invisBtn.onClicked(e -> {
            invisActive = !invisActive;
            updateToggleBtn(invisBtn, "👻 Invisibilidade", invisActive);
            sendCheat(PacketCheatAction.CheatType.TOGGLE_INVIS);
        });
        playerTab.addComponent(invisBtn);

        // Remover Debuffs
        FormTextButton clearDebuffsBtn = new FormTextButton("✨ Limpar Debuffs / Efeitos Ruins", col2X, startY + 64, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        clearDebuffsBtn.onClicked(e -> {
            clearDebuffsBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.CLEAR_DEBUFFS);
        });
        playerTab.addComponent(clearDebuffsBtn);

        // Seção Restauração
        FormLabel lblRest = new FormLabel("=== Restauracao Instantanea ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 112);
        playerTab.addComponent(lblRest);

        // Restaurar TUDO
        FormTextButton restoreAllBtn = new FormTextButton("🌟 Restaurar TUDO (Vida + Mana + Fome)", col1X, startY + 136, btnW, FormInputSize.SIZE_32, ButtonColor.GREEN);
        restoreAllBtn.onClicked(e -> {
            restoreAllBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.RESTORE_ALL);
        });
        playerTab.addComponent(restoreAllBtn);

        // Curar Vida 100%
        FormTextButton healBtn = new FormTextButton("❤️ Curar Vida 100%", col2X, startY + 136, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        healBtn.onClicked(e -> {
            healBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.HEAL);
        });
        playerTab.addComponent(healBtn);

        // Restaurar Mana 100%
        FormTextButton manaBtn = new FormTextButton("🔵 Restaurar Mana 100%", col1X, startY + 176, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        manaBtn.onClicked(e -> {
            manaBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.RESTORE_MANA);
        });
        playerTab.addComponent(manaBtn);

        // Saciar Fome 100%
        FormTextButton hungerBtn = new FormTextButton("🍖 Saciar Fome 100%", col2X, startY + 176, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        hungerBtn.onClicked(e -> {
            hungerBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.RESTORE_HUNGER);
        });
        playerTab.addComponent(hungerBtn);

        // Seção Economia & Inventário
        FormLabel lblEco = new FormLabel("=== Moedas de Ouro & Inventario ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 224);
        playerTab.addComponent(lblEco);

        // +1.000 Moedas
        FormTextButton coins1kBtn = new FormTextButton("💰 Dar +1.000 Moedas", col1X, startY + 248, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        coins1kBtn.onClicked(e -> {
            coins1kBtn.startCooldown(250);
            sendGiveItem("coin", 1000);
        });
        playerTab.addComponent(coins1kBtn);

        // +50.000 Moedas
        FormTextButton coins50kBtn = new FormTextButton("👑 Dar +50.000 Moedas", col2X, startY + 248, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        coins50kBtn.onClicked(e -> {
            coins50kBtn.startCooldown(250);
            sendGiveItem("coin", 50000);
        });
        playerTab.addComponent(coins50kBtn);

        // Limpar Mochila (preservando hotbar e itens trancados)
        FormTextButton clearInvBtn = new FormTextButton("🗑️ Limpar Mochila (Preserva Hotbar/Trancados)", col1X, startY + 288, btnW * 2 + 20, FormInputSize.SIZE_32, ButtonColor.BASE);
        clearInvBtn.onClicked(e -> {
            clearInvBtn.startCooldown(800);
            sendCheat(PacketCheatAction.CheatType.CLEAR_INVENTORY);
        });
        playerTab.addComponent(clearInvBtn);
    }

    private void updateToggleBtn(FormTextButton btn, String label, boolean active) {
        if (btn == null) {
            return;
        }
        btn.color = active ? ButtonColor.GREEN : ButtonColor.BASE;
        btn.setText(label + ": " + (active ? "ON" : "OFF"));
    }

    private void syncPlayerBuffButtons() {
        Client client = getClient();
        if (client == null) {
            return;
        }
        PlayerMob player = client.getPlayer();
        if (player == null || player.buffManager == null) {
            return;
        }

        boolean realGod = player.hasInvincibility || player.buffManager.hasBuff(BuffRegistry.GODMODE_BUFF);
        boolean realSpeed = player.buffManager.hasBuff(BuffRegistry.DEBUG_SPEED);
        boolean realInvis = player.buffManager.hasBuff(BuffRegistry.DEBUG_INVIS);

        if (realGod != godModeActive) {
            godModeActive = realGod;
            updateToggleBtn(godModeBtn, "🛡️ God Mode (Invencivel)", godModeActive);
        }
        if (realSpeed != speedActive) {
            speedActive = realSpeed;
            updateToggleBtn(speedBtn, "⚡ Super Velocidade", speedActive);
        }
        if (realInvis != invisActive) {
            invisActive = realInvis;
            updateToggleBtn(invisBtn, "👻 Invisibilidade", invisActive);
        }
    }

    // =========================================================================
    // ABA 3: MUNDO E MAPA (WORLD & MAP CHEATS)
    // =========================================================================
    private void setupWorldTab() {
        int startY = 74;
        int col1X = 20;
        int col2X = 310;
        int btnW = 270;

        // Seção Horário
        FormLabel lblTime = new FormLabel("=== Controle do Tempo e Horario ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY);
        worldTab.addComponent(lblTime);

        // Definir Manhã
        FormTextButton dayBtn = new FormTextButton("🌅 Mudar para MANHA (06:00)", col1X, startY + 24, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        dayBtn.onClicked(e -> {
            dayBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.SET_DAY);
        });
        worldTab.addComponent(dayBtn);

        // Definir Meio-Dia
        FormTextButton noonBtn = new FormTextButton("☀️ Mudar para MEIO-DIA (12:00)", col2X, startY + 24, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        noonBtn.onClicked(e -> {
            noonBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.SET_NOON);
        });
        worldTab.addComponent(noonBtn);

        // Definir Noite
        FormTextButton nightBtn = new FormTextButton("🌙 Mudar para NOITE (20:00)", col1X, startY + 64, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        nightBtn.onClicked(e -> {
            nightBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.SET_NIGHT);
        });
        worldTab.addComponent(nightBtn);

        // Seção Limpeza
        FormLabel lblClean = new FormLabel("=== Limpeza e Entidades ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 114);
        worldTab.addComponent(lblClean);

        // Eliminar Monstros Hostis
        FormTextButton killHostilesBtn = new FormTextButton("💀 Eliminar Monstros Hostis", col1X, startY + 138, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        killHostilesBtn.onClicked(e -> {
            killHostilesBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.KILL_HOSTILES);
        });
        worldTab.addComponent(killHostilesBtn);

        // Limpar Itens do Chão
        FormTextButton clearDropsBtn = new FormTextButton("🧹 Limpar Itens do Chao", col2X, startY + 138, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        clearDropsBtn.onClicked(e -> {
            clearDropsBtn.startCooldown(400);
            sendCheat(PacketCheatAction.CheatType.CLEAR_DROPS);
        });
        worldTab.addComponent(clearDropsBtn);

        // Seção Mapa (Gera partes ainda não exploradas no Servidor e sincroniza no Mapa do Cliente)
        FormLabel lblMap = new FormLabel("=== Exploracao e Geracao do Mapa ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 188);
        worldTab.addComponent(lblMap);

        // Gerar e Revelar Área ao Redor (Raio 160 Tiles)
        FormTextButton revealNearBtn = new FormTextButton("🗺️ Gerar e Revelar Area (Raio 160)", col1X, startY + 212, btnW, FormInputSize.SIZE_32, ButtonColor.GREEN);
        revealNearBtn.onClicked(e -> {
            revealNearBtn.startCooldown(1000);
            revealCurrentLevelMap(160);
        });
        worldTab.addComponent(revealNearBtn);

        // Gerar e Revelar Ilha / Região Ampla (Raio 280 Tiles)
        FormTextButton revealMapBtn = new FormTextButton("🌍 Gerar e Revelar Ilha Inteira (Raio 280)", col2X, startY + 212, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        revealMapBtn.onClicked(e -> {
            revealMapBtn.startCooldown(2000);
            revealCurrentLevelMap(280);
        });
        worldTab.addComponent(revealMapBtn);
    }

    // =========================================================================
    // ABA 4: VILAREJO E CONSTRUTOR REAL (VILLAGE & CASTLE GENERATOR)
    // =========================================================================
    private void setupVillageTab() {
        int startY = 74;
        int col1X = 20;
        int col2X = 310;
        int btnW = 270;
        int fullW = btnW * 2 + 20;

        // Seção Principal: Vilarejo Completo
        FormLabel lblMain = new FormLabel("=== Gerador de Vilarejo Real Completo ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY);
        villageTab.addComponent(lblMain);

        FormLabel lblDesc = new FormLabel(
            "Gera as regioes nao exploradas, cria Castelo, 6 Casas de NPC, 2 Salas de Baus e atualiza o Mapa!",
            new FontOptions(11),
            FormLabel.ALIGN_LEFT,
            col1X,
            startY + 20
        );
        villageTab.addComponent(lblDesc);

        FormTextButton fullVillageBtn = new FormTextButton(
            "🏰 GERAR VILAREJO COMPLETO (Castelo + Casas NPC + Baus)",
            col1X,
            startY + 40,
            fullW,
            FormInputSize.SIZE_32,
            ButtonColor.GREEN
        );
        fullVillageBtn.onClicked(e -> {
            fullVillageBtn.startCooldown(1500);
            sendBuildStructure(StructureType.FULL_VILLAGE);
        });
        villageTab.addComponent(fullVillageBtn);

        // Seção Estruturas Individuais
        FormLabel lblParts = new FormLabel("=== Construir Estruturas Individuais (Na sua Posicao) ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 92);
        villageTab.addComponent(lblParts);

        // Apenas Castelo Real
        FormTextButton castleBtn = new FormTextButton("👑 Construir Castelo Real + Oficinas", col1X, startY + 116, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        castleBtn.onClicked(e -> {
            castleBtn.startCooldown(1000);
            sendBuildStructure(StructureType.CASTLE_ONLY);
        });
        villageTab.addComponent(castleBtn);

        // Apenas Sala de Baús
        FormTextButton chestRoomBtn = new FormTextButton("📦 Construir Sala de Baus (Armazem)", col2X, startY + 116, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        chestRoomBtn.onClicked(e -> {
            chestRoomBtn.startCooldown(1000);
            sendBuildStructure(StructureType.CHEST_ROOM_ONLY);
        });
        villageTab.addComponent(chestRoomBtn);

        // Apenas Casa de NPC
        FormTextButton npcHouseBtn = new FormTextButton("🏠 Construir Casa de NPC Mobiliada", col1X, startY + 156, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        npcHouseBtn.onClicked(e -> {
            npcHouseBtn.startCooldown(800);
            sendBuildStructure(StructureType.NPC_HOUSE_ONLY);
        });
        villageTab.addComponent(npcHouseBtn);

        // Nivelar e Limpar Terreno
        FormTextButton flattenBtn = new FormTextButton("🚜 Limpar e Nivelar Terreno (35x35)", col2X, startY + 156, btnW, FormInputSize.SIZE_32, ButtonColor.BASE);
        flattenBtn.onClicked(e -> {
            flattenBtn.startCooldown(800);
            sendBuildStructure(StructureType.FLATTEN_AREA);
        });
        villageTab.addComponent(flattenBtn);

        // Seção NPCs e Mapa do Vilarejo
        FormLabel lblNpcs = new FormLabel("=== Povoamento de NPCs & Sincronizacao do Mapa ===", new FontOptions(14), FormLabel.ALIGN_LEFT, col1X, startY + 208);
        villageTab.addComponent(lblNpcs);

        FormTextButton spawnNpcsBtn = new FormTextButton(
            "🧑‍🌾 Spawnar 8 NPCs no Vilarejo",
            col1X,
            startY + 232,
            btnW,
            FormInputSize.SIZE_32,
            ButtonColor.BASE
        );
        spawnNpcsBtn.onClicked(e -> {
            spawnNpcsBtn.startCooldown(1000);
            sendBuildStructure(StructureType.SPAWN_NPCS);
        });
        villageTab.addComponent(spawnNpcsBtn);

        FormTextButton syncVillageMapBtn = new FormTextButton(
            "🗺️ Recarregar Vilarejo no Mapa",
            col2X,
            startY + 232,
            btnW,
            FormInputSize.SIZE_32,
            ButtonColor.BASE
        );
        syncVillageMapBtn.onClicked(e -> {
            syncVillageMapBtn.startCooldown(1000);
            revealCurrentLevelMap(80);
        });
        villageTab.addComponent(syncVillageMapBtn);
    }

    private void revealCurrentLevelMap(int radiusTiles) {
        Client client = getClient();
        if (client == null) {
            return;
        }
        PlayerMob player = client.getPlayer();
        int cx = (player != null) ? player.getTileX() : 0;
        int cy = (player != null) ? player.getTileY() : 0;

        // 1. Solicita ao Servidor que gere todas as regiões 16x16 ainda não geradas na área e envie os tiles para o Cliente
        if (client.network != null) {
            client.network.sendPacket(new PacketRevealMap(cx, cy, radiusTiles));
        }

        // 2. Agenda também a varredura local no ClientDiscoveredMap (suportando coordenadas negativas e positivas)
        RegionLoaderHelper.scheduleClientMapReveal(
            client,
            cx - radiusTiles,
            cy - radiusTiles,
            cx + radiusTiles,
            cy + radiusTiles
        );
    }

    @Override
    public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
        Client client = getClient();
        if (client != null) {
            RegionLoaderHelper.tickClientMapReveal(client);
        }
        if (this.currentTabIndex == 1) {
            syncPlayerBuffButtons();
        }
        super.draw(tickManager, perspective, renderBox);
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

    private void sendBuildStructure(StructureType type) {
        Client client = getClient();
        if (client != null && client.network != null) {
            PlayerMob player = client.getPlayer();
            int tx = (player != null) ? player.getTileX() : 0;
            int ty = (player != null) ? player.getTileY() : 0;
            client.network.sendPacket(new PacketBuildStructure(type, tx, ty));
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
        // Mantém a janela dentro dos limites da tela sem forçar recentralização se o usuário já moveu
        if (window != null) {
            int maxX = Math.max(0, window.getHudWidth() - getWidth());
            int maxY = Math.max(0, window.getHudHeight() - getHeight());
            setPosition(
                Math.max(0, Math.min(getX(), maxX)),
                Math.max(0, Math.min(getY(), maxY))
            );
        }
    }

    public static void init(MainGameFormManager formManager) {
        if (activeInstance != null) {
            lastHiddenState = activeInstance.isHidden();
            activeInstance.dispose();
        }
        ItemSpawnerForm form = new ItemSpawnerForm(formManager);
        form.setHidden(lastHiddenState);
        formManager.addComponent(form);
    }

    public static void toggle() {
        if (activeInstance != null) {
            boolean newHidden = !activeInstance.isHidden();
            activeInstance.setHidden(newHidden);
            lastHiddenState = newHidden;
            if (!newHidden) {
                activeInstance.syncPlayerBuffButtons();
            }
        }
    }

    public static void close() {
        if (activeInstance != null) {
            activeInstance.setHidden(true);
            lastHiddenState = true;
        }
    }

    public static boolean isOpen() {
        return activeInstance != null && !activeInstance.isHidden();
    }

    public static ItemSpawnerForm getActiveInstance() {
        return activeInstance;
    }
}

