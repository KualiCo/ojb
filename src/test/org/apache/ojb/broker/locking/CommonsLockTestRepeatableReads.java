package org.apache.ojb.broker.locking;

import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.junit.OJBTestCase;

/**
 * This is the TestSuite that checks the {@link LockManager} implementation
 * based on apache's commons-transaction locking part.
 * It performs 17 testMethods as defined in the Locking documentation.
 */
public class CommonsLockTestRepeatableReads extends OJBTestCase
{
    final int testIsoLevel = LockManager.IL_REPEATABLE_READ;

    public static void main(String[] args)
    {
        String[] arr = {CommonsLockTestRepeatableReads.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public CommonsLockTestRepeatableReads(String name)
    {
        super(name);
    }

    Object tx1;
    Object tx2;
    Article obj;
    LockManager lockManager;
    int oldIsolationLevel;
    ClassDescriptor cld;

    public void setUp() throws Exception
    {
        super.setUp();

        // change isolation-level used for test
        cld = MetadataManager.getInstance().getRepository().getDescriptorFor(Article.class);
        oldIsolationLevel = cld.getIsolationLevel();
        cld.setIsolationLevel(testIsoLevel);

        lockManager = LockManagerHelper.getCommonsLockManager();

        // initialize the dummies
        tx2 = new Object();
        tx1 = new Object();

        obj = Article.createInstance();
    }

    public void tearDown() throws Exception
    {
        // restore isolation level
        cld.setIsolationLevel(oldIsolationLevel);
        try
        {
            lockManager.releaseLock(tx1, obj);
            lockManager.releaseLock(tx2, obj);
        }
        finally
        {
            super.tearDown();
        }
    }

    /**
     * Test 19
     */
    public void testWriteReleaseCheckRead()
    {
        assertTrue(lockManager.writeLock(tx2, obj, testIsoLevel));
        assertTrue(lockManager.hasRead(tx2, obj));
        assertTrue(lockManager.releaseLock(tx2, obj));
        assertFalse(lockManager.hasRead(tx2, obj));
    }

    /**
     * Test 20
     */
    public void testReadWriteReleaseCheckRead()
    {
        assertTrue(lockManager.readLock(tx2, obj, testIsoLevel));
        assertTrue(lockManager.writeLock(tx2, obj, testIsoLevel));
        assertTrue(lockManager.hasRead(tx2, obj));
        assertTrue(lockManager.releaseLock(tx2, obj));
        assertFalse(lockManager.hasRead(tx2, obj));
    }

    /**
     * Test 1
     */
    public void testSingleReadLock()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
    }

    /**
     * Test 2
     */
    public void testUpgradeReadLock()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.upgradeLock(tx1, obj, testIsoLevel));
    }

    /**
     * Test3
     */
    public void testReadThenWrite()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.writeLock(tx1, obj, testIsoLevel));
    }

    /**
     * Test 4
     */
    public void testSingleWriteLock()
    {
        assertTrue(lockManager.writeLock(tx1, obj, testIsoLevel));
    }

    /**
     * Test 5
     */
    public void testWriteThenRead()
    {
        assertTrue(lockManager.writeLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
    }

    /**
     * Test 6
     */
    public void testMultipleReadLock()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.readLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 7
     */
    public void testUpgradeWithExistingReader()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.upgradeLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 8
     */
    public void testWriteWithExistingReader()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(!lockManager.writeLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 9
     */
    public void testUpgradeWithMultipleReaders()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.readLock(tx2, obj, testIsoLevel));
        assertTrue(lockManager.upgradeLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 10
     */
    public void testWriteWithMultipleReaders()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.readLock(tx2, obj, testIsoLevel));
        assertTrue(!lockManager.writeLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 11
     */
    public void testUpgradeWithMultipleReadersOn1()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.readLock(tx2, obj, testIsoLevel));
        assertTrue(lockManager.upgradeLock(tx1, obj, testIsoLevel));
    }

    /**
     * Test 12
     */
    public void testWriteWithMultipleReadersOn1()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.readLock(tx2, obj, testIsoLevel));
        assertTrue(!lockManager.writeLock(tx1, obj, testIsoLevel));
    }

    /**
     * Test 13
     */
    public void testReadWithExistingWriter()
    {
        assertTrue(lockManager.writeLock(tx1, obj, testIsoLevel));
        assertTrue(!lockManager.readLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 14
     */
    public void testMultipleWriteLock()
    {
        assertTrue(lockManager.writeLock(tx1, obj, testIsoLevel));
        assertTrue(!lockManager.writeLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 15
     */
    public void testReleaseReadLock()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.releaseLock(tx1, obj));
        assertTrue(lockManager.writeLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 16
     */
    public void testReleaseUpgradeLock()
    {
        assertTrue(lockManager.upgradeLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.releaseLock(tx1, obj));
        assertTrue(lockManager.writeLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 17
     */
    public void testReleaseWriteLock()
    {
        assertTrue(lockManager.writeLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.releaseLock(tx1, obj));
        assertTrue(lockManager.writeLock(tx2, obj, testIsoLevel));
    }

    /**
     * Test 18
     */
    public void testReadThenRead()
    {
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
        assertTrue(lockManager.readLock(tx1, obj, testIsoLevel));
    }
}
