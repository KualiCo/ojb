package org.apache.ojb.odmg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.Article;
import org.apache.ojb.odmg.shared.Person;
import org.apache.ojb.odmg.shared.PersonImpl;
import org.apache.ojb.odmg.shared.ProductGroup;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * OQLQuery tests.
 *
 * @version $Id: OQLTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class OQLTest extends ODMGTestCase
{
	private int COUNT = 10;
	private int id_filter = 2;

	public static void main(String[] args)
	{
		String[] arr = {OQLTest.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

	public OQLTest(String name)
	{
		super(name);
	}

	private void createPersons()
			throws Exception
	{
		Transaction tx = odmg.newTransaction();
		tx.begin();
		for (int i = 0; i < COUNT; i++)
		{
			Person aPerson = new PersonImpl();
			aPerson.setFirstname("firstname" + i);
			aPerson.setLastname("lastname" + i);
			database.makePersistent(aPerson);
		}
		tx.commit();
	}

	private void deleteData(Class target)
			throws Exception
	{
		Transaction tx = odmg.newTransaction();
		tx.begin();
		OQLQuery query = odmg.newOQLQuery();
		query.create("select allStuff from " + target.getName());
		Collection allTargets = (Collection) query.execute();
		Iterator it = allTargets.iterator();
		while (it.hasNext())
		{
			database.deletePersistent(it.next());
		}
		tx.commit();
	}

    public void testMultipleLoad() throws Exception
    {
        String name = "testMultipleLoad_" + System.currentTimeMillis();
        Fish fish = new Fish();
        fish.setName(name);
        fish.setTypeOfWater("normal");

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        odmg.getDatabase(null).makePersistent(fish);
        tx.commit();

        Fish newFish = (Fish) SerializationUtils.clone(fish);
        newFish.setTypeOfWater("salty");

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();

        // Query current fish for comparison
        query.create("select object from " + Fish.class.getName() + " where foodId=$1");
        query.bind(new Integer(newFish.getFoodId()));
        List result = (List) query.execute();

        assertNotNull(result);
        assertEquals(1, result.size());
        Fish current = (Fish) result.get(0);

        assertEquals(name, current.getName());
        assertEquals("normal", current.getTypeOfWater());

        assertEquals("salty", newFish.getTypeOfWater());
        tx.lock(newFish, Transaction.WRITE);
        tx.markDirty(newFish);
        tx.commit();

        // check for changes allow using cache
        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        query = odmg.newOQLQuery();

        // Query current fish for comparison
        query.create("select object from " + Fish.class.getName() + " where foodId=$1");
        query.bind(new Integer(newFish.getFoodId()));
        result = (List) query.execute();
        tx.commit();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        current = (Fish) result.get(0);
        assertEquals(name, current.getName());
        assertEquals("salty", current.getTypeOfWater());

        // check for changes without using cache
        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // clear cache to lookup object from DB
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();

        // Query current fish for comparison
        query.create("select object from " + Fish.class.getName() + " where foodId=$1");
        query.bind(new Integer(newFish.getFoodId()));
        result = (List) query.execute();
        tx.commit();

        assertNotNull(result);
        assertEquals(1, result.size());
        current = (Fish) result.get(0);
        assertEquals(name, current.getName());
        assertEquals("salty", current.getTypeOfWater());
    }

	/**
	 * test the following conditions:
	 * != support
	 * literal support at binding.
	 * A prior bug where criteria was reporting as bindable when it was in fact a literal.
	 */
	public void testGetWithLiteral() throws Exception
	{
        createPersons();

        // 3. Get a list of some articles
        Transaction tx = odmg.newTransaction();
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        String sql = "select allPersons from " + Person.class.getName() + " where id != 5 and id > $1";
        query.create(sql);
        query.bind(new Integer(id_filter));

        Collection allPersons = (Collection) query.execute();

        // Iterator over the restricted articles objects
        java.util.Iterator it = allPersons.iterator();

        while (it.hasNext())
        {
            /**
             * just make sure it's a string.
             */
            Object result = it.next();
            Person value = (Person) result;
            if (value.getId() <= id_filter)
                fail("oql didn't filter, got id (" + value.getId() + " where it should have been over " + id_filter);
        }
        tx.commit();
	}

	public void testQueryIn() throws Exception
	{
		// deleteData(database, odmg, Article.class);

		Transaction tx = odmg.newTransaction();
		tx.begin();

		OQLQuery query1 = odmg.newOQLQuery();
		query1.create("select anArticle from " + Article.class.getName() + " where articleId in(30, 31, 32) order by articleId");
		List result1 = (List) query1.execute();

		Collection ids = new ArrayList();
		ids.add(new Integer(30));
		ids.add(new Integer(31));
		ids.add(new Integer(32));
		OQLQuery query2 = odmg.newOQLQuery();
		query2.create("select anArticle from " + Article.class.getName() + " where articleId in($1) order by articleId");
		query2.bind(ids);
		List result2 = (List) query2.execute();

		assertEquals(result1.size(), result2.size());
		tx.commit();
	}

	public void testQueryNull() throws Exception
	{
		Transaction tx = odmg.newTransaction();
		tx.begin();

		OQLQuery query1 = odmg.newOQLQuery();
		query1.create("select anArticle from " + Article.class.getName() + " where is_undefined(articleName)");
		List result1 = (List) query1.execute();

		OQLQuery query2 = odmg.newOQLQuery();
		query2.create("select anArticle from " + Article.class.getName() + " where articleName = nil");
		List result2 = (List) query2.execute();

		assertEquals(result1.size(), result2.size());
		tx.commit();
	}

	public void testQueryNotNull() throws Exception
	{
		Transaction tx = odmg.newTransaction();
		tx.begin();

		OQLQuery query1 = odmg.newOQLQuery();
		query1.create("select anArticle from " + Article.class.getName() + " where is_defined(articleName)");
		List result1 = (List) query1.execute();

		OQLQuery query2 = odmg.newOQLQuery();
		query2.create("select anArticle from " + Article.class.getName() + " where articleName <> nil");
		List result2 = (List) query2.execute();

		assertEquals(result1.size(), result2.size());
		tx.commit();
	}

	public void testQueryBetween() throws Exception
	{
		Transaction tx = odmg.newTransaction();

		tx.begin();

		OQLQuery query1 = odmg.newOQLQuery();
		query1.create("select anArticle from " + Article.class.getName() + " where articleId between 30 and 32 order by articleId desc");
		List result1 = (List) query1.execute();

		OQLQuery query2 = odmg.newOQLQuery();
		query2.create("select anArticle from " + Article.class.getName() + " where articleId between $1 and $2 order by articleId asc");
		query2.bind(new Integer(30));
		query2.bind(new Integer(32));
		List result2 = (List) query2.execute();

//        System.out.println("#### OQLTest#testQueryBetween(): Size result_1=" + result1.size()
//                + ", size result_2=" + result2.size());
//        for(int i = 0; i < result1.size(); i++)
//        {
//            Article a = (Article) result1.get(i);
//            System.out.println("Article_Query_1: articles_in_group=" + a.getProductGroup().getAllArticlesInGroup().size()
//                    + " - " + a);
//        }
//
//        for(int i = 0; i < result2.size(); i++)
//        {
//            Article a = (Article) result2.get(i);
//            System.out.println("Article_Query_2: articles_in_group=" + a.getProductGroup().getAllArticlesInGroup().size()
//                    + " - " + a);
//        }
		tx.commit();

        tx.begin();
        OQLQuery query3 = odmg.newOQLQuery();
		query3.create("select Article from " + Article.class.getName() + " where articleId between $1 and $2 order by articleId asc");
		query3.bind(new Integer(30));
		query3.bind(new Integer(32));
        List result3 = (List) query3.execute();

        OQLQuery query4 = odmg.newOQLQuery();
		query4.create("select Article from " + Article.class.getName() + " where articleId between 30 and 32 order by articleId desc");
		List result4 = (List) query4.execute();

//        for(int i = 0; i < result3.size(); i++)
//        {
//            Article a = (Article) result3.get(i);
//            System.out.println("Article_Query_3: articles_in_group=" + a.getProductGroup().getAllArticlesInGroup().size()
//                    + " - " + a);
//        }
//
//        for(int i = 0; i < result4.size(); i++)
//        {
//            Article a = (Article) result4.get(i);
//            System.out.println("Article_Query_4: articles_in_group=" + a.getProductGroup().getAllArticlesInGroup().size()
//                    + " - " + a);
//        }
        tx.commit();

        assertEquals(result1.size(), result2.size());
        assertEquals(result1.size(), result3.size());
        assertEquals(result2.size(), result3.size());
        assertEquals(result1.size(), result4.size());
        assertEquals(result2.size(), result4.size());
        assertEquals(result1, result4);
        assertEquals(result2, result3);
	}

	public void testInListQuery() throws Exception
	{
		//objects that are part of a 1:n relation, i.e. they have fk-fields
		Mammal elephant = new Mammal(4, "Minnie", 4);
		Mammal cat = new Mammal(4, "Winston", 4);
		Reptile snake = new Reptile(4, "Skuzzlebutt", "green");

        Transaction tx = odmg.newTransaction();
		tx.begin();
		OQLQuery query = odmg.newOQLQuery();
		query.create("select animals from " + InterfaceAnimal.class.getName() +
					 " where name in list (\"Minnie\", \"Winston\", \"Skuzzlebutt\")");
		int before = ((Collection) query.execute()).size();
		tx.commit();


		tx = odmg.newTransaction();
		tx.begin();
		database.makePersistent(elephant);
		database.makePersistent(cat);
		database.makePersistent(snake);
		tx.commit();

		tx = odmg.newTransaction();
		tx.begin();
		List animals = (List) query.execute();
		tx.commit();
		assertEquals(before + 3, animals.size());
	}

	public void testPrefetchQuery() throws Exception
	{
		String oql = "select allProductGroups from " + ProductGroup.class.getName()
                + " where groupId <= $1 order by groupId prefetch allArticlesInGroup";
		OQLQuery query = odmg.newOQLQuery();
		query.create(oql);
		query.bind(new Integer(5));
		Transaction tx = odmg.newTransaction();
		tx.begin();
		Collection results = (Collection) query.execute();
		tx.commit();
		assertNotNull(results);
		assertTrue(results.size() > 0);
	}

	public void testInterfaceQuery() throws Exception
	{
		int age = (int)(Math.random() * Integer.MAX_VALUE);
		int calories = (int)(Math.random() * Integer.MAX_VALUE);
		int caloriesOther = (int)(Math.random() * Integer.MAX_VALUE);

        //objects that are part of a 1:n relation, i.e. they have fk-fields
		Mammal elephant = new Mammal(age, "Jumbo", 4);
		Mammal cat = new Mammal(age, "Silvester", 4);
		Reptile snake = new Reptile(age, "Kaa", "green");

		//objects that are independent or part of m:n relations, i.e. they
		//don't have fk-fields
		Fish tuna = new Fish("tuna", calories, "salt");
		Fish trout = new Fish("trout", calories, "fresh water");

		Salad radiccio = new Salad("Radiccio", calories, "red");
		Salad lolloverde = new Salad("Lollo verde", caloriesOther, "green");

		deleteData(InterfaceAnimal.class);
		deleteData(InterfaceFood.class);

		Transaction tx = odmg.newTransaction();
		tx.begin();
		database.makePersistent(elephant);
		database.makePersistent(cat);
		database.makePersistent(snake);
		database.makePersistent(tuna);
		database.makePersistent(trout);
		database.makePersistent(radiccio);
		database.makePersistent(lolloverde);
		tx.commit();

		tx = odmg.newTransaction();
		tx.begin();
		OQLQuery query = odmg.newOQLQuery();
		query.create("select animals from " + InterfaceAnimal.class.getName() +
					 " where age=$1");
		query.bind(new Integer(age));
		List animals = (List) query.execute();
		tx.commit();
		Iterator it = animals.iterator();
		while (it.hasNext())
		{
			Object obj = it.next();
            // System.out.println(obj);
		}
		assertEquals(3, animals.size());

		//test independent objects
		query = odmg.newOQLQuery();
		tx.begin();
		query.create("select food from " + InterfaceFood.class.getName() +
					 " where calories=$1");
		query.bind(new Integer(calories));
		List food = (List) query.execute();
		tx.commit();
		assertEquals(3, food.size());
	}

    /**
     *
     */
    public void _testFunctions() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select anArticle from " +
                Article.class.getName() +
                " where upper(articleName) like \"A%\" ");

        List results = (List) query.execute();
        tx.commit();
        assertTrue(results.size() > 0);
    }

    /**
     * ReportQuery returning rows with summed stock and price per article group
     */
    public void testReportQueryGroupBy() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select p.groupName, p.allArticlesInGroup.stock, p.allArticlesInGroup.price" +
               " from " + ProductGroup.class.getName() +
               " group by groupName, allArticlesInGroup.stock, allArticlesInGroup.price");

//        query.create("select p.groupName, sum(p.allArticlesInGroup.stock), sum(p.allArticlesInGroup.price)" +
//               " from " + ProductGroup.class.getName() +
//               "by groupName, allArticlesInGroup.stock, allArticlesInGroup.price");

        List results = (List) query.execute();
        tx.commit();
        assertTrue(results.size() > 0);
    }

    public void testRepeatableQuery() throws Exception
    {
        deleteData(Person.class);
        createPersons();

        // 3. Get a list of some articles
        Transaction tx = odmg.newTransaction();
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        String sql = "select allPersons from " + Person.class.getName() + " where id != $1 and id > $2";
        query.create(sql);
        query.bind(new Integer(5));
        query.bind(new Integer(id_filter));

        Collection allPersons = (Collection) query.execute();
        // Iterator over the restricted articles objects
        Iterator it = allPersons.iterator();
        while (it.hasNext())
        {
            /**
             * just make sure it's a string.
             */
            Object result = it.next();
            Person value = (Person) result;
            if (value.getId() <= id_filter || value.getId()==5)
                fail("oql didn't filter, got id (" + value.getId() + " where it should have been over "
                        + id_filter + " and not 5");
        }
        tx.commit();

        // now we try to reuse OQLQuery
        tx.begin();

        query.bind(new Integer(8));
        query.bind(new Integer(id_filter));
        allPersons = (Collection) query.execute();
        // Iterator over the restricted articles objects
        it = allPersons.iterator();
        while (it.hasNext())
        {
            /**
             * just make sure it's a string.
             */
            Object result = it.next();
            Person value = (Person) result;
            if (value.getId() <= id_filter || value.getId()==8)
                fail("oql didn't filter, got id (" + value.getId() + " where it should have been over "
                        + id_filter + " and not 8");
        }

        // reuse OQLQuery within same tx
        query.bind(new Integer(9));
        query.bind(new Integer(id_filter));
        allPersons = (Collection) query.execute();
        // Iterator over the restricted articles objects
        it = allPersons.iterator();
        while (it.hasNext())
        {
            /**
             * just make sure it's a string.
             */
            Object result = it.next();
            Person value = (Person) result;
            if (value.getId() <= id_filter || value.getId()==9)
                fail("oql didn't filter, got id (" + value.getId() + " where it should have been over "
                        + id_filter + " and not 9");
        }
        tx.commit();
    }


    /**
     * test Subquery
     * get all articles with price > avg(price)
     * PROBLEM: avg(price) is NOT extent aware !!
     *
     * test may fail if db does not support sub queries
     */
    public void _testSubQuery1() throws Exception
    {
        Transaction tx = odmg.newTransaction();
        tx.begin();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select anArticle from " +
                Article.class.getName() +
                " where " +
                " price >= (select avg(price) from " +
                Article.class.getName() +
                " where articleName like \"A%\") ");

        List results = (List) query.execute();
        tx.commit();
        assertTrue(results.size() > 0);
    }


    //*******************************************************************
    // test classes start here
    //*******************************************************************
    public interface InterfaceAnimal extends Serializable
    {
        int getAge();
        String getName();
    }

    public interface InterfaceFood extends Serializable
    {
        String getName();
        int getCalories();

    }

    public static abstract class AbstractAnimal
    {
        int animalId;
        String name;
        Integer zooId;

        public int getAnimalId()
        {
            return animalId;
        }

        public void setAnimalId(int animalId)
        {
            this.animalId = animalId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Integer getZooId()
        {
            return zooId;
        }

        public void setZooId(Integer zooId)
        {
            this.zooId = zooId;
        }
    }

    public static class Mammal extends AbstractAnimal implements InterfaceAnimal, Serializable
    {
        private int age;
        private int numLegs;

        public Mammal()
        {
            super();
        }

        public Mammal(int age, String name, int numLegs)
        {
            this.age = age;
            this.name = name;
            this.numLegs = numLegs;
        }

        public String toString()
        {
            return "Mammal: id = " + animalId + "\n name = " + name +
                    "\n age = " + age +
                    "\n Number of legs = " + numLegs +
                    "\n zooId = " + zooId;
        }

        public int getAge()
        {
            return age;
        }

        public int getNumLegs()
        {
            return numLegs;
        }

        public void setNumLegs(int numLegs)
        {
            this.numLegs = numLegs;
        }
    }


    public static class Reptile extends AbstractAnimal implements InterfaceAnimal, Serializable
    {
        private int age;
        private String color;
        /**
         * Constructor for Plant.
         */
        public Reptile()
        {
            super();
        }

        public Reptile(int age, String name, String color)
        {
         this.age = age;
         this.name = name;
         this.color = color;
        }

        public int getAge()
        {
            return age;
        }

        public int getAnimalId()
        {
            return animalId;
        }

        public void setAnimalId(int animalId)
        {
            this.animalId = animalId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getColor()
        {
            return color;
        }

        public void setColor(String color)
        {
            this.color = color;
        }

        public String toString()
        {
            return "Reptile: id = " + animalId + "\n name = " + name +
                    "\n age = " + age +
                    "\n color = " + color +
                    "\n zooId = " + zooId;
        }
    }


    public static class Fish implements InterfaceFood, Serializable
    {
        int foodId;
        String name;
        int calories;
        String typeOfWater;

        /**
         * Constructor for Fish.
         */
        public Fish()
        {
            super();
        }

        public Fish(String name, int calories, String typeOfWater)
        {
            this.calories = calories;
            this.name = name;
            this.typeOfWater = typeOfWater;
        }

        /**
         * @see org.apache.ojb.broker.InterfaceFood#getName()
         */
        public String getName()
        {
            return name;
        }

        /**
         * @see org.apache.ojb.broker.InterfaceFood#getCalories()
         */
        public int getCalories()
        {
            return calories;
        }



        /**
         * Returns the typeOfWater.
         * @return String
         */
        public String getTypeOfWater()
        {
            return typeOfWater;
        }

        /**
         * Returns the foodId.
         * @return int
         */
        public int getFoodId()
        {
            return foodId;
        }

        public String toString()
        {
           return "Fish: id = " + foodId + "\n name = " + name +
                    "\n calories = " + calories +
                    "\n Type of water = " + typeOfWater;
        }


        /**
         * Sets the calories.
         * @param calories The calories to set
         */
        public void setCalories(int calories)
        {
            this.calories = calories;
        }

        /**
         * Sets the foodId.
         * @param foodId The foodId to set
         */
        public void setFoodId(int foodId)
        {
            this.foodId = foodId;
        }

        /**
         * Sets the name.
         * @param name The name to set
         */
        public void setName(String name)
        {
            this.name = name;
        }

        /**
         * Sets the typeOfWater.
         * @param typeOfWater The typeOfWater to set
         */
        public void setTypeOfWater(String typeOfWater)
        {
            this.typeOfWater = typeOfWater;
        }

    }



    public static class Gourmet implements Serializable
    {
        int gourmetId;
        String name;
        List favoriteFood = new ArrayList();
        /**
         * Constructor for Gourmet.
         */
        public Gourmet()
        {
            super();
        }

        public Gourmet(String name)
        {
            this.name = name;
        }

        public List getFavoriteFood()
        {
            return favoriteFood;
        }

        public void addFavoriteFood(InterfaceFood food)
        {
            favoriteFood.add(food);
        }

        /**
         * Returns the gourmetId.
         * @return int
         */
        public int getGourmetId()
        {
            return gourmetId;
        }

        public String toString()
        {
         StringBuffer text = new StringBuffer("Gourmet: id = " + gourmetId + "\n");
         text.append("name = ");
         text.append(name);
         text.append("\nFavoriteFood:\n");
         for(Iterator it = favoriteFood.iterator(); it.hasNext();)
         {
            text.append(it.next().toString());
            text.append("\n-------\n");
         }
         return text.toString();
        }


        /**
         * Returns the name.
         * @return String
         */
        public String getName()
        {
            return name;
        }

        /**
         * Sets the favoriteFood.
         * @param favoriteFood The favoriteFood to set
         */
        public void setFavoriteFood(List favoriteFood)
        {
            this.favoriteFood = favoriteFood;
        }

        /**
         * Sets the gourmetId.
         * @param gourmetId The gourmetId to set
         */
        public void setGourmetId(int gourmetId)
        {
            this.gourmetId = gourmetId;
        }

        /**
         * Sets the name.
         * @param name The name to set
         */
        public void setName(String name)
        {
            this.name = name;
        }

    }


    public static class Salad implements InterfaceFood, Serializable
    {
        int foodId;
        String name;
        int calories;
        String color;
        /**
         * Constructor for Salad.
         */
        public Salad()
        {
            super();
        }

        public Salad(String name, int calories, String color)
        {
            this.name = name;
            this.calories = calories;
            this.color = color;
        }

        /**
         * @see org.apache.ojb.broker.InterfaceFood#getName()
         */
        public String getName()
        {
            return name;
        }

        /**
         * @see org.apache.ojb.broker.InterfaceFood#getCalories()
         */
        public int getCalories()
        {
            return calories;
        }



        /**
         * Returns the color.
         * @return String
         */
        public String getColor()
        {
            return color;
        }

        /**
         * Returns the foodId.
         * @return int
         */
        public int getFoodId()
        {
            return foodId;
        }

        public String toString()
        {
           return "Salad: id = " + foodId + "\n name = " + name +
                    "\n calories = " + calories +
                    "\n Color = " + color;
        }

        /**
         * Sets the calories.
         * @param calories The calories to set
         */
        public void setCalories(int calories)
        {
            this.calories = calories;
        }

        /**
         * Sets the color.
         * @param color The color to set
         */
        public void setColor(String color)
        {
            this.color = color;
        }

        /**
         * Sets the foodId.
         * @param foodId The foodId to set
         */
        public void setFoodId(int foodId)
        {
            this.foodId = foodId;
        }

        /**
         * Sets the name.
         * @param name The name to set
         */
        public void setName(String name)
        {
            this.name = name;
        }

    }

    public class Zoo implements Serializable
    {
        private int zooId;
        private String name;
        private List animals = new ArrayList();

        /**
         * Constructor for Zoo.
         */
        public Zoo()
        {
            super();
        }

        public Zoo(String name)
        {
            this.name = name;
        }

        public List getAnimals()
        {
            return animals;
        }

        public void addAnimal(InterfaceAnimal animal)
        {
            animals.add(animal);
        }

        public int getZooId()
        {
            return zooId;
        }

        public String toString()
        {
         StringBuffer text = new StringBuffer("Zoo: id = " + zooId + "\n");
         text.append("name = ");
         text.append(name);
         text.append("\nAnimals:\n");
         for(Iterator it = animals.iterator(); it.hasNext();)
         {
            text.append(it.next().toString());
            text.append("\n-------\n");
         }
         return text.toString();
        }

        /**
         * Returns the name.
         * @return String
         */
        public String getName()
        {
            return name;
        }

        /**
         * Sets the animals.
         * @param animals The animals to set
         */
        public void setAnimals(List animals)
        {
            this.animals = animals;
        }

        /**
         * Sets the name.
         * @param name The name to set
         */
        public void setName(String name)
        {
            this.name = name;
        }

        /**
         * Sets the zooId.
         * @param zooId The zooId to set
         */
        public void setZooId(int zooId)
        {
            this.zooId = zooId;
        }

    }


}
