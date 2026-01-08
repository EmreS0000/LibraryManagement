package com.library.management.repository;

import com.library.management.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    // Pagination
    Page<Member> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

    Page<Member> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}
