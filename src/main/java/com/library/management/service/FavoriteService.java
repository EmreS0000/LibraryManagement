package com.library.management.service;

import com.library.management.entity.Favorite;
import java.util.List;

public interface FavoriteService {
    void addFavorite(Long memberId, Long bookId);

    void removeFavorite(Long memberId, Long bookId);

    List<Favorite> getFavorites(Long memberId);

    boolean isFavorite(Long memberId, Long bookId);
}
