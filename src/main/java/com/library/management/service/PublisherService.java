package com.library.management.service;

import com.library.management.entity.Publisher;
import java.util.List;

public interface PublisherService {
    Publisher createPublisher(Publisher publisher);

    Publisher updatePublisher(Long id, Publisher publisher);

    void deletePublisher(Long id);

    Publisher getPublisherById(Long id);

    List<Publisher> getAllPublishers();
}
