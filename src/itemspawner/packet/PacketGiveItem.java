package itemspawner.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class PacketGiveItem extends Packet {

    private static final int MAX_SPAWN_AMOUNT = 99999;

    public final String itemStringID;
    public final int amount;

    public PacketGiveItem(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.itemStringID = reader.getNextStringLong();
        this.amount = reader.getNextInt();
    }

    public PacketGiveItem(String itemStringID, int amount) {
        this.itemStringID = itemStringID == null ? "" : itemStringID.trim();
        this.amount = Math.max(1, Math.min(amount, MAX_SPAWN_AMOUNT));
        PacketWriter writer = new PacketWriter(this);
        writer.putNextStringLong(this.itemStringID);
        writer.putNextInt(this.amount);
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient serverClient) {
        if (serverClient == null || serverClient.playerMob == null || this.itemStringID == null || this.itemStringID.isEmpty()) {
            return;
        }

        Item item = ItemRegistry.getItem(this.itemStringID);
        if (item != null) {
            int count = Math.max(1, Math.min(this.amount, MAX_SPAWN_AMOUNT));
            InventoryItem invItem = new InventoryItem(item, count);
            serverClient.playerMob.getInv().addItemsDropRemaining(invItem, "itemspawner", serverClient.playerMob, true, false);
            serverClient.playerMob.getInv().markFullDirty();
        }
    }
}

