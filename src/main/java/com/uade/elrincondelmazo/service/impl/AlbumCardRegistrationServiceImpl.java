package com.uade.elrincondelmazo.service.impl;

import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.entity.dto.album.*;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
public class AlbumCardRegistrationServiceImpl implements AlbumCardRegistrationService {
    private final AlbumService albumService;
    private final AlbumSectionRepository sections;
    private final AlbumCardRepository cards;
    private final OrderItemRepository orderItems;
    private final ImageStorageService imageStorage;

    public AlbumCardRegistrationServiceImpl(AlbumService albumService, AlbumSectionRepository sections,
                                            AlbumCardRepository cards, OrderItemRepository orderItems,
                                            ImageStorageService imageStorage) {
        this.albumService = albumService;
        this.sections = sections;
        this.cards = cards;
        this.orderItems = orderItems;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlbumPurchaseResponse> availablePurchases(Long userId) {
        return orderItems.findByOrder_User_IdOrderByIdDesc(userId).stream()
                .filter(item -> eligible(item))
                .map(item -> {
                    long used = cards.countByOrderItem_Id(item.getId());
                    int quantity = item.getQuantity();
                    return new AlbumPurchaseResponse(item.getId(), item.getOrder().getId(),
                            item.getProduct().getName(), firstImage(item.getProduct()),
                            quantity, used, Math.max(0, quantity - used));
                }).toList();
    }

    @Override
    @Transactional
    public AlbumResponse.CardResponse addPurchase(Long userId, PurchaseAlbumCardRequest request) {
        // Locks the owner and lazily initializes their album, serializing purchases and moves.
        AlbumSection target = destination(userId, request.section(), request.pageNumber(), request.slotIndex());
        OrderItem item = orderItems.findByIdAndOrder_User_Id(request.orderItemId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
        if (!eligible(item)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La compra no corresponde a una carta válida");
        }
        if (cards.countByOrderItem_Id(item.getId()) >= item.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No quedan ejemplares disponibles de esta compra");
        }

        Product product = item.getProduct();
        AlbumCard card = new AlbumCard();
        card.setSection(target);
        card.setPageNumber(request.pageNumber());
        card.setSlotIndex(request.slotIndex());
        card.setName(product.getName() == null ? "Carta" : product.getName());
        card.setImageUrl(firstImage(product));
        card.setOrigin(AlbumCardOrigin.PURCHASE);
        card.setOrderItem(item);
        return saveCardAndUnlock(target, card);
    }

    @Override
    @Transactional
    public AlbumResponse.CardResponse addManual(Long userId, AlbumSectionKey section, int pageNumber,
                                                 int slotIndex, String name, MultipartFile image) {
        if (name == null || name.isBlank() || name.trim().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre debe tener entre 1 y 255 caracteres");
        }
        AlbumSection target = destination(userId, section, pageNumber, slotIndex);
        // Storage validates MIME type, content, 10 MB limit and converts to WebP.
        String imageUrl = imageStorage.uploadAlbumImage(userId, image);
        // External Storage is not transactional: compensate if the database transaction rolls back.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    imageStorage.deleteAlbumImage(userId, imageUrl);
                }
            }
        });
        AlbumCard card = new AlbumCard();
        card.setSection(target);
        card.setPageNumber(pageNumber);
        card.setSlotIndex(slotIndex);
        card.setName(name.trim());
        card.setImageUrl(imageUrl);
        card.setOrigin(AlbumCardOrigin.MANUAL);
        return saveCardAndUnlock(target, card);
    }

    private AlbumSection destination(Long userId, AlbumSectionKey key, int page, int slot) {
        if (key == null || page < 1 || slot < 0 || slot > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Posición de álbum inválida");
        }
        AlbumResponse own = albumService.getMine(userId);
        AlbumResponse.SectionResponse destination = own.sections().stream()
                .filter(section -> section.key() == key)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
        if (page > destination.pageCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La página aún no existe");
        }
        if (cards.findBySectionIdAndPageNumberAndSlotIndex(destination.id(), page, slot).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La posición está ocupada");
        }
        return sections.findById(destination.id()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));
    }

    private AlbumResponse.CardResponse saveCardAndUnlock(AlbumSection target, AlbumCard card) {
        cards.saveAndFlush(card);
        if (cards.countBySectionIdAndPageNumber(target.getId(), target.getPageCount()) == 9) {
            target.setPageCount(target.getPageCount() + 1);
            sections.save(target);
        }
        return new AlbumResponse.CardResponse(card.getId(), card.getPageNumber(), card.getSlotIndex(),
                card.getName(), card.getImageUrl(), card.getOrigin());
    }

    private boolean eligible(OrderItem item) {
        return item.getOrder().getState() != StateOrder.CANCELADO
                && item.getProduct().getType() == ProductType.CARTA
                && item.getQuantity() != null && item.getQuantity() > 0;
    }

    private String firstImage(Product product) {
        List<String> urls = product.getImageUrls();
        return urls == null || urls.isEmpty() || urls.getFirst() == null ? "" : urls.getFirst();
    }
}
