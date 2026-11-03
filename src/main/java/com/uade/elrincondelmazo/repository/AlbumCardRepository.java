package com.uade.elrincondelmazo.repository;
import com.uade.elrincondelmazo.entity.AlbumCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AlbumCardRepository extends JpaRepository<AlbumCard, Long> {
    List<AlbumCard> findBySectionIdInOrderByPageNumberAscSlotIndexAsc(List<Long> sectionIds);
}
