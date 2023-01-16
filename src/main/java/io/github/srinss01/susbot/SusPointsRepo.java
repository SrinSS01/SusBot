package io.github.srinss01.susbot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SusPointsRepo extends JpaRepository<SusPoints, SusPoints.ID> {
    @Query("select new io.github.srinss01.susbot.Points(s.tag, s.susPoints) from SusPoints s where s.guildId = ?1 order by s.susPoints DESC, s.tag")
    Page<Points> findByGuildIdOrderBySusPointsDescTagAsc(long guildId, Pageable pageable);

}
