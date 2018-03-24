package ru.ifmo.rain.rykunov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static java.lang.Math.min;


public class IterativeParallelism implements ScalarIP {

    private <T, E> List<E> applyFunction(int threads, List<? extends T> values, Function<Stream<? extends T>, E> e) throws InterruptedException {
        var threadsList = new ArrayList<Thread>();
        threads = min(threads, values.size());
        var elementsPerThread = values.size() / threads;
        final var answers = new ArrayList<E>(threads);
        for (int i = 0; i < threads; i++) {
            answers.add(e.apply(Stream.of(values.get(i))));
        }
        for (int i = 0; i < threads; i++) {
            final int thread = i;
            final var left = thread * elementsPerThread;
            final var right = min(left + elementsPerThread, values.size());
            var tempThread = new Thread(() -> answers.add(thread, e.apply(values.subList(left, right).stream())));
            tempThread.start();
            threadsList.add(tempThread);
        }
        for (var thread : threadsList) {
            thread.join();
        }
        return answers;
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> max = x -> x.max(comparator).get();
        return applyFunction(threads, values, max).stream().max(comparator).get();
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        Function<Stream<? extends T>, Boolean> all = x -> x.allMatch(predicate);
        return applyFunction(threads, values, all).stream().allMatch(Boolean::booleanValue);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        Function<Stream<? extends T>, Boolean> any = x -> x.anyMatch(predicate);
        return applyFunction(threads, values, any).stream().anyMatch(Boolean::booleanValue);
    }
}
