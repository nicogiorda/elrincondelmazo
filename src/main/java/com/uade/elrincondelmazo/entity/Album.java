package com.uade.elrincondelmazo.entity;
import com.uade.elrincondelmazo.enums.AlbumVisibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "album")
@Getter @Setter
public class Album {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private AlbumVisibility visibility = AlbumVisibility.PRIVATE;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
