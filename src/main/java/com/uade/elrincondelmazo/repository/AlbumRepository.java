package com.uade.elrincondelmazo.repository;
import com.uade.elrincondelmazo.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByUserId(Long userId);
}
