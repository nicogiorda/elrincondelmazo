package com.uade.elrincondelmazo.entity;
import com.uade.elrincondelmazo.enums.AlbumCardOrigin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "album_card", uniqueConstraints = @UniqueConstraint(name = "album_card_slot_uq", columnNames = {"section_id", "page_number", "slot_index"}))
@Getter @Setter
public class AlbumCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false) private AlbumSection section;
    @Column(name = "page_number", nullable = false) private int pageNumber;
    @Column(name = "slot_index", nullable = false) private int slotIndex;
    @Column(nullable = false) private String name;
    @Column(name = "image_url", length = 2048, nullable = false) private String imageUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private AlbumCardOrigin origin;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id") private OrderItem orderItem;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
