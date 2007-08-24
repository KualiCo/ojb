package org.apache.ojb.broker.metadata;

import junit.framework.TestCase;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.ObjectRepository;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldFactory;

/**
 * Test used to check if it is possible to start OJB
 * without defined descriptors.
 * Before running this test delete/comment out all
 * jdbc-connection-descriptors, class-descriptors in the
 * repository file.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: RuntimeConfigurationTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class RuntimeConfigurationTest extends TestCase
{
    public RuntimeConfigurationTest()
    {
    }

    public RuntimeConfigurationTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {RuntimeConfigurationTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testStartWithout() throws Exception
    {
        ConnectionRepository cr = MetadataManager.getInstance().connectionRepository();
        System.out.println("ConnectionRepository before add: "+cr.toXML());
        JdbcConnectionDescriptor jcd = new JdbcConnectionDescriptor();
        jcd.setDbAlias("test");
        jcd.setDbms("test2");
        jcd.setDefaultConnection(true);
        cr.addDescriptor(jcd);
        System.out.println("ConnectionRepository after add: "+cr.toXML());

        DescriptorRepository dr = MetadataManager.getInstance().getRepository();
        System.out.println("DescriptorRepository before add: "+dr.toXML());
        ClassDescriptor cld = new ClassDescriptor(dr);
        cld.setClassOfObject(ObjectRepository.A.class);
        FieldDescriptor fd = new FieldDescriptor(cld, 1);
        PersistentField pf = PersistentFieldFactory.createPersistentField(ObjectRepository.A.class, "someAField");
        fd.setPersistentField(pf);
        cld.addFieldDescriptor(fd);

        dr.setClassDescriptor(cld);
        System.out.println("DescriptorRepository after add: "+dr.toXML());
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        broker.close();
//        Query query = QueryFactory.newQuery(A.class, (Criteria)null);
//        broker.getCollectionByQuery(query);
    }
}
