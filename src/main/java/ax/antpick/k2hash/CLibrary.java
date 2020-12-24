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

/**
 * This JNA interface provides functions in the C library.
 *
 * <p><b>Usage Examples.</b> Suppose you want to set the K2HDBGMODE enviroment "INFO" and print it,
 * You could write this as:
 *
 * <pre>{@code
 * package ax.antpick;
 *
 * import com.sun.jna.*;
 * import com.sun.jna.ptr.*;
 *
 * public class App {
 *   public static void main(String[] args) {
 *     CLibrary INSTANCE =
 *         (CLibrary) Native.synchronizedLibrary(Native.loadLibrary("c", CLibrary.class));
 *     INSTANCE.setenv("K2HDBGMODE", "INFO", 1);
 *     System.out.println(INSTANCE.getenv("K2HDBGMODE"));
 *   }
 * }
 * }</pre>
 */
public interface CLibrary extends Library {
  /**
   * Frees the memory space pointed to by ptr
   *
   * @param ptr a pointer to be free
   */
  // void free(void *ptr);
  void free(Pointer ptr);

  /**
   * Adds an enviroments
   *
   * @param name an environment string
   * @param value the value
   * @param overwrite if <code>name</code> already exists in the environment, then its <code>value
   *     </code> is changed to <code>value</code> if <code>overwrite</code> is nonzero. If <code>
   *     overwrite</code> is zero, then the <code>value</code> of <code>name</code> is not changed.
   * @return returns zero on success, or -1 on error, with errno set to indicate the cause of the
   *     error.
   */
  // int setenv(const char *name, const char *value, int overwrite);
  int setenv(String name, String value, int overwrite);

  /**
   * Deletes an variable name from the environment.
   *
   * @param name an environment string
   * @return returns zero on success, or -1 on error, with errno set to indicate the cause of the
   *     error.
   */
  // int unsetenv(const char *name);
  int unsetenv(String name);

  /**
   * Gets the value of an enviroment.
   *
   * @param name an environment string
   * @return the value of an enviroment.
   */
  // char *getenv(const char *name);
  String getenv(String name);
}

//
// VIM modelines
//
// vim:set ts=4 fenc=utf-8:
//
