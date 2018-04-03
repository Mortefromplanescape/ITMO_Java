package ru.ifmo.rain.rykunov.mapper;

import java.util.List;

public class ConcurrentUtils {
    public static void startThreadAndAddItToList(Thread thread, List<Thread> threads) {
        thread.start();
        threads.add(thread);
    }

    public static boolean waitForThreads(List<Thread> threads) {
        boolean throwsInterruptedExc = false;

        for (var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throwsInterruptedExc = true;
            }
        }

        return throwsInterruptedExc;
    }
}
