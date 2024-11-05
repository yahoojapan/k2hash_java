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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
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
public class K2hashFileTest {
  private static final Logger logger = LoggerFactory.getLogger(K2hashFileTest.class);
  private static final String FILEDB = "test.k2h";

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

  /** K2hash Constructor */
  @Test
  public void testOfArg1() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash Constructor */
  @Test
  public void testOfArg2() {
    try (K2hash db = K2hash.of(FILEDB, K2hash.OPEN_MODE.RDWR)) {
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash create */
  @Test
  public void testCreateArg1() {
    boolean isTrue = K2hash.create(FILEDB);
    assertTrue(isTrue);
    File fileDb = new File(FILEDB);
    assertTrue(fileDb.exists());
  }

  /** K2hash create */
  @Test
  public void testCreateArg5() {
    boolean isTrue = K2hash.create(FILEDB, 4, 2, 16, 2048);
    assertTrue(isTrue);
    File fileDb = new File(FILEDB);
    assertTrue(fileDb.exists());
  }

  /** K2hash start transaction */
  @Test
  public void testBeginTxArg1() {
    File fileDb = new File("/tmp/testTxArg1.log");
    if (fileDb.exists()) {
      fileDb.delete();
    }
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.beginTx("/tmp/testTxArg1.log"));
      assertTrue(db.setValue("key", "val"));
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.out.println("InterruptedException " + e.getMessage());
        assertFalse(true);
      }
      assertTrue(fileDb.length() > 0);
      assertTrue(db.getTxFileFd() != -1);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash start transaction */
  @Test
  public void testBeginTxArg5() {
    File fileDb = new File("/tmp/testTxArg5.log");
    if (fileDb.exists()) {
      fileDb.delete();
    }
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(
          db.beginTx("/tmp/testTxArg5.log", "testTxArg5", "testTxArg5", 8)); // 8 secs to expire
      assertTrue(db.setValue("key", "val"));
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.out.println("InterruptedException " + e.getMessage());
        assertFalse(true);
      }
      assertTrue(fileDb.length() > 0);
      assertTrue(db.getTxFileFd() != -1);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash stop transaction */
  @Test
  public void testStopTx() {
    File fileDb = new File("/tmp/testStopTx.log");
    if (fileDb.exists()) {
      fileDb.delete();
    }
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.beginTx("/tmp/testTxArg1.log"));
      assertTrue(db.stopTx());
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getTxPoolSize */
  @Test
  public void testGetTxPoolSize() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertEquals(0, db.getTxPoolSize());
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setTxPoolSize */
  @Test
  public void testSetTxPoolSize() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setTxPoolSize(1));
      assertEquals(1, db.getTxPoolSize());
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash dumpToFile */
  @Test
  public void testDumpToFileArg1() {
    File fileDb = new File("/tmp/testDumpToFileArg1.log");
    if (fileDb.exists()) {
      fileDb.delete();
    }
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testDumpToFileArg1", "val"));
      assertTrue(db.dumpToFile("/tmp/testDumpToFileArg1.log"));
      assertTrue(fileDb.length() != 0);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash dumpToFile */
  @Test
  public void testDumpToFileArg2() {
    File fileDb = new File("/tmp/testDumpToFileArg2.log");
    if (fileDb.exists()) {
      fileDb.delete();
    }
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testDumpToFileArg2", "val"));
      assertTrue(db.dumpToFile("/tmp/testDumpToFileArg2.log", false));
      assertTrue(fileDb.length() != 0);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash loadFromFile */
  @Test
  public void testLoadFromFileArg1() {
    File fileDb = new File("/tmp/testLoadFromFileArg1.log");
    if (fileDb.exists()) {
      fileDb.delete();
    }
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testLoadFromFileArg1", "val"));
      assertTrue(db.dumpToFile("/tmp/testLoadFromFileArg1.log"));
      assertTrue(fileDb.length() != 0);
      assertTrue(db.remove("testLoadFromFileArg1"));
      assertTrue(db.loadFromFile("/tmp/testLoadFromFileArg1.log"));
      assertTrue(db.getValue("testLoadFromFileArg1") != null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash loadFromFile */
  @Test
  public void testLoadFromFileArg2() {
    File fileDb = new File("/tmp/testLoadFromFileArg2.log");
    if (fileDb.exists()) {
      fileDb.delete();
    }
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testLoadFromFileArg2", "val"));
      assertTrue(db.dumpToFile("/tmp/testLoadFromFileArg2.log"));
      assertTrue(fileDb.length() != 0);
      assertTrue(db.remove("testLoadFromFileArg2"));
      assertTrue(db.loadFromFile("/tmp/testLoadFromFileArg2.log", false));
      assertTrue(db.getValue("testLoadFromFileArg2") != null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash enableMtime */
  @Test
  public void testEnableMtime() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.enableMtime(true));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash enableEncrypt */
  @Test
  public void testEnableEncryptFalse() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.enableEncryption(false));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash enableEncrypt */
  @Test
  public void testEnableEncryptTrue() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setDefaultEncryptionPassword("secretstring"));
      assertTrue(db.enableEncryption(true));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setEncryptionPassword */
  @Test
  public void testSetEncryptionPassword() {
    File file = new File("password.txt");
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      if (!file.exists()) {
        file.createNewFile();
      }
      assertTrue(db.setEncryptionPasswordFile("password.txt"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setEncryptionPassword */
  @Test
  public void testSetEncryptionPasswordException() {
    File file = new File("password.txt");
    if (file.exists()) {
      file.delete();
    }
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      db.setEncryptionPasswordFile("password.txt");
      assertTrue(false);
    } catch (IOException e) {
      assertTrue(true);
    }
  }

  /** K2hash addDecryptionPassword */
  @Test
  public void testAddDecryptionPassword() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.addDecryptionPassword("secretstring"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash enableHistory */
  @Test
  public void testEnableHistory() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.enableHistory(true));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setExpirationDuration */
  @Test
  public void testSetExpireDuration() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setExpirationDuration(3, TimeUnit.SECONDS));
      assertTrue(db.setValue("testSetExpireDuration", "val"));
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.out.println("InterruptedException " + e.getMessage());
        assertFalse(true);
      }
      assertTrue(db.getValue("testSetExpireDuration") == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /**
   * K2hash printAttributePlugins
   *
   * <p>NOTE: This test is disabled because k2hash library corrupted the channel used by the plugin
   * in order to transmit events with test status back to Maven process.
   */
  @Test
  @Disabled
  public void testPrintAttributePlugins() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      db.printAttributePlugins();
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /**
   * K2hash printAttributes
   *
   * <p>NOTE: This test is disabled because k2hash library corrupted the channel used by the plugin
   * in order to transmit events with test status back to Maven process.
   */
  @Test
  @Disabled
  public void testPrintAttributes() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      db.printAttributes();
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getValue */
  @Test
  public void testGetValueArg1() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      String val = db.getValue("testGetValueArg1");
      assertTrue(val == null);
      assertTrue(db.setValue("testGetValueArg1", "val"));
      assertTrue(db.getValue("testGetValueArg1").equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getValue */
  @Test
  public void testGetValueArg2() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      String val = db.getValue("testGetValueArg2", "pass");
      assertTrue(val == null);
      assertTrue(db.setValue("testGetValueArg2", "value", "pass", 0, TimeUnit.SECONDS));
      assertTrue(db.getValue("testGetValueArg2", "pass").equals("value"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getSubkeys */
  @Test
  public void testGetSubkeysArg1() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.getSubkeys("testGetSubkeysArg1") == null);
      assertTrue(db.setSubkey("testGetSubkeysArg1", "testGetSubkeysArg1_subkey"));
      List<String> list = db.getSubkeys("testGetSubkeysArg1");
      assertTrue(list != null);
      assertTrue(list.get(0).equals("testGetSubkeysArg1_subkey"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setSubkey */
  @Test
  public void testSetSubkey() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setSubkey("testSetSubkey", "testSetSubkey_subkey"));
      List<String> list = db.getSubkeys("testSetSubkey");
      assertTrue(list != null);
      assertTrue(list.get(0).equals("testSetSubkey_subkey"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setSubkeys */
  @Test
  public void testSetSubkeys() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      String[] subkeys = {"testSetSubkeys_subkey1", "testSetSubkeys_subkey2"};
      assertTrue(db.setSubkeys("testSetSubkeys", subkeys));
      List<String> list = db.getSubkeys("testSetSubkeys");
      assertTrue(list != null);
      assertTrue(list.size() == 2);
      assertTrue(list.get(0).equals("testSetSubkeys_subkey1"));
      assertTrue(list.get(1).equals("testSetSubkeys_subkey2"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash addSubkey */
  @Test
  public void testAddSubkey() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.addSubkey("testAddSubkey", "testAddSubkey_subkey"));
      List<String> list = db.getSubkeys("testAddSubkey");
      assertTrue(list != null);
      assertTrue(list.size() == 1);
      assertTrue(list.get(0).equals("testAddSubkey_subkey"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash addSubkeys */
  @Test
  public void testAddSubkeys() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      String[] subkeys = {"testAddSubkeys_subkey1", "testAddSubkeys_subkey2"};
      assertTrue(db.addSubkeys("testAddSubkeys", subkeys));
      List<String> list = db.getSubkeys("testAddSubkeys");
      assertTrue(list != null);
      assertTrue(list.size() == 2);
      assertTrue(list.get(0).equals("testAddSubkeys_subkey1"));
      assertTrue(list.get(1).equals("testAddSubkeys_subkey2"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getAttributes */
  @Test
  public void testGetAttributes() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testGetAttributes", "value"));
      Map<String, String> val = db.getAttributes("testGetAttributes");
      assertTrue(val == null);
      assertTrue(db.setAttribute("testGetAttributes", "testGetAttributes_attrname", "attrval"));
      Map<String, String> val2 = db.getAttributes("testGetAttributes");
      assertTrue(val2.containsKey("testGetAttributes_attrname"));
      assertTrue(val2.containsValue("attrval"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getAttribute */
  @Test
  public void testGetAttribute() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testGetAttribute", "value"));
      String val = db.getAttribute("testGetAttribute", "testGetAttribute_attrname");
      assertTrue(val == null);
      assertTrue(db.setAttribute("testGetAttribute", "testGetAttribute_attrname", "attrval"));
      String attrVal = db.getAttribute("testGetAttribute", "testGetAttribute_attrname");
      assertTrue(attrVal != null);
      assertTrue(attrVal.equals("attrval"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setValue */
  @Test
  public void testSetValueArg2() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testSetValueArg2", "value"));
      assertTrue(db.getValue("testSetValueArg2").equals("value"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setValue */
  @Test
  public void testSetValueArg5() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testSetValueArg5", "value", "password", 3, TimeUnit.SECONDS));
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.out.println("InterruptedException " + e.getMessage());
        assertFalse(true);
      }
      assertTrue(db.getValue("testSetValueArg5") == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setAttribute */
  @Test
  public void testSetAttribute() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testSetAttribute", "value"));
      assertTrue(db.setAttribute("testSetAttribute", "attrname", "attrval"));
      assertTrue(db.getAttribute("testSetAttribute", "attrname").equals("attrval"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash remove */
  @Test
  public void testRemoveArg1() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testRemoveArg1", "val"));
      assertTrue(db.getValue("testRemoveArg1") != null);
      assertTrue(db.remove("testRemoveArg1"));
      assertTrue(db.getValue("testRemoveArg1") == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash remove */
  @Test
  public void testRemoveArg2_1() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      /* 1. setvalue */
      assertTrue(db.setValue("testRemoveArg2_1", "val"));
      assertTrue(db.setValue("testRemoveArg2_1_subkey1", "subval1"));
      assertTrue(db.setValue("testRemoveArg2_1_subkey2", "subval2"));
      String[] subkeys = {"testRemoveArg2_1_subkey1", "testRemoveArg2_1_subkey2"};
      assertTrue(db.addSubkeys("testRemoveArg2_1", subkeys));

      /* 2. remove subkey1 */
      assertTrue(db.remove("testRemoveArg2_1", "testRemoveArg2_1_subkey1"));
      // make sure subkey1 itself is removed whe subkey1 is removed from subkeys
      List<String> list = db.getSubkeys("testRemoveArg2_1"); // TODO core dump here
      assertTrue(list != null);
      assertTrue(list.size() == 1);
      assertTrue(list.get(0).equals("testRemoveArg2_1_subkey2"));
      assertTrue(db.getValue("testRemoveArg2_1") != null);
      assertTrue(db.getValue("testRemoveArg2_1_subkey1") == null);
      assertTrue(db.getValue("testRemoveArg2_1_subkey2") != null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash remove */
  @Test
  public void testRemoveArg2_2() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testRemoveArg2_2", "val"));
      assertTrue(db.setValue("testRemoveArg2_2_subkey1", "subval1"));
      assertTrue(db.setValue("testRemoveArg2_2_subkey2", "subval2"));
      String[] subkeys = {"testRemoveArg2_2_subkey1", "testRemoveArg2_2_subkey2"};
      assertTrue(db.addSubkeys("testRemoveArg2_2", subkeys));
      assertTrue(db.remove("testRemoveArg2_2", true));
      assertTrue(db.getValue("testRemoveArg2_2") == null);
      assertTrue(db.getValue("testRemoveArg2_2_subkey1") == null);
      assertTrue(db.getValue("testRemoveArg2_2_subkey2") == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash rename */
  @Test
  public void testRename() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      assertTrue(db.setValue("testRename", "val"));
      assertTrue(db.rename("testRename", "testRename_newkey"));
      assertTrue(db.getValue("testRename") == null);
      assertTrue(db.getValue("testRename_newkey") != null);
      assertTrue(db.getValue("testRename_newkey").equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /**
   * K2hash printTableStats
   *
   * <p>NOTE: This test is disabled because k2hash library corrupted the channel used by the plugin
   * in order to transmit events with test status back to Maven process.
   */
  @Test
  @Disabled
  public void testPrintTableStatsArg1() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      db.printTableStats(K2hash.STATS_DUMP_LEVEL.HEADER);
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /**
   * K2hash printDataStats
   *
   * <p>NOTE: This test is disabled because k2hash library corrupted the channel used by the plugin
   * in order to transmit events with test status back to Maven process.
   */
  @Test
  @Disabled
  public void testPrintDataStats() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      db.printDataStats();
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /**
   * K2hash version
   *
   * <p>NOTE: This test is disabled because k2hash library corrupted the channel used by the plugin
   * in order to transmit events with test status back to Maven process.
   */
  @Test
  @Disabled
  public void testVersion() {
    try (K2hash db = K2hash.of(K2hash.OPEN_MODE.MEM)) {
      db.version();
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
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
