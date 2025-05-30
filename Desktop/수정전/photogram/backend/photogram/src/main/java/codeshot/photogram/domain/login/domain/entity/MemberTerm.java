package codeshot.photogram.domain.login.domain.entity;

import codeshot.photogram.domain.member.domain.entity.Member;
import codeshot.photogram.global.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class MemberTerm extends BaseEntity {

    @EmbeddedId
    private MemberTermId id = new MemberTermId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("termId")
    @JoinColumn(name = "term_id")
    private Term term;

    @Builder
    public MemberTerm(Member member, Term term) {
        this.member = member;
        this.term = term;
        this.id = new MemberTermId(member.getMemberID(), term.getId());
    }
}

