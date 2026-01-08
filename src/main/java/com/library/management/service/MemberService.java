package com.library.management.service;

import com.library.management.entity.Member;
import java.util.List;

public interface MemberService {
    Member createMember(Member member);

    Member getMemberById(Long id);

    List<Member> getAllMembers();

    Member updateMember(Long id, Member member);

    void deleteMember(Long id);
}
