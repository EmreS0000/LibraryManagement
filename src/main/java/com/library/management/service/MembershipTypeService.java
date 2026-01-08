package com.library.management.service;

import com.library.management.entity.MembershipType;
import java.util.List;

public interface MembershipTypeService {
    MembershipType createMembershipType(MembershipType membershipType);

    MembershipType updateMembershipType(Long id, MembershipType membershipType);

    void deleteMembershipType(Long id);

    MembershipType getMembershipType(Long id);

    List<MembershipType> getAllMembershipTypes();
}
