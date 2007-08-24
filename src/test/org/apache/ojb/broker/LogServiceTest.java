package org.apache.ojb.broker;

//JUNIT

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.ojb.broker.accesslayer.RsIterator;
import org.apache.ojb.broker.core.PersistenceBrokerImpl;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;


/**
 * This TestCase contains the OJB performance benchmarks for the
 * PersistenceBroker API.
 * @author Thomas Mahler
 */
public class LogServiceTest
        extends TestCase
{
    public static void main(String[] args)
    {
        String[] arr = {LogServiceTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public LogServiceTest(String name)

    {
        super(name);
    }

    /**
     * Return the Test
     */
    public static Test suite()
    {
        return new TestSuite(LogServiceTest.class);
    }


    public void testLogggers() throws Exception
    {
        String prefix = "Ignore this!! LOGGING TEST OUTPUT: ";
        Logger pbroker = LoggerFactory.getLogger(PersistenceBrokerImpl.class);
        Logger rsiterator = LoggerFactory.getLogger(RsIterator.class);
        Logger boot = LoggerFactory.getBootLogger();

        pbroker = LoggerFactory.getLogger(PersistenceBrokerImpl.class);
        rsiterator = LoggerFactory.getLogger(RsIterator.class);

        pbroker.debug(prefix + "Should be with DEBUG level");
        pbroker.info(prefix + "Should be with INFO level");
        pbroker.warn(prefix + "Should be with WARN level");

        rsiterator.debug(prefix + "Should be with DEBUG level");
        rsiterator.info(prefix + "Should be with INFO level");
        rsiterator.warn(prefix + "Should be with WARN level");

        boot.debug(prefix + "Should be with DEBUG level");
        boot.info(prefix + "Should be with INFO level");
        boot.warn(prefix + "Should be with WARN level");
    }
}
