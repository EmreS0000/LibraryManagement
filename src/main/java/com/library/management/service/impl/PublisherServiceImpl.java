package com.library.management.service.impl;

import com.library.management.entity.Publisher;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.PublisherRepository;
import com.library.management.service.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;

    @Override
    public Publisher createPublisher(Publisher publisher) {
        if (publisherRepository.existsByName(publisher.getName())) {
            throw new BusinessException("Publisher with name " + publisher.getName() + " already exists");
        }
        return publisherRepository.save(publisher);
    }

    @Override
    public Publisher updatePublisher(Long id, Publisher publisher) {
        Publisher existingPublisher = publisherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found with id: " + id));

        if (publisher.getName() != null) {
            existingPublisher.setName(publisher.getName());
        }
        if (publisher.getCountry() != null) {
            existingPublisher.setCountry(publisher.getCountry());
        }
        if (publisher.getFoundedYear() != null) {
            existingPublisher.setFoundedYear(publisher.getFoundedYear());
        }
        if (publisher.getAddress() != null) {
            existingPublisher.setAddress(publisher.getAddress());
        }
        if (publisher.getPhone() != null) {
            existingPublisher.setPhone(publisher.getPhone());
        }
        if (publisher.getEmail() != null) {
            existingPublisher.setEmail(publisher.getEmail());
        }
        return publisherRepository.save(existingPublisher);
    }

    @Override
    public void deletePublisher(Long id) {
        if (!publisherRepository.existsById(id)) {
            throw new ResourceNotFoundException("Publisher not found with id: " + id);
        }
        publisherRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Publisher getPublisherById(Long id) {
        return publisherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publisher> getAllPublishers() {
        return publisherRepository.findAll();
    }
}
