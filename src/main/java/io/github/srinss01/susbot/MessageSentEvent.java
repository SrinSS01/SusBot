package io.github.srinss01.susbot;

import lombok.AllArgsConstructor;
import lombok.val;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
public class MessageSentEvent extends ListenerAdapter {
    SusPointsRepo repo;
    private static final Pattern SUSSY_PATTERN = Pattern.compile("sus|su|sussy|baka", Pattern.CASE_INSENSITIVE);
    private static final Random RANDOM = new Random();
    private static final String SUS_EMOTES = "<a:susrainbow:981399317703163954> <a:sustwerk:981399316935630960>";

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = event.getAuthor();
        List<User> mentionedUsers = message.getMentions().getUsers();
        long guildID = event.getGuild().getIdLong();

        if (!author.isBot() && isSussy(message.getContentRaw()) && mentionedUsers.size() != 0) {
            val theOneWhoSussed = author.getIdLong();
            val theOneWhoSussedOptional = repo.findById(SusPoints.ID.of(theOneWhoSussed, guildID));
            if (theOneWhoSussedOptional.isPresent()) {
                val _theOneWhoSussed = theOneWhoSussedOptional.get();
                val lastCommandTime = _theOneWhoSussed.getLastCommandTime();
                long timeElapsedFromInSec = timeElapsedFromInSec(lastCommandTime);
                if (timeElapsedFromInSec < 300) {
                    return;
                }

                val victim = mentionedUsers.get(0);
                val victimIdLong = victim.getIdLong();
                if (victim.isBot() || victimIdLong == theOneWhoSussed) return;
                val victimOptional = repo.findById(SusPoints.ID.of(victimIdLong, guildID));
                if (victimOptional.isPresent()) {
                    _theOneWhoSussed.setLastCommandTime(System.currentTimeMillis());
                    val _victim = victimOptional.get();
                    val incrementValue = sus(_theOneWhoSussed, _victim);
                    if (incrementValue == 1)
                        message.replyFormat("+1 sus to %s %s", victim.getAsMention(), SUS_EMOTES)
                                .mentionRepliedUser(false)
                                .queue();
                    else message
                            .replyFormat("%s is not sus\n-1 sus to %s %s", victim.getName(), victim.getAsMention(), SUS_EMOTES)
                            .mentionRepliedUser(false)
                            .queue();
                }
            }
        }
    }

    private static int getIncrementValue() {
        return RANDOM.nextInt(0, 10) == RANDOM.nextInt(0, 10)? -1: +1;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        val guildIdLong = Objects.requireNonNull(event.getGuild()).getIdLong();
        switch (event.getName()) {
            case "sus" -> {
                val theOneWhoSussed = event.getUser().getIdLong();
                val theOneWhoSussedOptional = repo.findById(SusPoints.ID.of(theOneWhoSussed, guildIdLong));
                if (theOneWhoSussedOptional.isPresent()) {
                    val _theOneWhoSussed = theOneWhoSussedOptional.get();
                    val lastCommandTime = _theOneWhoSussed.getLastCommandTime();
                    long timeElapsedFromInSec = timeElapsedFromInSec(lastCommandTime);
                    if (timeElapsedFromInSec < 300) {
                        event.reply("you can use sus again after %d sec".formatted(300 - timeElapsedFromInSec)).setEphemeral(true).queue();
                        return;
                    }
                    User victim = Objects.requireNonNull(event.getOption("member")).getAsUser();
                    var victimId = victim.getIdLong();
                    if (victim.isBot()) {
                        event.reply("don't you dare sus me and my kind >:(").setEphemeral(true).queue();
                        return;
                    } else if (victimId == theOneWhoSussed) {
                        event.reply("you cannot sus yourself LMAO").setEphemeral(true).queue();
                        return;
                    }
                    val victimOptional = repo.findById(SusPoints.ID.of(victimId, guildIdLong));
                    if (victimOptional.isPresent()) {
                        val _victim = victimOptional.get();
                        val incrementValue = sus(_theOneWhoSussed, _victim);
                        if (incrementValue == 1)
                            event.replyFormat("+1 sus to %s %s", victim.getAsMention(), SUS_EMOTES)
                                    .mentionRepliedUser(false)
                                    .queue();
                        else event
                                .replyFormat("%s is not sus\n-1 sus to %s %s", victim.getName(), victim.getAsMention(), SUS_EMOTES)
                                .mentionRepliedUser(false)
                                .queue();
                    }
                }
            }
            case "leaderboard" -> event.reply("under maintenance").setEphemeral(true).queue();
        }
    }

    private int sus(SusPoints theOneWhoSussed, SusPoints victim) {
        val incrementValue = getIncrementValue();
        theOneWhoSussed.setLastCommandTime(System.currentTimeMillis());
        victim.setSus_points(victim.getSus_points() + incrementValue);
        repo.save(theOneWhoSussed);
        repo.save(victim);
        return incrementValue;
    }

    private static boolean isSussy(String contentRaw) {
        Matcher matcher = SUSSY_PATTERN.matcher(contentRaw);
        return matcher.find();
    }
    static long timeElapsedFromInSec(long startTime) {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
}
