package org.prolegacy.atom2027.common.audit;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.prolegacy.atom2027.common.auth.entities.id.UserId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class Auditable {
    @CreatedDate
    @EqualsAndHashCode.Exclude
    private Instant createdDate;

    @LastModifiedDate
    @EqualsAndHashCode.Exclude
    private Instant modifiedDate;

    @CreatedBy
    @EqualsAndHashCode.Exclude
    private UserId createdBy;

    @LastModifiedBy
    @EqualsAndHashCode.Exclude
    private UserId modifiedBy;
}
