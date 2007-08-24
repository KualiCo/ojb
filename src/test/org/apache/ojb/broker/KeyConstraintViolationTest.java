package org.apache.ojb.broker;

import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.junit.PBTestCase;

/**
 * @author Matthew Baird
 *
 */
public class KeyConstraintViolationTest  extends PBTestCase
{
	public static void main(String[] args)
	{
		String[] arr = {KeyConstraintViolationTest.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

    public KeyConstraintViolationTest(String name)
	{
		super(name);
	}

	/**
	 * Test creating two objects with the same ID, should fail with
	 * key constraint error
	 **/
	public void testKeyViolation() throws Exception
	{
        broker.beginTransaction();
        Article obj = new Article();
        obj.setProductGroupId(new Integer(1));
        obj.articleName = "repeated Article";
        // storing once should be ok.
        broker.store(obj, ObjectModification.INSERT);
        broker.commitTransaction();

        broker.clearCache();
        try
		{
			broker.beginTransaction();
			Article obj2 = new Article();
			obj2.articleId = obj.getArticleId();
            obj2.setProductGroupId(new Integer(1));
			obj2.articleName = "repeated Article";

			// store it again!
			broker.store(obj2, ObjectModification.INSERT);
			broker.commitTransaction();

			fail("Should have thrown a KeyConstraintViolatedException");
		}
		catch (KeyConstraintViolatedException t)
		{
			// this is a success.
            broker.abortTransaction();
		}
	}
}
