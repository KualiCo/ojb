package org.apache.ojb.otm;

/**
 * This is the TestSuite based on the ODMG locking documentation
 */
import org.apache.ojb.otm.lock.isolation.TransactionIsolation;
import org.apache.ojb.otm.lock.isolation.ReadCommittedIsolation;

public class LockTestCommittedReads extends LockTestBase
{
    private static Class CLASS = LockTestCommittedReads.class;

    public LockTestCommittedReads(String name)
    {
        super(name);
    }

    protected TransactionIsolation newIsolation()
    {
        return new ReadCommittedIsolation();
    }

    /**
     * Test 1
     */
    public void testSingleReadLock()
    {
        assertTrue(readLock(_tx1));
    }

    /**
     * Test3
     */
    public void testReadThenWrite()
    {
        assertTrue(readLock(_tx1));
        assertTrue(writeLock(_tx1));
    }

    /**
     * Test 4
     */
    public void testSingleWriteLock()
    {
        assertTrue(writeLock(_tx1));
    }

    /**
     * Test 5
     */
    public void testWriteThenRead()
    {
        assertTrue(writeLock(_tx1));
        assertTrue(readLock(_tx1));
    }

    /**
     * Test 6
     */
    public void testMultipleReadLock()
    {
        assertTrue(readLock(_tx1));
        assertTrue(readLock(_tx2));
    }

    /**
     * Test 8
     */
    public void testWriteWithExistingReader()
    {
        assertTrue(readLock(_tx1));
        assertTrue(writeLock(_tx2));
    }

    /**
     * Test 10
     */
    public void testWriteWithMultipleReaders()
    {
        assertTrue(readLock(_tx1));
        assertTrue(readLock(_tx2));
        assertTrue(writeLock(_tx2));
    }

    /**
     * Test 12
     */
    public void testWriteWithMultipleReadersOn1()
    {
        assertTrue(readLock(_tx1));
        assertTrue(readLock(_tx2));
        assertTrue(writeLock(_tx1));
    }

    /**
     * Test 13
     */
    public void testReadWithExistingWriter()
    {
        assertTrue(writeLock(_tx1));
        assertTrue(!readLock(_tx2));
    }

    /**
     * Test 14
     */
    public void testMultipleWriteLock()
    {
        assertTrue(writeLock(_tx1));
        assertTrue(!writeLock(_tx2));
    }

    /**
     * Test 15
     */
    public void testReleaseReadLock()
    {
        assertTrue(readLock(_tx1));
        assertTrue(releaseLock(_tx1));
        assertTrue(writeLock(_tx2));
    }

    /**
     * Test 17
     */
    public void testReleaseWriteLock()
    {
        assertTrue(writeLock(_tx1));
        assertTrue(releaseLock(_tx1));
        assertTrue(writeLock(_tx2));
    }

    /**
     * Test 18
     */
    public void testReadThenRead()
    {
        assertTrue(readLock(_tx1));
        assertTrue(readLock(_tx1));
    }

    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }
}
