package com.aurapc.admin.data.api;

import java.util.List;

public class PagedResponse<T> {
    public List<T> docs;
    public int page;
    public int totalPages;
    public long totalDocs;
}
