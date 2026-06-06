package com.vfdcb.bolao.championship.repository;

import com.vfdcb.bolao.championship.model.Match;
import com.vfdcb.bolao.championship.model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByStatusInOrderByMatchTimeAsc(List<MatchStatus> statuses);

    List<Match> findAllByOrderByMatchTimeAsc();

    java.util.Optional<Match> findByExternalId(Long externalId);
}
