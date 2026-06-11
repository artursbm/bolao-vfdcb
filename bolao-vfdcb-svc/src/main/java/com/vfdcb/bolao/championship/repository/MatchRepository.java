package com.vfdcb.bolao.championship.repository;

import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByStatusInAndMatchTimeAfterOrderByMatchTimeAsc(List<MatchStatus> statuses, ZonedDateTime today);

    List<Match> findAllByOrderByMatchTimeAsc();

    Optional<Match> findByExternalId(Long externalId);

    List<Match> findAllByStatus(MatchStatus status);
}
