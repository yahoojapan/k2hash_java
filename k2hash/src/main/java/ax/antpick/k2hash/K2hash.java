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

import com.sun.jna.IntegerType;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@code K2hash} encapsulates the {@link K2hashLibrary} class and represents a K2hash
 * key-value store.
 *
 * <p><b>Usage Examples.</b> Suppose you want to set a key of "keystring" with a value of
 * "valuestring". You could write this as:
 *
 * <pre>{@code
 * import com.sun.jna.*;
 * import com.sun.jna.ptr.*;
 * import java.io.IOException;
 * import ax.antpick.k2hash.K2hash;
 *
 * public class App {
 *   public static void main(String[] args) {
 *     try (K2hash db = K2hash.of("App.k2h")) {
 *       db.setValue("keystring", "valstring");
 *       System.out.println(db.getValue("keystring"));
 *     } catch (IOException ex) {
 *       System.out.println(ex.getMessage());
 *     }
 *   }
 * }
 * }</pre>
 */
public final class K2hash implements Closeable {

  /* -- private Static members -- */
  /** A logger instance. */
  private static final Logger logger = LoggerFactory.getLogger(K2hash.class);
  /** The default file system that is accessible to the JVM. */
  private static final FileSystem fs = FileSystems.getDefault();
  /** Address of a K2hash shared library. */
  private static K2hashLibrary INSTANCE = null;
  /** Address of a C shared library. */
  private static CLibrary C_INSTANCE = null;

  /* -- public Static members -- */
  /** Default k2hash data handle. */
  public static final long K2H_INVALID_HANDLE = 0;
  /** The library maps the whole file to memory using mmap. */
  public static final boolean DEFAULT_IS_FULLMAP = true;
  /** The library opens a k2hash file with read only access. */
  public static final boolean DEFAULT_IS_READONLY = false;
  /** The library doesn't remove a k2hash file when no process has attached it. */
  public static final boolean DEFAULT_IS_REMOVE_FILE = false;
  /** Default key mask bits. */
  public static final int DEFAULT_MASK_BITS = 8;
  /** Default key collision mask bits. */
  public static final int DEFAULT_CMASK_BITS = 4;
  /** Default maximum elements of a collision key. */
  public static final int DEFAULT_MAX_ELEMENT = 32;
  /** Default page size. */
  public static final int DEFAULT_PAGE_SIZE = 4096;

  /** Defines access modes used to open a file. */
  public enum OPEN_MODE {
    /** Read and write access to a k2hash file. */
    DEFAULT,
    /** Read and write access to a k2hash file. */
    RDWR,
    /** Read only access to a k2hash file. */
    RDONLY,
    /** Read and write access to a temporary file. */
    TMPFILE,
    /** Read and write access to memory. */
    MEM
  };

  /** Defines levels used to dump a k2hash data. */
  public enum STATS_DUMP_LEVEL {
    /** Dump headers. */
    HEADER,
    /** Dump headers and hash tables. */
    HASH_TABLE,
    /** Dump headers, hash tables and sub hash tables. */
    SUB_HASH_TABLE,
    /** Dump headers, hash tables, sub hash tables and elements. */
    ELEMENT,
    /** Dump headers, hash tables, sub hash tables, elements and pages. */
    PAGE
  };

  /* -- private instance members -- */
  /** K2hash data handle. */
  private long handle = K2H_INVALID_HANDLE;

  /**
   * Returns full of members as a string.
   *
   * @return full of members as a string in a key=value manner
   */
  @Override
  public String toString() {
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
      sb.append(",");
    }
    sb.append("]");
    return sb.toString();
  }

  /* -- private Static methods -- */
  /**
   * Checks if a string is not null and not empty. This function is for processing errors easily.
   *
   * @param val string to be checked
   * @param varName variable name used for the error message
   * @exception IllegalArgumentException If {@code val} is null
   * @throws IllegalArgumentException If {@code val} is negative or zero
   */
  static void isStringNotNull(final String val, final String varName) {
    if (val == null) {
      throw new IllegalArgumentException(varName + "is null");
    }
  }

  /**
   * Checks if a string is not null and not empty. This function is for processing errors easily.
   *
   * @param val string to be checked
   * @param varName variable name used for the error message
   * @exception IllegalArgumentException If {@code val} is null
   * @throws IllegalArgumentException If {@code val} is negative or zero
   */
  static void isStringNotEmpty(final String val, final String varName) {
    if (val == null) {
      throw new IllegalArgumentException(varName + "is null");
    }
    if (val.isEmpty()) {
      throw new IllegalArgumentException(varName + "is empty");
    }
  }

  /**
   * Checks if a String array is not null. This function is for processing errors easily.
   *
   * <p>Note: This function doesn't investigate the value of elements in array.
   *
   * @param array an array to be checked
   * @param varName variable name used for the error message
   * @exception IllegalArgumentException If {@code val} is null
   * @exception IllegalArgumentException If {@code val} is negative or zero
   */
  static void isStringArrayNotEmpty(final String[] array, final String varName) {
    if (array == null || array.length == 0) {
      throw new IllegalArgumentException(varName + "is null");
    }
  }

  /**
   * Checks if a byte array is not null. This function is for processing errors easily.
   *
   * <p>Note: This function doesn't investigate the value of elements in array.
   *
   * @param array an array to be checked
   * @param varName variable name used for the error message
   * @exception IllegalArgumentException If {@code val} is null
   * @exception IllegalArgumentException If {@code val} is negative or zero
   */
  static void isByteArrayNotEmpty(final byte[] array, final String varName) {
    if (array == null || array.length == 0) {
      throw new IllegalArgumentException(varName + "is null");
    }
  }

  /**
   * Checks if val in integer is positive.
   *
   * @param val value to be checked
   * @param varName variable name used for the error message
   * @throws IllegalArgumentException If {@code val} is negative or zero
   */
  static void isPositive(final int val, final String varName) {
    if (val < 0) {
      throw new IllegalArgumentException(varName + " is less than equal zero");
    }
  }

  /**
   * Checks if val in long is positive.
   *
   * @param val value to be checked
   * @param varName variable name used for the error message
   * @throws IllegalArgumentException If {@code val} is negative or zero
   */
  static void isPositive(final long val, final String varName) {
    if (val < 0) {
      throw new IllegalArgumentException(varName + "is less than equal zero");
    }
  }

  /* -- Constructors -- */

  /**
   * Creates a K2hash instance.
   *
   * @param pathname a K2hash file path string
   * @param mode an access mode to open a file
   * @throws IllegalArgumentException if pathname is null
   * @throws IOException if a k2hash file is unavailable
   */
  private K2hash(final Path pathname, final OPEN_MODE mode) throws IOException {
    if (pathname == null || pathname.toString().isEmpty()) {
      throw new IllegalArgumentException("pathname is null");
    }
    logger.debug("try to open {} with {}", pathname, mode.name());
    switch (mode) {
      case DEFAULT:
        this.handle =
            INSTANCE.k2h_open(
                pathname.toString(),
                DEFAULT_IS_READONLY,
                DEFAULT_IS_REMOVE_FILE,
                DEFAULT_IS_FULLMAP,
                DEFAULT_MASK_BITS,
                DEFAULT_CMASK_BITS,
                DEFAULT_MAX_ELEMENT,
                DEFAULT_PAGE_SIZE);
        break;
      case RDWR:
        this.handle =
            INSTANCE.k2h_open_rw(
                pathname.toString(),
                DEFAULT_IS_FULLMAP,
                DEFAULT_MASK_BITS,
                DEFAULT_CMASK_BITS,
                DEFAULT_MAX_ELEMENT,
                DEFAULT_PAGE_SIZE);
        break;
      case RDONLY:
        this.handle =
            INSTANCE.k2h_open_ro(
                pathname.toString(),
                DEFAULT_IS_FULLMAP,
                DEFAULT_MASK_BITS,
                DEFAULT_CMASK_BITS,
                DEFAULT_MAX_ELEMENT,
                DEFAULT_PAGE_SIZE);
        break;
      case TMPFILE:
        this.handle =
            INSTANCE.k2h_open_rw(
                pathname.toString(),
                DEFAULT_IS_FULLMAP,
                DEFAULT_MASK_BITS,
                DEFAULT_CMASK_BITS,
                DEFAULT_MAX_ELEMENT,
                DEFAULT_PAGE_SIZE);
        break;
      case MEM:
        this.handle =
            INSTANCE.k2h_open_mem(
                DEFAULT_MASK_BITS, DEFAULT_CMASK_BITS, DEFAULT_MAX_ELEMENT, DEFAULT_PAGE_SIZE);
        break;
      default:
        break;
    }
    if (this.handle <= K2H_INVALID_HANDLE) {
      throw new IOException("k2hash.open failed");
    }
  }

  /**
   * Creates a K2hash instance using k2h_open_mem.
   *
   * @param mode file open mode
   * @throws IllegalArgumentException when mode is invalid
   * @throws IOException if a k2hash file is unavailable
   */
  private K2hash(final K2hash.OPEN_MODE mode) throws IOException {
    if (mode == K2hash.OPEN_MODE.MEM) {
      this.handle =
          INSTANCE.k2h_open_mem(
              DEFAULT_MASK_BITS, DEFAULT_CMASK_BITS, DEFAULT_MAX_ELEMENT, DEFAULT_PAGE_SIZE);
      if (this.handle <= K2H_INVALID_HANDLE) {
        throw new IOException("k2hash.open failed");
      }
    } else {
      throw new IllegalArgumentException("only MEM mode is currently supported");
    }
  }

  /* -- public Static methods -- */
  /**
   * Creates a K2hashLibrary instance.
   *
   * @throws IOException if failed to load the k2hash library
   * @return a K2hashLibrary instance
   */
  public static synchronized K2hashLibrary getLibrary() throws IOException {
    if (INSTANCE == null) {
      INSTANCE =
          (K2hashLibrary) Native.synchronizedLibrary(Native.load("k2hash", K2hashLibrary.class));
    }
    if (INSTANCE == null) {
      throw new IOException("loading shared library error");
    }
    return INSTANCE;
  }

  /**
   * Creates a C Library instance.
   *
   * @throws IOException if failed to load the C library
   * @return a C library instance
   */
  public static synchronized CLibrary getCLibrary() throws IOException {
    if (C_INSTANCE == null) {
      C_INSTANCE = (CLibrary) Native.synchronizedLibrary(Native.load("c", CLibrary.class));
    }
    if (C_INSTANCE == null) {
      throw new IOException("loading shared library error");
    }
    return C_INSTANCE;
  }

  /**
   * Creates a k2hash instance.
   *
   * @param pathname a k2hash file path string
   * @return a K2hash instance
   * @throws IllegalArgumentException if pathname is null
   * @throws IOException if creating a k2hash file failed
   */
  public static synchronized K2hash of(final String pathname) throws IOException {
    if (pathname == null) {
      throw new IllegalArgumentException();
    }
    Path path = fs.getPath(pathname).toAbsolutePath();
    if (INSTANCE == null) {
      INSTANCE =
          (K2hashLibrary) Native.synchronizedLibrary(Native.load("k2hash", K2hashLibrary.class));
    }
    return new K2hash(path, K2hash.OPEN_MODE.DEFAULT);
  }

  /**
   * Creates a k2hash instance.
   *
   * @param mode an access mode to open a file
   * @return a K2hash instance
   * @throws IOException if creating a k2hash file failed
   */
  public static synchronized K2hash of(final K2hash.OPEN_MODE mode) throws IOException {
    if (INSTANCE == null) {
      INSTANCE =
          (K2hashLibrary) Native.synchronizedLibrary(Native.load("k2hash", K2hashLibrary.class));
    }
    return new K2hash(mode);
  }

  /**
   * Creates a k2hash instance.
   *
   * @param pathname a k2hash file path string
   * @param mode an access mode to open a file
   * @return a K2hash instance
   * @throws IllegalArgumentException if pathname is null
   * @throws IOException if creating a k2hash file failed
   */
  public static synchronized K2hash of(final String pathname, final K2hash.OPEN_MODE mode)
      throws IOException {
    if (pathname == null) {
      throw new IllegalArgumentException("pathname is null");
    }
    Path path = fs.getPath(pathname).toAbsolutePath();
    if (INSTANCE == null) {
      INSTANCE =
          (K2hashLibrary) Native.synchronizedLibrary(Native.load("k2hash", K2hashLibrary.class));
    }
    return new K2hash(path, mode);
  }

  /**
   * Creates a k2hash file.
   *
   * @param pathname a k2hash file path string
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public static boolean create(final String pathname) {
    try {
      isStringNotEmpty(pathname, "pathname");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    return create(
        pathname, DEFAULT_MASK_BITS, DEFAULT_CMASK_BITS, DEFAULT_MAX_ELEMENT, DEFAULT_PAGE_SIZE);
  }

  /**
   * Creates a k2hash file.
   *
   * @param pathname a k2hash file path string
   * @param maskBit key mask bits
   * @param cmaskBit key collision mask bits
   * @param maxElement maximum elements of a collision key
   * @param pageSize page size
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public static boolean create(
      final String pathname,
      final int maskBit,
      final int cmaskBit,
      final int maxElement,
      final int pageSize) {
    try {
      isStringNotEmpty(pathname, "pathname");
      isPositive(maskBit, "maskBit");
      isPositive(cmaskBit, "cmaskBit");
      isPositive(maxElement, "maxElement");
      isPositive(pageSize, "pageSize");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    boolean isSuccess = INSTANCE.k2h_create(pathname, maskBit, cmaskBit, maxElement, pageSize);
    if (!isSuccess) {
      logger.error("k2h_create returns false");
    }
    return isSuccess;
  }

  /* -- Instance methods -- */
  /**
   * Returns a k2hash data handle.
   *
   * @return a k2hash data handle.
   */
  public long getHandle() {
    assert (this.handle > K2H_INVALID_HANDLE);
    return this.handle;
  }

  /** Closes a k2h file. */
  @Override
  public void close() throws IOException {
    assert (this.handle > K2H_INVALID_HANDLE);
    boolean isSuccess = INSTANCE.k2h_close(this.handle);
    if (!isSuccess) {
      logger.error("k2h_close returns false");
    }
  }

  /**
   * Starts a transaction.
   *
   * @param txFile a transaction log file path string
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean beginTx(final String txFile) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(txFile, "txFile");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    return this.beginTx(txFile, null, null, 0L);
  }

  /** The class {@code time_t} encapsulates the C.time_t structure. */
  public final class time_t extends IntegerType {
    /**
     * Constructs a time_t instance.
     *
     * @param value an integer(32 bit) value holding the number of seconds 1970-01-01T00:00:00 UTC
     */
    public time_t(final long value) {
      super(Native.LONG_SIZE, value);
    }
  }

  /**
   * Starts a transaction logging.
   *
   * @param txFile a transaction log file path
   * @param prefix a prefix of transaction log entry
   * @param param a parameter string to pass to a transaction processing handler
   * @param expirationDuration transaction log entry expiration(second)
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean beginTx(
      final String txFile, final String prefix, final String param, final long expirationDuration) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(txFile, "txFile");
      isPositive(expirationDuration, "expirationDuration");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    time_t timeExpirationDuration = new time_t(expirationDuration);
    Pointer ptr = new Memory(Long.SIZE);
    int offset = 0;
    ptr.setLong(offset, timeExpirationDuration.longValue()); // java.lang.Long.longValue()

    boolean isSuccess =
        INSTANCE.k2h_transaction_param_we(
            this.handle,
            true,
            txFile,
            prefix,
            (prefix != null) ? prefix.length() : 0,
            param,
            (param != null) ? param.length() : 0,
            (expirationDuration != 0) ? ptr : (Pointer) null);
    if (!isSuccess) {
      logger.error("k2h_transaction_param_we returns false");
    }
    return isSuccess;
  }

  /**
   * Gets a transaction log file descriptor.
   *
   * @return a transaction file descriptor
   */
  public int getTxFileFd() {
    assert (this.handle > K2H_INVALID_HANDLE);
    return INSTANCE.k2h_get_transaction_archive_fd(this.handle);
  }

  /**
   * Stops a transaction logging.
   *
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  public boolean stopTx() {
    assert (this.handle > K2H_INVALID_HANDLE);
    boolean isSuccess =
        INSTANCE.k2h_transaction_param_we(handle, false, null, null, 0, null, 0, null);
    if (!isSuccess) {
      logger.error("k2h_transaction_param_we returns false");
    }
    return isSuccess;
  }

  /**
   * Gets the number of transaction thread pool.
   *
   * @return the number of transaction thread pool
   */
  public int getTxPoolSize() {
    assert (this.handle > K2H_INVALID_HANDLE);
    return INSTANCE.k2h_get_transaction_thread_pool();
  }

  /**
   * Sets the number of transaction thread pool.
   *
   * @param txPoolSize the number of threads
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean setTxPoolSize(final int txPoolSize) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isPositive(txPoolSize, "txPoolSize");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    boolean isSuccess = INSTANCE.k2h_set_transaction_thread_pool(txPoolSize);
    if (!isSuccess) {
      logger.error("k2h_set_transaction_thread_pool returns false");
    }
    return isSuccess;
  }

  /**
   * Dumps k2hash data to a file.
   *
   * @param path a path string to dump data to
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean dumpToFile(final String path) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(path, "path");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    return this.dumpToFile(path, true);
  }

  /**
   * Dumps data to a file.
   *
   * @param path a path string to dump data to
   * @param isSkipError true if a dump process continues on errors
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean dumpToFile(final String path, final boolean isSkipError) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(path, "path");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    boolean isSuccess = INSTANCE.k2h_put_archive(this.handle, path, isSkipError);
    if (!isSuccess) {
      logger.error("k2h_put_archive returns false");
    }
    return isSuccess;
  }

  /**
   * Loads data from a file.
   *
   * @param path a path string to load data from
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   * @throws IOException if a path doesn't exist
   */
  public boolean loadFromFile(final String path) throws IOException {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(path, "path");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    return this.loadFromFile(path, true);
  }

  /**
   * Loads data from a file.
   *
   * @param path a path string to load data from
   * @param isSkipError true if a dump process continues on errors
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   * @throws IOException if a path doesn't exist
   */
  public boolean loadFromFile(final String path, final boolean isSkipError) throws IOException {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(path, "path");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    File file = new File(path);
    if (!file.exists()) {
      logger.error("{} doesn't exist", path);
      throw new IOException(path + " doesn't exist");
    }

    boolean isSuccess = INSTANCE.k2h_load_archive(this.handle, path, isSkipError);
    if (!isSuccess) {
      logger.error("k2h_load_archive returns false");
    }
    return isSuccess;
  }

  /**
   * Enables a feature to record value modification time.
   *
   * @param enable <code>true</code> if enable <code>false</code> if disable <code>null</code> set
   *     K2H_ATTR_DEFAULT
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  public boolean enableMtime(final boolean enable) {
    assert (this.handle > K2H_INVALID_HANDLE);
    IntByReference enableMtime = new IntByReference();
    if (enable) {
      enableMtime.setValue(1);
    } else {
      enableMtime.setValue(0);
    }
    boolean isSuccess =
        INSTANCE.k2h_set_common_attr(this.handle, enableMtime, null, null, null, null);
    if (!isSuccess) {
      logger.error("k2h_set_common_attr returns false");
    }
    return isSuccess;
  }

  /**
   * Enables a feature to encrypt a value.
   *
   * @param enable <code>true</code> if enable <code>false</code> if disable <code>null</code> set
   *     K2H_ATTR_DEFAULT
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  public boolean enableEncryption(final boolean enable) {
    assert (this.handle > K2H_INVALID_HANDLE);
    IntByReference enableEncryption = new IntByReference();
    if (enable) {
      enableEncryption.setValue(1);
    } else {
      enableEncryption.setValue(0);
    }
    boolean isSuccess =
        INSTANCE.k2h_set_common_attr(this.handle, null, enableEncryption, null, null, null);
    if (!isSuccess) {
      logger.error("k2h_set_common_attr returns false");
    }
    return isSuccess;
  }

  /**
   * Sets the data encryption password file.
   *
   * @param path file string that contains passphrase. <code>null</code> for initializing the
   *     current value.
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   * @throws IOException if a path doesn't exist
   */
  public boolean setEncryptionPasswordFile(final String path) throws IOException {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(path, "path");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    File file = new File(path);
    if (!file.exists()) {
      logger.error("{} doesn't exist", path);
      throw new IOException(path + " doesn't exist");
    }
    // path(null) is acceptable for initializing the current value
    boolean isSuccess = INSTANCE.k2h_set_common_attr(this.handle, null, null, path, null, null);
    if (!isSuccess) {
      logger.error("k2h_set_common_attr returns false");
    }
    return isSuccess;
  }

  /**
   * Enables a feature to record a key modification history.
   *
   * @param enable <code>true</code> if enable <code>false</code> if disable <code>null</code> set
   *     K2H_ATTR_DEFAULT
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  public boolean enableHistory(final boolean enable) {
    assert (this.handle > K2H_INVALID_HANDLE);
    IntByReference enableHistory = new IntByReference();
    if (enable) {
      enableHistory.setValue(1);
    } else {
      enableHistory.setValue(0);
    }
    boolean isSuccess =
        INSTANCE.k2h_set_common_attr(this.handle, null, null, null, enableHistory, null);
    if (!isSuccess) {
      logger.error("k2h_set_common_attr returns false");
    }
    return isSuccess;
  }

  /**
   * Sets the duration to expire a value.
   *
   * @param duration the duration to expire a value in second <code>null</code> set K2H_ATTR_DEFAULT
   * @param unit unit of duration. <code>0</code> for initializing the current value.
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean setExpirationDuration(final int duration, final TimeUnit unit) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isPositive(duration, "expirationDuration");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    long durationSec = 0L;
    // Conversions from finer to coarser granularities truncate, so lose precision. For example,
    // converting 999 milliseconds to seconds results in 0.
    // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html
    switch (unit.name()) {
      case "DAYS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.DAYS);
        break;
      case "HOURS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.HOURS);
        break;
      case "MICROSECONDS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.MICROSECONDS);
        break;
      case "MILLISECONDS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS);
        break;
      case "MINUTES":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.MINUTES);
        break;
      case "NANOSECONDS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.NANOSECONDS);
        break;
      case "SECONDS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.SECONDS);
        break;
      default:
        logger.warn("unknown enum name. use SECONDS as a TimeUnit");
        break;
    }
    logger.debug("durationSec {}", durationSec);

    LongByReference lref = new LongByReference(durationSec);
    boolean isSuccess =
        INSTANCE.k2h_set_common_attr(
            this.handle,
            null,
            null,
            null,
            null,
            (durationSec != 0L) ? lref : (LongByReference) null);
    if (!isSuccess) {
      logger.error("k2h_set_common_attr returns false");
    }
    return isSuccess;
  }

  /**
   * Adds a passphrase to decrypt a value.
   *
   * @param password a passphrase string to decrypt a value
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean addDecryptionPassword(final String password) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(password, "password");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    boolean isSuccess = INSTANCE.k2h_add_attr_crypt_pass(this.handle, password, false);
    if (!isSuccess) {
      logger.error("k2h_add_attr_crypt_pass returns false");
    }
    return isSuccess;
  }

  /**
   * Sets the default encryption passphrase.
   *
   * @param password a passphrase string to encrypt a value
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean setDefaultEncryptionPassword(final String password) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(password, "password");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    boolean isSuccess = INSTANCE.k2h_add_attr_crypt_pass(this.handle, password, true);
    if (!isSuccess) {
      logger.error("k2h_add_attr_crypt_pass returns false");
    }
    return isSuccess;
  }

  /**
   * Adds a shared library that handles an attribute.
   *
   * @param path a path string to the library
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   * @throws IOException if a path does't exist
   */
  public boolean addAttributePluginLib(final String path) throws IOException {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(path, "path");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    File file = new File(path);
    if (!file.exists()) {
      logger.error("{} doesn't exist", path);
      throw new IOException(path + " doesn't exist");
    }
    boolean isSuccess = INSTANCE.k2h_add_attr_plugin_library(this.handle, path);
    if (!isSuccess) {
      logger.error("k2h_add_attr_plugin_library returns false");
    }
    return isSuccess;
  }

  /** Prints attribute plugins to stderr. */
  public void printAttributePlugins() {
    assert (this.handle > K2H_INVALID_HANDLE);
    INSTANCE.k2h_print_attr_version(this.handle, null);
  }

  /** Prints attributes to stderr. */
  public void printAttributes() {
    assert (this.handle > K2H_INVALID_HANDLE);
    INSTANCE.k2h_print_attr_information(this.handle, null);
  }

  /**
   * Gets the value of a key.
   *
   * @param key a key string to retrieve the value
   * @return the value of the key
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public String getValue(final String key) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    return this.getValue(key, null);
  }

  /**
   * Gets the value of a key.
   *
   * <p>The 2nd parameter is a password to decrypt the value. The value is same with either the 2nd
   * augment of the {@link setValue} or the 1st augment of the {@link setDefaultEncryptionPassword}.
   *
   * @param key a key string to retrieve the value
   * @param pass a passphrase string to decrypt the value
   * @return value of the key
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public String getValue(final String key, final String pass) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    // TODO use k2h_get_value_wp instead of k2h_get_str_direct_value_wp because of performance.
    Pointer ptr = INSTANCE.k2h_get_str_direct_value_wp(this.handle, key, pass);
    if (ptr == null) {
      logger.warn("k2h_get_str_direct_value_wp returns null");
      return null;
    }
    String rval = ptr.getString(0);
    try {
      getCLibrary().free(ptr);
    } catch (IOException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      return null;
    }
    return rval;
  }

  /**
   * Gets the value of a key in binary format.
   *
   * <p>The 2nd parameter is a password to decrypt the value. The value is same with either the 2nd
   * augment of the {@link setValue} or the 1st augment of the {@link setDefaultEncryptionPassword}.
   *
   * <p>Note: TODO add a test.
   *
   * @param key a key string to retrieve the value
   * @param pass a passphrase string to decrypt the value
   * @return value of the key
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public byte[] getValue(final byte[] key, final String pass) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isByteArrayNotEmpty(key, "key");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    PointerByReference ppval = new PointerByReference();
    IntByReference pvallength = new IntByReference();
    boolean isSuccess =
        INSTANCE.k2h_get_value_wp(this.handle, key, key.length, ppval, pvallength, pass);
    if (!isSuccess) {
      logger.error("INSTANCE.k2h_get_value_wp returns false");
      return null;
    }
    Pointer p = ppval.getValue();
    byte[] buffer = p.getByteArray(0, pvallength.getValue());
    if (buffer == null) {
      logger.warn("k2h_get_value_wp returns null");
    }
    return buffer;
  }

  /**
   * Gets keys of subkeys of a key. Values of keys are not returned.
   *
   * @param key a key string to retrieve the value
   * @return list of subkeys of the key. null if no subkeys exist.
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public List<String> getSubkeys(final String key) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    PointerByReference pskeyarray = INSTANCE.k2h_get_str_direct_subkeys(this.handle, key);
    if (pskeyarray != null) {
      Pointer ptr = pskeyarray.getPointer();
      if (ptr != null) {
        String[] array = ptr.getStringArray(0);
        if (array.length > 0) {
          // we need a copy and use it before we free the original value.
          String[] newArray = Arrays.copyOfRange(array, 0, array.length);
          List<String> list = Arrays.asList(newArray);
          boolean isSuccess = INSTANCE.k2h_free_keyarray(pskeyarray);
          if (!isSuccess) {
            logger.error("INSTANCE.free_keyarray error");
          }
          return list;
        } else {
          logger.warn("list is empty");
        }
      } else {
        logger.warn("keypack pointer is null");
      }
    } else {
      logger.error("INSTANCE.k2h_get_str_direct_subkeys null");
    }
    return null;
  }

  /**
   * Sets a subkey of a key. The current key will be replaces with new one.
   *
   * @param key a key string
   * @param subkey the value of a subkey
   * @return <code>true</code> if succeeded. <code>false</code> otherwise.
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean setSubkey(final String key, final String subkey) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isStringNotNull(subkey, "subkey");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    String[] array = {subkey};
    return this.setSubkeys(key, array);
  }

  /**
   * Sets subkeys.
   *
   * @param key a key string
   * @param subkeys an array of subkeys
   * @return <code>true</code> if succeeded. <code>false</code> otherwise.
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean setSubkeys(final String key, final String[] subkeys) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isStringArrayNotEmpty(subkeys, "subkeys");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    StringArray jnaArray = new StringArray(subkeys);
    boolean isSuccess = INSTANCE.k2h_set_str_subkeys(handle, key, jnaArray);
    if (!isSuccess) {
      logger.error("INSTANCE.k2h_set_str_subkeys false");
      return false;
    }
    return true;
  }

  /**
   * Adds a subkey to a key.
   *
   * @param key a key string
   * @param subkey a subkey string
   * @return <code>true</code> if succeeded. <code>false</code> otherwise.
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean addSubkey(final String key, final String subkey) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isStringNotNull(subkey, "subkey");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    String[] array = {subkey};
    return addSubkeys(key, array);
  }

  /**
   * Adds subkeys to a key.
   *
   * @param key a key string
   * @param newKeys an array of subkey string
   * @return <code>true</code> if succeeded. <code>false</code> otherwise.
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean addSubkeys(String key, String[] newKeys) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isStringArrayNotEmpty(newKeys, "newKeys");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    // 1. get current subkeys
    PointerByReference pskeyarray;
    pskeyarray = INSTANCE.k2h_get_str_direct_subkeys(this.handle, key);

    String[] currentKeys = null;
    if (pskeyarray != null) {
      Pointer ptr = pskeyarray.getPointer();
      if (ptr != null) {
        currentKeys = ptr.getStringArray(0);
        if (currentKeys.length == 0) {
          logger.warn("The current subkey is empty");
        }
      } else {
        logger.warn("The current subkey is null");
      }
    } else {
      logger.warn(
          "INSTANCE.k2h_get_str_direct_subkeys returns null. Probably no key exists in k2hash database");
    }
    // 2. append requested subkey to current subkeys
    if (currentKeys == null || currentKeys.length == 0) {
      return setSubkeys(key, newKeys);
    }
    // 3. allocate array of current keys and new keys
    String[] allKeys = new String[currentKeys.length + newKeys.length];
    System.arraycopy(currentKeys, 0, allKeys, 0, currentKeys.length);
    System.arraycopy(newKeys, 0, allKeys, currentKeys.length, newKeys.length);
    INSTANCE.k2h_free_keyarray(pskeyarray);
    return setSubkeys(key, allKeys);
  }

  /**
   * Sets an attribute of a key.
   *
   * @param key a key string
   * @param attrName an attribute name
   * @param attrVal an attribute value
   * @return <code>true</code> if succeeded. <code>false</code> otherwise.
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean setAttribute(final String key, final String attrName, final String attrVal) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isStringNotEmpty(attrName, "attrName");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    boolean isSuccess = INSTANCE.k2h_add_str_attr(this.handle, key, attrName, attrVal);
    if (!isSuccess) {
      logger.error("INSTANCE.k2h_add_attr returns false");
    }
    return isSuccess;
  }

  /**
   * Gets attributes of a key.
   *
   * @param key a key string to retrieve the value
   * @return attributes of the key
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public Map<String, String> getAttributes(final String key) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    IntByReference iref = new IntByReference();
    K2hashAttrPack pp = INSTANCE.k2h_get_str_direct_attrs(handle, key, iref);
    if (pp == null || iref.getValue() == 0) {
      logger.warn("no attribute exists");
      return null;
    } else {
      pp.setAutoRead(false);
      logger.warn("{} attribute exists", iref.getValue());
    }

    K2hashAttrPack[] attrs = (K2hashAttrPack[]) pp.toArray(iref.getValue());
    logger.debug("iref.getValue() {}", iref.getValue());

    Map<String, String> map = new HashMap<>();
    Stream<K2hashAttrPack> stream = Stream.of(attrs);
    stream.forEach(
        attr -> {
          map.put(attr.pkey, attr.pval);
        });
    INSTANCE.k2h_free_attrpack(pp, iref.getValue());
    INSTANCE.k2h_close(handle);
    return map;
  }

  /**
   * Gets an attribute of a key.
   *
   * @param key a key string
   * @param attribute an attribute name of the key
   * @return the value of the attribute name of the key
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public String getAttribute(final String key, final String attribute) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isStringNotEmpty(attribute, "attribute");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }

    Map<String, String> attrs = this.getAttributes(key);
    if (attrs != null) {
      for (Map.Entry<String, String> entry : attrs.entrySet()) {
        if (attribute.equals(entry.getKey())) {
          return entry.getValue();
        }
      }
    }
    return null;
  }

  /**
   * Sets a key with a value.
   *
   * @param key a key string
   * @param val the value of the key
   * @return <code>true</code> if succeeded. <code>false</code> otherwise.
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean setValue(final String key, final String val) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    return this.setValue(key, val, "", 0, TimeUnit.SECONDS);
  }

  /**
   * Sets a key with a value.
   *
   * @param key the key string
   * @param val the value of the key
   * @param password the password
   * @param duration duration to expire
   * @param unit unit of duration
   * @return <code>true</code> if succeeded. <code>false</code> otherwise.
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean setValue(
      final String key,
      final String val,
      final String password,
      final long duration,
      final TimeUnit unit) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isPositive(duration, "duration");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    long durationSec = 0L;
    // Conversions from finer to coarser granularities truncate, so lose precision. For example,
    // converting 999 milliseconds to seconds results in 0.
    // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html
    switch (unit.name()) {
      case "DAYS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.DAYS);
        break;
      case "HOURS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.HOURS);
        break;
      case "MICROSECONDS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.MICROSECONDS);
        break;
      case "MILLISECONDS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS);
        break;
      case "MINUTES":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.MINUTES);
        break;
      case "NANOSECONDS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.NANOSECONDS);
        break;
      case "SECONDS":
        durationSec = TimeUnit.SECONDS.convert(duration, TimeUnit.SECONDS);
        break;
      default:
        logger.warn("unknown enum name. use SECONDS as a TimeUnit");
        break;
    }
    logger.debug("durationSec {}", durationSec);

    LongByReference lref = new LongByReference(durationSec);
    boolean isSuccess =
        INSTANCE.k2h_set_str_value_wa(
            this.handle, key, val, password, (durationSec != 0L) ? lref : null);
    if (!isSuccess) {
      logger.error("INSTANCE.k2h_set_str_value_wa returns false");
    }
    return isSuccess;
  }

  /**
   * Removes a key.
   *
   * @param key a key string
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean remove(final String key) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    boolean isSuccess = INSTANCE.k2h_remove_str(handle, key);
    if (!isSuccess) {
      logger.error("INSTANCE.k2h_remove_str returns false");
    }
    return isSuccess;
  }

  /**
   * Removes a key.
   *
   * @param key a key string
   * @param removeAllSubkeys <code>true</code> if removes all subkeys
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean remove(final String key, final boolean removeAllSubkeys) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    boolean isSuccess = false;
    if (removeAllSubkeys) {
      isSuccess = INSTANCE.k2h_remove_str_all(this.handle, key);
      if (!isSuccess) {
        logger.error("INSTANCE.k2h_remove_str_all returns false");
      }
    } else {
      isSuccess = INSTANCE.k2h_remove_str(handle, key);
      if (!isSuccess) {
        logger.error("INSTANCE.k2h_remove_str returns false");
      }
    }
    return isSuccess;
  }

  /**
   * Removes a key from a subkey list and removes the subkey itself.
   *
   * <p>Note: TODO add a test.
   *
   * @param key a key string
   * @param subkey a subkey string
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean remove(final String key, final String subkey) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isStringNotEmpty(subkey, "subkey");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    boolean isSuccess = INSTANCE.k2h_remove_str_subkey(this.handle, key, subkey);
    if (!isSuccess) {
      logger.error("INSTANCE.k2h_remove_str_subkey returns false");
    }
    return isSuccess;
  }

  /**
   * Renames a key with a new key.
   *
   * @param key a key string
   * @param newkey a subkey string
   * @return <code>true</code> if success <code>false</code> otherwise
   * @throws IllegalArgumentException if an illegal augment exists
   */
  public boolean rename(final String key, final String newkey) {
    assert (this.handle > K2H_INVALID_HANDLE);
    try {
      isStringNotEmpty(key, "key");
      isStringNotEmpty(newkey, "newKey");
    } catch (IllegalArgumentException ex) {
      logger.error(ex.getMessage()); /* we catch the first exception only */
      throw ex;
    }
    return INSTANCE.k2h_rename_str(this.handle, key, newkey);
  }

  /**
   * Prints k2hash key table information.
   *
   * @param level level of details of dump table information
   */
  public void printTableStats(final STATS_DUMP_LEVEL level) {
    assert (this.handle > K2H_INVALID_HANDLE);
    // TODO put stats to the logger file pointer.
    switch (level) {
      case HEADER:
        // call the k2h_dump_head
        INSTANCE.k2h_dump_head(this.handle, null);
        break;
      case HASH_TABLE:
        // call the k2h_dump_keytable
        INSTANCE.k2h_dump_keytable(this.handle, null);
        break;
      case SUB_HASH_TABLE:
        // call the k2h_dump_full_keytable
        INSTANCE.k2h_dump_full_keytable(this.handle, null);
        break;
      case ELEMENT:
        // call the k2h_dump_elementtable
        INSTANCE.k2h_dump_elementtable(this.handle, null);
        break;
      case PAGE:
        // call the k2h_dump_full
        INSTANCE.k2h_dump_full(this.handle, null);
        break;
      default:
        break;
    }
  }

  /** Prints data statistics. */
  public void printDataStats() {
    assert (this.handle > K2H_INVALID_HANDLE);
    INSTANCE.k2h_print_state(this.handle, null);
  }

  /** Prints version information. */
  public void version() {
    assert (this.handle > K2H_INVALID_HANDLE);
    INSTANCE.k2h_print_version(null);
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
