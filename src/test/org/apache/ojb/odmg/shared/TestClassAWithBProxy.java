package org.apache.ojb.odmg.shared;

import java.io.Serializable;

import org.apache.ojb.odmg.shared.TestClassA;

/**
 * This class is used in the TestCase for testing correct foreign-key assignment
 * when the referenced object happens to be a proxy (i.e. it was loaded before as
 * a proxy and is now being assigned to another object)
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class TestClassAWithBProxy extends TestClassA implements Serializable
{

    private transient TestClassBProxyI bp;

    public TestClassAWithBProxy()
    {
    }
    
    /**
     * Gets the bp.
     * @return Returns a TestClassBProxy
     */
    public TestClassBProxyI getBProxy()
    {
        return bp;
    }

    /**
     * Sets the bp.
     * @param bp The bp to set
     */
    public void setBProxy(TestClassBProxyI bp)
    {
        this.bp = bp;
    }
    
    /**
     * Returns the bp.
     * @return TestClassBProxyI
     */
    public TestClassBProxyI getBp()
    {
        return bp;
    }

    /**
     * Sets the bp.
     * @param bp The bp to set
     */
    public void setBp(TestClassBProxyI bp)
    {
        this.bp = bp;
    }

}
