package com.stocktrading.repository;

import com.stocktrading.model.ExperimentDecision;
import com.stocktrading.model.ExperimentSession;
import com.stocktrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExperimentDecisionRepository extends JpaRepository<ExperimentDecision, Long> {
    
    // Find all decisions for a session
    List<ExperimentDecision> findBySession(ExperimentSession session);
    
    // Find decisions for a specific stock in a session, ordered by day
    List<ExperimentDecision> findBySessionAndStockIndexOrderByDayNumberAsc(
        ExperimentSession session, 
        Integer stockIndex
    );
    
    // Find all decisions for a session, ordered by stock and day
    List<ExperimentDecision> findBySessionOrderByStockIndexAscDayNumberAsc(
        ExperimentSession session
    );
    
    // Count decisions for a session
    long countBySession(ExperimentSession session);

    // Fetch/count decisions across all sessions of a user
    List<ExperimentDecision> findBySession_User(User user);
    long countBySession_User(User user);

    @Query("SELECT d FROM ExperimentDecision d WHERE d.session.user.id = :userId")
    List<ExperimentDecision> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(d) FROM ExperimentDecision d WHERE d.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
}
