package com.library.management.repository;

import com.library.management.entity.Fine;
import com.library.management.entity.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    List<Fine> findByMemberId(Long memberId);

    List<Fine> findByMemberIdAndStatus(Long memberId, FineStatus status);

    Optional<Fine> findByLoanId(Long loanId);
}
