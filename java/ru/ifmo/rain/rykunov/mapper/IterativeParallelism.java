package ru.ifmo.rain.rykunov.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.min;
import static ru.ifmo.rain.rykunov.mapper.ConcurrentUtils.*;


public class IterativeParallelism implements ListIP {
    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T> List<Stream<? extends T>> split(int threads, List<? extends T> values) {
        final var splitList = new ArrayList<Stream<? extends T>>();

        int elementsPerThread = values.size() / threads;
        int tail = values.size() % threads;
        int pos = 0;

        for (int i = 0; i < threads; i++) {
            final var right = min(values.size(), pos + elementsPerThread + (tail > 0 ? 1 : 0));
            splitList.add(values.subList(pos, right).stream());
            if (tail > 0) {
                tail--;
            }
            pos = right;
        }

        return splitList;
    }

    private <T, E> List<Thread> execute(int threads, List<? extends Stream<? extends T>> values, Function<Stream<? extends T>, E> funcForThreads, List<E> answers) {
        var threadsList = new ArrayList<Thread>();

        for (int i = 0; i < threads; i++) {
            final int thread = i;
            startThreadAndAddItToList(
                    new Thread(() ->
                            answers.set(thread, funcForThreads.apply(values.get(thread)))),
                    threadsList);
        }

        return threadsList;
    }

    private <T, E, F> F applyFunction(int threads, List<? extends T> values, Function<Stream<? extends T>, E> funcForThreads, Function<Stream<? extends E>, F> funcAfterThreads) throws InterruptedException {
        if (threads <= 0 || values == null) {
            throw new IllegalArgumentException("ERROR: excepted natural number `threads` and non-null `values`");
        }

        threads = min(threads, values.size());
        final var splitList = split(threads, values);
        List<E> answers;
        if (mapper != null) {
            answers = mapper.map(funcForThreads, splitList);
        } else {
            answers = new ArrayList<>(Collections.nCopies(threads, null));
            final var threadsList = execute(threads, splitList, funcForThreads, answers);
            if (waitForThreads(threadsList)) {
                throw new InterruptedException("Interrupted when counting function");
            }
        }

        return funcAfterThreads.apply(answers.stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                x -> x.max(comparator).get(),
                x -> x.max(comparator).get()
        );
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                x -> x.allMatch(predicate),
                x -> x.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                x -> x.anyMatch(predicate),
                x -> x.anyMatch(Boolean::booleanValue)
        );
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                x -> x.map(Object::toString).collect(Collectors.joining()),
                x -> x.collect(Collectors.joining())
        );
    }

    private <T> Function<Stream<? extends Stream<? extends T>>, List<T>> mergeStreams() {
        return x -> x.flatMap(Function.identity()).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                x -> x.filter(predicate),
                mergeStreams()
        );
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                x -> x.map(f),
                mergeStreams()
        );
    }
}
