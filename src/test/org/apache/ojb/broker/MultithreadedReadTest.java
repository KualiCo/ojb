package org.apache.ojb.broker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.junit.JUnitExtensions;

/**
 * Tests multithreaded read of objects using proxy for nested 1:1 references
 * Account --> Buyer --> Address --> AddressType
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: MultithreadedReadTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class MultithreadedReadTest extends JUnitExtensions.MultiThreadedTestCase
{
    static final int NONE = ObjectReferenceDescriptor.CASCADE_NONE;
    static final int LINK = ObjectReferenceDescriptor.CASCADE_LINK;
    static final int OBJECT = ObjectReferenceDescriptor.CASCADE_OBJECT;

    int loops = 2;
    int concurrentThreads = 19;
    int numberOfObjects = 30;

    public MultithreadedReadTest(String s)
    {
        super(s);
    }

    public static void main(String[] args)
    {
        String[] arr = {MultithreadedReadTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        PersistenceBroker broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        try
        {
            changeReferenceSetting(broker, AccountImpl.class, "buyer", true, NONE, NONE, false);
            changeReferenceSetting(broker, BuyerImpl.class, "address", true, NONE, NONE, false);
            changeReferenceSetting(broker, BuyerImpl.class, "invoices", true, NONE, NONE, false);
            changeReferenceSetting(broker, BuyerImpl.class, "articles", true, NONE, NONE, false);
        }
        finally
        {
            if(broker != null)
            {
                broker.close();
            }
        }
        super.tearDown();
    }

    public void testClosedPB() throws Throwable
    {
        String name = "testClosedPB_"+System.currentTimeMillis();
        Account account = null;
        PersistenceBroker broker = null;
        try
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            changeReferenceSetting(broker, AccountImpl.class, "buyer", true, OBJECT, OBJECT, false);
            changeReferenceSetting(broker, BuyerImpl.class, "address", true, OBJECT, OBJECT, false);
            changeReferenceSetting(broker, BuyerImpl.class, "invoices", true, OBJECT, OBJECT, false);
            changeReferenceSetting(broker, BuyerImpl.class, "articles", true, OBJECT, OBJECT, false);
            broker.beginTransaction();
            Integer[] ids = prepareTestRead(broker, name, 5);
            broker.commitTransaction();
            broker.clearCache();

            Criteria crit = new Criteria();
            crit.addIn("id", Arrays.asList(ids));
            QueryByCriteria query = new QueryByCriteria(Account.class, crit);
            Collection result = broker.getCollectionByQuery(query);
            Iterator iter = result.iterator();
            //			 iter.next();
            account = (Account) iter.next();
            while (iter.hasNext())
            {
                iter.next();
            }
        }
        finally
        {
            if (broker != null) broker.close();
        }

        TestCaseRunnable tct [] = new TestCaseRunnable[50];
        for (int i = 0; i < concurrentThreads; i++)
        {
            tct[i] = new TestHandleMaterialize(account, name);
        }
        // run test classes
        runTestCaseRunnables(tct);
    }

    /**
     * Read objects using lazy materialization for references from DB. Different threads
     * call the references on the read objects
     */
    public void testObjectMaterializationByDifferentThread() throws Exception
    {
        for (int k = 0; k < loops; k++)
        {
            String searchCriteria = "testObjectMaterializationByDifferentThread_" + System.currentTimeMillis();
            PersistenceBroker broker = null;
            Collection accounts;
            try
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                changeReferenceSetting(broker, AccountImpl.class, "buyer", true, OBJECT, OBJECT, false);
                changeReferenceSetting(broker, BuyerImpl.class, "address", true, OBJECT, OBJECT, true);
                changeReferenceSetting(broker, BuyerImpl.class, "invoices", true, OBJECT, OBJECT, true);
                changeReferenceSetting(broker, BuyerImpl.class, "articles", true, OBJECT, OBJECT, false);
                broker.beginTransaction();
                prepareTestRead(broker, searchCriteria, concurrentThreads);
                broker.commitTransaction();
                broker.clearCache();

                Criteria crit = new Criteria();
                crit.addEqualTo("name", searchCriteria);
                QueryByCriteria query = new QueryByCriteria(Account.class, crit);
                accounts = broker.getCollectionByQuery(query);
                assertEquals(concurrentThreads, accounts.size());
            }
            finally
            {
                if (broker != null) broker.close();
            }
            Iterator iter = accounts.iterator();
            TestCaseRunnable tct [] = new TestCaseRunnable[concurrentThreads];
            for (int i = 0; i < concurrentThreads; i++)
            {
                tct[i] = new TestHandleMaterialize((Account) iter.next(), searchCriteria);
            }
            // run test classes
            runTestCaseRunnables(tct);
        }
    }

    /**
     * Different threads try to materialize the same bunch of objects.
     */
    public void testMultithreadedRead() throws Exception
    {
        String searchCriteria = "testMultithreadedRead_" + System.currentTimeMillis();
        PersistenceBroker broker = null;
        try
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            changeReferenceSetting(broker, AccountImpl.class, "buyer", true, OBJECT, OBJECT, false);
            changeReferenceSetting(broker, BuyerImpl.class, "address", true, OBJECT, OBJECT, false);
            changeReferenceSetting(broker, BuyerImpl.class, "invoices", true, OBJECT, OBJECT, false);
            changeReferenceSetting(broker, BuyerImpl.class, "articles", true, OBJECT, OBJECT, false);
            broker.beginTransaction();
            prepareTestRead(broker, searchCriteria, numberOfObjects);
            broker.commitTransaction();
            broker.clearCache();
        }
        finally
        {
            if(broker != null) broker.close();
        }

        System.out.println();
        System.out.println("Multithreaded read of objects - start");
        System.out.println("" + concurrentThreads + " concurrent threads read "
                + numberOfObjects + " objects per thread, loop " + loops + " times");
        for (int k = 0; k < loops; k++)
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            broker.clearCache();
            broker.close();

            TestCaseRunnable tct [] = new TestCaseRunnable[concurrentThreads];
            for (int i = 0; i < concurrentThreads; i++)
            {
                tct[i] = new TestHandleRead(searchCriteria);
            }
            // run test classes
            runTestCaseRunnables(tct);
        }

        System.out.println("Multithreaded read of objects - end");
    }

    /**
     * Different threads try to materialize the same bunch of objects
     */
    public void testMultithreadedLazyRead() throws Exception
    {
        String name = "testMultithreadedLazyRead" + System.currentTimeMillis();
        PersistenceBroker broker = null;
        List identityList = null;
        try
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            changeReferenceSetting(broker, AccountImpl.class, "buyer", true, OBJECT, OBJECT, false);
            changeReferenceSetting(broker, BuyerImpl.class, "address", true, OBJECT, OBJECT, true);
            changeReferenceSetting(broker, BuyerImpl.class, "invoices", true, OBJECT, OBJECT, true);
            changeReferenceSetting(broker, BuyerImpl.class, "articles", true, OBJECT, OBJECT, true);
            broker.beginTransaction();
            identityList = prepareTestLazyRead(broker, name, concurrentThreads);
            broker.commitTransaction();
            broker.clearCache();
        }
        finally
        {
            if(broker != null) broker.close();
        }

        System.out.println();
        System.out.println("Multithreaded lazy read of objects - start");
        System.out.println("" + concurrentThreads + " concurrent threads read different object with lazy" +
                " materialization reference, loop " + loops + " times");
        for (int k = 0; k < loops; k++)
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            broker.clearCache();
            broker.close();

            TestCaseRunnable tct [] = new TestCaseRunnable[concurrentThreads];
            for (int i = 0; i < concurrentThreads; i++)
            {
                tct[i] = new TestHandleLazyRead(identityList, name);
            }
            // run test classes
            runTestCaseRunnables(tct);
        }
        System.out.println("Multithreaded lazy read of objects - end");
    }

    private Integer[] prepareTestRead(PersistenceBroker broker, String name, int numbers) throws Exception
    {
        Integer[] ids = new Integer[numbers];
        for (int i = 0; i < numbers; i++)
        {
            AddressType type = new AddressTypeImpl(name);
            Address address = new AddressImpl(name, type);
            Buyer buyer = new BuyerImpl(name, address);
            buyer.setArticles(buildArticles(name, numbers));
            buyer.setInvoices(buildInvoices(name, numbers));
            Account account = new AccountImpl(name, buyer);
            broker.store(account);
            ids[i] = account.getId();
        }
        return ids;
    }

    private List prepareTestLazyRead(PersistenceBroker broker, String searchCriteria, int numbers) throws Exception
    {
        List result = new ArrayList();
        for (int i = 0; i < numbers; i++)
        {
            AddressType type = new AddressTypeImpl(searchCriteria);
            Address address = new AddressImpl(searchCriteria, type);
            Buyer buyer = new BuyerImpl(searchCriteria, address);
            buyer.setArticles(buildArticles(searchCriteria, numbers));
            buyer.setInvoices(buildInvoices(searchCriteria, numbers));
            Account account = new AccountImpl(searchCriteria, buyer);
            broker.store(account);
            Identity oid = broker.serviceIdentity().buildIdentity(account);
            result.add(oid);
        }
        return result;
    }

    private List buildInvoices(String name, int numbers)
    {
        List result = new ArrayList();
        for(int i = 0; i < numbers; i++)
        {
            String invoiceNumber = "I_" + (long)(Math.random() * Long.MAX_VALUE);
            Invoice invoice = new InvoiceImpl(name, invoiceNumber);
            result.add(invoice);
        }
        return result;
    }

    private List buildArticles(String name, int numbers)
    {
        List result = new ArrayList();
        for(int i = 0; i < numbers; i++)
        {
            Article a = new ArticleImpl(name, "a article description");
            result.add(a);
        }
        return result;
    }

    void changeReferenceSetting(PersistenceBroker broker, Class clazz, String fieldName, boolean autoRetrieve, int autoUpdate, int autoDelete, boolean proxy)
    {
        ClassDescriptor cld = broker.getClassDescriptor(clazz);
        ObjectReferenceDescriptor descriptor = cld.getCollectionDescriptorByName(fieldName);
        if(descriptor == null)
        {
            descriptor = cld.getObjectReferenceDescriptorByName(fieldName);
        }
        if(descriptor == null)
        {
            throw new RuntimeException("Field name " + fieldName + " does not represent a reference in class '" + clazz.getName() + "'");
        }
        descriptor.setLazy(proxy);
        descriptor.setCascadeRetrieve(autoRetrieve);
        descriptor.setCascadingStore(autoUpdate);
        descriptor.setCascadingDelete(autoDelete);
    }


    //***********************************************
    // test handle of multithreaded test
    //***********************************************
    class TestHandleRead extends JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        String searchCriteria;

        public TestHandleRead(String searchCriteria)
        {
            this.searchCriteria = searchCriteria;
        }

        public void runTestCase() throws Throwable
        {
            readByCollection();
            readByIterator();
        }

        private void readByCollection() throws Exception
        {
            PersistenceBroker broker = null;
            try
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                broker.clearCache();
                Criteria crit = new Criteria();
                crit.addEqualTo("name", searchCriteria);
                QueryByCriteria query = new QueryByCriteria(Account.class, crit);
                Collection accounts = broker.getCollectionByQuery(query);
                assertEquals("Wrong number of expected objects", numberOfObjects, accounts.size());
                for (Iterator iter = accounts.iterator(); iter.hasNext();)
                {
                    Account account = (Account) iter.next();
                    assertEquals(searchCriteria, account.getName());
                    assertNotNull("All accounts have a reference to an Buyer", account.getBuyer());
                    assertNotNull("All buyers have a reference to an Address", account.getBuyer().getAddress());
                    assertNotNull("All addresses have a reference to an AdressType", account.getBuyer().getAddress().getType());
                    assertNotNull("All AddressType have a name", account.getBuyer().getAddress().getType().getName());
                    assertNotNull("All buyers have populated 1:n reference to Invoice", account.getBuyer().getInvoices());
                    assertNotNull("All buyers have populated 1:n reference to Article", account.getBuyer().getArticles());
                    // System.out.println(""+Thread.currentThread().toString()+": passed");
                }
            }
            finally
            {
                if (broker != null) broker.close();
            }
        }

        private void readByIterator() throws Exception
        {
            PersistenceBroker broker = null;
            try
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                broker.clearCache();
                Criteria crit = new Criteria();
                crit.addEqualTo("name", searchCriteria);
                QueryByCriteria query = new QueryByCriteria(Account.class, crit);
                Iterator iter = broker.getIteratorByQuery(query);
                for (; iter.hasNext();)
                {
                    Account account = (Account) iter.next();
                    assertEquals(searchCriteria, account.getName());
                    assertNotNull("All accounts have a reference to an Buyer", account.getBuyer());
                    assertNotNull("All buyers have a reference to an Address", account.getBuyer().getAddress());
                    assertNotNull("All addresses have a reference to an AdressType", account.getBuyer().getAddress().getType());
                    assertNotNull("All AddressType have a name", account.getBuyer().getAddress().getType().getName());
                    assertNotNull("All buyers have populated 1:n reference to Invoice", account.getBuyer().getInvoices());
                    assertNotNull("All buyers have populated 1:n reference to Article", account.getBuyer().getArticles());
                    // System.out.println(""+Thread.currentThread().toString()+": passed");
                }
            }
            finally
            {
                if (broker != null) broker.close();
            }
        }
    }

    class TestHandleLazyRead extends JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        List identityList;
        String name;

        public TestHandleLazyRead(List identityList, String name)
        {
            this.identityList = identityList;
            this.name = name;
        }

        public void runTestCase() throws Throwable
        {
            PersistenceBroker broker = null;
            Account account = null;
            try
            {
                broker = PersistenceBrokerFactory.defaultPersistenceBroker();
                Iterator it = identityList.iterator();
                while (it.hasNext())
                {
                    Identity oid = (Identity) it.next();
                    account = (Account) broker.getObjectByIdentity(oid);
                }
            }
            finally
            {
                if (broker != null) broker.close();
            }
            assertEquals(name, account.getName());
            assertNotNull("All accounts have a reference to an Buyer", account.getBuyer());
            assertNotNull("All buyers have a reference to an Address", account.getBuyer().getAddress());
            assertNotNull("All addresses have a reference to an AdressType", account.getBuyer().getAddress().getType());
            assertNotNull("All AddressType have a name", account.getBuyer().getAddress().getType().getName());
            assertNotNull("All buyers have populated 1:n reference to Invoice", account.getBuyer().getInvoices());
                    assertNotNull("All buyers have populated 1:n reference to Article", account.getBuyer().getArticles());
        }
    }


    class TestHandleMaterialize extends JUnitExtensions.MultiThreadedTestCase.TestCaseRunnable
    {
        Account account;
        String name;

        public TestHandleMaterialize(Account account, String name)
        {
            this.account = account;
            this.name = name;
        }

        public void runTestCase() throws Throwable
        {
            assertEquals(name, account.getName());
            assertNotNull("All accounts have a reference to an Buyer", account.getBuyer());
            assertEquals(name, account.getBuyer().getName());
            assertNotNull("All buyers have a reference to an Address", account.getBuyer().getAddress());
            assertEquals(name, account.getBuyer().getAddress().getName());
            assertNotNull("All addresses have a reference to an AdressType", account.getBuyer().getAddress().getType());
            assertNotNull("All AddressType have a name", account.getBuyer().getAddress().getType().getName());
            assertNotNull("All buyers have populated 1:n reference to Invoice", account.getBuyer().getInvoices());
            assertNotNull("All buyers have populated 1:n reference to Article", account.getBuyer().getArticles());
        }
    }


    //***********************************************
    // test classes/interfaces starts here
    //***********************************************
    public interface Account extends Base
    {
        Buyer getBuyer();

        void setBuyer(Buyer buyer);
    }

    public static class AccountImpl extends BaseImpl implements Account
    {
        Buyer buyer;

        public AccountImpl(String name, Buyer buyer)
        {
            super(name);
            this.buyer = buyer;
        }

        public AccountImpl(Buyer buyer)
        {
            this.buyer = buyer;
        }

        public AccountImpl()
        {

        }

        public Buyer getBuyer()
        {
            return buyer;
        }

        public void setBuyer(Buyer buyer)
        {
            this.buyer = buyer;
        }
    }

    public interface Buyer extends Base
    {
        Address getAddress();
        void setAddress(Address address);
        public List getInvoices();
        public void setInvoices(List invoices);
        public List getArticles();
        public void setArticles(List articles);
    }

    public static class BuyerImpl extends BaseImpl implements Buyer
    {
        private Address address;
        private List invoices;
        private List articles;

        public BuyerImpl(String name, Address address)
        {
            super(name);
            this.address = address;
        }

        public BuyerImpl(String name, Address address, List invoices, List articles)
        {
            super(name);
            this.address = address;
            this.invoices = invoices;
            this.articles = articles;
        }

        public BuyerImpl(Address address)
        {
            this.address = address;
        }

        public BuyerImpl()
        {

        }

        public List getInvoices()
        {
            return invoices;
        }

        public void setInvoices(List invoices)
        {
            this.invoices = invoices;
        }

        public List getArticles()
        {
            return articles;
        }

        public void setArticles(List articles)
        {
            this.articles = articles;
        }

        public Address getAddress()
        {
            return address;
        }

        public void setAddress(Address address)
        {
            this.address = address;
        }
    }

    public interface Address extends Base
    {
        AddressType getType();

        void setType(AddressType type);
    }

    public static class AddressImpl extends BaseImpl implements Address
    {
        AddressType type;

        public AddressImpl(String name, AddressType type)
        {
            super(name);
            this.type = type;
        }

        public AddressImpl(AddressType type)
        {
            this.type = type;
        }

        public AddressImpl()
        {

        }

        public AddressType getType()
        {
            return type;
        }

        public void setType(AddressType type)
        {
            this.type = type;
        }
    }

    public interface AddressType extends Base
    {
    }

    public static class AddressTypeImpl extends BaseImpl implements AddressType
    {
        public AddressTypeImpl(String name)
        {
            super(name);
        }

        public AddressTypeImpl()
        {
        }
    }

    public interface Base
    {
        Integer getId();

        void setId(Integer id);

        String getName();

        void setName(String name);
    }

    public static class BaseImpl
    {
        Integer id;
        String name;

        public BaseImpl(String name)
        {
            this.name = name;
        }

        public BaseImpl()
        {
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static interface Invoice extends Base
    {
        public String getInvoiceNumber();
        public void setInvoiceNumber(String invoiceNumber);
    }

    public static class InvoiceImpl extends BaseImpl implements Invoice
    {
        private String invoiceNumber;
        private Integer buyerId;

        public InvoiceImpl()
        {
        }

        public InvoiceImpl(String name, String invoiceNumber)
        {
            super(name);
            this.invoiceNumber = invoiceNumber;
        }


        public Integer getBuyerId()
        {
            return buyerId;
        }

        public void setBuyerId(Integer buyerId)
        {
            this.buyerId = buyerId;
        }

        public String getInvoiceNumber()
        {
            return invoiceNumber;
        }

        public void setInvoiceNumber(String invoiceNumber)
        {
            this.invoiceNumber = invoiceNumber;
        }
    }

    public static interface Article extends Base
    {
        public String getDescription();
        public void setDescription(String description);
    }

    public static class ArticleImpl extends BaseImpl implements Article
    {
        private String description;
        private Integer buyerId;

        public ArticleImpl()
        {
        }

        public ArticleImpl(String name, String description)
        {
            super(name);
            this.description = description;
        }

        public Integer getBuyerId()
        {
            return buyerId;
        }

        public void setBuyerId(Integer buyerId)
        {
            this.buyerId = buyerId;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }
}
