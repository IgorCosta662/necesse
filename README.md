# Necesse Cheat Menu & Item Spawner (v2.2.0)

Um mod completo e otimizado para **Necesse** que adiciona um **Cheat Menu, Item Spawner & Construtor de Vilarejo** com geração instantânea de Castelo Real, Casas de NPCs mobiliadas e Salas de Baús!

---

## 🌟 Funcionalidades e Abas do Menu (v2.2.0)

O menu é dividido em **4 abas principais**:

### 1. 📦 Aba de Itens (Item Spawner)
- **Pesquisa Instantânea com Cache**: Busque qualquer item por nome ou pelo ID interno sem travamentos (nomes e categorias pré-indexados em memória).
- **Filtro por Categorias**: Filtre rapidamente entre **`Todos`**, **`★ Fav`**, **`Combate`**, **`Ferram.`**, **`Consum.`** e **`Blocos`**.
- **Sistema de Favoritos (`★`)**: Clique com o **botão do meio do mouse** em qualquer item para destacá-lo com estrela dourada e acessá-lo na aba `★ Fav`.
- **Atalhos de Clique nos Slots**:
  - **Clique Esquerdo**: Entrega a quantidade selecionada (`x1`, `x5`, `x10`, `x50` ou `Max`).
  - **Clique Direito**: Entrega rapidamente `x1` unidade do item.
  - **Clique do Meio**: Adiciona ou remove o item dos Favoritos.
- **Renderização com Viewport Culling**: Apenas os itens visíveis na caixa de rolagem são renderizados a cada frame, mantendo o FPS alto.

### 2. ⚡ Aba do Jogador (Player Cheats)
- **🛡️ God Mode (Invencibilidade)**: Sincronizado em tempo real com os buffs do personagem e restaura a vida ao ativar.
- **⚡ Super Velocidade (Speed Boost)**: Aumenta consideravelmente sua velocidade de movimento com duração estendida (24h).
- **👻 Invisibilidade**: Fique invisível para os inimigos.
- **✨ Limpar Debuffs / Efeitos Ruins**: Remove efeitos negativos ativos no personagem.
- **🌟 Restaurar TUDO**: Cura Vida, Mana, Fome e limpa debuffs simultaneamente com 1 clique.
- **❤️ Curar Vida 100%** / **🔵 Restaurar Mana 100%** / **🍖 Saciar Fome 100%**.
- **💰 +1.000 Moedas** e **👑 +50.000 Moedas**.
- **🗑️ Limpar Mochila**: Remove itens da mochila principal preservando sua Hotbar (slots 0–9) e itens trancados.

### 3. 🌍 Aba de Mundo e Mapa (World & Map)
- **🌅 Mudar para Manhã (06:00)**, **☀️ Meio-Dia (12:00)** e **🌙 Noite (20:00)**.
- **💀 Eliminar Monstros Hostis**: Remove todos os inimigos hostis do mapa/ilha atual com segurança (sem `ConcurrentModificationException`).
- **🧹 Limpar Itens do Chão**: Remove todos os itens jogados no chão para reduzir lag.
- **🗺️ Revelar Mapa Inteiro da Ilha**: Explora e revela toda a névoa da ilha atual com malha densa (sem buracos no minimapa).

### 4. 🏰 Aba de Vilarejo e Construções (Village & Castle Builder)
- **🏰 Gerar Vilarejo Completo (1 Clique)**: Constrói instantaneamente ao redor do jogador uma fortaleza de `49x46 tiles` contendo:
  - **Muralha Externa Fortificada** com 4 portões cardeais, avenidas pavimentadas de pedra e Praça Central iluminada.
  - **Castelo Real ao Norte (`19x14`)**: Com 4 torres de vigia nos cantos, Salão do Trono com tapete real e candelabros, Aposentos Reais (cama dourada, estantes, baús) e **Ala de Oficinas Completas** (`workstation`, `forge`, `ironanvil`, `alchemybench`, `carpentersbench`).
  - **6 Casas de NPCs Mobiliadas (`8x6` cada)**: 3 na Ala Oeste e 3 na Ala Leste, todas com piso e paredes de madeira, porta voltada para a avenida, cama, mesa, cadeira, baú e iluminação (atendendo aos requisitos de quarto de colono do Necesse).
  - **2 Grandes Salas de Baús / Armazéns (`13x9` cada)**: No setor Sul-Oeste e Sul-Leste, com fileiras organizadas de baús e corredores iluminados.
  - **Povoamento Automático de 8 NPCs**: Spawna Ancião (`elder`), Ferreiro (`blacksmith`), Mago (`mage`), Alquimista (`alchemist`), Caçador (`hunter`), Fazendeiro (`farmer`), Guarda (`guard`) e Minerador (`miner`).
- **Construções Individuais Avulsas**:
  - **👑 Construir Apenas Castelo Real + Oficinas**
  - **📦 Construir Apenas Sala de Baús (Armazém)**
  - **🏠 Construir Apenas Casa de NPC Mobiliada**
  - **🚜 Limpar e Nivelar Terreno (`35x35`)**
  - **🧑‍🌾 Spawnar 8 NPCs Moradores**

---

## 🎮 Controles e Atalhos

- **`F6`**: Abre ou fecha o Cheat Menu a qualquer momento (ignora teclas comuns caso você esteja digitando no chat ou na busca).
- **`ESC`**: Fecha o menu com detecção de borda (edge-trigger).
- **Botão no HUD**: Um botão de atalho com ícone de baú na barra lateral direita da tela (com textura em cache).

---

## 🛠️ Compilação e Instalação Automática

Execute o script PowerShell na raiz do projeto (detecta automaticamente o JDK 17+, a pasta da Steam e `%APPDATA%\Necesse\mods`):
```powershell
.\build.ps1
```

