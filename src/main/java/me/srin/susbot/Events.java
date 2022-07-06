package me.srin.susbot;

import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonInteraction;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Events extends ListenerAdapter {
    public static final Events INSTANCE = new Events();
    private static final Logger LOGGER = LoggerFactory.getLogger(Events.class);
    ArrayList<Pair<MessageEmbed, File>> messageEmbeds = new ArrayList<>();
    private static final Random random = new Random();
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color SILVER = new Color(192, 192, 192);
    private static final Color BRONZE = new Color(192, 128, 0);
    private static final Color color = new Color(234, 193, 106);
    private static final Pattern SUSSY_PATTERN = Pattern.compile("sus|su|sussy|baka", Pattern.CASE_INSENSITIVE);

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        LOGGER.info("shutting down...");
        Main.mongoClient.close();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        LOGGER.info("%s is ready".formatted(event.getJDA().getSelfUser().getName()));
    }
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        User user = event.getUser();
        if (user.isBot()) return;
        Document document = SusUser.create(user.getId(), event.getGuild().getId(), user.getAsTag()).toDocument();
        try {
            Main.suspoints.insertOne(document);
        } catch (MongoException e) {
            LOGGER.error(e.toString(), e);
        }
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        User user = event.getUser();
        if (user.isBot()) return;
        try {
            Main.suspoints.deleteOne(Filters.and(
                    Filters.eq("user_id", user.getId()),
                    Filters.eq("guild_id", event.getGuild().getId())
            ));
        } catch (MongoException e) {
            LOGGER.error(e.toString(), e);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = event.getAuthor();
        List<User> mentionedUsers = message.getMentionedUsers();
        try {
            if (!author.isBot() && isSussy(message.getContentRaw()) && mentionedUsers.size() != 0) {
                String theOneWhoSussed = author.getId();
                String guildID = event.getGuild().getId();
                Document document = Main.suspoints.find(Filters.and(
                        Filters.eq("user_id", theOneWhoSussed),
                        Filters.eq("guild_id", guildID)
                )).first();
                if (document != null) {
                    long timeCommandIssuedAt = document.getLong("lastCommandTime");
                    long timeElapsedFromInSec = timeElapsedFromInSec(timeCommandIssuedAt);
                    if (timeElapsedFromInSec < 300) {
                        return;
                    }
                }

                User sussyUser = mentionedUsers.get(0);
                String sussyUserId = sussyUser.getId();
                // you cannot sus a bot or yourself
                if (sussyUser.isBot() || sussyUserId.equals(author.getId())) return;
                final int incrementValue = random.nextInt(0, 10) == random.nextInt(0, 10)? -1: +1;
                Main.suspoints.updateOne(Filters.and(
                        Filters.eq("user_id", theOneWhoSussed),
                        Filters.eq("guild_id", guildID)
                ), Filters.and(
                        Filters.eq("$set", Filters.eq("lastCommandTime", System.currentTimeMillis()))
                ));
                Main.suspoints.updateOne(Filters.and(
                        Filters.eq("user_id", sussyUserId),
                        Filters.eq("guild_id", guildID)
                ), Filters.and(
                        Filters.eq("$inc", Filters.eq("sus_points", incrementValue))
                ));
                if (incrementValue == 1)
                    message
                        .reply("+1 sus to " + sussyUser.getAsMention() + " <a:susrainbow:981399317703163954> <a:sustwerk:981399316935630960>")
                        .mentionRepliedUser(false)
                        .queue();
                else message
                        .reply(sussyUser.getName() + " is not sus")
                        .mentionRepliedUser(false)
                        .queue(msg ->
                            msg
                              .reply("-1 sus to " + sussyUser.getAsMention() + " <a:susrainbow:981399317703163954> <a:sustwerk:981399316935630960>")
                              .queueAfter(1, TimeUnit.SECONDS));
            }
        } catch (MongoException e) {
            LOGGER.error(e.toString(), e);
            message.reply(e.toString()).queue();
        }
    }
    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        String commandName = event.getName();
        try {
            var guild_id = Objects.requireNonNull(event.getGuild()).getId();
            if (commandName.equals("sus")) {
                String theOneWhoSussed = event.getUser().getId();
                Document document = Main.suspoints.find(Filters.and(
                    Filters.eq("user_id", theOneWhoSussed),
                    Filters.eq("guild_id", guild_id)
                )).first();
//                var result = STATEMENT.executeQuery("select lastCommandTime from sustem where guild_id = " + guild_id + " and user_id = " + theOneWhoSussed);
                if (document != null) {
                    long timeCommandIssuedAt = document.getLong("lastCommandTime");
                    long timeElapsedFromInSec = timeElapsedFromInSec(timeCommandIssuedAt);
                    if (timeElapsedFromInSec < 300) {
                        event.reply("you can use sus again after %d sec".formatted(300 - timeElapsedFromInSec)).setEphemeral(true).queue();
                        return;
                    }
                }
                User victim = Objects.requireNonNull(event.getOption("member")).getAsUser();
                var victimId = victim.getId();
                if (victim.isBot()) {
                    event.reply("don't you dare sus me and my kind >:(").setEphemeral(true).queue();
                    return;
                } else if (victimId.equals(event.getUser().getId())) {
                    event.reply("you cannot sus yourself LMAO").setEphemeral(true).queue();
                    return;
                }
                final int incrementValue = random.nextInt(0, 10) == random.nextInt(0, 10)? -1: +1;
                Main.suspoints.updateOne(Filters.and(
                   Filters.eq("user_id", theOneWhoSussed),
                   Filters.eq("guild_id", guild_id)
                ), Filters.and(
                        Filters.eq("$set", Filters.eq("lastCommandTime", System.currentTimeMillis()))
                ));
                Main.suspoints.updateOne(Filters.and(
                   Filters.eq("user_id", victimId),
                   Filters.eq("guild_id", guild_id)
                ), Filters.and(
                        Filters.eq("$inc", Filters.eq("sus_points", incrementValue))
                ));
                if (incrementValue == 1)
                    event
                        .reply("+1 sus to " + victim.getAsMention() + " <a:susrainbow:981399317703163954> <a:sustwerk:981399316935630960>")
                        .mentionRepliedUser(false)
                        .queue();
                else event
                        .reply(victim.getName() + " is not sus")
                        .mentionRepliedUser(false)
                        .queue(interactionHook ->
                                interactionHook
                                    .sendMessage("-1 sus to " + victim.getAsMention() + " <a:susrainbow:981399317703163954> <a:sustwerk:981399316935630960>")
                                    .queueAfter(1, TimeUnit.SECONDS));
            } else if (commandName.equals("leaderboard")) {
                event.deferReply().queue(interactionHook -> {
                    try {
                        messageEmbeds.clear();
                        Document maxLengthTagQuery = Main.suspoints.aggregate(List.of(
                                Filters.eq("$match", Filters.eq("guild_id", guild_id)),
                                Filters.eq("$addFields", Filters.eq("length", Filters.eq("$strLenCP", "$tag"))),
                                Filters.eq("$group", Filters.and(
                                        Filters.eq("_id", "$_id"),
                                        Filters.eq("tag", Filters.eq("$first", "$tag")),
                                        Filters.eq("length", Filters.eq("$first", "$length"))
                                )),
                                Filters.eq("$sort", Filters.eq("length", -1)),
                                Filters.eq("$limit", 1)
                        )).first();
                        if (maxLengthTagQuery == null) return;
                        var tagWithMaxLength = maxLengthTagQuery.getString("tag");
                        //LOGGER.info(tagWithMaxLength);
                        final long[] countOfTag = { Main.suspoints.countDocuments(Filters.eq("guild_id", guild_id)) };
                        try (var result = Main.suspoints.aggregate(List.of(
                                Filters.eq("$match", Filters.eq("guild_id", guild_id)),
                                Filters.eq("$group", Filters.and(
                                        Filters.eq("_id", "$_id"),
                                        Filters.eq("user_id", Filters.eq("$first", "$user_id")),
                                        Filters.eq("tag", Filters.eq("$first", "$tag")),
                                        Filters.eq("sus_points", Filters.eq("$first", "$sus_points"))
                                )),
                                Filters.eq("$sort", Filters.and(
                                        Filters.eq("sus_points", -1),
                                        Filters.eq("tag", 1)
                                ))
                        )).iterator()) {
                            int imageMaxWidth = "#0  %s  sus: 12".formatted(tagWithMaxLength).length() * 5 + 150;
                            var imageSize = 25;
                            Guild guild = event.getGuild();
                            for (int k = 0; countOfTag[0] > 0; ) {
                                long size = countOfTag[0] - 10 > 0 ? 10 : countOfTag[0];
                                countOfTag[0] -= 10;
                                BufferedImage image = new BufferedImage(imageMaxWidth, (int) (imageSize * size + 1), BufferedImage.TYPE_INT_ARGB);
                                Graphics2D graphics = image.createGraphics();
                                for (int j = 0; j < size && result.hasNext(); j++, k++) {
                                    var doc = result.next();
                                    String user_id = doc.getString("user_id");
                                    int susPoints = doc.getInteger("sus_points");
                                    //LOGGER.info("user id: %s sus: %d".formatted(user_id, susPoints));
                                    Member member = guild.getMemberById(user_id);
                                    if (member == null) continue;
                                    User user = member.getUser();
                                    graphics.setColor(Color.BLACK);
                                    String avatarUrl = user.getAvatarUrl();
                                    if (avatarUrl == null) {
                                        avatarUrl = user.getDefaultAvatarUrl();
                                    }
                                    BufferedImage avatar = ImageIO.read(new URL(avatarUrl));
                                    graphics.setColor(Color.DARK_GRAY);
                                    graphics.fillRect(2, j * imageSize + 2, imageMaxWidth - 4, imageSize - 4);
                                    graphics.setColor(Color.WHITE);
                                    graphics.drawImage(avatar, 2, imageSize * j + 2, imageSize - 4, imageSize - 4, null);
                                    // set the font to bold
                                    graphics.setFont(graphics.getFont().deriveFont(Font.BOLD));
                                    // check if k + 1 is 1st place
                                    int textY = imageSize * j + (imageSize / 2 + 4);
                                    switch (k + 1) {
                                        case 1 -> {
                                            graphics.setColor(GOLD);
                                            graphics.drawString("#1", imageSize + 5, textY);
                                        }
                                        case 2 -> {
                                            graphics.setColor(SILVER);
                                            graphics.drawString("#2", imageSize + 5, textY);
                                        }
                                        case 3 -> {
                                            graphics.setColor(BRONZE);
                                            graphics.drawString("#3", imageSize + 5, textY);
                                        }
                                        default -> {
                                            graphics.setColor(Color.WHITE);
                                            graphics.drawString("#" + (k + 1), imageSize + 5, textY);
                                        }
                                    }
                                    // set the font to normal
                                    String rankStr = "#" + (k + 1);
                                    String nameStr = "  %s  ".formatted(user.getAsTag());
                                    String susPointsStr = "sus: " + susPoints;
                                    graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN));
                                    FontMetrics fontMetrics = graphics.getFontMetrics();
                                    int widthOfRank = fontMetrics.stringWidth(rankStr);
                                    int widthOfName = fontMetrics.stringWidth(nameStr);

                                    graphics.setColor(Color.PINK);
                                    graphics.drawString(nameStr, imageSize + 5 + widthOfRank, textY);
                                    graphics.setColor(color);
                                    graphics.drawString(susPointsStr, imageSize + 5 + widthOfRank + widthOfName, textY);
                                }
                                var file = new File("sus%d.png".formatted(k));
                                ImageIO.write(image, "png", file);
                                EmbedBuilder embed = new EmbedBuilder();
                                User user = event.getUser();
                                embed.setTitle("sus Leaderboard")
                                        .setDescription("list of all the sussy bakas in this server")
                                        .setImage("attachment://" + file.getName())
                                        .setColor(randomColor())
                                        .setFooter("requested by " + user.getAsTag(), user.getAvatarUrl());
                                messageEmbeds.add(Pair.of(embed.build(), file));
                            }
                            var first = messageEmbeds.get(0);
                            interactionHook.editOriginalComponents(
                                    ActionRow.of(
                                            Button.primary("left", Emoji.fromUnicode("\u25C0")),
                                            Button.primary("pageNumber", "1").asDisabled(),
                                            Button.primary("right", Emoji.fromUnicode("\u25B6"))
                                    )).setEmbeds(first.getLeft()).addFile(first.getRight()).queue();
                        }
                    } catch (MongoException e) {
                        LOGGER.error(e.toString(), e);
                        event.reply(e.toString()).queue();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (MongoException e) {
            LOGGER.error(e.toString(), e);
            event.reply(e.toString()).queue();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            event.reply("```\nerror occurred please contact srin :(\nerror: %s```".formatted(e)).queue();
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (messageEmbeds.isEmpty()) {
            event.deferEdit().queue();
            return;
        }
        var button = event.getButton();
        if (button == null) return;
        var id = button.getId();
        if (id == null) {
            return;
        }
        ButtonInteraction interaction = event.getInteraction();
        Message message = interaction.getMessage();
        Button pageNumberButton = message.getButtonById("pageNumber");
        Button left = message.getButtonById("left");
        Button right = message.getButtonById("right");
        if (pageNumberButton == null) {
            return;
        }
        int[] pageNumber = { Integer.parseInt(pageNumberButton.getLabel()) - 1 };

        if (id.equals("left")) {
            --pageNumber[0];
            if (pageNumber[0] < 0) pageNumber[0] += messageEmbeds.size();
            var page = messageEmbeds.get(pageNumber[0]);
            interaction.deferEdit().queue(interactionHook -> interactionHook.editOriginalComponents(
                    ActionRow.of(left, pageNumberButton.withLabel(String.valueOf(pageNumber[0] + 1)), right)
            ).setEmbeds(page.getLeft()).retainFilesById(List.of()).addFile(page.getRight()).queue());
        } else if (id.equals("right")) {
            pageNumber[0] = ++pageNumber[0] % messageEmbeds.size();
            var page = messageEmbeds.get(pageNumber[0]);
            interaction.deferEdit().queue(interactionHook -> interactionHook.editOriginalComponents(
                    ActionRow.of(left, pageNumberButton.withLabel(String.valueOf(pageNumber[0] + 1)), right)
            ).setEmbeds(page.getLeft()).retainFilesById(List.of()).addFile(page.getRight()).queue());
        }
    }

    static boolean isSussy(String str) {
        Matcher matcher = SUSSY_PATTERN.matcher(str);
        return matcher.find();
    }
    // random color generator
    static Color randomColor() {
        return new Color(random.nextInt(0, 255), random.nextInt(0, 255), random.nextInt(0, 255));
    }
    static long timeElapsedFromInSec(long startTime) {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
}
