package com.library.management.service.impl;

import com.library.management.entity.Book;
import com.library.management.entity.Favorite;
import com.library.management.entity.Member;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRepository;
import com.library.management.repository.FavoriteRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;

    @Override
    public void addFavorite(Long memberId, Long bookId) {
        if (favoriteRepository.existsByMemberIdAndBookId(memberId, bookId)) {
            throw new BusinessException("Book is already in favorites");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        Favorite favorite = new Favorite();
        favorite.setMember(member);
        favorite.setBook(book);
        favoriteRepository.save(favorite);
    }

    @Override
    public void removeFavorite(Long memberId, Long bookId) {
        Favorite favorite = favoriteRepository.findByMemberIdAndBookId(memberId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));
        favoriteRepository.delete(favorite);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorite> getFavorites(Long memberId) {
        return favoriteRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long memberId, Long bookId) {
        return favoriteRepository.existsByMemberIdAndBookId(memberId, bookId);
    }
}
