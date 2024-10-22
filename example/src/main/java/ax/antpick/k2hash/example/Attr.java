/*
 * K2hash Java Driver
 *
 * Copyright 2023 Yahoo Japan Corporation.
 *
 * K2hash is a key-value store base library.
 * K2hash Java Driver is the one for Java.
 *
 * See the LICENSE file with this source code for
 * the details of the MIT License.
 *
 * AUTHOR:   Hirotaka Wakabayashi
 * CREATE:   Fri, 30 Sep 2023
 * REVISION:
 *
 */
package ax.antpick.k2hash.example;

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// javac -cp \
// "logback-classic-1.3.11.jar:logback-core-1.3.11.jar:slf4j-api-2.0.7.jar:jna-5.13.0.jar:jna-platform-5.13.0.jar" \
// ax/antpick/k2hash/example/Attr.java
//

public class Attr {
  static final Logger logger = LoggerFactory.getLogger(Attr.class);
  public static final int DEFAULT_MASK_BITS = 8;
  public static final int DEFAULT_CMASK_BITS = 4;
  public static final int DEFAULT_MAX_ELEMENT = 32;
  public static final int DEFAULT_PAGE_SIZE = 4096;

  public static class K2hashAttrPack extends Structure {
    /** Fields of K2hashAttrPack. */
    public static final List<String> FIELDS =
        createFieldsOrder("pkey", "keylength", "pval", "vallength");

    /** An attribute key. */
    public String pkey;
    // public byte[] pkey;
    /** The length of an attribute key. */
    public int keylength;
    /** An attribute value. */
    public String pval;
    // public byte[] pval;
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

  public interface CLibrary extends Library {
    // prototypes
    void free(Pointer ptr);
  }

  public interface K2hashLibrary extends Library {
    // prototypes
    long k2h_open_mem(int maskbitcnt, int cmaskbitcnt, int maxelementcnt, long pagesize);

    boolean k2h_close(long handle);

    boolean k2h_set_str_value_wa(
        long handle, String pkey, String pval, String pass, LongByReference expirationDuration);

    Pointer k2h_get_str_direct_value_wp(long handle, String pkey, String pass);

    boolean k2h_add_str_attr(long handle, String pkey, String attrName, String attrVal);

    // extern PK2HATTRPCK k2h_get_str_direct_attrs(k2h_h handle, const char* pkey, int*
    // pattrspckcnt);
    K2hashAttrPack k2h_get_str_direct_attrs(long handle, String pkey, IntByReference pattrspckcnt);

    // extern bool k2h_free_attrpack(PK2HATTRPCK pattrs, int attrcnt);
    boolean k2h_free_attrpack(K2hashAttrPack pattrs, int attrcnt);
  }

  public static class time_t extends IntegerType {
    public time_t(long value) {
      super(Native.LONG_SIZE, value);
    }
  }

  public static void main(String[] args) {
    System.setProperty("jna.dump_memory", "true");
    CLibrary C_INSTANCE = (CLibrary) Native.synchronizedLibrary(Native.load("c", CLibrary.class));
    K2hashLibrary INSTANCE =
        (K2hashLibrary) Native.synchronizedLibrary(Native.load("k2hash", K2hashLibrary.class));
    long handle =
        INSTANCE.k2h_open_mem(
            DEFAULT_MASK_BITS, DEFAULT_CMASK_BITS, DEFAULT_MAX_ELEMENT, DEFAULT_PAGE_SIZE);
    INSTANCE.k2h_set_str_value_wa(handle, "testkey", "testval", "", null);
    Pointer val = INSTANCE.k2h_get_str_direct_value_wp(handle, "testkey", "");
    logger.debug("val {}", val.getString(0));
    C_INSTANCE.free(val);

    String attrName = "testkey_attr";
    String attrVal = "testkey_attr_val";
    boolean isSuccess = INSTANCE.k2h_add_str_attr(handle, "testkey", attrName, attrVal);
    if (!isSuccess) {
      logger.error("INSTANCE.k2h_add_attr returns false");
    }

    isSuccess = INSTANCE.k2h_add_str_attr(handle, "testkey", "testkey_attr2", "testkey_attr2_val");
    if (!isSuccess) {
      logger.error("INSTANCE.k2h_add_attr returns false");
    }

    IntByReference iref = new IntByReference();
    K2hashAttrPack pp = INSTANCE.k2h_get_str_direct_attrs(handle, "testkey", iref);
    pp.setAutoRead(false);
    logger.debug("pp {}", pp);

    if (pp == null || iref.getValue() == 0) {
      logger.warn("no attribute exists");
      return;
    }
    K2hashAttrPack[] attrs = (K2hashAttrPack[]) pp.toArray(iref.getValue());
    logger.debug("iref.getValue() {}", iref.getValue());

    Map<String, String> map = new HashMap<>();
    Stream<K2hashAttrPack> stream = Stream.of(attrs);
    stream.forEach(
        attr -> {
          logger.debug("{} {} {} {}", attr.pkey, attr.keylength, attr.pval, attr.vallength);
          logger.debug("attr.toString() {} ", attr.toString());
          logger.debug("attr.getPointer() {} ", attr.getPointer());
          logger.debug("attr.pkey.toString() {} ", attr.pkey);
          logger.debug("attr.pval.toString() {} ", attr.pval);
          map.put(attr.pkey, attr.pval);
        });
    logger.debug("K2hashAttrPack address={} size={}", pp.toString(), pp.size());

    // INSTANCE.k2h_free_attrpack(pp, iref.getValue());
    INSTANCE.k2h_close(handle);
  }
}
//
// Local variables:
// tab-width: 2
// c-basic-offset: 2
// End:
// vim600: noexpandtab sw=2 ts=2 fdm=marker
// vim<600: noexpandtab sw=2 ts=2
//
