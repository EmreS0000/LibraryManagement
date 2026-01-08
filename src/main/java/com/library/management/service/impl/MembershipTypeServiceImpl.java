package com.library.management.service.impl;

import com.library.management.entity.MembershipType;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.MembershipTypeRepository;
import com.library.management.service.MembershipTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MembershipTypeServiceImpl implements MembershipTypeService {

    private final MembershipTypeRepository membershipTypeRepository;

    @Override
    public MembershipType createMembershipType(MembershipType membershipType) {
        return membershipTypeRepository.save(membershipType);
    }

    @Override
    public MembershipType updateMembershipType(Long id, MembershipType membershipType) {
        MembershipType existingType = membershipTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership Type not found"));
        existingType.setName(membershipType.getName());
        existingType.setMaxBooks(membershipType.getMaxBooks());
        existingType.setMaxLoanDays(membershipType.getMaxLoanDays());
        return membershipTypeRepository.save(existingType);
    }

    @Override
    public void deleteMembershipType(Long id) {
        if (!membershipTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Membership Type not found");
        }
        membershipTypeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipType getMembershipType(Long id) {
        return membershipTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership Type not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipType> getAllMembershipTypes() {
        return membershipTypeRepository.findAll();
    }
}
