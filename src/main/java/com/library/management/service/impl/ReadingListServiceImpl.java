package com.library.management.service.impl;

import com.library.management.entity.Book;
import com.library.management.entity.Member;
import com.library.management.entity.ReadingListItem;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.ReadingListItemRepository;
import com.library.management.service.ReadingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReadingListServiceImpl implements ReadingListService {

    private final ReadingListItemRepository readingListItemRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;

    @Override
    public void addToReadingList(Long memberId, Long bookId) {
        if (readingListItemRepository.existsByMemberIdAndBookId(memberId, bookId)) {
            throw new BusinessException("Book is already in reading list");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        ReadingListItem item = new ReadingListItem();
        item.setMember(member);
        item.setBook(book);
        readingListItemRepository.save(item);
    }

    @Override
    public void removeFromReadingList(Long memberId, Long bookId) {
        ReadingListItem item = readingListItemRepository.findByMemberIdAndBookId(memberId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading list item not found"));
        readingListItemRepository.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingListItem> getReadingList(Long memberId) {
        return readingListItemRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInReadingList(Long memberId, Long bookId) {
        return readingListItemRepository.existsByMemberIdAndBookId(memberId, bookId);
    }
}
