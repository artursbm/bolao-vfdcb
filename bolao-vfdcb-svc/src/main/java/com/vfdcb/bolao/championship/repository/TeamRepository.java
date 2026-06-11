package com.vfdcb.bolao.championship.repository;

import com.vfdcb.bolao.championship.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    Optional<Team> findByExternalId(Long externalId);

    Optional<Team> findByCode(String code);

    @Query(value = "SELECT count(*) AS exact_count FROM Team t")
    Integer countTeams();
}
