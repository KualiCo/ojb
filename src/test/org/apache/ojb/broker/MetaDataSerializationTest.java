package org.apache.ojb.broker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Vector;

import junit.framework.TestCase;
import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.MetadataManager;

/**
 * Insert the type's description here.
 * Creation date: (06.12.2000 21:47:56)
 * @author Thomas Mahler
 */
public class MetaDataSerializationTest extends TestCase
{
    private static Class CLASS = MetaDataSerializationTest.class;

    /**
     * BrokerTests constructor comment.
     * @param name java.lang.String
     */
    public MetaDataSerializationTest(String name)
    {
        super(name);
    }

    /**
     * Insert the method's description here.
     * Creation date: (23.12.2000 18:30:38)
     * @param args java.lang.String[]
     */
    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * test serialization of ClassDescriptors to disk
     */
    public void testToDisk() throws Exception
    {
        DescriptorRepository repository = MetadataManager.getInstance().getRepository();
        Iterator iter = repository.iterator();
        Vector vec = new Vector();
        while (iter.hasNext())
        {
            vec.add(iter.next());
        }
        File outfile = new File("repository.serialized");
        FileOutputStream fos = new FileOutputStream(outfile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(vec);
        oos.close();
        fos.close();
    }

    /**
     * test deserialization of ClassDescriptors from disk
     */
    public void testFromDisk() throws Exception
    {
        File infile = new File("repository.serialized");
        FileInputStream fis = new FileInputStream(infile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Vector vec = (Vector) ois.readObject();
        ois.close();
        fis.close();

        Iterator iter = vec.iterator();
        while (iter.hasNext())
        {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
        }
    }


    /**
     * test serialization and deserialisation of all ClassDescriptors in
     * Descriptor repository
     */
    public void testRemote() throws Exception
    {
        ClassDescriptor cld = null;
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        for (int i = 0; i < 5; i++)
        {
            DescriptorRepository repository = MetadataManager.getInstance().getRepository();
            Iterator iter = repository.iterator();
            while (iter.hasNext())
            {
                cld = (ClassDescriptor) iter.next();
                //System.out.println("CLD remote: " + cld.getClassOfObject().getName());

                ClassDescriptor cldRemote = broker.getClassDescriptor(cld.getClassOfObject());
            }
        }
        broker.close();
    }

    /**
     * test serialization and deserialisation of all ClassDescriptors in
     * Descriptor repository
     */
    public void testIdentityStuff() throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        DescriptorRepository repository = MetadataManager.getInstance().getRepository();
        Iterator iter = repository.iterator();
        while (iter.hasNext())
        {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            //System.out.println("CLD: " + cld.getClassOfObject().getName());
            Class c = cld.getClassOfObject();
            if (!c.isInterface())
            {

                Object o = null;
                try
                {
                    o = c.newInstance();
                    Identity oid = new Identity(o, broker);
                    //System.out.println(oid.toString());
                }
                catch (InstantiationException e)
                {
                }
                catch (IllegalAccessException e)
                {
                }
            }
        }
        broker.close();
    }

    /**
     * test serialization and deserialisation of all ClassDescriptors in
     * Descriptor repository
     */
    public void XXXtestClassDescriptorSerialization() throws Exception
    {
        DescriptorRepository repository = MetadataManager.getInstance().getRepository();
        Iterator iter = repository.iterator();
        while (iter.hasNext())
        {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
//            System.out.println("CLD: " + cld.getClassOfObject().getName());
//            byte[] arr = serialize(cld);
//
//            ClassDescriptor cld1 = deserialize(arr);

            byte[] arr = SerializationUtils.serialize(cld);
            ClassDescriptor cld1 = (ClassDescriptor) SerializationUtils.deserialize(arr);
        }
    }
}
