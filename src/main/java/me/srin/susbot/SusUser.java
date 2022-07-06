package me.srin.susbot;

import org.bson.Document;

import java.util.Map;

public record SusUser(String userId, String guildId, String tag, int sus_points, long lastCommandTime) {
    public static SusUser create(String userId, String guildId, String tag) {
        return new SusUser(userId, guildId, tag, 0, 0L);
    }
    public Document toDocument() {
        return new Document(Map.of(
                "user_id", userId,
                "guild_id", guildId,
                "tag", tag,
                "sus_points", sus_points,
                "lastCommandTime", lastCommandTime
        ));
    }
}
