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

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * This JNA interface provides functions defined in the K2hash C library. This class is a
 * package-private class.
 *
 * <p><b>Usage Examples.</b> Suppose you want to set the K2HDBGMODE enviroment "INFO". You could
 * write this as:
 *
 * <pre>{@code
 * package ax.antpick;
 *
 * import com.sun.jna.*;
 * import com.sun.jna.ptr.*;
 *
 * public class App {
 *   public static void main(String[] args) {
 *     K2hashLibrary INSTANCE =
 *         (K2hashLibrary) Native.synchronizedLibrary(Native.loadLibrary("k2hash", CLibrary.class));
 *     INSTANCE.k2h_set_debug_level_message();
 *   }
 * }
 * }</pre>
 */
public interface K2hashLibrary extends Library {
  /**
   * Changes the logging level up. The level will go back to the "silent" level after the "debug"
   * level.
   */
  void k2h_bump_debug_level();

  /** Changes the logging level to "silent" level. */
  void k2h_set_debug_level_silent();

  /** Changes the logging level to "error" level. error messages apear in the log file. */
  void k2h_set_debug_level_error();

  /**
   * Changes the logging level to "warning" level. Error messages and warning ones apear in the log
   * file.
   */
  void k2h_set_debug_level_warning();

  /** Changes the logging level to "error" level. all messages apear in the log file. */
  void k2h_set_debug_level_message();

  /**
   * Redirects stderr to a log file.
   *
   * @param filepath A file path string
   * @return <code>true</code> if set the debug file, <code>false</code> otherwise
   */
  boolean k2h_set_debug_file(String filepath);

  /**
   * Prints messages to stderr.
   *
   * @return <code>true</code> if unset the debug file, <code>false</code> otherwise
   */
  boolean k2h_unset_debug_file();

  /**
   * Applies the K2HDBGMODE environment and the K2HDBGFILE one to redirect logging and logging
   * level.
   *
   * @return <code>true</code> if load debug environments, <code>false</code> otherwise
   */
  boolean k2h_load_debug_env();

  /**
   * Send the SIGUSR1 signal to the process that changes the logging level up.
   *
   * @return <code>true</code> if set the signal handler, <code>false</code> otherwise
   */
  boolean k2h_set_bumup_debug_signal_user1();

  /**
   * Prints k2hash file header information to the stream.
   *
   * @param handle a k2hash data handle
   * @param stream the file pointer to which prints headers
   * @return <code>true</code> if prints headers, <code>false</code> otherwise
   */
  boolean k2h_dump_head(long handle, Pointer stream);

  /**
   * Prints the informaton abount file header and hash table to the stream.
   *
   * @param handle a k2hash data handle
   * @param stream the file pointer to which prints headers
   * @return <code>true</code> if prints headers, <code>false</code> otherwise
   */
  boolean k2h_dump_keytable(long handle, Pointer stream);

  /**
   * Prints the information about header, hash tables and secondary hash tables to the stream.
   *
   * @param handle a k2hash data handle
   * @param stream the file pointer to which prints keytables
   * @return <code>true</code> if prints keytables, <code>false</code> otherwise
   */
  boolean k2h_dump_full_keytable(long handle, Pointer stream);

  /**
   * Prints the information about header, hash tables, secondary hash tables and elements to the
   * stream.
   *
   * @param handle a k2hash data handle
   * @param stream the file pointer to which prints element tables
   * @return <code>true</code> if prints element tables, <code>false</code> otherwise
   */
  boolean k2h_dump_elementtable(long handle, Pointer stream);

  /**
   * Prints the information about header, hash tables, secondary hash tables, elements and others to
   * the stream.
   *
   * @param handle a k2hash data handle
   * @param stream the file pointer to which prints headers, tables, element tables and others
   * @return <code>true</code> if prints headers, tables, element tables and others <code>false
   *     </code> otherwise
   */
  boolean k2h_dump_full(long handle, Pointer stream);

  /**
   * Prints the status information about k2hash file to the stream.
   *
   * @param handle a k2hash data handle
   * @param stream the file pointer to which prints the current file state
   * @return <code>true</code> if prints the current file state, <code>false</code> otherwise
   */
  boolean k2h_print_state(long handle, Pointer stream);

  /**
   * Prints the version information about k2hash file to the stream.
   *
   * @param stream the file pointer to which prints version
   */
  void k2h_print_version(Pointer stream);

  /**
   * Initializes a k2hash file.
   *
   * @param filepath a file path string
   * @param maskbitcnt a mask bit
   * @param cmaskbitcnt a cmask bit
   * @param pagesize a page size
   * @param maxelementcnt a max element count
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_create(
      String filepath, int maskbitcnt, int cmaskbitcnt, int maxelementcnt, long pagesize);

  /**
   * Opens a k2hash file.
   *
   * @param filepath a file path string
   * @param readonly the flag whether readonly
   * @param removefile the flag whether remove file after detaching the memory
   * @param fullmap the flag whether mmap whole of a file into memory
   * @param maskbitcnt a mask bit
   * @param cmaskbitcnt a cmask bit
   * @param maxelementcnt a max element count
   * @param pagesize a page size
   * @return a k2hash data handle
   */
  long k2h_open(
      String filepath,
      boolean readonly,
      boolean removefile,
      boolean fullmap,
      int maskbitcnt,
      int cmaskbitcnt,
      int maxelementcnt,
      long pagesize);

  /**
   * Opens a k2hash file with read and write mode.
   *
   * @param filepath a file path string
   * @param fullmap the flag whether mmap whole of a file into memory
   * @param maskbitcnt a mask bit
   * @param cmaskbitcnt a cmask bit
   * @param maxelementcnt a max element count
   * @param pagesize a page size
   * @return a k2hash data handle
   */
  long k2h_open_rw(
      String filepath,
      boolean fullmap,
      int maskbitcnt,
      int cmaskbitcnt,
      int maxelementcnt,
      long pagesize);

  /**
   * Opens a k2hash file with read mode.
   *
   * @param filepath a file path string
   * @param fullmap the flag whether mmap whole of a file into memory
   * @param maskbitcnt a mask bit
   * @param cmaskbitcnt a cmask bit
   * @param maxelementcnt a max element count
   * @param pagesize a page size
   * @return a k2hash data handle
   */
  long k2h_open_ro(
      String filepath,
      boolean fullmap,
      int maskbitcnt,
      int cmaskbitcnt,
      int maxelementcnt,
      long pagesize);

  /**
   * Opens a k2hash file with temporary file mode.
   *
   * @param filepath a file path string
   * @param fullmap the flag whether mmap whole of a file into memory
   * @param maskbitcnt a mask bit
   * @param cmaskbitcnt a cmask bit
   * @param maxelementcnt a max element count
   * @param pagesize a page size
   * @return a k2hash data handle
   */
  long k2h_open_tempfile(
      String filepath,
      boolean fullmap,
      int maskbitcnt,
      int cmaskbitcnt,
      int maxelementcnt,
      long pagesize);

  /**
   * Opens a k2hash table on memoy.
   *
   * @param maskbitcnt a mask bit
   * @param cmaskbitcnt a cmask bit
   * @param maxelementcnt a max element count
   * @param pagesize a page size
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  long k2h_open_mem(int maskbitcnt, int cmaskbitcnt, int maxelementcnt, long pagesize);

  /**
   * Closes a k2hash file.
   *
   * @param handle a k2hash data handle
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_close(long handle);

  /**
   * Closes a k2hash file after waiting for the dedicated milliseconds.
   *
   * @param handle a k2hash data handle
   * @param waitms milliseconds to wait for closing a file
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_close_wait(long handle, long waitms);

  /**
   * Enables/Disables the transaction.
   *
   * @param handle a k2hash data handle
   * @param enable <code>true</code> if enable transaction
   * @param transfile a file string to write transaction logs
   * @param pprefix a prefix string of a transaction log entry
   * @param prefixlen the length of a prefix string of a transaction log entry
   * @param pparam a parameter passes to transaction log parser program
   * @param paramlen the length of parameter passes to transaction log parser program
   * @param pExpirationDuration the duration to expire a log
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_transaction_param_we(
      long handle,
      boolean enable,
      String transfile,
      String pprefix,
      int prefixlen,
      String pparam,
      int paramlen,
      Pointer pExpirationDuration);

  /**
   * Gets the number of workers for transactions.
   *
   * @return the number of workers
   */
  int k2h_get_transaction_thread_pool();

  /**
   * Gets the file descriptor of transaction archive file when you enable transaction.
   *
   * @param handle a k2hash data handle
   * @return the file descriptor
   */
  int k2h_get_transaction_archive_fd(long handle);

  /**
   * Sets the number of workers for transactions.
   *
   * @param count the number of workers
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_set_transaction_thread_pool(int count);

  /**
   * Disables the number of workers for transactions.
   *
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_unset_transaction_thread_pool();

  /**
   * Loads data from serialized data in a file.
   *
   * @param handle a k2hash data handle
   * @param filepath a file path string
   * @param errskip <code>true</code> if skip errors while loading a file
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_load_archive(long handle, String filepath, boolean errskip);

  /**
   * Saves serialized data to an file.
   *
   * @param handle a k2hash data handle
   * @param filepath a file path string
   * @param errskip <code>true</code> if skip errors while writing data to a file
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_put_archive(long handle, String filepath, boolean errskip);

  /**
   * Enables/Disables the designated attributes.
   *
   * @param handle a k2hash data handle
   * @param is_mtime the flag whether the library records when files are created and modified
   * @param is_defenc the flag whether the library encrypts data
   * @param passfile a password file path string
   * @param is_history the flag whether the library save its history
   * @param expirationDuration the duration data expires
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_set_common_attr(
      long handle,
      IntByReference is_mtime,
      IntByReference is_defenc,
      String passfile,
      IntByReference is_history,
      LongByReference expirationDuration);

  /**
   * Enables attributes by using shared library.
   *
   * @param handle a k2hash data handle
   * @param libpath a shared library file path string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_add_attr_plugin_library(long handle, String libpath);

  /**
   * Registers a password for encryption.
   *
   * @param handle a k2hash data handle
   * @param pass a passphrase string
   * @param is_default_encrypt the flag whether the library encrypts data and decrypts by it
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_add_attr_crypt_pass(long handle, String pass, boolean is_default_encrypt);

  /**
   * Prints the version of a library for attributes.
   *
   * @param handle a k2hash data handle
   * @param stream the file pointer to which prints versions
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_print_attr_version(long handle, Pointer stream);

  /**
   * Prints attributes information.
   *
   * @param handle a k2hash data handle
   * @param stream the file pointer to which prints attributes
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_print_attr_information(long handle, Pointer stream);

  /**
   * Gets the value of a key in binary format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key byte array
   * @param keylength the length of a key
   * @param ppval a value string
   * @param pvallength the length of a value
   * @param pass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_get_value_wp(k2h_h handle, const unsigned char* pkey, size_t keylength,
  // unsigned char** ppval, size_t* pvallength, const char* pass);
  boolean k2h_get_value_wp(
      long handle,
      byte[] pkey,
      long keylength,
      PointerByReference ppval,
      IntByReference pvallength,
      String pass);

  /**
   * Gets the value of a key in direct mode.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param pass a passphrase string
   * @return the pointer to the value string
   */
  // extern char* k2h_get_str_direct_value_wp(k2h_h handle, const char* pkey, const char* pass);
  Pointer k2h_get_str_direct_value_wp(long handle, String pkey, String pass);

  /**
   * Sets the value of a key in binary format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key byte array
   * @param keylength the length of a key
   * @param pval a value byte array
   * @param vallength the length of a value
   * @param pass a passphrase string
   * @param expirationDuration a duration in seconds to expire the data
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_set_value_wa(k2h_h handle, const unsigned char* pkey, size_t keylength, const
  // unsigned char* pval, size_t vallength, const char* pass, const time_t* expire);
  boolean k2h_set_value_wa(
      long handle,
      byte[] pkey,
      long keylength,
      byte[] pval,
      long vallength,
      String pass,
      LongByReference expirationDuration);

  // extern bool k2h_set_str_value_wa(k2h_h handle, const char* pkey, const char* pval, const char*
  // pass, const time_t* expire);
  /**
   * Sets the value of a key in text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param pval a value string
   * @param pass a passphrase string
   * @param expirationDuration a duration in seconds to expire the data
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_set_str_value_wa(
      long handle, String pkey, String pval, String pass, LongByReference expirationDuration);

  /**
   * Gets the subkeys of a key in direct mode.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @return an array list of a subkey
   */
  // extern char** k2h_get_str_direct_subkeys(k2h_h handle, const char* pkey);
  PointerByReference k2h_get_str_direct_subkeys(long handle, String pkey); // TODO text format

  /**
   * Gets the subkeys of a key in binary format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key byte array
   * @param keylength the length of a key
   * @param ppskeypck a pointer to an array of a pointer of a subkey string
   * @param pskeypckcnt the number of subkeys
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_get_subkeys(k2h_h handle, const unsigned char* pkey, size_t keylength,
  // PK2HKEYPCK* ppskeypck, int* pskeypckcnt);
  boolean k2h_get_subkeys(
      long handle,
      byte[] pkey,
      long keylength,
      PointerByReference ppskeypck,
      IntByReference pskeypckcnt); // TODO binary format

  /**
   * Frees the address of a keypack object.
   *
   * @param pkeys a pointer to an array of a pointer of a subkey string
   * @param keycnt the number of subkeys
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_free_keypack(PK2HKEYPCK pkeys, int keycnt);
  boolean k2h_free_keypack(PointerByReference pkeys, int keycnt);

  /**
   * Frees the address of an array of an array of char object.
   *
   * @param pkeys a pointer to an array of a pointer of a subkey string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_free_keyarray(char** pkeys);
  boolean k2h_free_keyarray(PointerByReference pkeys);

  /**
   * Sets the subkeys of a key in binary format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key byte array
   * @param keylength the length of a key
   * @param pskeypck a pointer to an array of a pointer of a subkey string
   * @param skeypckcnt the number of subkeys
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_set_subkeys(k2h_h handle, const unsigned char* pkey, size_t keylength, const
  // PK2HKEYPCK pskeypck, int skeypckcnt);
  boolean k2h_set_subkeys(
      long handle, byte[] pkey, long keylength, PointerByReference pskeypck, int skeypckcnt);

  /**
   * Sets the subkeys of a key in text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param pskeyarray a pointer to an array of a pointer of a subkey string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_set_str_subkeys(k2h_h handle, const char* pkey, const char** pskeyarray);
  boolean k2h_set_str_subkeys(long handle, String pkey, StringArray pskeyarray);

  /**
   * Adds a subkey to a key in binary format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key byte array
   * @param keylength the number of a key
   * @param psubkey a subkey byte array
   * @param skeylength the number of a subkey
   * @param pval a value byte array
   * @param vallength the number of a value
   * @param pass the passphrase byte array
   * @param expirationDuration a duration in seconds to expire the data
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_add_subkey_wa(k2h_h handle, const unsigned char* pkey, size_t keylength, const
  // unsigned char* psubkey, size_t skeylength, const unsigned char* pval, size_t vallength, const
  // char* pass, const time_t* expire);
  boolean k2h_add_subkey_wa(
      long handle,
      byte[] pkey,
      long keylength,
      byte[] psubkey,
      long skeylength,
      byte[] pval,
      long vallength,
      String pass,
      IntByReference expirationDuration);

  /**
   * Adds an attribute to a key in binary format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key byte array
   * @param keylength the number of a key
   * @param pattrkey an attribute key byte array
   * @param attrkeylength the number of an attribute key
   * @param pattrval an attribute value byte array
   * @param attrvallength the number of an attribute value
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_add_attr(k2h_h handle, const unsigned char* pkey, size_t keylength, const
  // unsigned char* pattrkey, size_t attrkeylength, const unsigned char* pattrval, size_t
  // attrvallength);
  boolean k2h_add_attr(
      long handle,
      byte[] pkey,
      long keylength,
      byte[] pattrkey,
      long attrkeylength,
      byte[] pattrval,
      long attrvallength);

  /**
   * Adds an attribute to a key in text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param pattrkey an attribute key string
   * @param pattrval an attribute value string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_add_str_attr(long handle, String pkey, String pattrkey, String pattrval);

  /**
   * Gets an attribute of a key in binary format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key byte array
   * @param keylength the number of a key
   * @param ppattrspck a pointer to an array of a pointer of an attribute pack structure
   * @param pattrspckcnt the number of attribute structures
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_get_attrs(k2h_h handle, const unsigned char* pkey, size_t keylength,
  // PK2HATTRPCK* ppattrspck, int* pattrspckcnt);
  boolean k2h_get_attrs(
      long handle,
      byte[] pkey,
      long keylength,
      PointerByReference ppattrspck,
      IntByReference pattrspckcnt);

  /**
   * Gets an attribute of a key in direct mode.
   *
   * @param handle a k2hash data handle
   * @param pkey a key byte array
   * @param keylength the number of a key
   * @param pattrspckcnt the number of attribute structures
   * @return a pointer to an array of a pointer of an attribute pack structure
   */
  // extern PK2HATTRPCK k2h_get_direct_attrs(k2h_h handle, const unsigned char* pkey, size_t
  // keylength, int* pattrspckcnt);
  PointerByReference k2h_get_direct_attrs(
      long handle, byte[] pkey, long keylength, IntByReference pattrspckcnt);

  /**
   * Gets an attribute of a key in direct mode in text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param pattrspckcnt the number of attribute structures
   * @return a pointer to an array of a pointer of an attribute pack structure
   */
  // extern PK2HATTRPCK k2h_get_str_direct_attrs(k2h_h handle, const char* pkey, int* pattrspckcnt);
  K2hashAttrPack k2h_get_str_direct_attrs(long handle, String pkey, IntByReference pattrspckcnt);

  /**
   * Releases the memory area pointed by the K2HATTRPCK object.
   *
   * @param pattrs an array of a pointer of an attribute pack structure
   * @param attrcnt the number of attribute structures
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_free_attrpack(PK2HATTRPCK pattrs, int attrcnt);
  boolean k2h_free_attrpack(K2hashAttrPack pattrs, int attrcnt);

  /**
   * Removes the key and all the subkeys of the key.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param keylength the number of a key string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_remove_all(k2h_h handle, const unsigned char* pkey, size_t keylength);
  boolean k2h_remove_all(long handle, String pkey, long keylength);

  /**
   * Removes the key and all the subkeys of the key using text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_remove_all(k2h_h handle, const unsigned char* pkey, size_t keylength);
  boolean k2h_remove_str_all(long handle, String pkey);

  /**
   * Removes the key and the subkeys of the key using text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param keylength the number of a key string
   * @param psubkey a subkey string
   * @param skeylength the number of a subkey string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_remove_subkey(k2h_h handle, const unsigned char* pkey, size_t keylength, const
  // unsigned char* psubkey, size_t skeylength);
  boolean k2h_remove_subkey(
      long handle, String pkey, long keylength, String psubkey, long skeylength);

  /**
   * Removes the key and the subkeys of the key using text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param psubkey a subkey string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_remove_str_subkey(k2h_h handle, const char* pkey, const char* psubkey);
  boolean k2h_remove_str_subkey(long handle, String pkey, String psubkey);

  /**
   * Removes the key.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param keylength the number of a key string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_remove(long handle, String pkey, long keylength);

  /**
   * Removes the key using text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_remove_str(long handle, String pkey);

  /**
   * Renames the old key the new key.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param keylength the number of a key string
   * @param pnewkey a new key string
   * @param newkeylength the number of a new key string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_rename(k2h_h handle, const unsigned char* pkey, size_t keylength, const
  // unsigned char* pnewkey, size_t newkeylength);
  boolean k2h_rename(long handle, String pkey, long keylength, String pnewkey, long newkeylength);

  /**
   * Renames the old key the new key using text format.
   *
   * @param handle a k2hash data handle
   * @param pkey a key string
   * @param pnewkey a new key string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  boolean k2h_rename_str(long handle, String pkey, String pnewkey);

  /* -- K2hashQueue methods -- */

  /**
   * Creates a queue handle of FIFO or LIFO.
   *
   * @param handle a k2hash data handle
   * @param isFifo the flag whether the data comes first goes out first
   * @return a queue handle
   */
  // extern k2h_q_h k2h_q_handle(k2h_h handle, bool is_fifo);
  long k2h_q_handle(long handle, boolean isFifo);

  /**
   * Creates a queue handle, which contains the prefix string.
   *
   * @param handle a k2hash data handle
   * @param isFifo the flag whether the data comes first goes out first
   * @param pref a prefix string
   * @param preflen the number of a prefix string
   * @return a queue handle
   */
  // extern k2h_q_h k2h_q_handle_prefix(k2h_h handle, bool is_fifo, const unsigned char* pref,
  // size_t preflen);
  long k2h_q_handle_prefix(long handle, boolean isFifo, String pref, int preflen);

  /**
   * Creates a queue handle, which contains the prefix string using text format.
   *
   * @param handle a k2hash data handle
   * @param isFifo the flag whether the data comes first goes out first
   * @param pref a prefix string
   * @return a queue handle
   */
  // extern k2h_q_h k2h_q_handle_str_prefix(k2h_h handle, bool is_fifo, const char* pref);
  long k2h_q_handle_str_prefix(long handle, boolean isFifo, String pref);

  /**
   * Releases the queue handle.
   *
   * @param qhandle a queue handle
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_free(k2h_q_h qhandle);
  boolean k2h_q_free(long qhandle);

  /**
   * Removes all items of the key queue.
   *
   * @param qhandle a queue handle
   * @return <code>true</code> if empty <code>false</code> otherwise
   */
  // extern bool k2h_q_empty(k2h_q_h qhandle);
  boolean k2h_q_empty(long qhandle);

  /**
   * Counts the number of items in the queue.
   *
   * @param qhandle a queue handle
   * @return the number of data in this queue
   */
  // extern int k2h_q_count(k2h_q_h qhandle);
  int k2h_q_count(long qhandle);

  /**
   * Retrives an item from the queue.
   *
   * @param qhandle a queue handle
   * @param ppdata data in this queue
   * @param pdatalen the length of data in this queue
   * @param pos starting position to read in this queue
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_read(k2h_q_h qhandle, unsigned char** ppdata, size_t* pdatalen, int pos);
  boolean k2h_q_read(long qhandle, PointerByReference ppdata, IntByReference pdatalen, int pos);

  /**
   * Retrives an item from the queue.
   *
   * @param qhandle a queue handle
   * @param ppdata data in this queue
   * @param pdatalen the length of data in this queue
   * @param pos starting position to read in this queue
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_read_wp(k2h_q_h qhandle, unsigned char** ppdata, size_t* pdatalen, int pos,
  // const char* encpass);
  boolean k2h_q_read_wp(
      long qhandle, PointerByReference ppdata, IntByReference pdatalen, int pos, String encpass);

  /**
   * Adds an item to the tail of the queue.
   *
   * @param qhandle a queue handle
   * @param bydata a queue string
   * @param datalen the length of a queue string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_push(k2h_q_h qhandle, const unsigned char* bydata, size_t datalen);
  boolean k2h_q_push(long qhandle, String bydata, int datalen);

  /**
   * Retrieves an item from the front of the queue and removes it.
   *
   * @param qhandle a queue handle
   * @param ppdata a read data string
   * @param pdatalen the length of a read data string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_pop(k2h_q_h qhandle, unsigned char** ppdata, size_t* pdatalen);
  boolean k2h_q_pop(long qhandle, PointerByReference ppdata, IntByReference pdatalen);

  /**
   * Removes an item from the front of the queue.
   *
   * @param qhandle a queue handle
   * @param count the number of data to be removed
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_remove(k2h_q_h qhandle, int count);
  boolean k2h_q_remove(long qhandle, int count);

  /**
   * Adds an item to the tail of the queue.
   *
   * @param qhandle a queue handle
   * @param bydata a queue string
   * @param datalen the length of a queue string
   * @param attrspck a pointer to an array of a pointer of an attribute pack structure
   * @param attrspckcnt the number of attribute structures
   * @param encpass a passphrase string
   * @param expirationDuration a duration in seconds to expire the data
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_push_wa(k2h_q_h qhandle, const unsigned char* bydata, size_t datalen, const
  // PK2HATTRPCK pattrspck, int attrspckcnt, const char* encpass, const time_t* expire);
  boolean k2h_q_push_wa(
      long qhandle,
      String bydata,
      int datalen,
      K2hashAttrPack[] attrspck,
      int attrspckcnt,
      String encpass,
      LongByReference expirationDuration);

  /**
   * Retrives an item from the front of the queue.
   *
   * @param qhandle a queue handle
   * @param ppdata a pointer to a queue string
   * @param pdatalen the length of a pointer of a queue string
   * @param ppattrspck a pointer to an array of a pointer of an attribute pack structure
   * @param pattrspckcnt the number of attribute structures
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_pop_wa(k2h_q_h qhandle, unsigned char** ppdata, size_t* pdatalen,
  // PK2HATTRPCK* ppattrspck, int* pattrspckcnt, const char* encpass);
  boolean k2h_q_pop_wa(
      long qhandle,
      PointerByReference ppdata,
      IntByReference pdatalen,
      PointerByReference ppattrspck,
      IntByReference pattrspckcnt,
      String encpass);

  /**
   * Retrieves an item from the front of the queue and removes it.
   *
   * @param qhandle a queue handle
   * @param ppdata a pointer to a queue string
   * @param pdatalen the length of a pointer of a queue string
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_pop_wp(k2h_q_h qhandle, unsigned char** ppdata, size_t* pdatalen, const char*
  // encpass);
  boolean k2h_q_pop_wp(
      long qhandle, PointerByReference ppdata, IntByReference pdatalen, String encpass);

  /**
   * Removes data in a queue.
   *
   * @param qhandle a queue handle
   * @param count the number of data to be removed
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_remove_wp(k2h_q_h qhandle, int count, const char* encpass);
  boolean k2h_q_remove_wp(long qhandle, int count, String encpass);

  /**
   * Prints data in a queue.
   *
   * @param qhandle a queue handle
   * @param stream the file pointer to which prints
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_q_dump(k2h_q_h qhandle, FILE* stream);
  boolean k2h_q_dump(long qhandle, Pointer stream);

  /* -- K2hashKeyQueue methods -- */

  /**
   * Creates a key queue handle of FIFO or LIFO.
   *
   * @param handle a k2hash data handle
   * @param isFifo the flag whether the data comes first goes out first
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern k2h_keyq_h k2h_keyq_handle(k2h_h handle, bool is_fifo);
  long k2h_keyq_handle(long handle, boolean isFifo);

  /**
   * Creates a key queue handle of FIFO or LIFO, which contains the prefix string.
   *
   * @param handle a k2hash data handle
   * @param isFifo the flag whether the data comes first goes out first
   * @param pref a prefix string
   * @param preflen the number of a prefix string
   * @return a key queue handle
   */
  // extern k2h_keyq_h k2h_keyq_handle_prefix(k2h_h handle, bool is_fifo, const unsigned char* pref,
  // size_t preflen);
  long k2h_keyq_handle_prefix(long handle, boolean isFifo, String pref, int preflen);

  /**
   * Creates a key queue handle, which contains the prefix string.
   *
   * @param handle a k2hash data handle
   * @param isFifo the flag whether the data comes first goes out first
   * @param pref a prefix string
   * @return a key queue handle
   */
  // extern k2h_keyq_h k2h_keyq_handle_str_prefix(k2h_h handle, bool is_fifo, const char* pref);
  long k2h_keyq_handle_str_prefix(long handle, boolean isFifo, String pref);

  /**
   * Releases the key queue handle.
   *
   * @param keyqhandle a key queue handle
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_free(k2h_keyq_h keyqhandle);
  boolean k2h_keyq_free(long keyqhandle);

  /**
   * Removes all items of the key queue.
   *
   * @param keyqhandle a key queue handle
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_empty(k2h_keyq_h keyqhandle);
  boolean k2h_keyq_empty(long keyqhandle);

  /**
   * Counts the number of keys in the key queue.
   *
   * @param keyqhandle a key queue handle
   * @return the number of data in this queue
   */
  // extern int k2h_keyq_count(k2h_keyq_h keyqhandle);
  int k2h_keyq_count(long keyqhandle);

  /**
   * Retrieves an item from the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param ppdata a pointer to a queue string
   * @param pdatalen the length of a pointer of a queue string
   * @param pos starting position to read in this queue
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_read(k2h_keyq_h keyqhandle, unsigned char** ppdata, size_t* pdatalen, int
  // pos);
  boolean k2h_keyq_read(
      long keyqhandle, PointerByReference ppdata, IntByReference pdatalen, int pos);

  /**
   * Retrieves an item from the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param ppkey a key string
   * @param pkeylen the number of a key string
   * @param ppval a value string
   * @param pvallen the number of a value string
   * @param pos starting position to read in this queue
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_read_keyval(k2h_keyq_h keyqhandle, unsigned char** ppkey, size_t* pkeylen,
  // unsigned char** ppval, size_t* pvallen, int pos);
  boolean k2h_keyq_read_keyval(
      long keyqhandle,
      PointerByReference ppkey,
      IntByReference pkeylen,
      PointerByReference ppval,
      IntByReference pvallen,
      int pos);

  /**
   * Adds an item to the tail of the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param bykey a key string
   * @param keylen the number of a key string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_push(k2h_keyq_h keyqhandle, const unsigned char* bykey, size_t keylen);
  boolean k2h_keyq_push(long keyqhandle, String bykey, int keylen);

  /**
   * Adds an item to the tail of the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param bykey a key string
   * @param keylen the number of a key string
   * @param byval a value string
   * @param vallen the number of a value string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_push_keyval(k2h_keyq_h keyqhandle, const unsigned char* bykey, size_t
  // keylen, const unsigned char* byval, size_t vallen);
  boolean k2h_keyq_push_keyval(long keyqhandle, String bykey, int keylen, String byval, int vallen);

  /**
   * Retrieves an item from the front of the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param ppval a pointer to a value string
   * @param pvallen the length of a value string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_pop(k2h_keyq_h keyqhandle, unsigned char** ppval, size_t* pvallen);
  boolean k2h_keyq_pop(long keyqhandle, PointerByReference ppval, IntByReference pvallen);

  /**
   * Retrieves an item from the front of the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param ppkey a key string
   * @param pkeylen the number of a key string
   * @param ppval a pointer of a value string
   * @param pvallen the number of a value string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_pop_keyval(k2h_keyq_h keyqhandle, unsigned char** ppkey, size_t* pkeylen,
  boolean k2h_keyq_pop_keyval(
      int keyqhandle,
      PointerByReference ppkey,
      IntByReference pkeylen,
      PointerByReference ppval,
      IntByReference pvallen);

  /**
   * Retrieves an item from the front of the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param ppkey a key string
   * @param pkeylen the number of a key string
   * @param ppval a pointer to a value string
   * @param pvallen the number of a value string
   * @param pass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_pop_keyval_wp(k2h_keyq_h keyqhandle, unsigned char** ppkey, size_t*
  // pkeylen, unsigned char** ppval, size_t* pvallen, const char* encpass);
  boolean k2h_keyq_pop_keyval_wp(
      int keyqhandle,
      PointerByReference ppkey,
      IntByReference pkeylen,
      PointerByReference ppval,
      IntByReference pvallen,
      String pass);

  /**
   * Removes an item from the front of the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param count the number of data to be removed
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_remove(k2h_keyq_h keyqhandle, int count);
  boolean k2h_keyq_remove(long keyqhandle, int count);

  /**
   * Retrieves an item from the key queue. The item will not be removed from the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param ppdata a pointer to a queue string
   * @param pdatalen the length of a pointer of a queue string
   * @param pos starting position to read in this queue
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_read_wp(k2h_keyq_h keyqhandle, unsigned char** ppdata, size_t* pdatalen,
  // int pos, const char* encpass);
  boolean k2h_keyq_read_wp(
      long keyqhandle, PointerByReference ppdata, IntByReference pdatalen, int pos, String encpass);

  /**
   * Retrieves an item from the key queue. The item will not be removed from the key queue.
   *
   * @param keyqhandle a key queue handle
   * @param ppkey a key string
   * @param pkeylen the length of a key string
   * @param ppval a value string
   * @param pvallen the length of a value string
   * @param pos starting position to read in this queue
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_read_keyval_wp(k2h_keyq_h keyqhandle, unsigned char** ppkey, size_t*
  // pkeylen, unsigned char** ppval, size_t* pvallen, int pos, const char* encpass);
  boolean k2h_keyq_read_keyval_wp(
      long keyqhandle,
      PointerByReference ppkey,
      IntByReference pkeylen,
      PointerByReference ppval,
      IntByReference pvallen,
      int pos,
      String encpass);

  /**
   * Adds a key to the tail of the queue.
   *
   * @param keyqhandle a key queue handle
   * @param bykey a key string
   * @param keylen the number of a key string
   * @param encpass a passphrase string
   * @param expirationDuration a duration in seconds to expire the data
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_push_wa(k2h_keyq_h keyqhandle, const unsigned char* bykey, size_t keylen,
  // const char* encpass, const time_t* expire);
  boolean k2h_keyq_push_wa(
      long keyqhandle,
      String bykey,
      int keylen,
      String encpass,
      LongByReference expirationDuration);

  /**
   * Adds a key and a value to the tail of the queue.
   *
   * @param keyqhandle a key queue handle
   * @param bykey a key string
   * @param keylen the number of a key string
   * @param byval a value string
   * @param vallen the number of a value string
   * @param encpass a passphrase string
   * @param expirationDuration a duration in seconds to expire the data
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_push_keyval_wa(k2h_keyq_h keyqhandle, const unsigned char* bykey, size_t
  // keylen, const unsigned char* byval, size_t vallen, const char* encpass, const time_t* expire);
  boolean k2h_keyq_push_keyval_wa(
      long keyqhandle,
      String bykey,
      int keylen,
      String byval,
      int vallen,
      String encpass,
      LongByReference expirationDuration);

  /**
   * Retrieve and removes an value from the front of the queue.
   *
   * @param keyqhandle a key queue handle
   * @param ppval a pointer to a key string
   * @param pvallen the number of a key string
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_pop_wp(k2h_keyq_h keyqhandle, unsigned char** ppval, size_t* pvallen,
  // const char* encpass);
  boolean k2h_keyq_pop_wp(
      long keyqhandle, PointerByReference ppval, IntByReference pvallen, String encpass);

  /**
   * Retrieves an item from the front of the key queue and removes it.
   *
   * @param keyqhandle a key queue handle
   * @param ppkey a key string
   * @param pkeylen the length of a key string
   * @param ppval a value string
   * @param pvallen the length of a value string
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_pop_keyval_wp(k2h_keyq_h keyqhandle, unsigned char** ppkey, size_t*
  // pkeylen, unsigned char** ppval, size_t* pvallen, const char* encpass);
  boolean k2h_keyq_pop_keyval_wp(
      long keyqhandle,
      PointerByReference ppkey,
      IntByReference pkeylen,
      PointerByReference ppval,
      IntByReference pvallen,
      String encpass);

  /**
   * Removes data in a key queue.
   *
   * @param keyqhandle a key queue handle
   * @param count the number of data to be removed
   * @param encpass a passphrase string
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_remove_wp(k2h_keyq_h keyqhandle, int count, const char* encpass);
  boolean k2h_keyq_remove_wp(long keyqhandle, int count, String encpass);

  /**
   * Prints data in a key queue.
   *
   * @param keyqhandle a key queue handle
   * @param stream the file pointer to which prints
   * @return <code>true</code> if success <code>false</code> otherwise
   */
  // extern bool k2h_keyq_dump(k2h_keyq_h keyqhandle, FILE* stream);
  boolean k2h_keyq_dump(long keyqhandle, Pointer stream);
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
