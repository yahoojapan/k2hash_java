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

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Queue holds elements in a FIFO (first-in-first-out) manner. Order of elements is configurable by
 * passing a Boolean variable to the 2nd parameter of constructors. Passing a true variable
 * constructs a Queue instance, Otherwise a Stack instance is created.
 *
 * <p>The difference of KeyQueue and Queue: KeyQueue stores references to keys in k2hash database
 * whereas Queue holds any kinds of elements.
 *
 * <p><b>Usage Examples.</b> Suppose you want to make a new sorted data collection in a K2hashQueue.
 * You could write this as:
 *
 * <pre>{@code
 * package com.example;
 *
 * import java.io.IOException;
 * import java.util.*;
 * import java.util.stream.*;
 * import ax.antpick.*;
 *
 * public class App {
 *   public static void main(String[] args) {
 *     // 1. make a random string array for test
 *     String[] suffleArray = {"e", "b", "d", "a", "c"};
 *     // 2. sort it!
 *     Arrays.sort(suffleArray);
 *     // 3. offer to a Queue
 *     Stream<String> stream = Stream.of(suffleArray);
 *     try (K2hash db = K2hash.of("App.k2h")) {
 *       long handle = db.getHandle();
 *       K2hashQueue queue = K2hashQueue.of(handle);
 *       stream.forEach(s -> { queue.offer(s); });
 *     } catch (IOException ex) {
 *       System.out.println(ex.getMessage());
 *       return;
 *     }
 *     // 4. poll from the Queue
 *     try (K2hash db = K2hash.of("App.k2h")) {
 *       long handle = db.getHandle();
 *       K2hashQueue queue = K2hashQueue.of(handle);
 *       String s = null;
 *       do {
 *         s = queue.poll();
 *         System.out.println(s);
 *       } while (s != null);
 *     } catch (IOException ex) {
 *       System.out.println(ex.getMessage());
 *       return;
 *     }
 *   }
 * }
 *
 * }</pre>
 */
public final class K2hashQueue implements Closeable {

  /** A logger instance. */
  private static final Logger logger = LoggerFactory.getLogger(K2hashQueue.class);
  /** Address of a K2hash shared library. */
  private static K2hashLibrary INSTANCE = null; // get a INSTANCE from K2hash class

  /* --  Constants -- */
  /** Default k2hash data handle. */
  public static final long K2H_INVALID_HANDLE = 0L;
  /** Default FIFO is <code>true</code>. */
  public static final boolean DEFAULT_FIFO = true;
  /** Default prefix string is <code>null</code>. */
  public static final String DEFAULT_PREFIX = null;
  /** Default position of a peek operation is <code>0</code>. */
  public static final int DEFAULT_POSITION = 0;
  /** Default passphrase string is <code>null</code>. */
  public static final String DEFAULT_PASS = null;
  /** Default data expiration duration is <code>0</code>. */
  public static final long DEFAULT_EXPIRATION_DURATION = 0;
  /** Default K2hashAttrPack array is <code>null</code>. */
  public static final K2hashAttrPack[] DEFAULT_ATTRPACK = null;
  /** Default size of a K2hashAttrPack array is <code>0</code>. */
  public static final int DEFAULT_ATTRPACK_SIZE = 0;

  /* --  Members -- */
  /** a K2hash data handle. */
  private long handle = K2H_INVALID_HANDLE;
  /** FIFO(Queue) or LIFO(Stack). */
  private boolean fifo = true;
  /** a prefix string of this queue. */
  private String prefix = "";
  /** a K2hashQueue data handle. */
  private long queueHandle = K2H_INVALID_HANDLE;
  /** a password string. */
  private String pass;
  /** data expiration duration. */
  private long expirationDuration;

  /* -- debug methods -- */

  @Override
  public String toString() {
    // K2hash[path=test.k2h,handle=0]
    StringBuilder sb = new StringBuilder();
    sb.append(this.getClass().getName());
    sb.append("[");
    for (Field field : this.getClass().getDeclaredFields()) {
      sb.append(field.getName());
      sb.append("=");
      try {
        sb.append(field.get(this));
      } catch (IllegalArgumentException | IllegalAccessException ex) {
        sb.append("null");
      }
      sb.append(" ");
    }
    sb.append("]");
    return sb.toString();
  }

  /* --  Constructors -- */
  /**
   * Constructs a K2hashQueue instance.
   *
   * @param handle a K2hash data handle
   * @return a K2hashQueue instance
   * @throws IOException if a K2hash data handle is invalid
   */
  public static K2hashQueue of(final long handle) throws IOException {
    if (handle <= K2H_INVALID_HANDLE) {
      throw new IOException("K2hashQueue.open failed");
    }
    return new K2hashQueue(
        handle, DEFAULT_FIFO, DEFAULT_PREFIX, DEFAULT_PASS, DEFAULT_EXPIRATION_DURATION);
  }

  /**
   * Constructs a K2hashQueue instance.
   *
   * @param handle a K2hash data handle
   * @param fifo <code>true</code> if elements are in a FIFO (first-in-first-out) manner
   * @param prefix a prefix string of this queue
   * @return a K2hashQueue instance
   * @throws IOException if a K2hash data handle is invalid
   */
  public static K2hashQueue of(final long handle, final boolean fifo, final String prefix)
      throws IOException {
    if (handle <= K2H_INVALID_HANDLE) {
      throw new IOException("K2H_INVALID_HANDLE");
    }
    return new K2hashQueue(handle, fifo, prefix, DEFAULT_PASS, DEFAULT_EXPIRATION_DURATION);
  }

  /**
   * Constructs a K2hashQueue instance.
   *
   * @param handle a K2hash data handle
   * @param fifo <code>true</code> if elements are in a FIFO (first-in-first-out) manner
   * @param prefix a prefix string of this queue
   * @param pass a passphrase string to access data
   * @param expirationDuration a duration to expire data in seconds
   * @return a K2hashQueue instance
   * @throws IOException if a K2hash handle is invalid
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public static K2hashQueue of(
      final long handle,
      final boolean fifo,
      final String prefix,
      final String pass,
      final long expirationDuration)
      throws IOException {
    if (handle <= K2H_INVALID_HANDLE) {
      throw new IOException("K2H_INVALID_HANDLE");
    }
    return new K2hashQueue(handle, fifo, prefix, pass, expirationDuration);
  }

  /**
   * Constructs a K2hashQueue instance.
   *
   * @param handle a K2hash data handle
   * @param fifo <code>true</code> if elements are in a FIFO (first-in-first-out) manner
   * @param prefix a prefix string of this queue
   * @param pass a passphrase string to access data
   * @param expirationDuration a duration to expire data in seconds
   * @throws IllegalArgumentException if a expirationDuration is negative
   * @throws IOException if a K2hash handle is invalid
   */
  private K2hashQueue(
      final long paramHandle,
      final boolean paramFifo,
      final String paramPrefix,
      final String paramPass,
      final long paramExpirationDuration)
      throws IOException {
    if (paramHandle <= K2H_INVALID_HANDLE) {
      throw new IOException("handle is K2H_INVALID_HANDLE");
    }
    if (paramExpirationDuration < 0) {
      throw new IllegalArgumentException("expirationDuration is greater than equal zero");
    }

    this.handle = paramHandle;
    this.prefix = paramPrefix;
    this.fifo = paramFifo;
    this.pass = paramPass;
    this.expirationDuration = paramExpirationDuration;

    // gets the library address
    if (INSTANCE == null) {
      INSTANCE = K2hash.getLibrary(); // throws IOException if INSTANCE is null
    }

    // calls the C API
    this.queueHandle =
        INSTANCE.k2h_q_handle_prefix(
            this.handle, this.fifo, this.prefix, (this.prefix != null) ? this.prefix.length() : 0);
    if (this.queueHandle <= K2H_INVALID_HANDLE) {
      throw new IOException("queueHandle is K2H_INVALID_HANDLE");
    }
  }

  /* -- Instance methods -- */
  /** Free KeyQueueHandle. */
  @Override
  public void close() throws IOException {
    assert (this.queueHandle > K2H_INVALID_HANDLE);
    INSTANCE.k2h_q_free(this.queueHandle);
  }

  /**
   * Returns a K2hashQueue handle.
   *
   * @return a K2hashQueue handle
   */
  public long getQueueHandle() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    return this.queueHandle;
  }

  /**
   * Returns true if, and only if, queue size is 0.
   *
   * @return <code>true</code> if empty <code>false</code> otherwise
   */
  public boolean isEmpty() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);

    // calls the C API
    boolean isEmpty = INSTANCE.k2h_q_empty(this.queueHandle);
    logger.debug("isEmpty:{}", isEmpty);

    return isEmpty;
  }

  /**
   * Returns the number of queue.
   *
   * @return the number of queue.
   */
  public long count() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);

    // calls the C API
    int count = INSTANCE.k2h_q_count(this.queueHandle);
    logger.debug("count:{}", count);

    return count;
  }

  /**
   * Finds and gets a object at the head of this queue. Null returns if this queue is empty. This
   * operation is a read-only access. The object will not remove from this queue. This method is the
   * same function as k2h_q_read in C API.
   *
   * @return an object at the head of this queue
   */
  public String peek() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    return peek(DEFAULT_POSITION);
  }

  /**
   * Finds and gets a object from the head of this queue. Null returns if this queue is empty. This
   * operation is a read-only access. The object will not remove from this queue. This method is the
   * same function as k2h_q_read in C API.
   *
   * @param position a position to peek in this queue
   * @return an object at the position of this queue
   * @throws IllegalArgumentException if the position is less than zero
   */
  public String peek(final int position) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);

    if (position < 0) {
      throw new IllegalArgumentException("position is greater than equal zero");
    }

    // calls the C API
    PointerByReference ppdata = new PointerByReference();
    IntByReference pdatalen = new IntByReference();
    boolean isSuccess =
        INSTANCE.k2h_q_read_wp(this.queueHandle, ppdata, pdatalen, position, this.pass);

    if (pdatalen.getValue() != 0) {
      Pointer p = ppdata.getValue();
      byte[] buffer = p.getByteArray(0, pdatalen.getValue());
      String data = new String(buffer);
      logger.debug("isSuccess:{} data:{}", isSuccess, data);
      return data;
    }

    logger.warn("isSuccess:{} data:null", isSuccess);
    return null;
  }

  /**
   * Inserts an element into the tail of this queue.
   *
   * @param obj an object to be inserted to this queue
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException - if arguments are illegal.
   */
  public boolean offer(final Object obj) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    if (obj == null) {
      throw new IllegalArgumentException("obj is null. shouldn't be null.");
    }
    String val = null;
    boolean isSuccess = false;
    try {
      if (obj instanceof String) {
        val = (String) obj;
      } else {
        throw new IllegalArgumentException("String object is needed");
      }

      LongByReference duration = null;
      if (this.expirationDuration > 0) {
        duration = new LongByReference();
        duration.setValue(this.expirationDuration);
      }

      // calls the C API
      isSuccess =
          INSTANCE.k2h_q_push_wa(
              this.queueHandle,
              val,
              val.length(),
              DEFAULT_ATTRPACK, // TODO currently null
              DEFAULT_ATTRPACK_SIZE, // TODO currently zero
              this.pass,
              duration);

      logger.debug(
          "isSuccess:{} val:{} pass:{} expirationDuration:{}",
          isSuccess,
          val,
          this.pass,
          this.expirationDuration);
    } catch (java.lang.ClassCastException ex) {
      logger.error(ex.getMessage());
    }
    if (!isSuccess) {
      logger.error("offer false");
    }
    return isSuccess;
  }

  /**
   * Finds and gets a object from the head of this queue. Null returns if this queue is empty. This
   * operation is a write access. The object will remove from this queue.
   *
   * @return the object at the head of this queue. returns null if empty.
   */
  public String poll() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);

    // calls the C API
    PointerByReference ppdata = new PointerByReference(); // unsigned char** ppdata
    IntByReference pdatalen = new IntByReference();
    boolean isSuccess =
        INSTANCE.k2h_q_pop_wa(this.queueHandle, ppdata, pdatalen, null, null, this.pass);
    if (pdatalen.getValue() != 0) {
      Pointer p = ppdata.getValue();
      if (p != null) {
        byte[] buffer = p.getByteArray(0, pdatalen.getValue());
        String data = new String(buffer);
        logger.debug("isSuccess:{} data:{}", isSuccess, data);
        return data;
      }
      logger.warn("the pointer to the val is null. Probably the head of this queue is null");
    }
    logger.warn("isSuccess:{} data:null", isSuccess);
    return null;
  }

  /**
   * Inserts an element into the tail of this queue.
   *
   * @param obj an object to be inserted into this queue
   * @throws NoSuchElementException - if this queue is empty
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean add(final Object obj) {
    if (obj == null) {
      throw new IllegalArgumentException("obj is null. shouldn't be null.");
    }
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    String val = null;
    try {
      if (obj instanceof String) {
        val = (String) obj;
      } else {
        throw new IllegalArgumentException("String object is needed");
      }
      return this.offer(val);
    } catch (java.lang.ClassCastException ex) {
      logger.error(ex.getMessage());
    }
    return false;
  }

  /**
   * Finds and removes the object at the top of this queue and returns the object.
   *
   * @return the object at the top of this queue
   * @throws NoSuchElementException - if this queue is empty
   */
  public String remove() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);

    PointerByReference ppdata = new PointerByReference();
    IntByReference pdatalen = new IntByReference();
    boolean isSuccess = INSTANCE.k2h_q_pop_wp(this.queueHandle, ppdata, pdatalen, this.pass);
    if (pdatalen.getValue() != 0) {
      Pointer p = ppdata.getValue();
      if (p != null) {
        byte[] buffer = p.getByteArray(0, pdatalen.getValue());
        String data = new String(buffer);
        logger.debug("isSuccess:{} data:{}", isSuccess, data);
        return data;
      }
      logger.warn("the pointer to the val is null. Probably the head of this queue is null");
    }
    throw new NoSuchElementException("this queue is empty");
  }

  /**
   * Removes objects from this queue.
   *
   * @param count the number of objects to be removed
   * @return list of removed objects
   * @throws NoSuchElementException - if this queue is empty
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public List<String> removeList(final long count) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    if (count < 0) {
      throw new IllegalArgumentException("count is negative. should be positive");
    }
    List<String> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      try {
        String data = this.remove();
        if (data != null) {
          list.add(data);
        }
      } catch (NoSuchElementException e) {
        logger.warn("NoSuchElementException {}", e.getMessage());
        break;
      }
    }
    return list;
  }

  /**
   * Finds and gets a object from the head of this queue. Throws NoSuchElementException if this
   * queue is empty. This operation is a read-only access. The object will not remove from this
   * queue.
   *
   * @return a copy of the string in queue
   * @throws NoSuchElementException - if this queue is empty
   */
  public String element() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    String val = this.peek(DEFAULT_POSITION);
    if (val != null) {
      return val;
    }
    throw new NoSuchElementException("this queue is empty");
  }

  /**
   * Finds and gets a object from the head of this queue. Throws NoSuchElementException if this
   * queue is empty. This operation is a read-only access. The object will not remove from this
   * queue.
   *
   * @param position position where the data exists in queue
   * @return a copy of the string in queue
   * @throws NoSuchElementException - if this queue is empty
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public String element(final int position) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    if (position < 0) {
      throw new IllegalArgumentException("position is negative. should be positive");
    }
    String val = this.peek(position);
    if (val != null) {
      return val;
    }
    throw new NoSuchElementException("this queue is empty");
  }

  /**
   * Removes all of the elements from this collection (optional operation). The collection will be
   * empty after this method returns.
   */
  public void clear() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    long count = this.count();
    if (count > 0) {
      INSTANCE.k2h_q_remove(this.queueHandle, (int) count);
    }
  }

  /**
   * Print the objects in this queue.
   *
   * @throws NoSuchElementException - if this queue is empty
   */
  public void print() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    INSTANCE.k2h_q_dump(this.queueHandle, null);
  }
}
//
// Local variables:
// tab-width: 2
// c-basic-offset: 2
// indent-tabs-mode: nil
// End:
// vim600: noexpandtab sw=2 ts=2 fdm=marker
// vim<600: noexpandtab sw=2 ts=2
//
