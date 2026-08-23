package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.CatalogSearchResultComposer;
import gnu.trove.iterator.TIntObjectIterator;

public class CatalogSearchedItemEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int offerId = this.packet.readInt();

        CatalogItem item = Emulator.getGameEnvironment().getCatalogManager().getCatalogItemBySprite(offerId);
        if (item == null) {
            item = Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(offerId);
        }
        if (item == null) {
            int itemId = Emulator.getGameEnvironment().getCatalogManager().offerDefs.get(offerId);
            if (itemId != 0) {
                item = Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(itemId);
            }
        }

        if (item != null) {
            CatalogPage page = Emulator.getGameEnvironment().getCatalogManager().getCatalogPage(item.getPageId());
            if (page != null && page.getRank() > this.client.getHabbo().getHabboInfo().getRank().getId()) {
                return; // User cannot view or buy staff items
            }

            this.client.sendResponse(new CatalogSearchResultComposer(item));
            return;
        }
    }
}
