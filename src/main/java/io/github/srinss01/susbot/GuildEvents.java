package io.github.srinss01.susbot;

import lombok.AllArgsConstructor;
import lombok.val;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GuildEvents extends ListenerAdapter {
    SusPointsRepo repo;
    private static final Logger LOGGER = LoggerFactory.getLogger(GuildEvents.class);

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        val guild = event.getGuild();
        guild.updateCommands().addCommands(
                Commands.slash("sus", "grants a sus point to the user mentioned")
                        .addOption(OptionType.USER, "member", "mention the user to give sus points to", true),
                Commands.slash("leaderboard", "shows sus leaderboard")
        ).queue();
        guild.getMembers().forEach(member -> insertMemberToGuild(guild, member));
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        insertMemberToGuild(event.getGuild(), event.getMember());
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        repo.deleteById(SusPoints.ID.of(event.getUser().getIdLong(), event.getGuild().getIdLong()));
    }

    void insertMemberToGuild(Guild guild, Member member) {
        if (member.getUser().isBot()) return;
        val guildIdLong = guild.getIdLong();
        val id = SusPoints.ID.of(member.getIdLong(), guildIdLong);
        val memberOptional = repo.findById(id);
        if (memberOptional.isEmpty()) {
            val entity = new SusPoints();
            entity.setGuildId(id.getGuildId());
            entity.setUserId(id.getUserId());
            entity.setSusPoints(0);
            val tag = member.getUser().getAsTag();
            entity.setTag(tag);
            entity.setLastCommandTime(0);
            repo.save(entity);
            LOGGER.info("inserted {} guild {}", tag, guildIdLong);
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOGGER.info("{} is ready", event.getJDA().getSelfUser().getName());
    }
}
