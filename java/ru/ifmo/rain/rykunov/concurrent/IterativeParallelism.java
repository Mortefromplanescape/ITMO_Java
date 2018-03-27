package ru.ifmo.rain.rykunov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.min;


public class IterativeParallelism implements ListIP {

    private <T, E> E applyFunction(int threads, List<? extends T> values, Function<Stream<? extends T>, E> funcForThreads, Function<Stream<? extends E>, E> funcAfterThreads) throws InterruptedException {
        var threadsList = new ArrayList<Thread>();
        threads = min(threads, values.size());
        var elementsPerThread = values.size() / threads;
        final var answers = new ArrayList<E>(threads);
        for (int i = 0; i < threads; i++) {
            answers.add(funcForThreads.apply(Stream.of(values.get(i))));
        }
        for (int i = 0; i < threads; i++) {
            final int thread = i;
            final var left = thread * elementsPerThread;
            final var right = i == threads - 1 ? values.size() : left + elementsPerThread;
            var tempThread = new Thread(() -> answers.set(thread, funcForThreads.apply(values.subList(left, right).stream())));
            tempThread.start();
            threadsList.add(tempThread);
        }
        for (var thread : threadsList) {
            thread.join();
        }
        return funcAfterThreads.apply(answers.stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                (Function<Stream<? extends T>, T>) x -> x.max(comparator).get(),
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
                (Function<Stream<?>, String>) x -> x.map(Object::toString).collect(Collectors.joining()),
                x -> x.collect(Collectors.joining())
        );
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                (Function<Stream<? extends T>, Stream<? extends T>>) x -> x.filter(predicate),
                x -> x.flatMap(Function.identity())
        ).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return applyFunction(
                threads,
                values,
                (Function<Stream<? extends T>, Stream<? extends U>>) x -> x.map(f),
                x -> x.flatMap(Function.identity())
        ).collect(Collectors.toCollection(ArrayList::new));
    }
}
