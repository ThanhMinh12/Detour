package com.detour.trip;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "activity_votes", uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "member_id"}))
public class ActivityVote {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;
    @Column(name = "member_id", nullable = false)
    private UUID memberId;
    @Column(name = "vote_value", nullable = false)
    private int value;

    protected ActivityVote() {}

    public ActivityVote(Activity activity, UUID memberId, int value) {
        this.activity = activity;
        this.memberId = memberId;
        setValue(value);
    }

    public void setValue(int value) {
        if (value < -1 || value > 1) throw new IllegalArgumentException("vote value must be -1, 0, or 1");
        this.value = value;
    }

    public int getValue() { return value; }
}
