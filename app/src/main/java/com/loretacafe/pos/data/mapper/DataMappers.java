package com.loretacafe.pos.data.mapper;

import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.ReportEntity;
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.entity.SaleItemEntity;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.remote.dto.AuthResponseDto;
import com.loretacafe.pos.data.remote.dto.ProductResponseDto;
import com.loretacafe.pos.data.remote.dto.SaleResponseDto;
import com.loretacafe.pos.data.remote.dto.SaleResponseItemDto;
import com.loretacafe.pos.data.remote.dto.SalesSummaryDto;
import com.loretacafe.pos.data.remote.dto.TopProductDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class DataMappers {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private DataMappers() {
    }

    public static ProductEntity toEntity(ProductResponseDto dto) {
        ProductEntity entity = new ProductEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setSupplier(dto.getSupplier());
        entity.setCost(dto.getCost());
        entity.setPrice(dto.getPrice());
        entity.setQuantity(dto.getQuantity());
        entity.setStatus(dto.getStatus());
        return entity;
    }

    public static SaleEntity toEntity(SaleResponseDto dto) {
        SaleEntity saleEntity = new SaleEntity();
        saleEntity.setId(dto.getSaleId());
        saleEntity.setCashierId(dto.getCashierId());
        saleEntity.setSaleDate(parseDate(dto.getSaleDate()));
        saleEntity.setTotalAmount(dto.getTotalAmount());
        return saleEntity;
    }

    public static List<SaleItemEntity> toSaleItems(long saleId, List<SaleResponseItemDto> items) {
        List<SaleItemEntity> entities = new ArrayList<>();
        if (items == null) {
            return entities;
        }
        for (SaleResponseItemDto itemDto : items) {
            SaleItemEntity entity = new SaleItemEntity();
            entity.setSaleId(saleId);
            entity.setProductId(itemDto.getProductId());
            entity.setQuantity(itemDto.getQuantity());
            entity.setPrice(itemDto.getPrice() != null ? itemDto.getPrice() : BigDecimal.ZERO);
            entity.setSubtotal(itemDto.getSubtotal() != null ? itemDto.getSubtotal() : BigDecimal.ZERO);
            entities.add(entity);
        }
        return entities;
    }

    public static ReportEntity toEntity(SalesSummaryDto dto, String type, long userId) {
        ReportEntity entity = new ReportEntity();
        entity.setId(System.currentTimeMillis());
        entity.setType(type);
        entity.setStartDate(parseDate(dto.getStartDate()));
        entity.setEndDate(parseDate(dto.getEndDate()));
        entity.setTotalSales(dto.getTotalSales());
        entity.setTotalOrders(dto.getTotalOrders());
        entity.setTotalItems(dto.getTotalItems());
        entity.setCreatedBy(userId);
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }

    public static List<ProductEntity> toProductEntities(List<ProductResponseDto> dtos) {
        List<ProductEntity> entities = new ArrayList<>();
        if (dtos == null) {
            return entities;
        }
        for (ProductResponseDto dto : dtos) {
            entities.add(toEntity(dto));
        }
        return entities;
    }

    public static UserEntity toEntity(AuthResponseDto dto) {
        UserEntity entity = new UserEntity();
        entity.setId(dto.getUserId());
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }

    private static OffsetDateTime parseDate(String iso) {
        if (iso == null) {
            return null;
        }
        return OffsetDateTime.parse(iso, DATE_FORMATTER);
    }
}

