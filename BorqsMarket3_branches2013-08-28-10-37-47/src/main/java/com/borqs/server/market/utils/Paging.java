package com.borqs.server.market.utils;


public class Paging {
    private final int page;
    private final int count;

    public Paging(int page, int count) {
        this.page = page;
        this.count = count;
    }

    public int getPage() {
        return page;
    }

    public int getCount() {
        return count;
    }

    public int getOffset() {
        return page * count;
    }

    public int getEndOffset() {
        return page * count + count;
    }
}
