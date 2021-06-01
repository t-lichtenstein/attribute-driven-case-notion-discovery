package helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class ConcurrencyHelper {

  public static <T> List<Future<T>> startAll(ThreadPoolExecutor executor, List<? extends Runnable> runnables) {
    List<Future<T>> futures = new ArrayList<>();
    for (Runnable runnable : runnables) {
      futures.add((Future<T>) executor.submit(runnable));
    }
    return futures;
  }

  public static <T> List<T> syncAll(List<Future<T>> futures) {
    List<T> returnValues = new ArrayList<>();
    try {
      for (Future<T> future : futures) {
        returnValues.add(future.get());
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return returnValues;
  }
}
