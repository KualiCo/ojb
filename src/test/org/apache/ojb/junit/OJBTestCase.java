package org.apache.ojb.junit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;

/**
 * Extension of the JUnit test class.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: OJBTestCase.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class OJBTestCase extends TestCase
{
    private static final String SKIP_STR = "OJB.skip.issues";
    private static final String SKIP_DEFAULT_VALUE = "false";
    private MetadataHelper referenceHelper;

    public OJBTestCase()
    {
    }

    public OJBTestCase(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        // sleep thread to guarantee different timestamp values for
        // each test
        ojbSleep();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        if(referenceHelper != null)
        {
            PersistenceBroker temp = PersistenceBrokerFactory.defaultPersistenceBroker();
            try
            {
                referenceHelper.restoreMetadataSettings(temp);
            }
            finally
            {
                if(temp != null)
                {
                    temp.close();
                }
            }
        }
    }

    /**
     * Sleep current thread for a minimal period.
     */
    public void ojbSleep()
    {
        try
        {
            // most systems has system time precision of 10 msec
            // so wait to guarantee new system time value for each test.
            Thread.sleep(11);
        }
        catch (InterruptedException ignore)
        {
        }
    }

    /**
     * This method could be used to print a message before skip 'problematic' test cases.
     */
    public void ojbSkipTestMessage(String message)
    {
        if(message == null)
        {
           message = "No description, please see test case";
        }
        String className = this.getClass().getName();
        System.out.println("# [Skip test in " + className + "] " + message + " #");
    }

    /**
     * This method could be used to skip 'problematic' test cases or known issues before
     * a release was made. To enable the skipped tests set a system property 'skip.issues'
     * to <tt>false</tt>.
     */
    public boolean ojbSkipKnownIssueProblem()
    {
        return ojbSkipKnownIssueProblem(null);
    }

    /**
     * This method could be used to skip 'problematic' test cases or known issues before
     * a release was made. To enable the skipped tests set a system property 'skip.issues'
     * to <tt>false</tt>.
     */
    public boolean ojbSkipKnownIssueProblem(String message)
    {
        String result = SKIP_DEFAULT_VALUE;
        boolean skip = false;
        try
        {
            result = System.getProperty(SKIP_STR, result);
            skip = new Boolean(result).booleanValue();
        }
        catch(Exception e)
        {
            System.err.println("Seems that system property '" + SKIP_STR + "=" + result + "' is not a valid boolean value");
        }
        if(skip)
        {
            if(message == null)
            {
               message = "No description, please see test case";
            }
            String className = this.getClass().getName();
            System.out.println("# [Skip known issue in " + className + "] " + message + " #");
        }
        return skip;
    }

    /**
     * Allows to do a global change of object/collection reference settings. When the test
     * is tear down the old settings will be restored. Be careful when override setUp/tearDown method, don't
     * forget the "super call", else this method couldn't work properly.
     *
     * @param clazz
     * @param referenceField
     * @param autoRetrieve
     * @param autoUpdate
     * @param autoDelete
     * @param useProxy
     */
    public void ojbChangeReferenceSetting(Class clazz, String referenceField, boolean autoRetrieve, int autoUpdate, int autoDelete, boolean useProxy)
    {
        if(referenceHelper == null)
        {
            referenceHelper = new MetadataHelper();
        }
        PersistenceBroker temp = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            referenceHelper.changeReferenceSetting(temp, clazz, referenceField, autoRetrieve, autoUpdate, autoDelete, useProxy);
        }
        finally
        {
            if(temp != null)
            {
                temp.close();
            }
        }
    }

    /**
     * Allows to do a global change of object/collection reference settings. When the test
     * is tear down the old settings will be restored. Be careful when override setUp/tearDown method, don't
     * forget the "super call", else this method couldn't work properly.
     *
     * @param clazz
     * @param referenceField
     * @param autoRetrieve
     * @param autoUpdate
     * @param autoDelete
     * @param useProxy
     */
    public void ojbChangeReferenceSetting(Class clazz, String referenceField, boolean autoRetrieve, boolean autoUpdate, boolean autoDelete, boolean useProxy)
    {
        if(referenceHelper == null)
        {
            referenceHelper = new MetadataHelper();
        }
        PersistenceBroker temp = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            referenceHelper.changeReferenceSetting(temp, clazz, referenceField, autoRetrieve, autoUpdate, autoDelete, useProxy);
        }
        finally
        {
            if(temp != null)
            {
                temp.close();
            }
        }
    }



    //================================================================
    // inner class
    //================================================================
    /**
     * Class that help us to do changes on metadata and restore old state on
     * tear down of the test.
     * NOTE: This strategy is not recommended in production application because
     * the made changes will be global and all threads will recognize them immediately.
     *
     */
    public class MetadataHelper
    {
        private Map oldSettings;

        public MetadataHelper()
        {
            oldSettings = new HashMap();
        }

        protected void restoreMetadataSettings(PersistenceBroker broker)
        {
            if(oldSettings.size() == 0) return;
            Iterator it = oldSettings.entrySet().iterator();
            Map.Entry entry;
            while(it.hasNext())
            {
                entry =  (Map.Entry) it.next();
                String clazz =  (String) entry.getKey();
                Map fieldMap = (Map) entry.getValue();
                Iterator iter = fieldMap.entrySet().iterator();
                Map.Entry entry2;
                ClassDescriptor cld = broker.getDescriptorRepository().getDescriptorFor(clazz);
                while(iter.hasNext())
                {
                    entry2 = (Map.Entry) iter.next();
                    String oldRefName = (String) entry2.getKey();
                    ObjectReferenceDescriptor oldRef = (ObjectReferenceDescriptor) entry2.getValue();
                    // lookup single object or collection descriptor
                    ObjectReferenceDescriptor ref = cld.getCollectionDescriptorByName(oldRefName);
                    if(ref == null) ref = cld.getObjectReferenceDescriptorByName(oldRefName);

//                    System.out.println("Restoring metadata for " + clazz
//                            + " from " + ref.toXML()
//                            + " === to ===> " + oldRef.toXML());
                    ref.setCascadeRetrieve(oldRef.getCascadeRetrieve());
                    ref.setCascadingStore(oldRef.getCascadingStore());
                    ref.setCascadingDelete(oldRef.getCascadingDelete());
                    ref.setLazy(oldRef.isLazy());
//                    System.out.println("Restore metadata for " + clazz
//                            + " to " + ref.toXML());
                }
            }
            oldSettings.clear();
        }

        public void changeReferenceSetting(PersistenceBroker broker, Class clazz,
                                           String referenceField, boolean autoRetrieve,
                                           int autoUpdate, int autoDelete, boolean useProxy)
        {
            ClassDescriptor cld = broker.getClassDescriptor(clazz);
            ObjectReferenceDescriptor ref = cld.getCollectionDescriptorByName(referenceField);
            ref = cld.getCollectionDescriptorByName(referenceField);
            if(ref == null) ref = cld.getObjectReferenceDescriptorByName(referenceField);
            if(ref == null)
            {
                throw new OJBRuntimeException("Given field " + referenceField + " does not match a reference in " + clazz);
            }

            prepareSetting(ref, cld, clazz, referenceField);

            ref.setLazy(useProxy);
            ref.setCascadeRetrieve(autoRetrieve);
            ref.setCascadingStore(autoUpdate);
            ref.setCascadingDelete(autoDelete);

//            System.out.println("old settings: " + oldRef.toXML());
//            System.out.println("new settings: " + ref.toXML());
        }

        public void changeReferenceSetting(PersistenceBroker broker, Class clazz,
                                           String referenceField, boolean autoRetrieve,
                                           boolean autoUpdate, boolean autoDelete, boolean useProxy)
        {
            ClassDescriptor cld = broker.getClassDescriptor(clazz);
            ObjectReferenceDescriptor ref = cld.getCollectionDescriptorByName(referenceField);
            ref = cld.getCollectionDescriptorByName(referenceField);
            if(ref == null) ref = cld.getObjectReferenceDescriptorByName(referenceField);
            if(ref == null)
            {
                throw new OJBRuntimeException("Given field " + referenceField + " does not match a reference in " + clazz);
            }

            prepareSetting(ref, cld, clazz, referenceField);

            ref.setLazy(useProxy);
            ref.setCascadeRetrieve(autoRetrieve);
            ref.setCascadeStore(autoUpdate);
            ref.setCascadeDelete(autoDelete);

//            System.out.println("old settings: " + oldRef.toXML());
//            System.out.println("new settings: " + ref.toXML());
        }

        void prepareSetting(ObjectReferenceDescriptor ref, ClassDescriptor cld, Class clazz, String referenceField)
        {
            HashMap fieldMap = (HashMap) oldSettings.get(cld.getClassNameOfObject());
            if(fieldMap == null)
            {
                fieldMap = new HashMap();
                oldSettings.put(cld.getClassNameOfObject(), fieldMap);
            }

            ObjectReferenceDescriptor oldRef = (ObjectReferenceDescriptor) fieldMap.get(ref.getPersistentField().getName());
            // if we don't find old settings buffer it
            if(oldRef == null)
            {
                // buffer deep copy of old settings
                oldRef = (ObjectReferenceDescriptor) SerializationUtils.clone(ref);
                fieldMap.put(ref.getPersistentField().getName(), oldRef);
            }
        }
    }
}
