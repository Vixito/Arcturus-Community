package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.CatalogSearchResultComposer;

public class CatalogSearchedItemEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int offerId = this.packet.readInt();
        CatalogManager catalogManager = Emulator.getGameEnvironment().getCatalogManager();

        CatalogItem item = catalogManager.getCatalogItem(offerId);
        if (item == null) {
            int itemId = catalogManager.offerDefs.get(offerId);
            if (itemId != 0) {
                item = catalogManager.getCatalogItem(itemId);
            }
        }
        if (item == null) {
            item = catalogManager.getCatalogItemBySprite(offerId);
        }

        if (item != null) {
            int userRank = this.client.getHabbo().getHabboInfo().getRank().getId();
            CatalogPage page = catalogManager.getCatalogPage(item.getPageId());
            if (page == null) {
                return;
            }

            CatalogPage cursor = page;
            int guard = 0;
            while (cursor != null && cursor.getId() > 0 && guard++ < 15) {
                if (!cursor.isVisible() || !cursor.isEnabled() || cursor.getRank() > userRank) {
                    return; // User cannot view or buy hidden or staff-restricted items
                }
                if (cursor.getParentId() <= 0) {
                    break;
                }
                cursor = catalogManager.getCatalogPage(cursor.getParentId());
            }

            this.client.sendResponse(new CatalogSearchResultComposer(item));
        }
    }
}
