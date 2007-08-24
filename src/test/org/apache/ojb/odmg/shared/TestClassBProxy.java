package org.apache.ojb.odmg.shared;

import java.io.Serializable;

import org.apache.ojb.odmg.shared.TestClassB;

/**
 * This class is used to test the correct assignment
 * of foreign keys when the referenced object (i.e. this
 * class is a dynamic proxy)
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class TestClassBProxy extends TestClassB implements TestClassBProxyI, Serializable
{
    public TestClassBProxy()
    {
    }
}
