package com.vfdcb.bolao.championship.repository;

import com.vfdcb.bolao.championship.model.Guess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuessRepository extends JpaRepository<Guess, UUID> {
    Optional<Guess> findByUserIdAndMatchId(UUID userId, UUID matchId);
    List<Guess> findByMatchId(UUID matchId);

    // This interface projection is used for the GetRanking query
    interface UserRankingProjection {
        UUID getUserId();
        String getUserName();
        Integer getTotalScore();
    }

    @Query("SELECT u.id as userId, u.name as userName, COALESCE(SUM(g.points), 0) as totalScore " +
           "FROM User u " +
           "LEFT JOIN Guess g ON u.id = g.userId " +
           "GROUP BY u.id, u.name " +
           "ORDER BY totalScore DESC, userName ASC")
    List<UserRankingProjection> getRanking();
}
