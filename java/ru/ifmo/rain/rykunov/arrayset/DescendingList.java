package ru.ifmo.rain.rykunov.arrayset;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public class DescendingList<T> extends AbstractList<T> implements RandomAccess {
    private boolean reversed;
    private final List<T> data;

    public DescendingList(List<T> list) {
        data = list;
        reversed = false;
    }

    public void reverse() {
        reversed = !reversed;
    }

    @Override
    public T get(int index) {
        return reversed ? data.get(size() - index - 1) : data.get(index);
    }

    @Override
    public int size() {
        return data.size();
    }
}
