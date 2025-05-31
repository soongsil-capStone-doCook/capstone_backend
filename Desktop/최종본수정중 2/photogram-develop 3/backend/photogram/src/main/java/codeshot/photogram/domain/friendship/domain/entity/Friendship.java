package codeshot.photogram.domain.friendship.domain.entity;

import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Friendship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 친구 요청 보낸 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;

    // 친구 요청 받은 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    // 수락 여부
    @Column(nullable = false)
    private boolean accepted;

    @Builder
    public Friendship(Member requester, Member receiver, boolean accepted) {
        this.requester = requester;
        this.receiver = receiver;
        this.accepted = accepted;
    }

    public void accept() {
        this.accepted = true;
    }
}

