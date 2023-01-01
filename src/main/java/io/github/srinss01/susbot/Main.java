package io.github.srinss01.susbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.Scanner;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;

@Component
@ComponentScan(
        basePackageClasses = {
            GuildEvents.class, MessageSentEvent.class
        },
        basePackages = "io.github.srinss01.susbot"
)
public class Main implements CommandLineRunner {
    @Value("${token}")
    String token;
    GuildEvents guildEvents;
    MessageSentEvent messageSentEvent;

    public Main(GuildEvents guildEvents, MessageSentEvent messageSentEvent) {
        this.guildEvents = guildEvents;
        this.messageSentEvent = messageSentEvent;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    @Override
    public void run(String... args) {
        LOGGER.info("Token: {}", token);
        var jda = JDABuilder.createDefault(
                        token
                ).enableIntents(
                        GUILD_PRESENCES,
                        GUILD_MEMBERS,
                        GUILD_MESSAGES,
                        GUILD_EMOJIS_AND_STICKERS,
                        GUILD_VOICE_STATES, MESSAGE_CONTENT
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.CLIENT_STATUS)
                .disableCache(
                        CacheFlag.EMOJI,
                        CacheFlag.STICKER,
                        CacheFlag.VOICE_STATE
                )
                .addEventListeners(guildEvents, messageSentEvent)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("amongus"))
                .build();
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String command = scanner.nextLine();
                if (command.equals("exit")) {
                    jda.shutdown();
                    break;
                }
            }
        }, "console").start();
    }
}
