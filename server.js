import express from 'express';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 3000;

app.use(express.json());
app.use('/resources', express.static(path.join(__dirname, 'resources')));
app.use(express.static(path.join(__dirname, 'public')));

const SOURCE_FILES = [
  { path: 'src/itemspawner/ItemSpawnerMod.java', title: 'ItemSpawnerMod.java', group: 'Core' },
  { path: 'src/itemspawner/world/RegionLoaderHelper.java', title: 'RegionLoaderHelper.java (NOVO)', group: 'World Builder' },
  { path: 'src/itemspawner/packet/PacketRevealMap.java', title: 'PacketRevealMap.java (NOVO)', group: 'Network' },
  { path: 'src/itemspawner/world/VillageGenerator.java', title: 'VillageGenerator.java', group: 'World Builder' },
  { path: 'src/itemspawner/packet/PacketBuildStructure.java', title: 'PacketBuildStructure.java', group: 'Network' },
  { path: 'src/itemspawner/ui/ItemSpawnerForm.java', title: 'ItemSpawnerForm.java', group: 'UI' },
  { path: 'src/itemspawner/ui/FormSpawnerItemSlot.java', title: 'FormSpawnerItemSlot.java', group: 'UI' },
  { path: 'src/itemspawner/packet/PacketCheatAction.java', title: 'PacketCheatAction.java', group: 'Network' },
  { path: 'src/itemspawner/packet/PacketGiveItem.java', title: 'PacketGiveItem.java', group: 'Network' },
  { path: 'src/itemspawner/patch/ItemSpawnerHotkeyPatch.java', title: 'ItemSpawnerHotkeyPatch.java', group: 'Patches' },
  { path: 'src/itemspawner/patch/ItemSpawnerHotkeyPatchLogic.java', title: 'ItemSpawnerHotkeyPatchLogic.java', group: 'Patches' },
  { path: 'src/itemspawner/patch/ItemSpawnerHudPatch.java', title: 'ItemSpawnerHudPatch.java', group: 'Patches' },
  { path: 'src/itemspawner/patch/ItemSpawnerHudPatchLogic.java', title: 'ItemSpawnerHudPatchLogic.java', group: 'Patches' },
  { path: 'build.ps1', title: 'build.ps1', group: 'Build & Config' },
  { path: 'resources/mod.info', title: 'mod.info', group: 'Build & Config' },
  { path: 'README.md', title: 'README.md', group: 'Build & Config' }
];

app.get('/api/files', (req, res) => {
  const result = SOURCE_FILES.map((item) => {
    const fullPath = path.join(__dirname, item.path);
    let content = '';
    try {
      content = fs.readFileSync(fullPath, 'utf-8');
    } catch (err) {
      content = `// Erro ao ler arquivo: ${item.path}`;
    }
    return {
      ...item,
      lines: content.split('\n').length,
      content
    };
  });
  res.json(result);
});

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Necesse Cheat Menu v2.1.0 Inspector & Simulator running at http://0.0.0.0:${PORT}`);
});
