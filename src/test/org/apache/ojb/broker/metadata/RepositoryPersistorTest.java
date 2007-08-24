package org.apache.ojb.broker.metadata;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.TestHelper;
import org.apache.ojb.junit.OJBTestCase;

/**
 * This TestClass tests the RepositoryPersitors facilities for
 * reading and writing a valid repository.
 */
public class RepositoryPersistorTest extends OJBTestCase
{
	public static void main(String[] args)
	{
		String[] arr = { RepositoryPersistorTest.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

	public RepositoryPersistorTest(String name)
	{
		super(name);
	}

	/** Test storing/read repository.*/
	public void testStoreReadRepository() throws Exception
	{
		String fileNew = "test_repository_rewritten.xml";
		String file = "repository.xml";
        RepositoryPersistor persistor = new RepositoryPersistor();
        DescriptorRepository repository = persistor.readDescriptorRepository(file);
        ConnectionRepository conRepository = MetadataManager.getInstance().connectionRepository();
        Iterator iter = repository.iterator();
        int numClasses = 0;
        List list = new ArrayList();
        while (iter.hasNext())
        {
            ClassDescriptor cld =((ClassDescriptor) iter.next());
            if(!list.contains(cld.getClassOfObject()))
            {
                list.add(cld.getClassOfObject());
            }
            else
            {
                fail("## Duplicate cld: " + cld.toXML());
            }
            numClasses++;
        }

        persistor = new RepositoryPersistor();
        FileOutputStream fos = new FileOutputStream(fileNew);
        persistor.writeToFile(repository, conRepository, fos);

        DescriptorRepository second = persistor.readDescriptorRepository(fileNew);
        iter = second.iterator();
        int numClasses2 = 0;
        List list2 = new ArrayList();
        while (iter.hasNext())
        {
            list2.add(((ClassDescriptor) iter.next()).getClassOfObject());
            numClasses2++;
        }
        assertEquals("read in persisted repository should have same number of classes, the differences are "
                + CollectionUtils.disjunction(list, list2),
            numClasses, numClasses2);
        assertTrue(numClasses2 > 0);
	}

    /** Test storing/read repository.*/
	public void testStoreReadConnectionRepository() throws Exception
	{
		String filename = "test_repository_database.xml";
        DescriptorRepository repository = MetadataManager.getInstance().getRepository();
        ConnectionRepository conRepository = MetadataManager.getInstance().connectionRepository();
        int connectionCount = conRepository.getAllDescriptor().size();

        FileOutputStream fos = new FileOutputStream(filename);
        RepositoryPersistor persistor = new RepositoryPersistor();
        persistor.writeToFile(repository, conRepository, fos);

        ConnectionRepository second = persistor.readConnectionRepository(filename);
        int connectionCount2 = second.getAllDescriptor().size();

        PBKey defaultKey = second.getStandardPBKeyForJcdAlias(TestHelper.DEF_JCD_ALIAS);
        assertNotNull(defaultKey);
        assertEquals(TestHelper.DEF_KEY, defaultKey);

        assertTrue(connectionCount2 > 0);
        assertEquals("read in persisted connection repository should have same number of classes",
            connectionCount, connectionCount2);
	}
}
