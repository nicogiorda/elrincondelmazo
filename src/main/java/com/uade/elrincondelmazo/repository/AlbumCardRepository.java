package com.uade.elrincondelmazo.repository;

import com.uade.elrincondelmazo.entity.AlbumCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlbumCardRepository extends JpaRepository<AlbumCard, Long> {
    List<AlbumCard> findBySectionIdInOrderByPageNumberAscSlotIndexAsc(List<Long> sectionIds);
    Optional<AlbumCard> findBySectionIdAndPageNumberAndSlotIndex(Long sectionId, int pageNumber, int slotIndex);
    long countBySectionIdAndPageNumber(Long sectionId, int pageNumber);
    long countByOrderItem_Id(Long orderItemId);
}
