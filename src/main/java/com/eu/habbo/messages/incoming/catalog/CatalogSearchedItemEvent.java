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

        int itemId = Emulator.getGameEnvironment().getCatalogManager().offerDefs.get(offerId);

        if (itemId != 0) {
            CatalogItem item = Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(itemId);

            if (item != null) {
                this.client.sendResponse(new CatalogSearchResultComposer(item));
                return;
            }
        }
    }
}
