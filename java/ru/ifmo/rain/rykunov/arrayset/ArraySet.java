package ru.ifmo.rain.rykunov.arraySet;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> cmp;

    public ArraySet() {
        data = Collections.emptyList();
        cmp = null;
    }

    public ArraySet(Collection<? extends T> collection) {
        data = new ArrayList<>(new TreeSet<>(collection));
        cmp = null;
    }

//    public ArraySet(Comparator<? super T> comparator) {
//        data = null;
//        cmp = comparator;
//    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        cmp = comparator;
        TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        data = new ArrayList<>(treeSet);
    }

    private ArraySet(List<T> prevData, Comparator<? super T> comparator) {
        cmp = comparator;
        data = prevData;
        if (prevData instanceof DescendingList) {
            ((DescendingList) prevData).reverse();
        }
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public Comparator<? super T> comparator() {
        return cmp;
    }

    @Override
    public T first() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    @Override
    public T last() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(size() - 1);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        // It's ok to throw ClassCastException
        return Collections.binarySearch(data, (T) o, cmp) >= 0;
    // cmp to T
    }

    private int findIndexFromHead(T element, boolean inclusive) {
        int index = Collections.binarySearch(data, element, cmp);
        return index < 0 ? ~index : (!inclusive ? index + 1 : index);
    }

    private int findIndexFromTail(T element, boolean inclusive) {
        int index = Collections.binarySearch(data, element, cmp);
        return index < 0 ? ~index - 1 : (!inclusive ? index - 1 : index);
    }

    @Override
    public T lower(T e) {
        int index = findIndexFromTail(e, false);
        return index >= 0 ? data.get(index) : null;
    }

    @Override
    public T floor(T e) {
        int index = findIndexFromTail(e, true);
        return index >= 0 ? data.get(index) : null;
    }

    @Override
    public T ceiling(T e) {
        int index = findIndexFromHead(e, true);
        return index < data.size() ? data.get(index) : null;
    }

    @Override
    public T higher(T e) {
        int index = findIndexFromHead(e, false);
        return index < data.size() ? data.get(index) : null;
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new DescendingList<>(data), Collections.reverseOrder(cmp));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int fromI = findIndexFromHead(fromElement, fromInclusive);
        int toI = findIndexFromTail(toElement, toInclusive) + 1;
        if (toI + 1 == fromI) {
            toI = fromI;
        }
        return new ArraySet<>(data.subList(fromI, toI), cmp);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        int index = findIndexFromTail(toElement, inclusive) + 1;
        return new ArraySet<>(data.subList(0, index), cmp);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int index = findIndexFromHead(fromElement, inclusive);
        return new ArraySet<>(data.subList(index, data.size()), cmp);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

}
