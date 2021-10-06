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
    try (K2hash db = K2hash.of(FILEDB)) {
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
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.beginTx("/tmp/testTxArg1.log"));
      assertTrue(db.setValue("key", "val"));
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
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(
          db.beginTx("/tmp/testTxArg5.log", "testTxArg5", "testTxArg5", 8)); // 8 secs to expire
      assertTrue(db.setValue("key", "val"));
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
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.beginTx("/tmp/testTxArg1.log"));
      assertTrue(db.stopTx());
      assertTrue(db.setValue("key", "val"));
      assertTrue(fileDb.length() == 0);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getTxPoolSize */
  @Test
  public void testGetTxPoolSize() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertEquals(0, db.getTxPoolSize());
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setTxPoolSize */
  @Test
  public void testSetTxPoolSize() {
    try (K2hash db = K2hash.of(FILEDB)) {
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
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "val"));
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
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "val"));
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
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "val"));
      assertTrue(db.dumpToFile("/tmp/testLoadFromFileArg1.log"));
      assertTrue(fileDb.length() != 0);
      assertTrue(db.remove("key"));
      assertTrue(db.loadFromFile("/tmp/testLoadFromFileArg1.log"));
      assertTrue(db.getValue("key") != null);
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
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "val"));
      assertTrue(db.dumpToFile("/tmp/testLoadFromFileArg2.log"));
      assertTrue(fileDb.length() != 0);
      assertTrue(db.remove("key"));
      assertTrue(db.loadFromFile("/tmp/testLoadFromFileArg2.log", false));
      assertTrue(db.getValue("key") != null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash enableMtime */
  @Test
  public void testEnableMtime() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.enableMtime(true));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash enableEncrypt */
  @Test
  public void testEnableEncryptFalse() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.enableEncryption(false));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash enableEncrypt */
  @Test
  public void testEnableEncryptTrue() {
    try (K2hash db = K2hash.of(FILEDB)) {
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
    try (K2hash db = K2hash.of(FILEDB)) {
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
    try (K2hash db = K2hash.of(FILEDB)) {
      db.setEncryptionPasswordFile("password.txt");
      assertTrue(false);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash addDecryptionPassword */
  @Test
  public void testAddDecryptionPassword() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.addDecryptionPassword("secretstring"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash enableHistory */
  @Test
  public void testEnableHistory() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.enableHistory(true));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setExpirationDuration */
  @Test
  public void testSetExpireDuration() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setExpirationDuration(3, TimeUnit.SECONDS));
      assertTrue(db.setValue("key", "val"));
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.out.println("InterruptedException " + e.getMessage());
        assertFalse(true);
      }
      assertTrue(db.getValue("key") == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash printAttributePlugins */
  @Disabled
  public void testPrintAttributePlugins() {
    try (K2hash db = K2hash.of(FILEDB)) {
      db.printAttributePlugins();
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash printAttributes */
  @Disabled
  public void testPrintAttributes() {
    try (K2hash db = K2hash.of(FILEDB)) {
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
    try (K2hash db = K2hash.of(FILEDB)) {
      String val = db.getValue("key");
      assertTrue(val == null);
      assertTrue(db.setValue("key", "val"));
      assertTrue(db.getValue("key").equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getValue */
  @Test
  public void testGetValueArg2() {
    try (K2hash db = K2hash.of(FILEDB)) {
      String val = db.getValue("key", "pass");
      assertTrue(val == null);
      assertTrue(db.setValue("key", "value", "pass", 0, TimeUnit.SECONDS));
      assertTrue(db.getValue("key", "pass").equals("value"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getSubkeys */
  @Test
  public void testGetSubkeysArg1() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.getSubkeys("key") == null);
      assertTrue(db.setSubkey("key", "subkey"));
      List<String> list = db.getSubkeys("key");
      assertTrue(list != null);
      assertTrue(list.get(0).equals("subkey"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setSubkey */
  @Test
  public void testSetSubkey() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setSubkey("key", "subkey"));
      List<String> list = db.getSubkeys("key");
      assertTrue(list != null);
      assertTrue(list.get(0).equals("subkey"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setSubkeys */
  @Test
  public void testSetSubkeys() {
    try (K2hash db = K2hash.of(FILEDB)) {
      String[] subkeys = {"subkey1", "subkey2"};
      assertTrue(db.setSubkeys("key", subkeys));
      List<String> list = db.getSubkeys("key");
      assertTrue(list != null);
      assertTrue(list.size() == 2);
      assertTrue(list.get(0).equals("subkey1"));
      assertTrue(list.get(1).equals("subkey2"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash addSubkey */
  @Test
  public void testAddSubkey() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.addSubkey("key", "subkey"));
      List<String> list = db.getSubkeys("key");
      assertTrue(list != null);
      assertTrue(list.size() == 1);
      assertTrue(list.get(0).equals("subkey"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash addSubkeys */
  @Test
  public void testAddSubkeys() {
    try (K2hash db = K2hash.of(FILEDB)) {
      String[] subkeys = {"subkey1", "subkey2"};
      assertTrue(db.addSubkeys("key", subkeys));
      List<String> list = db.getSubkeys("key");
      assertTrue(list != null);
      assertTrue(list.size() == 2);
      assertTrue(list.get(0).equals("subkey1"));
      assertTrue(list.get(1).equals("subkey2"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getAttributes */
  @Test
  public void testGetAttributes() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "value"));
      Map<String, String> val = db.getAttributes("key");
      assertTrue(val == null);
      assertTrue(db.setAttribute("key", "attrname", "attrval"));
      val = db.getAttributes("key");
      assertTrue(val != null);
      assertTrue(val.containsKey("attrname"));
      assertTrue(val.containsValue("attrval"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash getAttribute */
  @Test
  public void testGetAttribute() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "value"));
      String val = db.getAttribute("key", "attrname");
      assertTrue(val == null);
      assertTrue(db.setAttribute("key", "attrname", "attrval"));
      String attrVal = db.getAttribute("key", "attrname");
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
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "value"));
      assertTrue(db.getValue("key").equals("value"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setValue */
  @Test
  public void testSetValueArg5() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "value", "password", 3, TimeUnit.SECONDS));
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.out.println("InterruptedException " + e.getMessage());
        assertFalse(true);
      }
      assertTrue(db.getValue("key") == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash setAttribute */
  @Test
  public void testSetAttribute() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "value"));
      assertTrue(db.setAttribute("key", "attrname", "attrval"));
      // assertTrue(db.getAttribute("key", "attrname").equals("attrval"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash remove */
  @Test
  public void testRemoveArg1() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "val"));
      assertTrue(db.getValue("key") != null);
      assertTrue(db.remove("key"));
      assertTrue(db.getValue("key") == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash remove */
  @Test
  public void testRemoveArg2_1() {
    try (K2hash db = K2hash.of(FILEDB)) {
      /* 1. setvalue */
      assertTrue(db.setValue("key", "val"));
      assertTrue(db.setValue("subkey1", "subval1"));
      assertTrue(db.setValue("subkey2", "subval2"));
      String[] subkeys = {"subkey1", "subkey2"};
      assertTrue(db.addSubkeys("key", subkeys));

      /* 2. remove subkey1 */
      assertTrue(db.remove("key", "subkey1"));
      // make sure subkey1 itself is removed whe subkey1 is removed from subkeys
      List<String> list = db.getSubkeys("key");
      assertTrue(list != null);
      assertTrue(list.size() == 1);
      assertTrue(list.get(0).equals("subkey2"));
      assertTrue(db.getValue("key") != null);
      assertTrue(db.getValue("subkey1") == null);
      assertTrue(db.getValue("subkey2") != null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash remove */
  @Test
  public void testRemoveArg2_2() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "val"));
      assertTrue(db.setValue("subkey1", "subval1"));
      assertTrue(db.setValue("subkey2", "subval2"));
      String[] subkeys = {"subkey1", "subkey2"};
      assertTrue(db.addSubkeys("key", subkeys));
      assertTrue(db.remove("key", true));
      assertTrue(db.getValue("key") == null);
      assertTrue(db.getValue("subkey1") == null);
      assertTrue(db.getValue("subkey2") == null);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash rename */
  @Test
  public void testRename() {
    try (K2hash db = K2hash.of(FILEDB)) {
      assertTrue(db.setValue("key", "val"));
      assertTrue(db.rename("key", "newkey"));
      assertTrue(db.getValue("key") == null);
      assertTrue(db.getValue("newkey") != null);
      assertTrue(db.getValue("newkey").equals("val"));
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash printTableStats */
  @Disabled
  @Test
  public void testPrintTableStatsArg1() {
    try (K2hash db = K2hash.of(FILEDB)) {
      db.printTableStats(K2hash.STATS_DUMP_LEVEL.HEADER);
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash printDataStats */
  @Disabled
  @Test
  public void testPrintDataStats() {
    try (K2hash db = K2hash.of(FILEDB)) {
      db.printDataStats();
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }

  /** K2hash version */
  @Disabled
  @Test
  public void testVersion() {
    try (K2hash db = K2hash.of(FILEDB)) {
      db.version();
      assertTrue(true);
    } catch (IOException e) {
      System.out.println("IOException " + e.getMessage());
      assertFalse(true);
    }
  }
}
