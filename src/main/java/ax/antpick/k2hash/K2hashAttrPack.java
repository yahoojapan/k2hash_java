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
import com.sun.jna.Structure.*;
import com.sun.jna.ptr.*;
import java.util.List;

//
// C API structure is as followings.
//
// typedef struct k2h_attr_pack{
//  unsigned char*  pkey;
//  size_t                  keylength;
//  unsigned char*  pval;
//  size_t                  vallength;
// }K2HATTRPCK, *PK2HATTRPCK;
//

/**
 * Information about an attribute of a key. K2hashAttrPack in Java corresponds to the K2HATTRPCK
 * structure in C. Note: K2hashAttrPack must be a public class because com.sun.jna.Library has
 * access to it.
 *
 * <p><b>Usage Examples.</b> Suppose you want to get attributes of a key. You could write this as:
 *
 * <pre>{@code
 *  public Map<String, String> getAttributes(String path, String key) {
 *   if (path == null || path.isEmpty() || key == null || key.isEmpty()) {
 *     throw new IllegalArgumentException("invalid key. key must contains any lettrs");
 *   }
 *   K2hashLibrary INSTANCE = (K2hashLibrary)
 *       Native.synchronizedLibrary(Native.loadLibrary("k2hash", K2hashLibrary.class));
 *   assert(INSTANCE != null);
 *
 *   long handle = K2H_INVALID_HANDLE;
 *   try {
 *     handle = INSTANCE.k2h_open(path,
 *       DEFAULT_IS_READONLY, DEFAULT_IS_REMOVE_FILE, DEFAULT_IS_FULLMAP, DEFAULT_MASK_BITS,
 *       DEFAULT_CMASK_BITS, DEFAULT_MAX_ELEMENT, DEFAULT_PAGE_SIZE);
 *
 *     IntByReference iref = new IntByReference();
 *     // k2h_get_str_direct_attrs returns a pointer of K2hashAttrPack.
 *     K2hashAttrPack pp = INSTANCE.k2h_get_str_direct_attrs(handle, key, iref);
 *     if (pp == null || iref.getValue() == 0) {
 *       System.err.println("no attribute exists");
 *       INSTANCE.k2h_close(handle);
 *       return null;
 *     }
 *     K2hashAttrPack[] attrs = (K2hashAttrPack[]) pp.toArray(iref.getValue());
 *     // Note: We must copy data because attrs must be free later.
 *     K2hashAttrPack[] newAttrs = Arrays.copyOfRange(attrs, 0, attrs.length);
 *
 *     Map<String, String> map = new HashMap<>();
 *     Stream<K2hashAttrPack> stream = Stream.of(newAttrs);
 *     stream.forEach(
 *       attr -> {
 *         map.put(attr.pkey, attr.pval);
 *       });
 *     INSTANCE.k2h_free_attrpack(attrs, iref.getValue());
 *     INSTANCE.k2h_close(handle);
 *     return map;
 *   } catch (IOException e) {
 *     if(handle != K2H_INVALID_HANDLE) {
 *       INSTANCE.k2h_close(handle);
 *     }
 *   }
 *   return null;
 * }
 * }</pre>
 */
public class K2hashAttrPack extends Structure {
  /** Fields of K2hashAttrPack. */
  public static final List<String> FIELDS =
      createFieldsOrder("pkey", "keylength", "pval", "vallength");

  /** An attribute key. */
  public String pkey;
  /** The length of an attribute key. */
  public int keylength;
  /** An attribute value. */
  public String pval;
  /** The length of an attribute value. */
  public int vallength;

  /** Constructor of a K2hash attribute. */
  public K2hashAttrPack() {}

  /**
   * Returns fields in the same order with the K2hash C API K2hashAttrPack structure.
   *
   * @return fields
   */
  @Override
  protected List<String> getFieldOrder() {
    return FIELDS;
  }
}
