package com.firstgrab.domain.event.repository;

import com.firstgrab.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Event e
            SET e.sold = e.sold + :quantity
            WHERE e.id = :eventId
              AND e.deletedAt IS NULL
              AND e.sold + :quantity <= e.totalStock
            """)
    int increaseSoldIfAvailable(@Param("eventId") Long eventId, @Param("quantity") int quantity);
}
