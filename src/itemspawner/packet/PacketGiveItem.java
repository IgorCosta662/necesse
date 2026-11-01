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
    public final String itemStringID;
    public final int amount;

    public PacketGiveItem(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.itemStringID = reader.getNextStringLong();
        this.amount = reader.getNextInt();
    }

    public PacketGiveItem(String itemStringID, int amount) {
        this.itemStringID = itemStringID;
        this.amount = amount;
        PacketWriter writer = new PacketWriter(this);
        writer.putNextStringLong(itemStringID);
        writer.putNextInt(amount);
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient serverClient) {
        if (serverClient != null && serverClient.playerMob != null) {
            Item item = ItemRegistry.getItem(this.itemStringID);
            if (item != null) {
                int count = Math.max(1, this.amount);
                InventoryItem invItem = new InventoryItem(item, count);
                serverClient.playerMob.getInv().addItemsDropRemaining(invItem, "itemspawner", serverClient.playerMob, true, false);
                serverClient.playerMob.getInv().markFullDirty();
            }
        }
    }
}

