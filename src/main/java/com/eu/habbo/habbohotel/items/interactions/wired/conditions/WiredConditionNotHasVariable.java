package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionNotHasVariable extends WiredConditionHasVariable {
    public WiredConditionNotHasVariable(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.setNegative(true);
    }

    public WiredConditionNotHasVariable(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.setNegative(true);
    }
}
