package ru.ifmo.rain.rykunov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.min;


public class IterativeParallelism implements ListIP {

    private <T, E, F> F applyFunction(int threads, List<? extends T> values, Function<Stream<? extends T>, E> funcForThreads, Function<Stream<E>, F> funcAfterThreads) throws InterruptedException {
        if (threads <= 0 || values == null) {
            throw new IllegalArgumentException("ERROR: excepted natural number `threads` and non-null `values`");
        }

        var threadsList = new ArrayList<Thread>();
        threads = min(threads, values.size());

        final var answers = new ArrayList<E>(threads);
        for (int i = 0; i < threads; i++) {
            answers.add(funcForThreads.apply(Stream.of(values.get(i))));
        }

        int elementsPerThread = values.size() / threads;
        int tail = values.size() % threads;
        int pos = 0;
        for (int i = 0; i < threads; i++) {
            final int thread = i;
            final var left = pos;
            final var right = min(values.size(), pos + elementsPerThread + (tail > 0 ? 1 : 0));

            var tempThread = new Thread(() -> answers.set(thread, funcForThreads.apply(values.subList(left, right).stream())));
            tempThread.start();
            threadsList.add(tempThread);

            if (tail > 0) {
                tail--;
            }
            pos = right;
        }

        boolean throwsInterruptedExc = false;
        for (var thread : threadsList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throwsInterruptedExc = true;
            }
        }

        if (throwsInterruptedExc) {
            throw new InterruptedException("Interrupted when counting function");
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

    private <T> Function<Stream<Stream<? extends T>>, List<T>> mergeStreams() {
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
