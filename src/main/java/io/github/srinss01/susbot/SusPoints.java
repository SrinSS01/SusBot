package io.github.srinss01.susbot;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "users")
@IdClass(SusPoints.ID.class)
@Getter @Setter
@ToString
public class SusPoints {
    @Id
    private long userId;
    @Id private long guildId;
    String tag;
    int susPoints;
    long lastCommandTime;

    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @ToString
    @Getter
    static class ID implements Serializable {
        private long userId;
        private long guildId;
    }
}
