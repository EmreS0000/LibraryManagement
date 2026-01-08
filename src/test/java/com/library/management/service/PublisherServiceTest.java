package com.library.management.service;

import com.library.management.entity.Publisher;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.PublisherRepository;
import com.library.management.service.impl.PublisherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublisherService Unit Tests")
class PublisherServiceTest {

    @Mock
    private PublisherRepository publisherRepository;

    @InjectMocks
    private PublisherServiceImpl publisherService;

    private Publisher testPublisher;

    @BeforeEach
    void setUp() {
        testPublisher = new Publisher();
        testPublisher.setId(1L);
        testPublisher.setName("Test Publisher");
        testPublisher.setCountry("USA");
        testPublisher.setFoundedYear(2000);
    }

    @Test
    @DisplayName("Should create publisher successfully")
    void testCreatePublisher_Success() {
        // Given
        when(publisherRepository.existsByName(testPublisher.getName())).thenReturn(false);
        when(publisherRepository.save(any(Publisher.class))).thenReturn(testPublisher);

        // When
        Publisher result = publisherService.createPublisher(testPublisher);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Publisher");
        assertThat(result.getCountry()).isEqualTo("USA");
        verify(publisherRepository, times(1)).save(any(Publisher.class));
    }

    @Test
    @DisplayName("Should throw exception when creating publisher with duplicate name")
    void testCreatePublisher_DuplicateName() {
        // Given
        when(publisherRepository.existsByName(testPublisher.getName())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> publisherService.createPublisher(testPublisher))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(publisherRepository, never()).save(any(Publisher.class));
    }

    @Test
    @DisplayName("Should get publisher by ID successfully")
    void testGetPublisherById_Success() {
        // Given
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));

        // When
        Publisher result = publisherService.getPublisherById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Publisher");
        verify(publisherRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when publisher not found by ID")
    void testGetPublisherById_NotFound() {
        // Given
        when(publisherRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> publisherService.getPublisherById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Publisher not found");
    }

    @Test
    @DisplayName("Should get all publishers successfully")
    void testGetAllPublishers_Success() {
        // Given
        Publisher publisher2 = new Publisher();
        publisher2.setId(2L);
        publisher2.setName("Another Publisher");
        publisher2.setCountry("UK");

        List<Publisher> publishers = Arrays.asList(testPublisher, publisher2);
        when(publisherRepository.findAll()).thenReturn(publishers);

        // When
        List<Publisher> result = publisherService.getAllPublishers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testPublisher, publisher2);
        verify(publisherRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should update publisher successfully")
    void testUpdatePublisher_Success() {
        // Given
        Publisher updatedPublisher = new Publisher();
        updatedPublisher.setName("Updated Publisher");
        updatedPublisher.setCountry("UK");
        updatedPublisher.setFoundedYear(1995);

        when(publisherRepository.findById(1L)).thenReturn(Optional.of(testPublisher));
        when(publisherRepository.save(any(Publisher.class))).thenReturn(testPublisher);

        // When
        Publisher result = publisherService.updatePublisher(1L, updatedPublisher);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Publisher");
        assertThat(result.getCountry()).isEqualTo("UK");
        verify(publisherRepository, times(1)).save(any(Publisher.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent publisher")
    void testUpdatePublisher_NotFound() {
        // Given
        Publisher updatedPublisher = new Publisher();
        updatedPublisher.setName("Updated");

        when(publisherRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> publisherService.updatePublisher(99L, updatedPublisher))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Publisher not found");
    }

    @Test
    @DisplayName("Should delete publisher successfully")
    void testDeletePublisher_Success() {
        // Given
        when(publisherRepository.existsById(1L)).thenReturn(true);
        doNothing().when(publisherRepository).deleteById(1L);

        // When
        publisherService.deletePublisher(1L);

        // Then
        verify(publisherRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent publisher")
    void testDeletePublisher_NotFound() {
        // Given
        when(publisherRepository.existsById(anyLong())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> publisherService.deletePublisher(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Publisher not found");
        verify(publisherRepository, never()).deleteById(anyLong());
    }
}
