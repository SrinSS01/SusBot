package me.srin.susbot;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Scanner;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.*;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    static final MongoClient mongoClient = MongoClients.create("mongodb+srv://srin:ynmIyXCLcb4dY6tx@bootssusbot.uivmy.mongodb.net/sustem?retryWrites=true&w=majority");
    static final MongoDatabase sustem = mongoClient.getDatabase("sustem");
    static final MongoCollection<Document> suspoints = sustem.getCollection("suspoints");

    public static void main(String[] args) throws LoginException, InterruptedException {
        String token = "OTcyNTI5MDM2NDY1MTcyNTkw.GAhuua.KFpfaok336np6oB9ni4xWOHkNnHRMfzIJXVVNU";
        var jda = JDABuilder.createDefault(
                        token,
                        GUILD_MESSAGES,
                        GUILD_MESSAGE_REACTIONS,
                        GUILD_VOICE_STATES,
                        GUILD_EMOJIS,
                        GUILD_MEMBERS,
                        GUILD_PRESENCES
                ).setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CLIENT_STATUS)
                .disableCache(VOICE_STATE, EMOTE).addEventListeners(Events.INSTANCE)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.of(Activity.ActivityType.DEFAULT, "amongus"))
                .build();
        for (Guild guild : jda.awaitReady().getGuilds()) {
            guild
                .upsertCommand("sus", "grants a sus point to the user mentioned")
                .addOption(OptionType.USER, "member", "mention the user to give sus points to", true)
                .queue();
            guild
                .upsertCommand("leaderboard", "shows sus leaderboard")
                .queue();
            String guild_id = guild.getId();
            for (Member member : guild.getMembers()) {
                String memberId = member.getId();
                var result = suspoints.find(
                    Filters.and(
                        Filters.eq("guild_id", guild_id),
                        Filters.eq("user_id", memberId)
                    )
                ).first();
                if (result == null) {
                    User user = member.getUser();
                    if (user.isBot()) continue;
                    var tag = user.getAsTag();
                    suspoints.insertOne(
                            new Document()
                                    .append("user_id", memberId)
                                    .append("guild_id", guild_id)
                                    .append("tag", tag)
                                    .append("sus_points", 0)
                                    .append("lastCommandTime", 0L)
                    );
                    LOGGER.info("inserted " + tag + " guild " + guild_id);
                }
            }
        }
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String command = scanner.nextLine();
                if (command.equals("exit")) {
                    jda.shutdownNow();
                    break;
                }
            }
        }, "console").start();
    }
}
