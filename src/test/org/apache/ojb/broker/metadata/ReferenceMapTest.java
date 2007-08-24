package org.apache.ojb.broker.metadata;

import junit.framework.TestCase;
import org.apache.ojb.broker.util.ReferenceMap;


public class ReferenceMapTest extends TestCase
{
    private ReferenceMap referenceMap = null;

    public ReferenceMapTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        String[] arr = {ReferenceMapTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }


    protected void setUp()
            throws Exception
    {
        super.setUp();
        /*
        todo: verify the constructors
        */
        referenceMap = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.HARD, 10, 0.75f, true);
    }

    protected void tearDown()
            throws Exception
    {
        referenceMap = null;
        super.tearDown();
    }

    public void testWeakIdentityMap()
    {
        String key1 = new String("abc");
        String key2 = new String("abc");
        String value1 = new String("abc");
        String value2 = new String("abc");

        assertNotSame("different references", key1, key2);
        assertEquals("identical strings", key1, key2);

        referenceMap.put(key1, "nonsence");
        assertEquals("size", 1, referenceMap.size());

        //if we put the same value size will remain the same
        referenceMap.put(key1, value1);
        assertEquals("size", 1, referenceMap.size());

        //if we put equal value size will increase
        referenceMap.put(key2, value2);
        assertEquals("size", 2, referenceMap.size());

        // test containsKey
        assertTrue("ref1 is there", referenceMap.containsKey(key1));
        assertTrue("ref2 is there", referenceMap.containsKey(key2));

        // test remove
        assertSame("key1=>value1", value1, referenceMap.remove(key1));
        assertEquals("size", 1, referenceMap.size());
        referenceMap.put(key1, value1); // put it back
        assertEquals("size", 2, referenceMap.size());


        key2 = ""; // will weaken ref2 key in referenceMap
        gc();

        assertEquals("GC didn't release weak references", 1, referenceMap.size());

        assertFalse("ref2 is not there", referenceMap.containsKey(key2));
        assertTrue("ref1 is there", referenceMap.containsKey(key1));

        if (key1.length() < 0) // lets do something with ref1 to avoid GC
            fail(key1); // never


        // check NULL
        //referenceMap.put(key1, null);

    }

    public static void gc()
    {
        try
        {
            // trigger GC
            byte[][] tooLarge = new byte[1000000000][1000000000];
            fail("you have too much RAM");
        }
        catch (OutOfMemoryError ex)
        {
            System.gc(); // ignore
        }
    }


}
