package com.uade.elrincondelmazo.repository;

import com.uade.elrincondelmazo.entity.AlbumSection;
import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlbumSectionRepository extends JpaRepository<AlbumSection, Long> {
    List<AlbumSection> findByAlbumIdOrderByIdAsc(Long albumId);
    Optional<AlbumSection> findByAlbumIdAndCollectionKey(Long albumId, AlbumSectionKey collectionKey);
}
