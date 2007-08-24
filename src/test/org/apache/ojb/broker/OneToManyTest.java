package org.apache.ojb.broker;

import java.util.List;

import org.apache.ojb.junit.PBTestCase;

/**
 * tests for the essential create/update/delete/read for one-to-many related objects
 */

public class OneToManyTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {OneToManyTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public OneToManyTest(String name)
    {
        super(name);
    }

	/**
	 * test the removal aware functionality.
	 */
    public void testDeleteWithRemovalAwareCollection()
    {
    	long timestamp = System.currentTimeMillis();

        ProductGroupWithRemovalAwareCollection pg = new ProductGroupWithRemovalAwareCollection();
    	// auto-increment was not enabled in repository
        // thus we set our own
        pg.setId((int) timestamp%Integer.MAX_VALUE);
        pg.setGroupName("testDeleteWithRemovalAwareCollection_"+timestamp);

    	Identity pgId = new Identity(pg, broker);

    	Article a = new Article();
    	a.setArticleName("testDeleteWithRemovalAwareCollection_"+timestamp);

        Article b = new Article();
    	b.setArticleName("testDeleteWithRemovalAwareCollection_"+timestamp);

        Article c = new Article();
    	c.setArticleName("testDeleteWithRemovalAwareCollection_"+timestamp);

    	pg.add(a);
    	pg.add(b);
    	pg.add(c);
        broker.beginTransaction();
    	broker.store(pg);
        broker.commitTransaction();

    	broker.clearCache();
    	pg = (ProductGroupWithRemovalAwareCollection) broker.getObjectByIdentity(pgId);
    	assertEquals(3,pg.getAllArticles().size());

    	pg.getAllArticles().remove(c);
    	pg.getAllArticles().remove(0);
        broker.beginTransaction();
    	broker.store(pg);
        broker.commitTransaction();

    	broker.clearCache();
    	pg = (ProductGroupWithRemovalAwareCollection) broker.getObjectByIdentity(pgId);
    	assertEquals(1,pg.getAllArticles().size());
    }

    /**
     * this tests if polymorph collections (i.e. collections of objects
     * implementing a common interface) are treated correctly
     */
    public void testPolymorphOneToMany()
    {
        long timestamp = System.currentTimeMillis();

        Zoo myZoo = new Zoo("London_"+timestamp);
        Identity id = new Identity(myZoo, broker);

        Mammal elephant = new Mammal(2,"Jumbo_"+timestamp,4);
        Mammal cat = new Mammal(2,"Silvester_"+timestamp,4);
        Reptile snake = new Reptile(2,"Kaa_"+timestamp,"green");

        myZoo.addAnimal(snake);
        myZoo.addAnimal(elephant);
        myZoo.addAnimal(cat);
// System.out.println("## "+myZoo);
        broker.beginTransaction();
        broker.store(myZoo);
        broker.commitTransaction();
// System.out.println("## "+myZoo);

        broker.clearCache();

        Zoo loadedZoo = (Zoo) broker.getObjectByIdentity(id);
        List animals = loadedZoo.getAnimals();
        assertEquals(3, animals.size());

        broker.beginTransaction();
        for (int i = 0; i < animals.size(); i++)
        {
            broker.delete(animals.get(i));
        }
        broker.delete(loadedZoo);
        broker.commitTransaction();
    }
}
