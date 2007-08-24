package org.apache.ojb.broker;

import junit.framework.TestCase;
import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.accesslayer.ConnectionFactory;
import org.apache.ojb.broker.accesslayer.ConnectionFactoryDBCPImpl;
import org.apache.ojb.broker.accesslayer.ConnectionFactoryFactory;
import org.apache.ojb.broker.accesslayer.ConnectionFactoryPooledImpl;
import org.apache.ojb.broker.accesslayer.LookupException;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;

import java.sql.Connection;

/**
 * ConnectionFactory implementation related tests.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ConnectionFactoryTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class ConnectionFactoryTest extends TestCase
{
    PersistenceBroker broker;

    public ConnectionFactoryTest()
    {
    }

    public ConnectionFactoryTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {ConnectionFactoryTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Simple test to check base functionality of the
     * ConnectionFactory implementation
     */
    public void testConnectionFactoryPooledImpl() throws Exception
    {
        checkFactory(ConnectionFactoryPooledImpl.class);
    }

    /**
     * Simple test to check base functionality of the
     * ConnectionFactory implementation
     */
    public void testConnectionFactoryDBCPImpl() throws Exception
    {
        checkFactory(ConnectionFactoryDBCPImpl.class);
    }

    private void checkFactory(Class factory) throws Exception
    {
        Class oldFac = null;
        ConnectionFactoryFactory fac = null;
        try
        {
            fac = ConnectionFactoryFactory.getInstance();
            oldFac = fac.getClassToServe();
            fac.setClassToServe(factory);
            ConnectionFactory conFac = (ConnectionFactory) fac.createNewInstance();

            MetadataManager mm = MetadataManager.getInstance();
            JdbcConnectionDescriptor jcd = (JdbcConnectionDescriptor) SerializationUtils.clone(
                    broker.serviceConnectionManager().getConnectionDescriptor());
            jcd.setJcdAlias(factory.getName() + "_test_checkFactory_a");
            jcd.setUseAutoCommit(2);
            // use this attribute to allow OJB changing initial state of connections
            jcd.addAttribute("initializationCheck", "true");

            mm.connectionRepository().addDescriptor(jcd);
            Connection con = conFac.lookupConnection(jcd);
            Connection con2 = conFac.lookupConnection(jcd);
            Connection con3 = conFac.lookupConnection(jcd);
            assertFalse("Expect autocommit state false", con.getAutoCommit());
            con.close();
            con2.close();
            con3.close();


            conFac = (ConnectionFactory) fac.createNewInstance();

            jcd = (JdbcConnectionDescriptor) SerializationUtils.clone(
                    broker.serviceConnectionManager().getConnectionDescriptor());
            jcd.setJcdAlias(factory.getName() + "_test_checkFactory_b");
            jcd.setUseAutoCommit(1);

            mm.connectionRepository().addDescriptor(jcd);
            con = conFac.lookupConnection(jcd);
            assertTrue("Expect autocommit state true", con.getAutoCommit());
        }
        finally
        {
            if (oldFac != null) fac.setClassToServe(oldFac);
        }
    }

    public void testExhaustedPoolConFacPooledImpl() throws Exception
    {
        checkFactoryPoolExhausted(ConnectionFactoryPooledImpl.class);
    }

    public void testExhaustedPoolConFacDBCPImpl() throws Exception
    {
        checkFactoryPoolExhausted(ConnectionFactoryDBCPImpl.class);
    }

    private void checkFactoryPoolExhausted(Class factory) throws Exception
    {
        Class oldFac = null;
        ConnectionFactoryFactory fac = null;
        try
        {
            fac = ConnectionFactoryFactory.getInstance();
            oldFac = fac.getClassToServe();
            fac.setClassToServe(factory);
            ConnectionFactory conFac = (ConnectionFactory) fac.createNewInstance();

            MetadataManager mm = MetadataManager.getInstance();
            JdbcConnectionDescriptor jcd = (JdbcConnectionDescriptor) SerializationUtils.clone(
                    broker.serviceConnectionManager().getConnectionDescriptor());
            jcd.setJcdAlias(factory.getName() + "_test_checkFactoryPoolExhausted_1");
            jcd.setUseAutoCommit(1);
            jcd.getConnectionPoolDescriptor().setMaxActive(2);
            jcd.getConnectionPoolDescriptor().setConnectionFactory(factory);
            mm.connectionRepository().addDescriptor(jcd);

            Connection con = null;
            Connection con2 = null;
            Connection con3 = null;
            try
            {
                con = conFac.lookupConnection(jcd);
                con2 = conFac.lookupConnection(jcd);
                try
                {
                    con3 = conFac.lookupConnection(jcd);
                    fail("We expect an exception indicating that the pool is exhausted");
                }
                catch (LookupException e)
                {
                    // we expected that
                    assertTrue(true);
                }
            }
            finally
            {
                try
                {
                    con.close();
                    con2.close();
                }
                catch (Exception e)
                {
                }
            }
        }
        finally
        {
            if (oldFac != null) fac.setClassToServe(oldFac);
        }
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
        try
        {
            broker.close();
        }
        catch (PersistenceBrokerException e)
        {
        }
    }
}
