package com.example.aura_pc_app.domain.model;

/**
 * Model đại diện cho một danh mục sản phẩm từ API /api/categories.
 */
public class Category {
    private final String categoryId;
    private final String name;
    private final String slug;
    private final String parentId;
    private final int level;

    public Category(String categoryId, String name, String slug, String parentId, int level) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.parentId = parentId;
        this.level = level;
    }

    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getParentId() { return parentId; }
    public int getLevel() { return level; }
}
