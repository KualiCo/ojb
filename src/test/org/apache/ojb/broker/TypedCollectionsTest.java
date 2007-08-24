package org.apache.ojb.broker;

import org.apache.ojb.broker.query.QueryByIdentity;
import org.apache.ojb.junit.PBTestCase;

/** This TestClass tests OJB facilities to work with typed collections.
 */
public class TypedCollectionsTest extends PBTestCase
{
	public static void main(String[] args)
	{
		String[] arr = {TypedCollectionsTest.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

	public TypedCollectionsTest(String name)

	{
		super(name);
	}

	/** Test support for 1-n relations modelled with Arrays*/
	public void testArray() throws Exception
	{
		int i;
        broker.beginTransaction();
        for (i = 1; i < 4; i++)
        {
            ProductGroupWithArray example = new ProductGroupWithArray();
            example.setId(i);
            ProductGroupWithArray group =
                (ProductGroupWithArray) broker.getObjectByQuery(new QueryByIdentity(example));
            assertEquals("should be equal", i, group.getId());
            //System.out.println(group + "\n\n");

            broker.delete(group);
            broker.store(group);
        }
        broker.commitTransaction();
	}

	/** TestThreadsNLocks support for modelling 1-n relations with typed collections*/
	public void testTypedCollection()
	{
        broker.beginTransaction();
        for (int i = 1; i < 4; i++)
        {
            ProductGroupWithTypedCollection example = new ProductGroupWithTypedCollection();
            example.setId(i);
            ProductGroupWithTypedCollection group =
                (ProductGroupWithTypedCollection) broker.getObjectByQuery(
                    new QueryByIdentity(example));
            assertEquals("should be equal", i, group.getId());
            //System.out.println(group + "\n\n");

            broker.delete(group);
            broker.store(group);
        }
        broker.commitTransaction();
	}
}
