package org.apache.ojb.broker.sequence;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.util.sequence.SequenceManager;
import org.apache.ojb.broker.util.sequence.SequenceManagerFactory;
import org.apache.ojb.broker.util.sequence.SequenceManagerException;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class, obtains sequence numbers from a SequenceManager
 * (in multi-threaded tests).
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SequenceManagerHandle.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class SequenceManagerHandle implements Runnable
{
    private PersistenceBroker broker;
    private int loops;
    private Class targetClass;
    private FieldDescriptor field;
    private List resultList;

    public SequenceManagerHandle(PersistenceBroker broker, Class targetClass, int loops)
    {
        this.broker = broker;
        this.targetClass = targetClass;
        this.field = broker.getClassDescriptor(targetClass).getAutoIncrementFields()[0];
        if(field == null)
        {
            String error = "No autoincrement field found for class "+targetClass+
            " using class descriptor from given broker: "+broker.getClassDescriptor(targetClass);
            throw new PersistenceBrokerException(error);
        }
        this.loops = loops;
        resultList = new ArrayList();
    }

    public void run()
    {
        SequenceManager sm = SequenceManagerFactory.getSequenceManager(broker);
        Object result;
        for (int i = 0; i < loops; i++)
        {
            try
            {
                result = sm.getUniqueValue(field);
                //System.err.println("result "+result);
                resultList.add(result);
                SequenceManagerTest.countKey();
            }
            catch (SequenceManagerException e)
            {
                // ignore
            }
        }
        SequenceManagerTest.addResultList(resultList);
    }
}
