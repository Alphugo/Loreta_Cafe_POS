package com.loretacafe.pos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;

/**
 * Recipe entity - stores recipe information for menu items
 * Each menu item can have one recipe (with multiple ingredients)
 */
@Entity(
        tableName = "recipes",
        indices = {@Index(value = "product_id")}
)
public class RecipeEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "recipe_id")
    private long id;

    @ColumnInfo(name = "product_id")
    private long productId; // Links to ProductEntity (menu item)

    @ColumnInfo(name = "recipe_name")
    private String recipeName; // e.g., "Hot", "Iced", "Default"

    @ColumnInfo(name = "recipe_json")
    private String recipeJson; // JSON string storing RecipeIngredient objects

    @ColumnInfo(name = "created_at")
    private OffsetDateTime createdAt;

    @ColumnInfo(name = "updated_at")
    private OffsetDateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getRecipeJson() {
        return recipeJson;
    }

    public void setRecipeJson(String recipeJson) {
        this.recipeJson = recipeJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

