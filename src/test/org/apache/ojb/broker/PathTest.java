package org.apache.ojb.broker;

/**
 * TestClasses for Per-Criteria-Path-Hints
 * @author PAW
 *
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.query.UserAlias;

public class PathTest extends TestCase
{

    private static Class CLASS = PathTest.class;
    private int COUNT = 10;
    private int id_filter = 10000;
    private PersistenceBroker broker = null;

    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:58:53)
     */
    public void setUp()
    {
        try
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        }
        catch (PBFactoryException e)
        {
        }
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:59:14)
     */
    public void tearDown()
    {
        broker.close();
    }

    public PathTest(String name)
    {
        super(name);
    }

    public void testDeleteData() throws Exception
    {
        broker.beginTransaction();
        Criteria crit = new Criteria();

        Query query = QueryFactory.newQuery(D.class, crit);
        Collection Ds = broker.getCollectionByQuery(query);
        for (Iterator iterator = Ds.iterator(); iterator.hasNext();)
        {
            broker.delete(iterator.next());
        }
        query = QueryFactory.newQuery(C.class, crit);
        Collection Cs = broker.getCollectionByQuery(query);
        for (Iterator iterator = Cs.iterator(); iterator.hasNext();)
        {
            broker.delete(iterator.next());
        }
        query = QueryFactory.newQuery(B.class, crit);
        Collection Bs = broker.getCollectionByQuery(query);
        for (Iterator iterator = Bs.iterator(); iterator.hasNext();)
        {
            broker.delete(iterator.next());
        }
        query = QueryFactory.newQuery(A.class, crit);
        Collection As = broker.getCollectionByQuery(query);
        for (Iterator iterator = As.iterator(); iterator.hasNext();)
        {
            broker.delete(iterator.next());
        }
        broker.commitTransaction();
    }

//    private static int NUM_A = 1;
//    private static int NUM_B_PER_A = 4;
//    private static int NUM_C_PER_B = 2;
//    private static int NUM_C1_PER_B = 3;
//    private static int NUM_D_PER_C = 1;

    private static int NUM_A = 3;
    private static int NUM_B_PER_A = 4;
    private static int NUM_C_PER_B = 2;
    private static int NUM_C1_PER_B = 3;
    private static int NUM_D_PER_C = 2;

    private static int A_OFFSET = 10000;
    private static int B_OFFSET = 1000;
    private static int C_OFFSET = 100;
    private static int D_OFFSET = 10;

    public void testCreateData() throws Exception
    {
        broker.beginTransaction();
        
        for (int ia = 0; ia < NUM_A; ia++)
        {
            A a = new A(A_OFFSET + A_OFFSET * ia);
            broker.store(a);
            System.out.println("A - " + a.getAAttrib());
            for (int ib = 0; ib < NUM_B_PER_A; ib++)
            {
                B b = new B(A_OFFSET + B_OFFSET * ib);
                b.setA(a);
                broker.store(b);
                System.out.println("\tB - " + b.getBAttrib());
                for (int ic = 0; ic < NUM_C_PER_B; ic++)
                {
                    C c = new C(A_OFFSET + B_OFFSET * ib + C_OFFSET * ic);
                    c.setB(b);
                    for (int id = 0; id < NUM_D_PER_C; id++)
                    {
                        D d = new D(A_OFFSET + B_OFFSET * ib + C_OFFSET * ic + D_OFFSET * id);
                        c.setD(d);
                        broker.store(d);
                        broker.store(c);
                        System.out.println("\t\tC - " + c.getCAttrib());
                        System.out.println("\t\t\tD - " + d.getDAttrib());
                    }
                }
                for (int ic = 0; ic < NUM_C1_PER_B; ic++)
                {
                    C1 c1 = new C1(A_OFFSET + B_OFFSET * ib + C_OFFSET * ic);
                    c1.setB(b);
                    for (int id = 0; id < NUM_D_PER_C; id++)
                    {
                        D d = new D(A_OFFSET + B_OFFSET * ib + C_OFFSET * ic + D_OFFSET * id);
                        c1.setD(d);
                        c1.setC1Attrib(c1.getCAttrib() + 1);
                        broker.store(d);
                        broker.store(c1);
                        System.out.println("\t\tC1 - " + c1.getC1Attrib());
                        System.out.println("\t\t\tD - " + d.getDAttrib());
                    }
                }
            }
        }
        
        broker.commitTransaction();

        broker.clearCache();

        Criteria crit = new Criteria();

        Query query = QueryFactory.newQuery(A.class, crit);
        Collection As = broker.getCollectionByQuery(query);
        assertEquals(NUM_A, As.size());

        query = QueryFactory.newQuery(B.class, crit);
        Collection Bs = broker.getCollectionByQuery(query);
        int numB = NUM_A * NUM_B_PER_A;
        assertEquals(numB, Bs.size());

        query = QueryFactory.newQuery(C.class, crit);
        Collection Cs = broker.getCollectionByQuery(query);
        int numC = numB * (NUM_C_PER_B + NUM_C1_PER_B);
        assertEquals(numC, Cs.size());

        query = QueryFactory.newQuery(D.class, crit);
        Collection Ds = broker.getCollectionByQuery(query);
        int numD = numC * NUM_D_PER_C;
        assertEquals(numD, Ds.size());

    }

//	This is the result of the above population for NUM_A = 1
//    
//	A - 10000
//		B - 10000
//			C - 10000
//				D - 10010
//			C - 10100
//				D - 10110
//			C1 - 10001
//				D - 10010
//			C1 - 10101
//				D - 10110
//			C1 - 10201
//				D - 10210
//		B - 11000
//			C - 11000
//				D - 11010
//			C - 11100
//				D - 11110
//			C1 - 11001
//				D - 11010
//			C1 - 11101
//				D - 11110
//			C1 - 11201
//				D - 11210
//		B - 12000
//			C - 12000
//				D - 12010
//			C - 12100
//				D - 12110
//			C1 - 12001
//				D - 12010
//			C1 - 12101
//				D - 12110
//			C1 - 12201
//				D - 12210
//		B - 13000
//			C - 13000
//				D - 13010
//			C - 13100
//				D - 13110
//			C1 - 13001
//				D - 13010
//			C1 - 13101
//				D - 13110
//			C1 - 13201
//				D - 13210

	/*
	 *  Find all Bs having a particular C1 (c1)
	 *  Works
	 */
	public void testPathClassOnSegment1() throws Exception
	{
		try
		{
			// c1 criteria 
			Criteria crit1 = new Criteria();
			crit1.addEqualTo("cSet.c1Attrib", new Integer("10001"));
			crit1.addPathClass("cSet", C1.class);
			
			Query query = new QueryByCriteria(B.class, crit1);

			Collection allBs = broker.getCollectionByQuery(query);

			java.util.Iterator itr = allBs.iterator();

			assertEquals(1 * NUM_A, allBs.size());
			System.out.println("testPathClassOnSegment1() iteration size:" + allBs.size());
			while (itr.hasNext())
			{
				B b = (B) itr.next();
				System.out.println("Found B:  " + b.getId() + " " + b.getBAttrib());
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			fail("testPathClassOnSegment1: " + t.getMessage());
		}
	}
	
	/*
	 *  Find all As having a particular C1 (c1)
	 *  Works
	 */
	public void testPathClassOnSegment2() throws Exception
	{
		try
		{
			// c1 criteria 
			Criteria crit1 = new Criteria();
			crit1.addEqualTo("bSet.cSet.c1Attrib", new Integer("10001"));
			crit1.addPathClass("bSet.cSet", C1.class);
			
			Query query = new QueryByCriteria(A.class, crit1);

			Collection allAs = broker.getCollectionByQuery(query);

			java.util.Iterator itr = allAs.iterator();

			assertEquals(allAs.size(), 1 * NUM_A);
			System.out.println("testPathClassOnSegment2() iteration size:" + allAs.size());
			while (itr.hasNext())
			{
				A a = (A) itr.next();
				System.out.println("Found A:  " + a.getId() + " " + a.getAAttrib());
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			fail("testPathClassOnSegment2: " + t.getMessage());
		}
	}
	
	/*
	 *  Find all Bs having a C with a particular D (d1)
	 *  Works
	 */
	public void testSingleAlias() throws Exception
	{
		try
		{
			// d1 criteria 
			Criteria crit1 = new Criteria();
			crit1.setAlias("cToD1", "cSet");  // unnecessary, but its use should not
											// cause incorrect results
			crit1.addEqualTo("cSet.d.dAttrib", new Integer("10010"));

			Query query = new QueryByCriteria(B.class, crit1);

			Collection allBs = broker.getCollectionByQuery(query);

			java.util.Iterator itr = allBs.iterator();

			assertEquals(2 * NUM_A, allBs.size());
			System.out.println("testSingleAlias() iteration size:" + allBs.size());
			while (itr.hasNext())
			{
				B b = (B) itr.next();
				System.out.println("Found B:  " + b.getId() + " " + b.getBAttrib());
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			fail("testSingleAlias: " + t.getMessage());
		}
	}
	
	/*
	 *  Find all Bs having any C with a particular D (d1) and any C with a particular D (d2)
	 *  Works
	 */
	public void testTwoAliasesTwoSegments() throws Exception
	{
		try
		{
			// d1 criteria 
			Criteria crit1 = new Criteria();
			crit1.setAlias("cToD1", "cSet.d");
			crit1.addEqualTo("cSet.d.dAttrib", new Integer("10010"));

			// d2 criteria
			Criteria crit2 = new Criteria();
			crit2.setAlias("cToD2", "cSet.d");
			crit2.addEqualTo("cSet.d.dAttrib", new Integer("10110"));

			crit1.addAndCriteria(crit2);

			Query query = new QueryByCriteria(B.class, crit1);

			Collection allBs = broker.getCollectionByQuery(query);

			java.util.Iterator itr = allBs.iterator();

			assertEquals(4 * NUM_A, allBs.size());
			System.out.println("testTwoAliasesTwoSegments() iteration size:" + allBs.size());
			while (itr.hasNext())
			{
				B b = (B) itr.next();
				System.out.println("Found B:  " + b.getId() + " " + b.getBAttrib());
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			fail("testTwoAliasesTwoSegments: " + t.getMessage());
		}
	}
	
	/*
	 *  Find all As having any B with any C with a particular D (d1) and any C with a particular D (d2)
	 *  Works
	 */
	public void testTwoAliasesThreeSegments() throws Exception
	{
		try
		{
			// d1 criteria 
			Criteria crit1 = new Criteria();
			crit1.addEqualTo("bSet.cSet.d.dAttrib", new Integer("10010"));
			crit1.setAlias("bToCToD1", "cSet.d");

			// d2 criteria
			Criteria crit2 = new Criteria();
			crit2.addEqualTo("bSet.cSet.d.dAttrib", new Integer("10110"));
			crit2.setAlias("bToCToD2", "cSet.d");

			crit1.addAndCriteria(crit2);

			boolean isDistinct = true;
			Query query = new QueryByCriteria(A.class, crit1, true);

			Collection allAs = broker.getCollectionByQuery(query);

			java.util.Iterator itr = allAs.iterator();

			assertEquals(1 * NUM_A, allAs.size());
			System.out.println("testTwoAliasesThreeSegments() iteration size:" + allAs.size());
			while (itr.hasNext())
			{
				A a = (A) itr.next();
				System.out.println("Found A:  " + a.getId() + " " + a.getAAttrib());
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			fail("testTwoAliasesThreeSegments: " + t.getMessage());
		}
	}	
	
	/*
	 *  Find all Bs having a particular C (c) and a particular C1 (c1)
	 */
    public void testPathClassPerCriteria() throws Exception
    {
        try
        {
            // C criteria
            Criteria crit1 = new Criteria();
            crit1.addEqualTo("cSet.cAttrib", new Integer("10200"));
            crit1.addPathClass("cSet", C.class);
			crit1.setAlias("alias1");

            // C1 criteria (subclass of C)
            Criteria crit2 = new Criteria();
            crit2.addEqualTo("cSet.c1Attrib", new Integer("10001"));
            crit2.addPathClass("cSet", C1.class);
			crit2.setAlias("alias2");

            crit1.addAndCriteria(crit2);

            Query query = new QueryByCriteria(B.class, crit1);

            Collection allBs = broker.getCollectionByQuery(query);

            java.util.Iterator itr = allBs.iterator();

            assertEquals(1 * NUM_A, allBs.size());
            System.out.println("testPathClassPerCriteria() iteration size:" + allBs.size());
            while (itr.hasNext())
            {
                B b = (B) itr.next();
                System.out.println("Found B:  " + b.getId() + " " + b.getBAttrib());
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.out);
            fail("testPathClassPerCriteria: " + t.getMessage());
        }
    }
    
	/*
	 *  Find all Bs having a particular C1 (c1_a) and a particular C3 (c1_b)
	 *  Works
	 */
	public void testPathClassPerQuery() throws Exception
	{
		try
		{
			// c1_a criteria 
			Criteria crit1 = new Criteria();
			crit1.addEqualTo("cSet.c1Attrib", new Integer("12001"));
			//crit1.addPathClass("cSet", C1.class); // can use 1 query setting instead
			crit1.setAlias("alias1");

			// c2_b criteria
			Criteria crit2 = new Criteria();
			crit2.addEqualTo("cSet.c1Attrib", new Integer("12101"));
			//crit2.addPathClass("cSet", C1.class); // can use 1 query setting instead
			crit2.setAlias("alias2");

			crit1.addAndCriteria(crit2);

			QueryByCriteria query = new QueryByCriteria(B.class, crit1);
			query.addPathClass("cSet", C1.class);

			Collection allBs = broker.getCollectionByQuery(query);

			java.util.Iterator itr = allBs.iterator();

			assertEquals(1 * NUM_A, allBs.size());
			System.out.println("testPathClassPerQuery() iteration size:" + allBs.size());
			while (itr.hasNext())
			{
				B b = (B) itr.next();
				System.out.println("Found B:  " + b.getId() + " " + b.getBAttrib());
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			fail("testPathClassPerQuery: " + t.getMessage());
		}
	}
	
	/*
	 *  Find all As having a B with both a particular C-D combination and 
	 *  another particular C-D combination
	 */
	public void testThreeSegmentsAliasOnSegment2And3() throws Exception
	{
		try
		{
			// d1 criteria 
			Criteria crit1 = new Criteria();
			crit1.addEqualTo("bSet.cSet.d.dAttrib", new Integer("10010"));
			crit1.setAlias("bToCToD1", "cSet.d");

			// d2 criteria
			Criteria crit2 = new Criteria();
			crit2.addEqualTo("bSet.cSet.d.dAttrib", new Integer("10110"));
			crit2.setAlias("bToCToD2", "cSet.d");

			crit1.addAndCriteria(crit2);

			boolean isDistinct = true;
			Query query = new QueryByCriteria(A.class, crit1, isDistinct);

			Collection allAs = broker.getCollectionByQuery(query);

			java.util.Iterator itr = allAs.iterator();

			assertEquals(1 * NUM_A, allAs.size());
			System.out.println("testThreeSegmentsAliasOnSegment2And3() iteration size:" + allAs.size());
			while (itr.hasNext())
			{
				A a = (A) itr.next();
				System.out.println("Found A:  " + a.getId() + " " + a.getAAttrib());
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			fail("testThreeSegmentsAliasOnSegment2And3: " + t.getMessage());
		}
	}	
	
	/*
	 *  Same as above using an explicit UserAlias
	 */
	public void testThreeSegmentsAliasOnSegment2And3UserAlias() throws Exception
	{
		try
		{
			UserAlias userAlias1 = new UserAlias("alias1");
			userAlias1.add("bSet.cSet");
			userAlias1.add("bSet.cSet.d");
			
			// d1 criteria 
			Criteria crit1 = new Criteria();
			crit1.addEqualTo("bSet.cSet.d.dAttrib", new Integer("10010"));
			crit1.setAlias(userAlias1);

			// d2 criteria
			UserAlias userAlias2 = new UserAlias("alias2");
			userAlias2.add("bSet.cSet");
			userAlias2.add("bSet.cSet.d");

			Criteria crit2 = new Criteria();
			crit2.addEqualTo("bSet.cSet.d.dAttrib", new Integer("10110"));
			crit2.setAlias(userAlias2);

			crit1.addAndCriteria(crit2);

			boolean isDistinct = true;
			Query query = new QueryByCriteria(A.class, crit1, isDistinct);

			Collection allAs = broker.getCollectionByQuery(query);

			java.util.Iterator itr = allAs.iterator();

			assertEquals(1 * NUM_A, allAs.size());
			System.out.println("testThreeSegmentsAliasOnSegment2And3UserAlias() iteration size:" + allAs.size());
			while (itr.hasNext())
			{
				A a = (A) itr.next();
				System.out.println("Found A:  " + a.getId() + " " + a.getAAttrib());
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			fail("testThreeSegmentsAliasOnSegment2And3UserAlias: " + t.getMessage());
		}
	}	
	
	public void testSubQueryExists()
    {
        // subquery
        Criteria subCrit = new Criteria();
        subCrit.addEqualTo(Criteria.PARENT_QUERY_PREFIX + "bSet.cSet.d.dAttrib", new Integer("10011"));
        //	    subCrit.setAlias("subAlias", "cSet.d");
        QueryByCriteria subQuery = new QueryByCriteria(A.class, subCrit);

        // parent query
        Criteria crit = new Criteria();
        crit.addEqualTo("bSet.cSet.d.dAttrib", new Integer("10010"));
        //	    crit.setAlias("alias", "cSet.d");
        crit.addNotExists(subQuery);
        QueryByCriteria query = new QueryByCriteria(A.class, crit,true);

        Collection result = broker.getCollectionByQuery(query);
		assertEquals(1 * NUM_A, result.size());
		
		Iterator itr = result.iterator();
		while (itr.hasNext())
		{
			A a = (A) itr.next();
			System.out.println("Found A:  " + a.getId() + " " + a.getAAttrib());
		}
    }
	
    // Inner Classes

    public static class A
    {
        private long id;
        private int aAttrib;
        private Collection bSet;

        public A()
        {
        }

        public A(int aAttrib)
        {
            this.aAttrib = aAttrib;
            this.bSet = new ArrayList();
        }

        // make javabeans conform
        public int getaAttrib()
        {
            return aAttrib;
        }
        // make javabeans conform
        public Collection getbSet()
        {
            return bSet;
        }
        // make javabeans conform
        public void setbSet(Collection bSet)
        {
            this.bSet = bSet;
        }
        // make javabeans conform
        public void setaAttrib(int aAttrib)
        {
            this.aAttrib = aAttrib;
        }

        /**
         * @return
         */
        public int getAAttrib()
        {
            return aAttrib;
        }

        /**
         * @return
         */
        public Collection getBSet()
        {
            return bSet;
        }

        /**
         * @return
         */
        public long getId()
        {
            return id;
        }

        /**
         * @param i
         */
        public void setAAttrib(int i)
        {
            aAttrib = i;
        }

        /**
         * @param collection
         */
        public void setBSet(Collection collection)
        {
            bSet = collection;
        }

        /**
         * @param l
         */
        public void setId(long l)
        {
            id = l;
        }

    }

    public static class B
    {
        private long id;
        private long aId;
        private int bAttrib;
        private A a;
        private Collection cSet;

        public B()
        {
        }

        public B(int bAttrib)
        {
            this.bAttrib = bAttrib;
            this.cSet = new ArrayList();
        }

        /**
         * @return
         */
        public int getBAttrib()
        {
            return bAttrib;
        }

        /**
         * @return
         */
        public Collection getCSet()
        {
            return cSet;
        }

        /**
         * @return
         */
        public long getId()
        {
            return id;
        }

        /**
         * @param i
         */
        public void setBAttrib(int i)
        {
            bAttrib = i;
        }

        /**
         * @param collection
         */
        public void setCSet(Collection collection)
        {
            cSet = collection;
        }

        /**
         * @param l
         */
        public void setId(long l)
        {
            id = l;
        }

        /**
         * @return
         */
        public A getA()
        {
            return a;
        }

        /**
         * @param a
         */
        public void setA(A a)
        {
            this.a = a;
            a.getBSet().add(this);
        }

    }

    public static class C
    {
        private long id;
        private long bId;
        private B b;
        private long dId;
        private D d;
        private int cAttrib;

        public C()
        {
        }

        public C(int cAttrib)
        {
            this.cAttrib = cAttrib;
        }

        /**
         * @return
         */
        public int getCAttrib()
        {
            return cAttrib;
        }

        /**
         * @return
         */
        public D getD()
        {
            return d;
        }

        /**
         * @return
         */
        public long getId()
        {
            return id;
        }

        /**
         * @param i
         */
        public void setCAttrib(int i)
        {
            cAttrib = i;
        }

        /**
         * @param collection
         */
        public void setD(D d)
        {
            this.d = d;
        }

        /**
         * @param l
         */
        public void setId(long l)
        {
            id = l;
        }

        /**
         * @return
         */
        public B getB()
        {
            return b;
        }

        /**
         * @param b
         */
        public void setB(B b)
        {
            this.b = b;
            b.getCSet().add(this);
        }

    }

    public static class C1 extends C
    {

        private int c1Attrib;

        /**
         * @param cAttrib
         */

        public C1()
        {
        }

        public C1(int cAttrib)
        {
            super(cAttrib);
        }

        /**
         * @return
         */
        public int getC1Attrib()
        {
            return c1Attrib;
        }

        /**
         * @param i
         */
        public void setC1Attrib(int i)
        {
            c1Attrib = i;
        }

    }

    public static class D
    {
        private long id;
        private int dAttrib;

        public D()
        {
        }

        public D(int dAttrib)
        {
            this.dAttrib = dAttrib;
        }

        /**
         * @return
         */
        public int getDAttrib()
        {
            return dAttrib;
        }

        /**
         * @return
         */
        public long getId()
        {
            return id;
        }

        /**
         * @param i
         */
        public void setDAttrib(int i)
        {
            dAttrib = i;
        }

        /**
         * @param l
         */
        public void setId(long l)
        {
            id = l;
        }

    }

}

