package ru.ifmo.rain.rykunov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

import static ru.ifmo.rain.rykunov.mapper.ConcurrentUtils.startThreadAndAddItToList;
import static ru.ifmo.rain.rykunov.mapper.ConcurrentUtils.waitForThreads;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    private final Queue<Runnable> tasks;
    private final int SIZE_MAX = 1000000;

    private void add(final Runnable task) throws InterruptedException {
        synchronized (tasks) {
            while (tasks.size() >= SIZE_MAX) {
                tasks.wait();
            }

            tasks.add(task);
            tasks.notify();
        }
    }

    private void solve() throws InterruptedException {
        Runnable currentTask;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            currentTask = tasks.poll();
        }

        currentTask.run();
    }

    public ParallelMapperImpl(int threads) {
        workers = new ArrayList<>();
        tasks = new ArrayDeque<>();
        for (int i = 0; i < threads; i++) {
            startThreadAndAddItToList(
                    new Thread(() -> {
                        try {
                            while (!Thread.interrupted()) {
                                solve();
                            }
                        } catch (InterruptedException ignored) {

                        } finally {
                            Thread.currentThread().interrupt();
                        }
                    }),
                    workers);
        }
    }

    private class concurrentList<E> {
        private final List<E> list;
        private int size;

        concurrentList(int initSize) {
            list = new ArrayList<>(Collections.nCopies(initSize, null));
            size = 0;
        }

        public void set(int ind, E value) {
            synchronized (list) {
                list.set(ind, value);
            }
            synchronized (this) {
                size++;
                if (size == list.size()) {
                    notify();
                }
            }
        }

        synchronized List<E> getList() throws InterruptedException {
            while (size < list.size()) {
                wait();
            }
            return list;
        }
    }


    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final var answers = new concurrentList<R>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int ind = i;
            add(() -> answers.set(ind, f.apply(args.get(ind))));
        }

        return answers.getList();
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        waitForThreads(workers);
    }
}
