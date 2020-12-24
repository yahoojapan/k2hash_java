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

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Queue holds keys of k2hash database in a FIFO (first-in-first-out) manner. Order of elements is
 * configurable by passing a boolean variable to the 2nd parameter of constructors. Passing a true
 * variable creates a Queue instance, Otherwise a Stack instance
 *
 * <p>The difference of KeyQueue and Queue: KeyQueue stores references to keys in k2hash database
 * whereas Queue holds any kinds of elements on the other.
 *
 * <p><b>Usage Examples.</b> Suppose you want to make a new data collection from current keys.
 * KeyQueue is very useful because current keys will not be deleted after removing date from
 * keyQueue. You can use KeyQueue as a temporary data container. You could write this as:
 *
 * <pre>{@code
 * package ax.antpick;
 *
 * import IOException;
 * import java.util.*;
 * import java.util.stream.*;
 * import java.math.*;
 *
 * public class App {
 *   public static void main(String[] args) {
 *     // 1. setvalues
 *     HashMap<String, String> data = new HashMap<String, String>() {{ put("k1","v1"); put("k2","v2"); put("k3","v3");}};
 *     try (K2hash db = K2hash.of("App.k2h")) {
 *       for(Map.Entry<String, String> entry : data.entrySet()) {
 *         db.setValue(entry.getKey(), entry.getValue()); // setValue
 *       }
 *     } catch (IOException ex) {
 *       System.out.println(ex.getMessage());
 *       return;
 *     }
 *     // 2. offer to KeyQueue
 *     try (K2hash db = K2hash.of("App.k2h")) {
 *       long handle = db.getHandle();
 *       K2hashKeyQueue queue = K2hashKeyQueue.of(handle);
 *       HashMap<String, String> k1v1 = new HashMap<String, String>() {{ put("k1","v1"); }};
 *       HashMap<String, String> k2v2 = new HashMap<String, String>() {{ put("k2","v3"); }};
 *       HashMap<String, String> k3v3 = new HashMap<String, String>() {{ put("k3","v3"); }};
 *       queue.offer(k1v1);
 *       queue.offer(k2v2);
 *       queue.offer(k3v3);
 *       queue.offer(k3v3);
 *       queue.offer(k2v2);
 *       queue.offer(k1v1);
 *     } catch (IOException ex) {
 *       System.out.println(ex.getMessage());
 *       return;
 *     }
 *     // 3. peek from KeyQueue
 *     try (K2hash db = K2hash.of("App.k2h")) {
 *       long handle = db.getHandle();
 *       K2hashKeyQueue queue = K2hashKeyQueue.of(handle);
 *       Map<String,String> val = null;
 *       do {
 *         if( (val = queue.poll()) != null) {
 *           System.out.println(val.toString());
 *         }
 *       } while (val != null);
 *     } catch (IOException ex) {
 *       System.out.println(ex.getMessage());
 *       return;
 *     }
 *   }
 * }
 * }</pre>
 */
public class K2hashKeyQueue implements Closeable {

  /** A logger instance. */
  private static final Logger logger = LoggerFactory.getLogger(K2hashKeyQueue.class);
  /** Address of a K2hash shared library. */
  private static K2hashLibrary INSTANCE = null; // get a INSTANCE from K2hash class
  /** Address of a C shared library. */
  private static CLibrary C_INSTANCE = null; // get a C_INSTANCE from K2hash class

  /* --  Constants -- */
  /** Default k2hash data handle. */
  public static final long K2H_INVALID_HANDLE = 0L;
  /** Default FIFO is <code>true</code>. */
  public static boolean DEFAULT_FIFO = true;
  /** Default prefix string is <code>null</code>. */
  public static String DEFAULT_PREFIX = null;
  /** Default position of a peek operation is <code>0</code>. */
  public static int DEFAULT_POSITION = 0;
  /** Default passphrase string is <code>null</code>. */
  public static String DEFAULT_PASS = null;
  /** Default data expiration duration is <code>0</code>. */
  public static long DEFAULT_EXPIRATION_DURATION = 0;
  /** Default K2hashAttrPack array is <code>null</code>. */
  public static K2hashAttrPack[] DEFAULT_ATTRPACK = null;
  /** Default size of a K2hashAttrPack array is <code>0</code>. */
  public static int DEFAULT_ATTRPACK_SIZE = 0;

  /* --  Members -- */
  /** a K2hash data handle */
  private long handle = K2H_INVALID_HANDLE;
  /** FIFO(Queue) or LIFO(Stack) */
  private boolean fifo = true;
  /** a prefix string of this queue */
  private String prefix = "";
  /** a K2hashQueue data handle */
  private long queueHandle = K2H_INVALID_HANDLE;
  /** a password string */
  private String pass;
  /** data expiration duration */
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
   * Constructs a K2hashKeyQueue instance.
   *
   * @param handle a K2hash data handle
   * @return a K2hashKeyQueue instance
   * @throws IOException if a K2hash data handle is invalid
   */
  public static K2hashKeyQueue of(long handle) throws IOException {
    if (handle <= K2H_INVALID_HANDLE) {
      throw new IOException("K2hashKeyQueue.open failed");
    }
    return new K2hashKeyQueue(
        handle, DEFAULT_FIFO, DEFAULT_PREFIX, DEFAULT_PASS, DEFAULT_EXPIRATION_DURATION);
  }

  /**
   * Constructs a K2hashKeyQueue instance.
   *
   * @param handle a K2hash data handle
   * @param fifo <code>true</code> if elements are in a FIFO (first-in-first-out) manner
   * @return a K2hashKeyQueue instance
   * @throws IOException if a K2hash data handle is invalid
   */
  public static K2hashKeyQueue of(long handle, boolean fifo) throws IOException {
    if (handle <= K2H_INVALID_HANDLE) {
      throw new IOException("K2hashKeyQueue.open failed");
    }
    return new K2hashKeyQueue(
        handle, fifo, DEFAULT_PREFIX, DEFAULT_PASS, DEFAULT_EXPIRATION_DURATION);
  }

  /**
   * Constructs a K2hashKeyQueue instance.
   *
   * @param handle a K2hash data handle
   * @param fifo <code>true</code> if elements are in a FIFO (first-in-first-out) manner
   * @param prefix a prefix string of this queue
   * @return a K2hashKeyQueue instance
   * @throws IOException if a K2hash data handle is invalid
   */
  public static K2hashKeyQueue of(long handle, boolean fifo, String prefix) throws IOException {
    if (handle <= K2H_INVALID_HANDLE) {
      throw new IOException("K2H_INVALID_HANDLE");
    }
    return new K2hashKeyQueue(handle, fifo, prefix, DEFAULT_PASS, DEFAULT_EXPIRATION_DURATION);
  }

  /**
   * Constructs a K2hashKeyQueue instance.
   *
   * @param handle a K2hash data handle
   * @param fifo <code>true</code> if elements are in a FIFO (first-in-first-out) manner
   * @param prefix a prefix string of this queue
   * @param pass a passphrase to access data
   * @param expirationDuration a duration to expire data in seconds
   * @return a K2hashKeyQueue instance
   * @throws IOException if a K2hash data handle is invalid
   * @throws IllegalArgumentException if position is less than zero
   */
  public static K2hashKeyQueue of(
      long handle, boolean fifo, String prefix, String pass, long expirationDuration)
      throws IOException {
    if (handle <= K2H_INVALID_HANDLE) {
      throw new IOException("K2H_INVALID_HANDLE");
    }
    return new K2hashKeyQueue(handle, fifo, prefix, pass, expirationDuration);
  }

  /**
   * Constructs a K2hashKeyQueue instance.
   *
   * @param handle a K2hash data handle
   * @param fifo <code>true</code> if elements are in a FIFO (first-in-first-out) manner
   * @param prefix a prefix string of this queue
   * @throws IllegalArgumentException if expirationDuration is negative
   * @throws IOException if a k2hash data handle is invalid
   */
  private K2hashKeyQueue(
      long handle, boolean fifo, String prefix, String pass, long expirationDuration)
      throws IOException {
    if (handle <= K2H_INVALID_HANDLE) {
      throw new IOException("handle is K2H_INVALID_HANDLE");
    }
    if (expirationDuration < 0) {
      throw new IllegalArgumentException("expirationDuration is greater than equal zero");
    }

    this.handle = handle;
    this.fifo = fifo;
    this.prefix = prefix;
    this.pass = pass;
    this.expirationDuration = expirationDuration;

    // gets the library address
    if (INSTANCE == null) {
      INSTANCE = K2hash.getLibrary();
    }
    if (C_INSTANCE == null) {
      C_INSTANCE = K2hash.getCLibrary();
    }
    // calls the C API
    this.queueHandle =
        INSTANCE.k2h_keyq_handle_prefix(
            this.handle, this.fifo, this.prefix, (this.prefix != null) ? this.prefix.length() : 0);
    if (this.queueHandle <= K2H_INVALID_HANDLE) {
      throw new IOException("queueHandle is K2H_INVALID_HANDLE");
    }
  }

  /* -- Instance methods -- */
  /** Free KeyQueueHandle */
  @Override
  public void close() throws IOException {
    assert (this.queueHandle > K2H_INVALID_HANDLE);
    INSTANCE.k2h_keyq_free(this.queueHandle);
  }

  /**
   * Returns a K2hashKeyQueue handle.
   *
   * @return a K2hashKeyQueue handle
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
    boolean isEmpty = INSTANCE.k2h_keyq_empty(this.queueHandle);
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
    int count = INSTANCE.k2h_keyq_count(this.queueHandle);
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
  public Map<String, String> peek() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    return this.peek(DEFAULT_POSITION);
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
  public Map<String, String> peek(int position) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    if (position < 0) {
      throw new IllegalArgumentException("position is greater than equal zero");
    }

    // calls the C API
    PointerByReference ppkey = new PointerByReference();
    IntByReference pkeylen = new IntByReference();
    PointerByReference ppval = new PointerByReference();
    IntByReference pvallen = new IntByReference();
    boolean isSuccess =
        INSTANCE.k2h_keyq_read_keyval_wp(
            this.queueHandle, ppkey, pkeylen, ppval, pvallen, position, this.pass);

    // key
    String key = null;
    if (ppkey != null && pkeylen != null && pkeylen.getValue() != 0) {
      Pointer p = ppkey.getValue();
      if (p != null) {
        byte[] buffer = p.getByteArray(0, pkeylen.getValue());
        key = new String(buffer);
        C_INSTANCE.free(p);
      } else {
        logger.warn("the pointer to the key is null. Probably the head of this queue is null");
      }
    }
    // val
    String val = null;
    if (ppval != null && pvallen != null && pvallen.getValue() != 0) {
      Pointer p = ppval.getValue();
      if (p != null) {
        byte[] buffer = p.getByteArray(0, pvallen.getValue());
        val = new String(buffer);
        C_INSTANCE.free(p);
      } else {
        logger.warn("the pointer to the val is null. Probably the head of this queue is null");
      }
    }
    if (key == null && val == null) {
      logger.warn("this queue is empty");
      return null;
    }
    // key and value
    Map<String, String> map = new HashMap<>();
    map.put(key, val);
    logger.debug("isSuccess:{} map:{}", isSuccess, map);

    return map;
  }

  /**
   * Inserts an element into the tail of this queue.
   *
   * @param obj an object to be inserted to this queue
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException - if arguments are illegal.
   */
  public boolean offer(Object obj) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    if (obj == null) {
      throw new IllegalArgumentException("obj is null. shouldn't be null.");
    }

    LongByReference duration = null;
    if (this.expirationDuration > 0L) {
      duration = new LongByReference();
      duration.setValue(this.expirationDuration);
    }
    boolean isSuccess = false;
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> map = (Map<String, String>) obj;
      Set<Map.Entry<String, String>> set = map.entrySet();
      Iterator<Map.Entry<String, String>> it = set.iterator();
      Map.Entry<String, String> entry = it.next();
      String bykey = entry.getKey();
      String byval = entry.getValue();
      // calls the C API
      isSuccess =
          INSTANCE.k2h_keyq_push_keyval_wa(
              this.queueHandle, bykey, bykey.length(), byval, byval.length(), this.pass, duration);

      logger.debug(
          "isSuccess:{} key:{} val:{} pass:{} expirationDuration:{}",
          isSuccess,
          bykey,
          byval,
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
  public Map<String, String> poll() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);

    // calls the C API
    PointerByReference ppkey = new PointerByReference();
    IntByReference pkeylen = new IntByReference();
    PointerByReference ppval = new PointerByReference();
    IntByReference pvallen = new IntByReference();
    boolean isSuccess =
        INSTANCE.k2h_keyq_pop_keyval_wp(
            this.queueHandle, ppkey, pkeylen, ppval, pvallen, this.pass);

    // key
    String key = null;
    if (ppkey != null && pkeylen != null && pkeylen.getValue() != 0) {
      Pointer p = ppkey.getValue();
      if (p != null) {
        byte[] buffer = p.getByteArray(0, pkeylen.getValue());
        key = new String(buffer);
        C_INSTANCE.free(p);
      } else {
        logger.warn("the pointer to the key is null. Probably the head of this queue is null");
      }
    }
    // val
    String val = null;
    if (ppval != null && pvallen != null && pvallen.getValue() != 0) {
      Pointer p = ppval.getValue();
      if (p != null) {
        byte[] buffer = p.getByteArray(0, pvallen.getValue());
        val = new String(buffer);
        C_INSTANCE.free(p);
      } else {
        logger.warn("the pointer to the val is null. Probably the head of this queue is null");
      }
    }
    if (key == null && val == null) {
      logger.warn("this queue is empty");
      return null;
    }
    // keyf and value
    Map<String, String> map = new HashMap<>();
    map.put(key, val);
    logger.debug("isSuccess:{} map:{}", isSuccess, map);

    return map;
  }

  /**
   * Inserts an element into the tail of this queue.
   *
   * @param obj an object to be inserted into this queue
   * @throws NoSuchElementException - if this queue is empty
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  public boolean add(Object obj) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    @SuppressWarnings("unchecked")
    Map<String, String> map = (Map<String, String>) obj;
    return this.offer(map);
  }

  /**
   * Finds and removes the object at the top of this queue and returns the object.
   *
   * @return the object at the top of this queue
   * @throws NoSuchElementException - if this queue is empty
   */
  public Map<String, String> remove() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);

    PointerByReference ppkey = new PointerByReference();
    IntByReference pkeylen = new IntByReference();
    PointerByReference ppval = new PointerByReference();
    IntByReference pvallen = new IntByReference();

    // calls C API
    boolean isSuccess =
        INSTANCE.k2h_keyq_pop_keyval_wp(
            this.queueHandle, ppkey, pkeylen, ppval, pvallen, this.pass);

    // key
    String key = null;
    if (ppkey != null && pkeylen != null && pkeylen.getValue() != 0) {
      Pointer p = ppkey.getValue();
      if (p != null) {
        byte[] buffer = p.getByteArray(0, pkeylen.getValue());
        key = new String(buffer);
        C_INSTANCE.free(p);
      } else {
        logger.warn("the pointer to the key is null. Probably the head of this queue is null");
      }
    }
    // val
    String val = null;
    if (ppval != null && pvallen != null && pvallen.getValue() != 0) {
      Pointer p = ppval.getValue();
      if (p != null) {
        byte[] buffer = p.getByteArray(0, pvallen.getValue());
        val = new String(buffer);
        C_INSTANCE.free(p);
      } else {
        logger.warn("the pointer to the val is null. Probably the head of this queue is null");
      }
    }
    if (key == null && val == null) {
      logger.warn("this queue is empty");
      throw new NoSuchElementException("this queue is empty");
    }
    // key and value
    Map<String, String> map = new HashMap<>();
    map.put(key, val);
    logger.debug("isSuccess:{} map:{}", isSuccess, map);

    return map;
  }

  /**
   * Removes the object(key) at the top of this queue and the object(value) outside of this queue
   * This remove method is derived from the Collection interface.
   *
   * @param o element to be removed from this collection, if present
   * @return true if an element was removed as a result of this call
   */
  public boolean remove(Object o) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    if (o == null) {
      throw new IllegalArgumentException("count is negative. should be positive");
    }
    long count = this.count();
    for (long i = 0L; i < count; i++) {
      Map<String, String> map = this.peek();
      if (o instanceof String) {
        if (map != null && map.containsKey((String) o)) {
          PointerByReference ppkey = new PointerByReference();
          IntByReference pkeylen = new IntByReference();
          PointerByReference ppval = new PointerByReference();
          IntByReference pvallen = new IntByReference();

          // calls C API
          boolean isSuccess =
              INSTANCE.k2h_keyq_pop_keyval_wp(
                  this.queueHandle, ppkey, pkeylen, ppval, pvallen, this.pass);

          // key
          String key = null;
          if (ppkey != null && pkeylen != null && pkeylen.getValue() != 0) {
            Pointer p = ppkey.getValue();
            if (p != null) {
              byte[] buffer = p.getByteArray(0, pkeylen.getValue());
              key = new String(buffer);
              C_INSTANCE.free(p);
            } else {
              logger.warn(
                  "the pointer to the key is null. Probably the head of this queue is null");
            }
          }
          // val
          String val = null;
          if (ppval != null && pvallen != null && pvallen.getValue() != 0) {
            Pointer p = ppval.getValue();
            if (p != null) {
              byte[] buffer = p.getByteArray(0, pvallen.getValue());
              val = new String(buffer);
              C_INSTANCE.free(p);
            } else {
              logger.warn(
                  "the pointer to the key is null. Probably the head of this queue is null");
            }
          }
          if (key == null && val == null) {
            logger.warn("this queue is empty");
            return false;
          }
          // key and value
          logger.debug("isSuccess:{} key:{} val:{}", isSuccess, key, val);

          return true;
        }
      }
    }
    return false;
  }

  /**
   * Removes the objects from this queue.
   *
   * @param count the number of objects to be removed
   * @return list of removed objects
   * @throws NoSuchElementException - if this queue is empty
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public List<Map<String, String>> removeList(long count) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    if (count < 0) {
      throw new IllegalArgumentException("count is negative. should be positive");
    }
    List<Map<String, String>> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      try {
        Map<String, String> data = this.remove();
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
  public Map<String, String> element() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    Map<String, String> val = this.peek(DEFAULT_POSITION);
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
  public Map<String, String> element(int position) {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    if (position < 0) {
      throw new IllegalArgumentException("position is negative. should be positive");
    }
    Map<String, String> val = this.peek(position);
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
      INSTANCE.k2h_keyq_remove(this.queueHandle, (int) count);
    }
  }
  /**
   * Print the objects in this queue.
   *
   * @throws NoSuchElementException - if this queue is empty
   */
  public void print() {
    assert (this.handle > K2H_INVALID_HANDLE && this.queueHandle > K2H_INVALID_HANDLE);
    INSTANCE.k2h_keyq_dump(this.queueHandle, null);
  }
}
