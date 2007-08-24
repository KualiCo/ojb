package org.apache.ojb.otm;

/**
 * This is the base abstract class for all isolation TestSuites
 * based on the ODMG locking documentation
 */

import junit.framework.TestCase;
import org.apache.ojb.broker.Article;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.otm.core.Transaction;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.lock.ObjectLock;
import org.apache.ojb.otm.lock.isolation.TransactionIsolation;
import org.apache.ojb.otm.lock.wait.NoWaitStrategy;
import org.apache.ojb.otm.lock.wait.TimeoutStrategy;

public abstract class LockTestBase extends TestCase
{
    protected TestKit _kit;
    protected OTMConnection _conn1;
    protected OTMConnection _conn2;
    protected Transaction _tx1;
    protected Transaction _tx2;
    protected ObjectLock _lock;
    protected TransactionIsolation _isolation;

    public LockTestBase(String name)
    {
        super(name);
    }

    protected abstract TransactionIsolation newIsolation();

    public void setUp()
    {
        _kit = TestKit.getTestInstance();
        _kit.setLockWaitStrategy(new NoWaitStrategy());
        _isolation = newIsolation();
        try
        {
            _conn1 = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
            _tx1 = _kit.getTransaction(_conn1);
            _tx1.begin();

            _conn2 = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
            _tx2 = _kit.getTransaction(_conn2);
            _tx2.begin();

            Article obj =  Article.createInstance();
            _lock = new ObjectLock(_conn1.getIdentity(obj));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    protected boolean readLock(Transaction tx)
    {
        try
        {
            _isolation.readLock(tx, _lock);
            return true;
        }
        catch (LockingException ex)
        {
            return false;
        }
    }

    protected boolean writeLock(Transaction tx)
    {
        try
        {
            _isolation.writeLock(tx, _lock);
            return true;
        }
        catch (LockingException ex)
        {
            return false;
        }
    }

    protected boolean releaseLock(Transaction tx)
    {
        _lock.releaseLock(tx);
        return true;
    }

    public void tearDown()
    {
        try
        {
            _tx1.rollback();
	    _conn1.close();
            _conn1 = null;

            _tx2.rollback();
	    _conn2.close();
            _conn2 = null;
        }
        catch (Throwable t)
        {
        }
        _kit.setLockWaitStrategy(new TimeoutStrategy());
    }
}
