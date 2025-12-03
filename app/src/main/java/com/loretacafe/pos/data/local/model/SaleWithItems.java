package com.loretacafe.pos.data.local.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.entity.SaleItemEntity;

import java.util.List;

public class SaleWithItems {

    @Embedded
    public SaleEntity sale;

    @Relation(
            parentColumn = "sale_id",
            entityColumn = "sale_id"
    )
    public List<SaleItemEntity> items;
}

