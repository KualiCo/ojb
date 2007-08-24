package org.apache.ojb.broker;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.collections.ManageableArrayList;
import org.apache.ojb.broker.util.collections.RemovalAwareCollection;
import org.apache.ojb.junit.PBTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * @author <a href="mailto:om@ppi.de">Oliver Matz</a>
 * @version $Id: MtoNTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class MtoNTest extends PBTestCase
{
    private static Class CLASS = MtoNTest.class;

    public static void main(String[] args)
    {
        String[] arr = { CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    private Paper createPaper()
    {
        String now = new Date().toString();
        Paper paper = new Paper();
        paper.setAuthor("Jonny Myers");
        paper.setDate(now);

        Qualifier qual1 = new Topic();
        qual1.setName("qual1 " + now);
        Qualifier qual2 = new Topic();
        qual2.setName("qual2 " + now);

        List qualifiers = new Vector();
        qualifiers.add(qual1);
        qualifiers.add(qual2);
        paper.setQualifiers(qualifiers);

        broker.beginTransaction();
        broker.store(qual1);
        broker.store(qual2);
        broker.store(paper);
        Identity paperId = new Identity(paper, broker);
        broker.commitTransaction();

        // sanity check
        broker.clearCache();
        broker.beginTransaction();
        Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
        qualifiers = retPaper.getQualifiers();

        assertEquals(2, qualifiers.size());
        broker.commitTransaction();

        return retPaper;
    }

    /**
     * Store m-side and intermediary
     */
    public void testStoringWithAutoUpdateFalse1()
    {
        ClassDescriptor cld = broker.getClassDescriptor(Paper.class);
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
        int autoUpdate = cod.getCascadingStore();

        cod.setCascadingStore(ObjectReferenceDescriptor.CASCADE_LINK);

        try
        {
            String now = new Date().toString();
            Paper paper = new Paper();
            paper.setAuthor("Jonny Myers");
            paper.setDate(now);
            Qualifier qual = new Topic();
            qual.setName("qual " + now);
            paper.setQualifiers(Arrays.asList(new Qualifier[] { qual }));
            broker.beginTransaction();
            // TODO: use constraint in DB and fix test
            // store paper and set indirection table, ignore new Qualifier
            // object. Will cause Key Constraint Exception when constraint are set
            broker.store(paper);
            Identity paperId = new Identity(paper, broker);
            broker.commitTransaction();

            broker.clearCache();
            broker.beginTransaction();
            Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
            assertEquals(0, retPaper.getQualifiers().size());
            broker.commitTransaction();
        }
        finally
        {
            cod.setCascadingStore(autoUpdate);
        }
    }

    /**
     * Store m-side, intermediary and n-side
     * n-side forced by using broker.store()
     */
    public void testStoringWithAutoUpdateFalse2()
    {
        ClassDescriptor cld = broker.getClassDescriptor(Paper.class);
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
        int autoUpdate = cod.getCascadingStore();

        cod.setCascadingStore(ObjectReferenceDescriptor.CASCADE_LINK);

        try
        {
            String now = new Date().toString();
            Paper paper = new Paper();
            paper.setAuthor("Jonny Myers");
            paper.setDate(now);
            Qualifier qual = new Topic();
            qual.setName("qual " + now);
            paper.setQualifiers(Arrays.asList(new Qualifier[] { qual }));
            broker.beginTransaction();
            broker.store(qual);         // store Qualifier
            broker.store(paper);        // store Paper and intermediary table only
            Identity paperId = broker.serviceIdentity().buildIdentity(paper);
            broker.commitTransaction();

            broker.clearCache();
            broker.beginTransaction();
            Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
            assertEquals(1, retPaper.getQualifiers().size());
            broker.commitTransaction();
        }
        finally
        {
            cod.setCascadingStore(autoUpdate);
        }
    }

    /**
     * Store m-side, intermediary and n-side
     */
    public void testStoringWithAutoUpdateTrue()
    {
        ClassDescriptor cld = broker.getClassDescriptor(Paper.class);
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
        int autoUpdate = cod.getCascadingStore();

        cod.setCascadingStore(ObjectReferenceDescriptor.CASCADE_OBJECT);

        try
        {
            String now = new Date().toString();
            Paper paper = new Paper();
            paper.setAuthor("Jonny Myers");
            paper.setDate(now);
            Qualifier qual = new Topic();
            qual.setName("qual " + now);
            paper.setQualifiers(Arrays.asList(new Qualifier[] { qual }));
            broker.beginTransaction();
            broker.store(paper);        // store Paper, intermediary and Qualifier
            Identity paperId = new Identity(paper, broker);
            broker.commitTransaction();

            broker.clearCache();
            broker.beginTransaction();
            Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
            assertEquals(1, retPaper.getQualifiers().size());
            broker.commitTransaction();
        }
        finally
        {
            cod.setCascadingStore(autoUpdate);
        }
    }


    // delete from intermediary table only when collection NOT removal aware
    public void testDelete_NonRemovalAware()
    {
        ClassDescriptor cld = broker.getClassDescriptor(Paper.class);
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
        Class collectionClass = cod.getCollectionClass();

        cod.setCollectionClass(ManageableArrayList.class);

        try
        {
            Paper paper = createPaper();
            Identity paperId = new Identity(paper, broker);
            List qualifiers = paper.getQualifiers();
            Qualifier qual1 = (Qualifier) qualifiers.get(0);
            Qualifier qual2 = (Qualifier) qualifiers.get(1);

            // remove first object
            qualifiers.remove(0);
            broker.beginTransaction();
            broker.store(paper);
            broker.commitTransaction();

            broker.clearCache();
            broker.beginTransaction();
            Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
            assertEquals(1, retPaper.getQualifiers().size());

            // target object qual1 should NOT be deleted
            Qualifier retQual1 = (Qualifier) broker.getObjectByIdentity(new Identity(qual1, broker));
            Qualifier retQual2 = (Qualifier) broker.getObjectByIdentity(new Identity(qual2, broker));

            assertNotNull(retQual1);
            assertNotNull(retQual2);

            broker.commitTransaction();
        }
        finally
        {
            cod.setCollectionClass(collectionClass);
        }

    }

    // delete from intermediary AND target-table when collection removal aware
    public void testDelete_RemovalAware()
    {
        ClassDescriptor cld = broker.getClassDescriptor(Paper.class);
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
        Class collectionClass = cod.getCollectionClass();

        cod.setCollectionClass(RemovalAwareCollection.class);

        try
        {
            Paper paper = createPaper();
            List qualifiers = paper.getQualifiers();
            Qualifier qual1 = (Qualifier) qualifiers.get(0);
            Qualifier qual2 = (Qualifier) qualifiers.get(1);
            Identity paperId = new Identity(paper, broker);

            // remove first object
            qualifiers.remove(0);
            broker.beginTransaction();
            broker.store(paper);
            broker.commitTransaction();

            broker.clearCache();
            broker.beginTransaction();
            Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
            assertEquals(1, retPaper.getQualifiers().size());

            // target object qual1 should be deleted
            Qualifier retQual1 = (Qualifier) broker.getObjectByIdentity(new Identity(qual1, broker));
            Qualifier retQual2 = (Qualifier) broker.getObjectByIdentity(new Identity(qual2, broker));

            assertNull(retQual1);
            assertNotNull(retQual2);

            broker.commitTransaction();
        }
        finally
        {
            cod.setCollectionClass(collectionClass);
        }
    }

    public void testDeletionFromIntermediaryTableWithNullList()
    {
        Paper paper = createPaper();
        Identity paperId = new Identity(paper, broker);
        List qualifiers = paper.getQualifiers();
        Qualifier qual1 = (Qualifier) qualifiers.get(0);
        Qualifier qual2 = (Qualifier) qualifiers.get(1);

        // now set collection to null and check if changes get persisted
        paper.setQualifiers(null);
        broker.beginTransaction();
        broker.store(paper);
        broker.commitTransaction();

        broker.clearCache();
        broker.beginTransaction();
        Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
        assertEquals(0, retPaper.getQualifiers().size());

        // target objects should NOT be deleted
        Qualifier retQual1 = (Qualifier) broker.getObjectByIdentity(new Identity(qual1, broker));
        Qualifier retQual2 = (Qualifier) broker.getObjectByIdentity(new Identity(qual2, broker));

        assertNotNull(retQual1);
        assertNotNull(retQual2);

        broker.commitTransaction();
    }

    public void testDeletionWithClearedList()
    {
        Paper paper = createPaper();
        Identity paperId = new Identity(paper, broker);
        List qualifiers = paper.getQualifiers();
        Qualifier qual1 = (Qualifier) qualifiers.get(0);
        Qualifier qual2 = (Qualifier) qualifiers.get(1);

        // now clear collection
        paper.getQualifiers().clear();
        broker.beginTransaction();
        broker.store(paper);
        broker.commitTransaction();

        broker.clearCache();
        broker.beginTransaction();
        Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
        assertEquals(0, retPaper.getQualifiers().size());

        // target objects should NOT be deleted
        Qualifier retQual1 = (Qualifier) broker.getObjectByIdentity(new Identity(qual1, broker));
        Qualifier retQual2 = (Qualifier) broker.getObjectByIdentity(new Identity(qual2, broker));

        assertNotNull(retQual1);
        assertNotNull(retQual2);

        broker.commitTransaction();
    }

    public void testDeletionFromIntermediaryTableWithEmptyList()
    {
        Paper paper = createPaper();
        Identity paperId = new Identity(paper, broker);
        List qualifiers = paper.getQualifiers();
        Qualifier qual1 = (Qualifier) qualifiers.get(0);
        Qualifier qual2 = (Qualifier) qualifiers.get(1);

        // now empty collection and check if changes get persisted
        paper.setQualifiers(new RemovalAwareCollection());
        broker.beginTransaction();
        broker.store(paper);
        broker.commitTransaction();

        broker.clearCache();
        broker.beginTransaction();
        Paper retPaper = (Paper) broker.getObjectByIdentity(paperId);
        assertEquals(0, retPaper.getQualifiers().size());

        // target objects should NOT be deleted
        Qualifier retQual1 = (Qualifier) broker.getObjectByIdentity(new Identity(qual1, broker));
        Qualifier retQual2 = (Qualifier) broker.getObjectByIdentity(new Identity(qual2, broker));

        assertNotNull(retQual1);
        assertNotNull(retQual2);

        broker.commitTransaction();
    }


    public void testDeleteMtoNImplementor()
        throws Exception
    {
        News newsId2 = new News(2);
		Identity id = new Identity(newsId2,broker);
		News newNews = (News) broker.getObjectByIdentity(id);
		int size = newNews.getQualifiers().size();

        Category categoryId1 = new Category(1);

        MtoNImplementor m2n = new MtoNImplementor(broker, "qualifiers", newsId2, categoryId1);
        broker.deleteMtoNImplementor(m2n);

		broker.clearCache();
		newNews = (News) broker.getObjectByIdentity(id);

        assertEquals(size - 1,newNews.getQualifiers().size());
    }

	public void testStoreMtoNImplementor()
		throws Exception
	{
		News newsId2 = new News(2);
		Category categoryId2 = new Category(2);

		Identity id = new Identity(newsId2,broker);
		News newNews = (News) broker.getObjectByIdentity(id);
		int size = newNews.getQualifiers().size();

		MtoNImplementor m2n = new MtoNImplementor(broker, "qualifiers", newsId2,categoryId2);
		broker.addMtoNImplementor(m2n);

		broker.clearCache();
		newNews = (News) broker.getObjectByIdentity(id);

		assertEquals(size + 1,newNews.getQualifiers().size());

	}


    // Bidirectional m:n relationship using Collection
    public void testStoreBidirectionalCollection()
    {
        Person personA = new Person();
        personA.setFirstname("Anton");

        Project proj1 = new Project();
        proj1.setTitle("Project 1");

        Project proj2 = new Project();
        proj2.setTitle("Project 2");

        Collection persons = new ArrayList();
        persons.add(personA);
        proj1.setPersons(persons);
        proj2.setPersons(persons);

        Collection projects = new ArrayList();
        projects.add(proj1);
        projects.add(proj2);
        personA.setProjects(projects);

        broker.beginTransaction();
        broker.store(personA);
        broker.store(proj1);
        broker.store(proj2);
        broker.commitTransaction();
    }

    // Bidirectional m:n relationship using Array
    public void testStoreBidirectionalArray()
    {
        PersonWithArray personA = new PersonWithArray();
        personA.setFirstname("Anton");

        ProjectWithArray proj1 = new ProjectWithArray();
        proj1.setTitle("Project 1");

        ProjectWithArray proj2 = new ProjectWithArray();
        proj2.setTitle("Project 2");

        proj1.setPersons(new PersonWithArray[] { personA });
        proj2.setPersons(new PersonWithArray[] { personA });
        personA.setProjects(new ProjectWithArray[] { proj1, proj2 });

        broker.beginTransaction();
        broker.store(personA);
        broker.store(proj1);
        broker.store(proj2);
        broker.commitTransaction();
    }
}
