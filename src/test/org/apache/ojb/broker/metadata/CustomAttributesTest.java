package org.apache.ojb.broker.metadata;

import junit.framework.TestCase;

import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.PBKey;

/**
 * This TestClass tests the RepositoryPersitors facilities for
 * reading and writing a valid repository.
 */
public class CustomAttributesTest extends TestCase
{
	public static void main(String[] args)
	{
		String[] arr = { CLASS.getName()};
		junit.textui.TestRunner.main(arr);
	}

	//PersistenceBroker broker;
	private static Class CLASS = CustomAttributesTest.class;

	/**
	 * Insert the method's description here.
	 * Creation date: (24.12.2000 00:33:40)
	 */
	public CustomAttributesTest(String name)
	{
		super(name);
	}

	/**
	 * Insert the method's description here.
	 * Creation date: (06.12.2000 21:58:53)
	 */
	public void setUp()
	{
	}

	/**
	 * Insert the method's description here.
	 * Creation date: (06.12.2000 21:59:14)
	 */
	public void tearDown()
	{

	}

    public void testCustomAttributesDescriptorRepository()
    {
        String repositoryAttribute_1 = "attribute-repository-1";
        String repositoryAttribute_2 = "attribute-repository-2";
        MetadataManager mm = MetadataManager.getInstance();

        DescriptorRepository dr = mm.readDescriptorRepository(MetadataTest.TEST_REPOSITORY);
        String res_1 = dr.getAttribute(repositoryAttribute_1);
        String res_2 = dr.getAttribute(repositoryAttribute_2);
        assertNotNull("No attributes found", res_1);
        assertNotNull("No attributes found", res_2);
        assertEquals("Found attribute does not match", "attribute-repository-test-value-1", res_1);
        assertEquals("Found attribute does not match", "attribute-repository-test-value-2", res_2);

    }

    public void testCustomAttributesConnectionDescriptor()
    {
        String connectionAttribute_1 = "attribute-connection-1";
        String connectionAttribute_2 = "attribute-connection-2";

        MetadataManager mm = MetadataManager.getInstance();
        ConnectionRepository cr = mm.readConnectionRepository(MetadataTest.TEST_CONNECTION_DESCRIPTOR);
        JdbcConnectionDescriptor jcd = cr.getDescriptor(new PBKey("runtime"));
        String res_1 = jcd.getAttribute(connectionAttribute_1);
        String res_2 = jcd.getAttribute(connectionAttribute_2);
        assertNotNull("No attributes found", res_1);
        assertNotNull("No attributes found", res_2);
        assertEquals("Found attribute does not match", "attribute-connection-test-value-1", res_1);
        assertEquals("Found attribute does not match", "attribute-connection-test-value-2", res_2);
    }


	/** Test storing repository.*/
	public void testCustomAttributesClassDescriptor()
	{
		DescriptorRepository repository = MetadataManager.getInstance().getRepository();

		ClassDescriptor cld = repository.getDescriptorFor(Article.class);
		assertTrue("blue".equals(cld.getAttribute("color")));
		assertTrue("big".equals(cld.getAttribute("size")));

		FieldDescriptor fld = cld.getFieldDescriptorByName("isSelloutArticle");
		assertTrue("green".equals(fld.getAttribute("color")));
		assertTrue("small".equals(fld.getAttribute("size")));


		ObjectReferenceDescriptor ord = cld.getObjectReferenceDescriptorByName("productGroup");
		assertNotNull("did not find ord for 'productGroup'!", ord);
		assertTrue("red".equals(ord.getAttribute("color")));
		assertTrue("tiny".equals(ord.getAttribute("size")));
	}

    /** Test using attributes on serialized/deserialized repository*/
	public void testSerializedCustomAttributesClassDescriptor()
	{
        DescriptorRepository repository = MetadataManager.getInstance().copyOfGlobalRepository();

		ClassDescriptor cld = repository.getDescriptorFor(Article.class);
		assertTrue("blue".equals(cld.getAttribute("color")));
		assertTrue("big".equals(cld.getAttribute("size")));

		FieldDescriptor fld = cld.getFieldDescriptorByName("isSelloutArticle");
		assertTrue("green".equals(fld.getAttribute("color")));
		assertTrue("small".equals(fld.getAttribute("size")));


		ObjectReferenceDescriptor ord = cld.getObjectReferenceDescriptorByName("productGroup");
		assertNotNull("did not find ord for 'productGroup'!", ord);
		assertTrue("red".equals(ord.getAttribute("color")));
		assertTrue("tiny".equals(ord.getAttribute("size")));
	}
}
