/*
 * The MIT License
 *
 * Copyright 2018 Yahoo Japan Corporation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * AUTHOR:   Hirotaka Wakabayashi
 * CREATE:   Fri, 14 Sep 2018
 * REVISION:
 *
 */
package ax.antpick.k2hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Unit test for simple App. */
/** @author hiwakaba */
public class K2hashQueueTest {
  private static final Logger logger = LoggerFactory.getLogger(K2hashQueueTest.class);
  private static final String FILEDB = "queue.k2h";

  @BeforeEach
  public void setUp() {
    File fileDb = new File(FILEDB);
    if (fileDb.exists()) {
      fileDb.delete();
    }
  }

  @AfterEach
  public void tearDown() {
    File fileDb = new File(FILEDB);
    if (fileDb.exists()) {
      fileDb.delete();
    }
  }

  /** K2hashQueue Constructor */
  public void testOfArg1() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle);
      assertTrue(queue.getQueueHandle() > K2hashQueue.K2H_INVALID_HANDLE);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue Constructor */
  public void testOfArg3() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      /* do nothing */
      assertTrue(queue.getQueueHandle() > K2hashQueue.K2H_INVALID_HANDLE);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue isEmpty */
  public void testEmpty() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      // 1. assure it's empty
      boolean isEmpty = queue.isEmpty();
      assertTrue(isEmpty);
      // 2. make sure it's not empty after offering data
      String data = "val";
      boolean isSuccess = queue.offer(data);
      assertTrue(isSuccess);
      isEmpty = queue.isEmpty();
      assertFalse(isEmpty);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue count */
  public void testCount() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      long size = queue.count();
      assertTrue(size == 0);
      // 2. assure it's 1 count
      String data = "val";
      boolean isSuccess = queue.offer(data);
      assertTrue(isSuccess);
      size = queue.count();
      assertTrue(size == 1);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue peek */
  public void testPeek() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      String data = queue.peek();
      assertTrue(data == null); // null if queue is empty.
      // 2. assure it's not null
      String data2 = "val";
      boolean isSuccess = queue.offer(data2);
      assertTrue(isSuccess);
      data = queue.peek();
      assertTrue(data != null);
      // 3. assure not null again because is a peek operation
      data = queue.peek();
      assertTrue(data != null);
      assertTrue(data.equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue offer */
  public void testOfferArg1() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      boolean isSuccess = queue.offer("val");
      assertTrue(isSuccess);
      // 2. assure it's not null
      String data2 = queue.peek(0);
      assertTrue(data2 != null);
      assertTrue(data2.equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue offer */
  public void testOfferArg4() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix", "pass", 10L);
      boolean isSuccess = queue.offer("val");
      assertTrue(isSuccess);
      // 2. assure it's not null
      String data2 = queue.peek(0);
      assertTrue(data2 != null);
      assertTrue(data2.equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue poll */
  public void testPoll() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      String val = queue.poll();
      assertTrue(val == null);

      // 2. assure it's not null
      String data = "val";
      boolean isSuccess = queue.offer(data);
      assertTrue(isSuccess);
      val = queue.poll();
      assertTrue(val != null);
      assertTrue(val.equals("val"));

      // 3. assure it's null
      val = queue.poll();
      assertTrue(val == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue poll */
  public void testPollArgSleep() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue =
          K2hashQueue.of(
              handle, true, "prefix", "pass", K2hashQueue.DEFAULT_EXPIRATION_DURATION + 3);

      // 1. assure it's null
      String val = queue.poll();
      assertTrue(val == null);

      // 2. assure it's null 5 seconds later
      String data = "val";
      boolean isSuccess = queue.offer(data);
      assertTrue(isSuccess);
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.out.println("InterruptedException " + e.getMessage());
      }
      val = queue.poll();
      assertTrue(val == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue add */
  public void testAddArg1() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      boolean isSuccess = queue.add("val");
      assertTrue(isSuccess);
      // 2. assure it's not null
      String data2 = null;
      data2 = queue.poll();
      assertTrue(data2 != null);
      assertTrue(data2.equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue add */
  public void testAddArg4() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix", "pass", 10L);
      boolean isSuccess = queue.add("val");
      assertTrue(isSuccess);
      // 2. assure it's not null
      String data2 = null;
      data2 = queue.poll();
      assertTrue(data2 != null);
      assertTrue(data2.equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue remove */
  public void testRemoveArg() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      String data = "val";
      boolean isSuccess = queue.add(data);
      assertTrue(isSuccess);
      // 2. assure it's not null
      String data2 = queue.remove();
      assertTrue(data2 != null);
      assertTrue(data2.equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hashQueue remove */
  public void testRemoveArgException() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      String val = queue.remove();
      System.out.println("NoSuchElementException should be thrown");
      assertFalse(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    } catch (NoSuchElementException e) {
      assertTrue(true);
    }
  }

  /** K2hashQueue print */
  public void testPrint() {
    try (K2hash db = K2hash.of(FILEDB)) {
      long handle = db.getHandle();
      K2hashQueue queue = K2hashQueue.of(handle, true, "prefix");
      queue.print();
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }
}
