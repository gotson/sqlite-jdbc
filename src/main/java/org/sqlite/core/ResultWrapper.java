package org.sqlite.core;

public class ResultWrapper {
    private final CoreResultSet rs;
    private final long updateCount;
    private ResultWrapper next;

    public ResultWrapper(CoreResultSet rs) {
        this.rs = rs;
        this.updateCount = -1;
    }

    public ResultWrapper(long updateCount) {
        this.rs = null;
        this.updateCount = updateCount;
    }


    public CoreResultSet getResultSet() {
        return rs;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public ResultWrapper getNext() {
        return next;
    }

    public void append(ResultWrapper newResult) {
        ResultWrapper tail = this;
        while (tail.next != null) {
            tail = tail.next;
        }

        tail.next = newResult;
    }
}
