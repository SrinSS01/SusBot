package io.github.srinss01.susbot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("suspoints")
@Getter @Setter
public class SusPoints {
    @Id
    ID id;
    String tag;
    int sus_points;
    long lastCommandTime;

    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    static class ID {
        long userId;
        long guildId;
    }
}
