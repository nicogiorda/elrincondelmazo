package com.uade.elrincondelmazo.entity;
import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name = "album_section", uniqueConstraints = @UniqueConstraint(name = "album_section_key_uq", columnNames = {"album_id", "collection_key"}))
@Getter @Setter
public class AlbumSection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false) private Album album;
    @Enumerated(EnumType.STRING) @Column(name = "collection_key", nullable = false, length = 32)
    private AlbumSectionKey collectionKey;
    @Column(name = "page_count", nullable = false) private int pageCount = 1;
}
