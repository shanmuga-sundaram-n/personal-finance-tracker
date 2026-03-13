package com.shan.cyber.tech.financetracker.category.domain.model;

import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.Objects;

public class Category {

    private CategoryId id;
    private UserId ownerId;
    private CategoryType categoryType;
    private CategoryId parentCategoryId;
    private String name;
    private String icon;
    private String color;
    private boolean isSystem;
    private boolean isActive;
    private AuditInfo auditInfo;

    public Category(CategoryId id, UserId ownerId, CategoryType categoryType,
                    CategoryId parentCategoryId, String name, String icon, String color,
                    boolean isSystem, boolean isActive, AuditInfo auditInfo) {
        this.id = id;
        this.ownerId = ownerId;
        this.categoryType = Objects.requireNonNull(categoryType, "categoryType must not be null");
        this.parentCategoryId = parentCategoryId;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.icon = icon;
        this.color = color;
        this.isSystem = isSystem;
        this.isActive = isActive;
        this.auditInfo = auditInfo;
    }

    public static Category create(UserId ownerId, CategoryType categoryType,
                                   CategoryId parentCategoryId, String name,
                                   String icon, String color) {
        return new Category(null, ownerId, categoryType, parentCategoryId, name,
                icon, color, false, true, null);
    }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName, "name must not be null");
    }

    public void updateIcon(String icon) {
        this.icon = icon;
    }

    public void updateColor(String color) {
        this.color = color;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean isTopLevel() {
        return parentCategoryId == null;
    }

    public CategoryId getId() { return id; }
    public UserId getOwnerId() { return ownerId; }
    public CategoryType getCategoryType() { return categoryType; }
    public CategoryId getParentCategoryId() { return parentCategoryId; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public boolean isSystem() { return isSystem; }
    public boolean isActive() { return isActive; }
    public AuditInfo getAuditInfo() { return auditInfo; }

    public void setId(CategoryId id) { this.id = id; }
    public void setAuditInfo(AuditInfo auditInfo) { this.auditInfo = auditInfo; }
}
