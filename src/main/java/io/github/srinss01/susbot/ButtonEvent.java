package io.github.srinss01.susbot;

import lombok.AllArgsConstructor;
import lombok.val;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

import static io.github.srinss01.susbot.MessageSentEvent.*;

@Component
@AllArgsConstructor
public class ButtonEvent extends ListenerAdapter {
    SusPointsRepo repo;
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        val messageIdLong = event.getMessage().getIdLong();
        val guildIdLong = Objects.requireNonNull(event.getGuild()).getIdLong();
        var page = MessageSentEvent.PAGEABLE_HASH_MAP.get(messageIdLong);
        event.deferEdit().queue();
        if (page != null) {
            val id = event.getButton().getId();
            int lastPageNumber = page.getTotalPages() - 1;
            Pageable pageable = page.getPageable();
            switch (Objects.requireNonNull(id)) {
                case "prev" -> {
                    if (page.isFirst()) {
                        pageable = pageable.withPage(lastPageNumber);
                    } else {
                        pageable = page.previousPageable();
                    }
                }
                case "next" -> {
                    if (page.isLast()) {
                        pageable = pageable.withPage(0);
                    } else {
                        pageable = page.nextPageable();
                    }
                }
            }
            val newPage = repo.findByGuildIdOrderBySusPointsDescTagAsc(guildIdLong, pageable);
            val pageNumber = pageable.getPageNumber() + 1;
            String fileName = "leaderboard" + System.currentTimeMillis() + ".png";
            val object = new PointsObject(
                    ((pageNumber - 1) * 10) + 1, newPage.getContent()
            );
            val json = GSON.toJson(object);
            RequestBody body = RequestBody.create(json, mediaType);
            Request request = new Request.Builder()
                    .url("https://susbot-next-app.vercel.app/api/og")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            try (
                    Response response = client.newCall(request).execute()
            ) {
                val responseBody = response.body();
                if (responseBody == null) {
                    return;
                }
                event.getHook().editOriginalEmbeds(new EmbedBuilder().setImage("attachment://" + fileName).build()).setAttachments(
                        FileUpload.fromData(responseBody.bytes(), fileName)
                ).queue(message ->
                        message.editMessageComponents(ActionRow.of(
                        Button.primary("prev", "prev"),
                        Button.primary("number", String.valueOf(pageNumber)).asDisabled(),
                        Button.primary("next", "next")
                )).queue());
            } catch (IOException ignored) {}

            PAGEABLE_HASH_MAP.put(messageIdLong, newPage);
        }
    }
}
