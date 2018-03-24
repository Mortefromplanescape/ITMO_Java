package ru.ifmo.rain.rykunov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Math.min;


public class IterativeParallelism implements ScalarIP {

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
            final int left = thread * elementsPerThread;
            final int right = min(left + elementsPerThread, values.size());
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
}
