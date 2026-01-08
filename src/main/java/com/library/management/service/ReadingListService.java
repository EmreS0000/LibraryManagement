package com.library.management.service;

import com.library.management.entity.ReadingListItem;
import java.util.List;

public interface ReadingListService {
    void addToReadingList(Long memberId, Long bookId);

    void removeFromReadingList(Long memberId, Long bookId);

    List<ReadingListItem> getReadingList(Long memberId);

    boolean isInReadingList(Long memberId, Long bookId);
}
