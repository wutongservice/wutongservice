package com.borqs.server.market.utils;


public class Pagination {
    private final int currentPage;
    private final int totalPages;

    public Pagination(int currentPage, int totalPages) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public static Pagination create(int pageBasedZero, int countPerPage, int totalCount) {
        return new Pagination(pageBasedZero + 1, calculateTotalPages(countPerPage, totalCount));
    }

    public static int calculateTotalPages(int countPerPage, int totalCount) {
        if (totalCount <= 0) {
            return 0;
        } else if (totalCount % countPerPage == 0) {
            return totalCount / countPerPage;
        } else {
            return totalCount / countPerPage + 1;
        }
    }
}
