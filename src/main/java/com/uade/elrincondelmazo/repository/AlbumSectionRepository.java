package com.uade.elrincondelmazo.repository;
import com.uade.elrincondelmazo.entity.AlbumSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AlbumSectionRepository extends JpaRepository<AlbumSection, Long> {
    List<AlbumSection> findByAlbumIdOrderByIdAsc(Long albumId);
}
