package com.eu.habbo.habbohotel.battlepass;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BattlePassMission {
    private final int id;
    private final int category;
    private final String type;
    private final String name;
    private final String description;
    private final String image;
    private final int rewardXp;
    private final int task;
    private final String extradata;

    public BattlePassMission(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.category = set.getInt("category");
        this.type = set.getString("type");
        this.name = set.getString("name");
        this.description = set.getString("description");
        this.image = set.getString("image");
        this.rewardXp = set.getInt("reward_xp");
        this.task = set.getInt("task");
        this.extradata = set.getString("extradata");
    }

    public int getId() {
        return this.id;
    }

    public int getCategory() {
        return this.category;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getImage() {
        return this.image;
    }

    public int getRewardXp() {
        return this.rewardXp;
    }

    public int getTask() {
        return this.task;
    }

    public String getExtradata() {
        return this.extradata;
    }
}
