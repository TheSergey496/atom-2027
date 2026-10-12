package org.prolegacy.atom2027.common.queue;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "queue_tasks", indexes = {
        @Index(
                name = "queue_tasks_name_time_desc_idx",
                columnList = "queue_name, next_process_at, id DESC"
        ),
        @Index(
                name = "idx_queue_pick",
                columnList = "queue_name, next_process_at"
        ),
        @Index(
                name = "idx_queue_business_created",
                columnList = "queue_name, business_key, created_at, id",
                options = "WHERE business_key IS NOT NULL"
        )
})
public class QueueTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queue_name", nullable = false, columnDefinition = "TEXT")
    private String queueName;

    @Column(name = "business_key", columnDefinition = "TEXT")
    private String businessKey;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "next_process_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private Instant nextProcessAt;

    @Column(name = "attempt", columnDefinition = "INTEGER DEFAULT 0")
    private Integer attempt;

    @Column(name = "reenqueue_attempt", columnDefinition = "INTEGER DEFAULT 0")
    private Integer reenqueueAttempt;

    @Column(name = "total_attempt", columnDefinition = "INTEGER DEFAULT 0")
    private Integer totalAttempt;
}
