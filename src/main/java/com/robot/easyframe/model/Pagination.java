package com.robot.easyframe.model;

import java.io.Serializable;

/**
 * 分页信息类（改编自wade-common包）
 *
 * @author luozhan
 * @date 2019-01
 */
public class Pagination implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_PAGE_SIZE = 500;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_RECODE_SIZE = 2147483647;
    private static final int MAX_FETCH_SIZE = 2000;
    public static final String X_PAGINCOUNT = "X_PAGINCOUNT";
    public static final String X_PAGINCURRENT = "X_PAGINCURRENT";
    public static final String X_PAGINSELCOUNT = "X_PAGINSELCOUNT";
    public static final String X_PAGINSIZE = "X_PAGINSIZE";
    public static final String X_RESULTCOUNT = "X_RESULTCOUNT";
    // 当前页数
    public static final String CURRENT_PAGE = "CURRENT_PAGE";
    // 每页显示条数
    public static final String PAGE_SIZE = "PAGE_SIZE";

    private boolean needCount = true;
    private boolean onlyCount = false;
    private long count;
    private int current = -1;
    private int pagesize = 1;
    private int originPageSize;
    private int fetchSize;
    private int currentSize;

    public Pagination() {
    }

    /**
     * 指定每页条数，默认第一页，这个方法可用于查询前N条数据
     *
     * @param pagesize 每页的数量
     */
    public Pagination(int pagesize) {
        this.pagesize = pagesize;
        this.current = 1;
    }

    public Pagination(int pagesize, int current) throws Exception {
        this.pagesize = pagesize;
        this.current = current;
    }

    public boolean next() {
        if ((long) this.current >= this.getPageCount()) {
            return false;
        } else {
            ++this.current;
            return true;
        }
    }

    public int getFetchSize() {
        if (this.fetchSize == 0 && this.pagesize > 0) {
            this.fetchSize = this.pagesize;
        } else {
            this.fetchSize = 20;
        }

        return this.fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        if (fetchSize <= 2000 && fetchSize >= 0) {
            this.fetchSize = fetchSize;
        } else {
            this.fetchSize = getDefaultPageSize();
        }

    }

    public static int getMaxPageSize() {
        return 500;
    }

    public static int getDefaultPageSize() {
        return 20;
    }

    public boolean isNeedCount() {
        return this.needCount;
    }

    public void setNeedCount(boolean needCount) {
        this.needCount = needCount;
    }

    public long getCount() {
        return this.count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public int getPageSize() {
        return this.pagesize;
    }

    public void setPageSize(int pagesize) {
        this.pagesize = pagesize;
    }

    public int getOriginPageSize() {
        return this.originPageSize;
    }

    public void setOriginPageSize(int originPageSize) {
        this.originPageSize = originPageSize;
    }

    public long getPageCount() {
        long pageCount = this.getCount() / (long) this.getPageSize();
        if (pageCount == 0L || this.getCount() % (long) this.getPageSize() != 0L) {
            ++pageCount;
        }

        return pageCount;
    }

    public boolean isOnlyCount() {
        return this.onlyCount;
    }

    public void setOnlyCount(boolean onlyCount) {
        this.onlyCount = onlyCount;
    }

    public int getCurrent() {
        return this.current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getStart() {
        return this.getEnd() - this.pagesize + 1;
    }

    public int getEnd() {
        return this.current * this.pagesize;
    }

    public int getCurrentSize() {
        return this.currentSize;
    }

    public void setCurrentSize(int currentSize) {
        this.currentSize = currentSize;
    }
}

