package com.library.management.repository;

import com.library.management.entity.ReadingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingListItemRepository extends JpaRepository<ReadingListItem, Long> {
    List<ReadingListItem> findByMemberId(Long memberId);

    Optional<ReadingListItem> findByMemberIdAndBookId(Long memberId, Long bookId);

    boolean existsByMemberIdAndBookId(Long memberId, Long bookId);
}
