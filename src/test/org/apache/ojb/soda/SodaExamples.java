package org.apache.ojb.soda;

import junit.framework.TestCase;
import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.odbms.ObjectSet;
import org.odbms.Query;

/**
 * Insert the type's description here.
 * Creation date: (06.12.2000 21:47:56)
 * @author Thomas Mahler
 */
public class SodaExamples extends TestCase
{
    PersistenceBroker broker;

    private static Class CLASS = SodaExamples.class;

    private Logger logger;

    /**
     * BrokerTests constructor comment.
     * @param name java.lang.String
     */
    public SodaExamples(String name)
    
    {
        super(name);
        logger = LoggerFactory.getLogger("soda");
    }

    /**
     * Insert the method's description here.
     * Creation date: (23.12.2000 18:30:38)
     * @param args java.lang.String[]
     */
    public static void main(String[] args)
    {
        String[] arr = { CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:58:53)
     */
    public void setUp() throws PBFactoryException
    {
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:59:14)
     */
    public void tearDown()
    {
		broker.close();
    }

	protected org.apache.ojb.broker.query.Query ojbQuery()
	{
	    Criteria crit = null;
		org.apache.ojb.broker.query.Query q = QueryFactory.newQuery(Article.class, crit);
		return q;   
	}

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:51:22)
     */
    public void testWithFakedQuery()
    {
        try
        {
            Query q = broker.query();
            // we are faking a soda query here:
            ((QueryImpl) q).setOjbQuery(ojbQuery());
            int limit = 13;
            q.limitSize(limit);
            ObjectSet oSet = q.execute();
            logger.info("Size of ObjectSet: " + oSet.size());
            assertEquals(limit,oSet.size());
            int count = 0;
            while (oSet.hasNext())
            {
                count++;
             	oSet.next();   
            }
            assertEquals(limit, count);
            oSet.reset();
            count = 0;
            while (oSet.hasNext())
            {
                count++;
             	oSet.next();   
            }
            assertEquals(limit, count);
            
        }
        catch (Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:51:22)
     */
    public void testWithFakedQueryPreEmpt()
    {
        try
        {
            Query q = broker.query();
            // we are faking a soda query here:
            ((QueryImpl) q).setOjbQuery(ojbQuery());
            int limit = 13;
            q.limitSize(limit);
            ObjectSet oSet = q.execute();
            logger.info("Size of ObjectSet: " + oSet.size());
            assertEquals(limit,oSet.size());
            int count = 0;
            for (int i=0; i<7; i++)
            {
                count++;
             	oSet.next(); 
            }
            oSet.reset();
            count = 0;
            while (oSet.hasNext())
            {
                count++;
             	oSet.next();   
            }
            assertEquals(limit, count);
            
        }
        catch (Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
    }

    /**
     * Insert the method's description here.
     * Creation date: (06.12.2000 21:51:22)
     */
    public void testWithFakedQueryPreEmptUnlimited()
    {
        try
        {
            Query q = broker.query();
            // we are faking a soda query here:
            ((QueryImpl) q).setOjbQuery(ojbQuery());
            
            ObjectSet oSet = q.execute();
            logger.info("Size of ObjectSet: " + oSet.size());
            
            int count = 0;
            for (int i=0; i<7; i++)
            {
                count++;
             	oSet.next(); 
            }
            oSet.reset();
            count = 0;
            while (oSet.hasNext())
            {
                count++;
             	oSet.next();   
            }
            assertEquals(oSet.size(), count);
            
        }
        catch (Throwable t)
        {
            logger.error(t);
            fail(t.getMessage());
        }
    }

}
