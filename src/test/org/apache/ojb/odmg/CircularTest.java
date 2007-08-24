package org.apache.ojb.odmg;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Testing complex object graphs with circular and bidirectional references when
 * using database foreign key settings (without support of deferred foreign keys).
 * <p/>
 * The classes hierarchy looks like:
 * <br/>
 * Class Shop has a bidirectional 1:1 reference with ShopDetail.<br/>
 * Shop has a 1:n relation with Product, Product has a 1:1 reference to Shop.<br/>
 * Shop has a m:n relation with Distributor.<br/>
 * Product has a 1:n relation to itself to handle sub-Products.<br/>
 * <p/>
 * In the database the following foreign keys are declared:
 * <br/>
 * - Shop has a FK to ShopDetail<br/>
 * - Product has a FK  to Product<br/>
 * - Product has a FK  to Shop<br/>
 * - CT_SHOP_DISTRIBUTOR indirection table has FK's to Shop and Distributor<br/>
 * <p/>
 * Here a summery of the dependencies:<br/>
 * Shop--1:1-->ShopDetail--1:1-->Shop<br/>
 * Shop--1:n-->Product--1:1-->Shop<br/>
 * Product--1:n-->Product<br/>
 * Shop--m:n-->Distributor<br/>
 * <p/>
 * <p/>
 * Class ObjectA has a 1:1 reference to ObjectAA,<br/>
 * ObjectAA has 1:1 to ObjectAAA,<br/>
 * ObjectAAA has 1:1 to ObjectA and to ObjectAAAA,<br/>
 * ObjectAAAA has 1:1 to ObjectA<br/>
 * <br/>
 * - ObjectA has FK to ObjectAA<br/>
 * - ObjectAA has FK to ObjectAAA<br/>
 * - ObjectAAA has FK to ObjectAAAA<br/>
 * <p/>
 * Here a summery of the dependencies:<br/>
 * ObjetA--1:1-->ObjectAA--1:1-->ObjectAAA--1:1-->ObjectA
 * ObjetA--1:1-->ObjectAA--1:1-->ObjectAAA--1:1-->ObjectAAAA--1:1-->ObjectA
 *
 * @version $Id: CircularTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class CircularTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {CircularTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /**
     * Handling circular 1:n references with FK settings and use of
     * auto-delete setting to delete object graph.
     */
    public void testCircularOneToN_1() throws Exception
    {
        String name = "testCircularOneToN_1_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Product.class, "subProducts", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);

        Product p1 = new Product(name + "_p1");
        Product p2 = new Product(name + "_p2");
        Product p3 = new Product(name + "_p3");

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        p1.addSubProduct(p2);
        p2.addSubProduct(p3);
        database.makePersistent(p3);
        // before establishing the circular references write
        // all objects to DB
        tx.flush();
        // now close the circular references
        p3.addSubProduct(p1);
        tx.commit();

        tx.begin();
        // on delete break the circular references first, then delete the
        // start object
        tx.lock(p3, Transaction.WRITE);
        // this call is only needed if auto-delete setting in repository is 'object'
        tx.setCascadingDelete(Product.class, "subProducts", false);
        p3.setSubProducts(null);
        tx.flush();
        database.deletePersistent(p3);
        database.deletePersistent(p2);
        database.deletePersistent(p1);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Product.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(0, result.size());
    }

    /**
     * Handling circular 1:n references with FK settings and use of
     * auto-delete setting to delete object graph.
     */
    public void testCircularOneToN_2() throws Exception
    {
        String name = "testCircularOneToN_2_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Product.class, "subProducts", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);

        Product p1 = new Product(name + "_p1");
        Product p2 = new Product(name + "_p2");
        Product p3 = new Product(name + "_p3");
        Product p4 = new Product(name + "_p4");
        Product p5 = new Product(name + "_p5");
        Product p6 = new Product(name + "_p6");
        Product p7 = new Product(name + "_p7");

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        p1.addSubProduct(p2);
        p1.addSubProduct(p3);
        p3.addSubProduct(p4);
        p3.addSubProduct(p5);
        p5.addSubProduct(p6);
        p6.addSubProduct(p7);
        database.makePersistent(p1);
        // before establishing the circular references write
        // all objects to DB
        tx.flush();
        // now close the circular references
        p6.addSubProduct(p1);
        tx.commit();

        tx.begin();
        // on delete break the circular references first, then delete the
        // start object
        tx.lock(p6, Transaction.WRITE);
        tx.setCascadingDelete(Product.class, "subProducts", false);
        p6.setSubProducts(null);
        tx.flush();
        tx.setCascadingDelete(Product.class, "subProducts", true);
        database.deletePersistent(p1);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Product.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        tx.commit();

        // we expect one Product object, because we set cascading delete 'false'
        // when do 'p6.setSubProducts(null);', so the '..._p7' Product will only be unlinked
        assertEquals(1, result.size());
        Product p7_new = (Product) result.iterator().next();
        assertEquals(name + "_p7", p7_new.getName());
    }

    /**
     * Handling circular 1:n references with FK settings and use of
     * auto-delete setting to delete object graph.
     */
    public void testCircularOneToN_3() throws Exception
    {
        String name = "testCircularOneToN_3_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Product.class, "subProducts", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);

        Product p1 = new Product(name + "_p1");
        Product p2 = new Product(name + "_p2");
        Product p3 = new Product(name + "_p3");

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        p1.addSubProduct(p2);
        p2.addSubProduct(p3);
        database.makePersistent(p3);
        // before establishing the circular references write
        // all objects to DB
        tx.flush();
        // now close the circular references
        p3.addSubProduct(p1);
        tx.commit();

        tx.begin();
        // on delete break the circular references first, then delete the
        // start object
        tx.lock(p3, Transaction.WRITE);
        // this call is only needed if auto-delete setting in repository is 'object'
        tx.setCascadingDelete(Product.class, "subProducts", false);
        p3.setSubProducts(null);
        tx.flush();
        // this call is only needed if auto-delete setting in repository is 'none'
        // to enable cascade delete, else we have to delete each object by hand
        tx.setCascadingDelete(Product.class, "subProducts", true);
        database.deletePersistent(p1);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + Product.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(0, result.size());
    }

    /**
     * Use auto-delete setting to delete object graph.
     */
    public void testCircularWithAutoDeleteEnabled() throws Exception
    {
        String name = "testCircularWithAutoDeleteEnabled_" + System.currentTimeMillis();

        ojbChangeReferenceSetting(ObjectA.class, "refAA", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ObjectAA.class, "refAAA", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ObjectAAA.class, "refAAAA", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ObjectAAAA.class, "refA", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        ObjectAAAA aaaa = new ObjectAAAA(name + "_ObjectAAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefAAAA(aaaa);
        aaaa.setRefA(a);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(a);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        tx.commit();

        tx.begin();
        /*
        arminw: When deleting a object which is part of circular 1:1 references
        with cascade delete enabled it's mandatory to break the circular reference
        before deleting it.
        */
        tx.lock(aaaa, Transaction.WRITE);
        // break the circular references
        aaaa.setRefA(null);
        database.deletePersistent(a);
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    /**
     * Use auto-delete setting to delete object graph.
     */
    public void testAutoDeleteEnabledNonCircular() throws Exception
    {
        String name = "testAutoDeleteEnabledNonCircular_" + System.currentTimeMillis();

        ojbChangeReferenceSetting(ObjectA.class, "refAA", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ObjectAA.class, "refAAA", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ObjectAAA.class, "refAAAA", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ObjectAAAA.class, "refA", true, ObjectReferenceDescriptor.CASCADE_NONE, ObjectReferenceDescriptor.CASCADE_OBJECT, false);

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        ObjectAAAA aaaa = new ObjectAAAA(name + "_ObjectAAAA");
        // now set the references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefAAAA(aaaa);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(a);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        tx.commit();

        tx.begin();
        database.deletePersistent(a);
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    /**
     * Handle circuler 1:1 with default methods.
     */
    public void testBidirectionalWithConstraint_1a() throws Exception
    {
        String name = "testBidirectionalWithConstraint_1a_" + System.currentTimeMillis();

        Shop s1 = new Shop(name + "_1");
        ShopDetail sd = new ShopDetail(name + "_1");
        s1.setDetail(sd);
        sd.setShop(s1);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(s1);
        tx.commit();

        tx.begin();
        database.deletePersistent(s1);
        tx.commit();
    }

    /**
     * Define order of object operations using flush() method.
     */
    public void testBidirectionalWithConstraint_1b() throws Exception
    {
        String name = "testBidirectionalWithConstraint_1b_" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        Shop s1 = new Shop(name + "_1");
        // when using flush() we can use the "natural" order
        // without getting DB constraint violence.
        database.makePersistent(s1);
        // write to DB object without references
        tx.flush();
        ShopDetail sd = new ShopDetail(name + "_1");
        // now set references
        s1.setDetail(sd);
        sd.setShop(s1);
        // no need to persist the ShopDetail object
        // (will be detected by OJB)
        // but it doesn't matter if you do
        // database.makePersistent(sd);
        tx.commit();

        tx.begin();
        // madatory to mark object with DB FK constraint first on delete
        // (FK from Shop to ShopDetail) then OJB will use this order to
        // delete the bidirectional objects
        database.deletePersistent(s1);
        tx.flush();
        database.deletePersistent(sd);
        tx.commit();
    }

    /**
     * If the user take care of the ordering itself the test pass.
     */
    public void testBidirectionalWithConstraint_1c() throws Exception
    {
        String name = "testBidirectionalWithConstraint_1c_" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();

        Shop s1 = new Shop(name + "_1");
        ShopDetail sd = new ShopDetail(name + "_1");
        s1.setDetail(sd);
        sd.setShop(s1);

        // set implicit locking false to determine order of objects
        tx.setImplicitLocking(false);
        // to prevent reordering of object, disable ordering
        // in many cases this is not needed, because OJB will leave ordering
        // tx.setOrdering(false);
        tx.begin();
        // madatory to persist referenced ShopDetail first, the Shop
        // object will be detected automatic. In this case first the ShopDetail
        // will be created and then the Shop
        database.makePersistent(sd);
        database.makePersistent(s1);
        tx.commit();

        // we using the same tx, thus locking and (ordering) is still disabled
        tx.begin();
        // madatory to mark object with DB FK constraint first on delete
        // then OJB will use this order to delete the bidirectional objects
        database.deletePersistent(s1);
        database.deletePersistent(sd);
        tx.commit();
    }

    /**
     * This test is only for comparison of ODMG- with PB-api. It's not recommended
     * to do this in ODMG production environment.
     *
     * @throws Exception
     */
    public void testBidirectionalWithConstraint_1d_PB() throws Exception
    {
        ojbChangeReferenceSetting(Shop.class, "detail", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ShopDetail.class, "shop", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        String name = "testBidirectionalWithConstraint_1a_" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        Shop s1 = new Shop(name + "_1");
        ShopDetail sd = new ShopDetail(name + "_1");
        s1.setDetail(sd);
        sd.setShop(s1);

        // only for testing, we completely bypass odmg
        tx.getBroker().beginTransaction();
        tx.getBroker().store(s1);
        tx.commit();


        tx.begin();
        // only for testing, we completely bypass odmg
        tx.getBroker().beginTransaction();
        // madatory to mark object with DB FK constraint first on delete
        // then OJB will use this order to delete the bidirectional objects
        tx.getBroker().delete(s1);
        tx.commit();
    }

    /**
     * Handle circular 1:1 by using a 'constraint'-flag property in
     * reference-descriptor to make OJB's ordering algorithm more
     * sophisticated.
     */
    public void testBidirectionalWithConstraint_1e() throws Exception
    {
        String name = "testBidirectionalWithConstraint_1e_" + System.currentTimeMillis();
        ObjectReferenceDescriptor ord = null;

        try
        {
            CircularTest.Shop s1 = new CircularTest.Shop(name + "_1");
            CircularTest.ShopDetail sd = new CircularTest.ShopDetail(name + "_1");
            s1.setDetail(sd);
            sd.setShop(s1);

            TransactionExt tx = (TransactionExt) odmg.newTransaction();
            tx.begin();
            // now we tell OJB that one 1:1 reference of the bidirectional 1:1 reference
            // between Shop and ShopDetail has a FK constraint
            ClassDescriptor cld = tx.getBroker().getClassDescriptor(CircularTest.Shop.class);
            ord = cld.getObjectReferenceDescriptorByName("detail");
            // current DB schema create a foreign key constraint and we can
            // inform OJB
            ord.setConstraint(true);
            // now it doesn't matter in which order we persist the new objects, OJB should
            // always reorder the objects before insert/update call
            database.makePersistent(sd);
            // or
            // database.makePersistent(s1);
            tx.commit();

            tx.begin();
            // with cascading delete and the declared FK constraint OJB
            // always use the correct order on delete.
            tx.setCascadingDelete(CircularTest.ShopDetail.class, true);
            database.deletePersistent(sd);
            // or
            // database.deletePersistent(s1);
            tx.commit();
        }
        finally
        {
            // restore old setting
            if(ord != null) ord.setConstraint(false);
        }
    }

    /**
     * Test show handling with circular references and database FK settings.
     */
    public void testCircularOneToOne_1a() throws Exception
    {
        String name = "testCircularOneToOne_1a_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefA(a);
        database.makePersistent(a);
        tx.commit();

        tx.begin();
        database.deletePersistent(a);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    /**
     * Test show handling with circular references and database FK settings.
     */
    public void testCircularOneToOne_1b() throws Exception
    {
        String name = "testCircularOneToOne_1b_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        ObjectAAAA aaaa = new ObjectAAAA(name + "_ObjectAAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefAAAA(aaaa);
        aaaa.setRefA(a);
        database.makePersistent(a);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        tx.commit();

        tx.begin();
        database.deletePersistent(a);
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        tx.commit();
    }

    /**
     * Do manually ordering using {@link TransactionExt#setOrdering(boolean)} to disable
     * OJB's ordering algorithm (and implicit locking).
     */
    public void testCircularOneToOne_1c() throws Exception
    {
        String name = "testCircularOneToOne_1d_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        ObjectAAAA aaaa = new ObjectAAAA(name + "_ObjectAAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefAAAA(aaaa);
        aaaa.setRefA(a);

        /*
        we want to manually insert new object, so we disable
        OJB's ordering and implicit object locking
        */
        tx.setOrdering(false);
        tx.setImplicitLocking(false);

        database.makePersistent(aaaa);
        database.makePersistent(aaa);
        database.makePersistent(aa);
        database.makePersistent(a);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        tx.commit();

        tx.begin();
        /*
        the ordering/implicit locking of the tx is still disabled (tx instance hasn't changed),
        so we have to take care of correct order of objects while deletion
        */
        database.deletePersistent(a);
        database.deletePersistent(aa);
        database.deletePersistent(aaa);
        database.deletePersistent(aaaa);
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    /**
     * Do manually ordering using in conjunction with OJB's ordering.
     */
    public void testCircularOneToOne_1dd() throws Exception
    {
        String name = "testCircularOneToOne_1dd_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        ObjectAAAA aaaa = new ObjectAAAA(name + "_ObjectAAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefAAAA(aaaa);
        aaaa.setRefA(a);

        /*
        we manually determine the insert object order
        */
        tx.setOrdering(false);
        tx.setImplicitLocking(false);

        database.makePersistent(aaaa);
        database.makePersistent(aaa);
        database.makePersistent(aa);
        database.makePersistent(a);
        tx.commit();

        tx.begin();
        /*
        the ordering/implicit locking/noteOrdering settings are still enabled
        */
        database.deletePersistent(a);
        database.deletePersistent(aa);
        database.deletePersistent(aaa);
        database.deletePersistent(aaaa);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    public void testCircularOneToOne_1e() throws Exception
    {
        String name = "testCircularOneToOne_1e_" + System.currentTimeMillis();

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        ObjectAAAA aaaa = new ObjectAAAA(name + "_ObjectAAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefAAAA(aaaa);
        aaaa.setRefA(a);

        database.makePersistent(a);
        tx.commit();

        tx.begin();
        // delete one object from an object graph
        tx.lock(aa, Transaction.WRITE);
        aa.setRefAAA(null);
        database.deletePersistent(aaa);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        tx.commit();
    }

    /**
     * User take care of the ordering itself.
     */
    public void testCircularOneToOne_2a() throws Exception
    {
        String name = "testCircularOneToOne_2_" + System.currentTimeMillis();

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefA(a);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        // we want control object insert order itself, thus
        // disable implicite locking and ordering
        tx.setImplicitLocking(false);
        tx.setOrdering(false);
        tx.begin();
        database.makePersistent(aaa);
        database.makePersistent(aa);
        database.makePersistent(a);
        tx.commit();

        // we use the same tx again, thus implicite locking
        // and ordering is still disabled
        tx.begin();
        database.deletePersistent(a);
        database.deletePersistent(aa);
        database.deletePersistent(aaa);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    /**
     * This test is only for comparison of ODMG- with PB-api. It's not recommended
     * to do this in ODMG production environment.
     * Handling of circular 1:1 reference: ObjetA--1:1-->ObjectAA--1:1-->ObjectAAA--1:1-->ObjectA
     * when each object has a FK constraint to it's reference.
     *
     * @throws Exception
     */
    public void testCircularOneToOne_PB_a() throws Exception
    {
        ojbChangeReferenceSetting(ObjectA.class, "refAA", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ObjectAA.class, "refAAA", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        //ojbChangeReferenceSetting(ObjectAAA.class, "refA", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        String name = "testCircularOneToOne_PB" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        ObjectA a = new ObjectA(name + "_ObjectA");
        // only for testing, we completely bypass odmg
        tx.getBroker().beginTransaction();
        //tx.getBroker().store(a);

        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefA(a);
        tx.getBroker().store(a);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();


        tx.begin();
        // only for testing, we completely bypass odmg
        tx.getBroker().beginTransaction();
        // madatory to mark object with DB FK constraint first on delete
        // then OJB will use this order to delete the bidirectional objects
        //aaa.setRefA(null);
        //tx.getBroker().store(aaa);
        tx.getBroker().delete(a);
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + ObjectAAAA.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    /**
     * This test is only for comparison of ODMG- with PB-api. It's not recommended
     * to do this in ODMG production environment.
     * Handling of circular 1:1 reference: ObjetA--1:1-->ObjectAA--1:1-->ObjectAAA--1:1-->ObjectA
     * when each object has a FK constraint to it's reference.
     *
     * @throws Exception
     */
    public void testCircularOneToOne_PB_b() throws Exception
    {
        ojbChangeReferenceSetting(ObjectA.class, "refAA", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        ojbChangeReferenceSetting(ObjectAA.class, "refAAA", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        //ojbChangeReferenceSetting(ObjectAAA.class, "refA", true, ObjectReferenceDescriptor.CASCADE_OBJECT, ObjectReferenceDescriptor.CASCADE_OBJECT, false);
        String name = "testCircularOneToOne_PB" + System.currentTimeMillis();
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();

        // only for testing, we completely bypass odmg
        tx.getBroker().beginTransaction();
        //tx.getBroker().store(a);

        ObjectA a = new ObjectA(name + "_ObjectA");
        ObjectAA aa = new ObjectAA(name + "_ObjectAA");
        ObjectAAA aaa = new ObjectAAA(name + "_ObjectAAA");
        ObjectAAAA aaaa = new ObjectAAAA(name + "_ObjectAAAA");
        // now set the circular references
        a.setRefAA(aa);
        aa.setRefAAA(aaa);
        aaa.setRefAAAA(aaaa);
        aaaa.setRefA(a);

        tx.getBroker().store(a);

        tx.commit();


        tx.begin();
        // only for testing, we completely bypass odmg
        tx.getBroker().beginTransaction();
        // madatory to mark object with DB FK constraint first on delete
        // then OJB will use this order to delete the bidirectional objects
        //aaa.setRefA(null);
        //tx.getBroker().store(aaa);
        tx.getBroker().delete(a);
        tx.commit();
    }

    /**
     * Class Shop has a bidirectional 1:1 reference with ShopDetail
     * (FK constraint from SHOP to SHOP_DETAIL table).
     * Shop has a m:n relation with Distributor.
     */
    public void testBidirectionalWithConstraint_2a() throws Exception
    {
        String name = "testBidirectionalWithConstraint_2a_" + System.currentTimeMillis();

        Shop s1 = new Shop(name + "_1");
        ShopDetail sd = new ShopDetail(name + "_1");
        s1.setDetail(sd);
        sd.setShop(s1);
        Distributor d1 = new Distributor(name + "_1");
        s1.addDistributor(d1);
        d1.addShop(s1);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(s1);
        tx.commit();

        // Now we delete the Shop with ShopDetail, but don't
        // touch the Distributor object
        tx.begin();
        database.deletePersistent(s1);
        // flush to avoid constraint error
        tx.flush();
        database.deletePersistent(sd);
        tx.commit();
    }

    /**
     * Class Shop has a bidirectional 1:1 reference with ShopDetail and the DB table of Shop
     * has a foreign key constraint on ShopDetail table. Shop has a m:n relation with Distributor.
     * <p/>
     * If the user take care of the ordering itself the test pass.
     */
    public void testBidirectionalWithConstraint_2b() throws Exception
    {
        String name = "testBidirectionalWithConstraint_2c_" + System.currentTimeMillis();

        Shop s1 = new Shop(name + "_1");
        Distributor d1 = new Distributor(name + "_1");
        s1.addDistributor(d1);
        d1.addShop(s1);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // When using flush() we can add objects step by step
        database.makePersistent(s1);
        tx.flush();

        // add the shop detail object to Shop
        ShopDetail sd = new ShopDetail(name + "_1");
        s1.setDetail(sd);
        sd.setShop(s1);

        tx.commit();

        // Delete all created objects, we disable implicit
        // locking and ordering to avoid constraint error
        tx.setImplicitLocking(false);
        tx.setOrdering(false);
        tx.begin();
        database.deletePersistent(d1);
        database.deletePersistent(s1);
        database.deletePersistent(sd);
        tx.commit();
    }

    /**
     * Class Shop has a bidirectional 1:1 reference with ShopDetail and the DB table of Shop
     * has a foreign key constraint on ShopDetail table. Shop has a m:n relation with Distributor.
     * <p/>
     * If the user take care of the ordering itself the test pass.
     */
    public void testBidirectionalWithConstraint_2d() throws Exception
    {
        String name = "testBidirectionalWithConstraint_2d_" + System.currentTimeMillis();
        Shop s1 = new Shop(name + "_1");
        ShopDetail sd = new ShopDetail(name + "_1");
        s1.setDetail(sd);
        sd.setShop(s1);
        Shop s2 = new Shop(name + "_2");
        Shop s3 = new Shop(name + "_3");
        Distributor d1 = new Distributor(name + "_1");
        Distributor d2 = new Distributor(name + "_2");
        Distributor d3 = new Distributor(name + "_3");
        Distributor d4 = new Distributor(name + "_4");
        s1.addDistributor(d1);
        d1.addShop(s1);
        s1.addDistributor(d2);
        d2.addShop(s1);
        s1.addDistributor(d3);
        d3.addShop(s1); // one
        s2.addDistributor(d1);
        d1.addShop(s2);
        s3.addDistributor(d4);
        d4.addShop(s3);
        s3.addDistributor(d1);
        d1.addShop(s3);
        d3.addShop(s2); // two
        s2.addDistributor(d3);
        d3.addShop(s3); // three
        s3.addDistributor(d3);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // mandatory to add the ShopDetail object first, because of the ordering bug
        database.makePersistent(sd);
        database.makePersistent(s1);
        database.makePersistent(s2);
        database.makePersistent(s3);
        tx.commit();

        tx.begin();
        database.deletePersistent(d1);
        database.deletePersistent(s1);
        database.deletePersistent(d4);
        tx.commit();

        tx.begin();
        // clear the cache to make this test work with all cache implementations
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select shops from " + Shop.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();

        assertEquals(2, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + Distributor.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();

        assertEquals(2, result.size());
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select objects from " + Distributor.class.getName() + " where name like $1");
        query.bind(name + "_3");
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Distributor d3New = (Distributor) result.iterator().next();

        assertNotNull(d3New.getShops());
        assertEquals(2, d3New.getShops().size());
    }

    public void testMtoNWithBackReference_2() throws Exception
    {
        String name = "testMtoNWithBackReference_2_" + System.currentTimeMillis();
        Shop s1 = new Shop(name + "_1");
        Shop s2 = new Shop(name + "_2");
        Shop s3 = new Shop(name + "_3");
        Distributor d1 = new Distributor(name + "_1");
        Distributor d2 = new Distributor(name + "_2");
        Distributor d3 = new Distributor(name + "_3");
        Distributor d4 = new Distributor(name + "_4");
        s1.addDistributor(d1);
        d1.addShop(s1);
        s1.addDistributor(d2);
        d2.addShop(s1);
        s1.addDistributor(d3);
        d3.addShop(s1);
        s2.addDistributor(d1);
        d1.addShop(s2);
        s3.addDistributor(d4);
        d4.addShop(s3);
        s3.addDistributor(d1);
        d1.addShop(s3);
        d3.addShop(s2);
        s2.addDistributor(d3);
        d3.addShop(s3);
        s3.addDistributor(d3);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // madatory to add the ShopDetail object first
        database.makePersistent(s1);
        database.makePersistent(s2);
        database.makePersistent(s3);
        tx.commit();

        tx.begin();
        tx.setCascadingDelete(Distributor.class, "shops", true);
        database.deletePersistent(d1);
        database.deletePersistent(s1);
        database.deletePersistent(d4);
        tx.commit();

        tx.begin();
        // clear cache to make it work with all caching implementations
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select shops from " + Shop.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();

        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select objects from " + Distributor.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (Collection) query.execute();

        assertEquals(2, result.size());
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select objects from " + Distributor.class.getName() + " where name like $1");
        query.bind(name + "_3");
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Distributor d3New = (Distributor) result.iterator().next();

        assertNotNull(d3New.getShops());
        assertEquals(0, d3New.getShops().size());
    }

    public void testOneToNWithSelfReference_1() throws Exception
    {
        String name = "testOneToNWithSelfReference_1_" + System.currentTimeMillis();
        Shop s = new Shop();
        s.setName(name);
        Product p1 = new Product();
        p1.setName(name + "_product_1");
        Product p2 = new Product();
        p2.setName(name + "_product_2");

        List products = new ArrayList();
        products.add(p1);
        products.add(p2);

        s.setProducts(products);
        p1.setShop(s);
        p2.setShop(s);

        Product p1a = new Product();
        p1a.setName(name + "_subProduct_A");
        Product p1b = new Product();
        p1b.setName(name + "_subProduct_B");
        Product p1c = new Product();
        p1c.setName(name + "_subProduct_C");

        List subProducts = new ArrayList();
        subProducts.add(p1a);
        subProducts.add(p1b);
        subProducts.add(p1c);

        p1.setSubProducts(subProducts);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(s);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select products from " + Shop.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Shop newShop = (Shop) result.iterator().next();
        assertNotNull(newShop.getProducts());
        assertEquals(2, newShop.getProducts().size());

        boolean match = false;
        for(Iterator iterator = newShop.getProducts().iterator(); iterator.hasNext();)
        {
            Product p = (Product) iterator.next();
            if(p.getSubProducts() != null && p.getSubProducts().size() > 0)
            {
                match = true;
                assertEquals(3, p.getSubProducts().size());
            }
        }
        assertTrue("Sub products aren't stored", match);

        tx.begin();
        database.deletePersistent(s);
        tx.commit();


        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select products from " + Product.class.getName() + " where name like $1");
        query.bind(name + "_subPro%");
        result = (Collection) query.execute();
        tx.commit();
        assertEquals(3, result.size());

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select products from " + Product.class.getName() + " where name like $1");
        query.bind(name + "_product_1");
        result = (Collection) query.execute();
        assertEquals(1, result.size());

        database.deletePersistent(result.iterator().next());
        tx.commit();


        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select products from " + Product.class.getName() + " where name like $1");
        query.bind(name + "_subPro%");
        result = (Collection) query.execute();
        tx.commit();
        assertEquals(3, result.size());
    }

    public void testOneToNWithSelfReference_2() throws Exception
    {
        String name = "testOneToNWithSelfReference_2_" + System.currentTimeMillis();
        Shop s = new Shop();
        s.setName(name);
        Product p1 = new Product();
        p1.setName(name + "_product_1");
        Product p2 = new Product();
        p2.setName(name + "_product_2");

        List products = new ArrayList();
        products.add(p1);
        products.add(p2);

        s.setProducts(products);
        p1.setShop(s);
        p2.setShop(s);

        Product p1a = new Product();
        p1a.setName(name + "_subProduct_A");
        Product p1b = new Product();
        p1b.setName(name + "_subProduct_B");
        Product p1c = new Product();
        p1c.setName(name + "_subProduct_C");

        List subProducts = new ArrayList();
        subProducts.add(p1a);
        subProducts.add(p1b);
        subProducts.add(p1c);

        p1.setSubProducts(subProducts);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(s);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select products from " + Shop.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Shop newShop = (Shop) result.iterator().next();
        assertNotNull(newShop.getProducts());
        assertEquals(2, newShop.getProducts().size());

        boolean match = false;
        for(Iterator iterator = newShop.getProducts().iterator(); iterator.hasNext();)
        {
            Product p = (Product) iterator.next();
            if(p.getSubProducts() != null && p.getSubProducts().size() > 0)
            {
                match = true;
                assertEquals(3, p.getSubProducts().size());
            }
        }
        assertTrue("Sub products aren't stored", match);

        tx.begin();
        // now we enable cascading delete
        tx.setCascadingDelete(Shop.class, true);
        database.deletePersistent(s);
        tx.commit();


        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select products from " + Product.class.getName() + " where name like $1");
        query.bind(name + "_subPro%");
        result = (Collection) query.execute();
        assertEquals(3, result.size());

        query = odmg.newOQLQuery();
        query.create("select products from " + Product.class.getName() + " where name like $1");
        query.bind(name + "_product_1");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        tx.commit();
    }

    public void testOneToNWithSelfReference_3() throws Exception
    {
        String name = "testOneToNWithSelfReference_3_" + System.currentTimeMillis();
        Shop s = new Shop();
        s.setName(name);
        Product p1 = new Product();
        p1.setName(name + "_product_1");
        Product p2 = new Product();
        p2.setName(name + "_product_2");

        List products = new ArrayList();
        products.add(p1);
        products.add(p2);

        s.setProducts(products);
        p1.setShop(s);
        p2.setShop(s);

        Product p1a = new Product();
        p1a.setName(name + "_subProduct_A");
        Product p1b = new Product();
        p1b.setName(name + "_subProduct_B");
        Product p1c = new Product();
        p1c.setName(name + "_subProduct_C");

        List subProducts = new ArrayList();
        subProducts.add(p1a);
        subProducts.add(p1b);
        subProducts.add(p1c);

        p1.setSubProducts(subProducts);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(s);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select products from " + Shop.class.getName() + " where name like $1");
        query.bind(name + "%");
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Shop newShop = (Shop) result.iterator().next();
        assertNotNull(newShop.getProducts());
        assertEquals(2, newShop.getProducts().size());

        boolean match = false;
        for(Iterator iterator = newShop.getProducts().iterator(); iterator.hasNext();)
        {
            Product p = (Product) iterator.next();
            if(p.getSubProducts() != null && p.getSubProducts().size() > 0)
            {
                match = true;
                assertEquals(3, p.getSubProducts().size());
            }
        }
        assertTrue("Sub products aren't stored", match);

        tx.begin();
        // now we enable cascading delete
        tx.setCascadingDelete(Shop.class, true);
        tx.setCascadingDelete(Product.class, true);
        database.deletePersistent(s);
        tx.commit();


        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select products from " + Product.class.getName() + " where name like $1");
        query.bind(name + "_subPro%");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select products from " + Product.class.getName() + " where name like $1");
        query.bind(name + "_product_1");
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        tx.commit();
    }

    public void testOneToOneWithBackReference_1() throws Exception
    {
        String name = "testOneToOneWithBackReference_1_" + System.currentTimeMillis();
        Shop s = new Shop();
        s.setName(name);
        ShopDetail sd = new ShopDetail();
        sd.setName(name);
        s.setDetail(sd);
        sd.setShop(s);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(s);
        // before establishing the circular references write
        // all objects to DB
        tx.flush();
        // now close the circular references
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select detail from " + ShopDetail.class.getName() + " where name like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        ShopDetail sdNew = (ShopDetail) result.iterator().next();
        assertNotNull(sdNew.getShop());

        tx.begin();
        tx.lock(sdNew, Transaction.WRITE);
        Shop tmp = sdNew.getShop();
        // it's bidirectional, so remove this references first
        tmp.setDetail(null);
        sdNew.setShop(null);
        database.deletePersistent(tmp);
        tx.flush();

        query = odmg.newOQLQuery();
        query.create("select detail from " + ShopDetail.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        ShopDetail newSd = (ShopDetail) result.iterator().next();
        assertNotNull(newSd);
        assertNull(newSd.getShop());
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select shops from " + Shop.class.getName() + " where name like $1");
        query.bind(name);
        Collection resultShop = (Collection) query.execute();
        assertEquals(0, resultShop.size());

        // delete ShopDetail too
        database.deletePersistent(newSd);
        tx.commit();
    }

    public void testOneToOneWithBackReference_3() throws Exception
    {
        String name = "testOneToOneWithBackReference_3_" + System.currentTimeMillis();
        Shop s = new Shop();
        s.setName(name);
        ShopDetail sd = new ShopDetail();
        sd.setName(name);
        s.setDetail(sd);
        sd.setShop(s);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(s);
        database.deletePersistent(s);
        database.makePersistent(s);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select shops from " + Shop.class.getName() + " where name like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Shop newShop = (Shop) result.iterator().next();
        assertNotNull(newShop.getDetail());

        tx.begin();
        database.deletePersistent(newShop);
        // add object again, we expect that nothing was deleted
        database.makePersistent(newShop);
        // flush changes to DB, we don't change anything
        tx.flush();
        query = odmg.newOQLQuery();
        query.create("select shops from " + Shop.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        Shop tmp = (Shop) result.iterator().next();
        assertNotNull(tmp.getDetail());

        tx.begin();
        database.deletePersistent(newShop);
        database.makePersistent(newShop);
        database.deletePersistent(newShop);
        tx.flush();
        database.deletePersistent(newShop.getDetail());
        tx.flush();

        query = odmg.newOQLQuery();
        query.create("select detail from " + ShopDetail.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select shops from " + Shop.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select detail from " + ShopDetail.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(0, result.size());

        query = odmg.newOQLQuery();
        query.create("select shops from " + Shop.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    public void testOneToOneWithBackReference_2() throws Exception
    {
        String name = "testOneToOneWithBackReference_2_" + System.currentTimeMillis();
        Shop s = new Shop();
        s.setName(name);
        ShopDetail sd = new ShopDetail();
        sd.setName(name);
        s.setDetail(sd);
        sd.setShop(s);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(s);
        // shop and shopDetail have a bidirectional 1:1 reference
        // on flush() OJB will insert both objects and set one FK
        tx.flush();
        // on commit() OJB will be aware of the bidirectional reference
        // and set the second FK
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select detail from " + ShopDetail.class.getName() + " where name like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        ShopDetail sdNew = (ShopDetail) result.iterator().next();
        assertNotNull(sdNew.getShop());

        tx.begin();
        // cascading delete should delete Shop too
        tx.setCascadingDelete(ShopDetail.class, true);
        database.deletePersistent(sdNew);
        tx.flush();

        query = odmg.newOQLQuery();
        query.create("select detail from " + ShopDetail.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();
        assertEquals(0, result.size());

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select detail from " + Shop.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(0, result.size());
        tx.commit();
    }

    public static class Shop
    {
        private Integer id;
        private String name;
        private ShopDetail detail;
        private List products;
        private List distributors;

        public Shop()
        {
        }

        public Shop(String name)
        {
            this.name = name;
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

        public ShopDetail getDetail()
        {
            return detail;
        }

        public void setDetail(ShopDetail detail)
        {
            this.detail = detail;
        }

        public List getProducts()
        {
            return products;
        }

        public void setProducts(List products)
        {
            this.products = products;
        }

        public List getDistributors()
        {
            return distributors;
        }

        public void setDistributors(List distributors)
        {
            this.distributors = distributors;
        }

        public void addDistributor(Distributor d)
        {
            if(this.distributors == null)
            {
                this.distributors = new ArrayList();
            }
            this.distributors.add(d);
        }
    }

    public static class Distributor
    {
        private Integer id;
        private String name;
        private List shops;

        public Distributor()
        {
        }

        public Distributor(String name)
        {
            this.name = name;
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

        public List getShops()
        {
            return shops;
        }

        public void setShops(List shops)
        {
            this.shops = shops;
        }

        public void addShop(Shop s)
        {
            if(this.shops == null)
            {
                this.shops = new ArrayList();
            }
            this.shops.add(s);
        }
    }

    public static class Product
    {
        private Integer id;
        private String name;
        private Shop shop;
        private Integer shopFk;
        private List subProducts;
        private Integer subProductFK;

        public Product()
        {
        }

        public Product(String name)
        {
            this.name = name;
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

        public Shop getShop()
        {
            return shop;
        }

        public void setShop(Shop shop)
        {
            this.shop = shop;
        }

        public Integer getShopFk()
        {
            return shopFk;
        }

        public void setShopFk(Integer shopFk)
        {
            this.shopFk = shopFk;
        }

        public Integer getSubProductFK()
        {
            return subProductFK;
        }

        public void setSubProductFK(Integer subProductFK)
        {
            this.subProductFK = subProductFK;
        }

        public void addSubProduct(Product p)
        {
            if(subProducts == null)
            {
                subProducts = new ArrayList();
            }
            subProducts.add(p);
        }

        public List getSubProducts()
        {
            return subProducts;
        }

        public void setSubProducts(List subProducts)
        {
            this.subProducts = subProducts;
        }
    }

    public static class ShopDetail
    {
        private Integer id;
        private String name;
        private Shop shop;

        public ShopDetail()
        {
        }

        public ShopDetail(String name)
        {
            this.name = name;
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

        public Shop getShop()
        {
            return shop;
        }

        public void setShop(Shop shop)
        {
            this.shop = shop;
        }
    }

    abstract static class BaseObject
    {
        private Integer id;
        private String name;
        private String ojbConcreteClass;

        public BaseObject()
        {
            this.ojbConcreteClass = this.getClass().getName();
        }

        public BaseObject(String name)
        {
            this();
            this.name = name;
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

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }

    public static class ObjectA extends BaseObject
    {
        private ObjectAA refAA;

        public ObjectA()
        {
        }

        public ObjectA(String name)
        {
            super(name);
        }

        public ObjectAA getRefAA()
        {
            return refAA;
        }

        public void setRefAA(ObjectAA refAA)
        {
            this.refAA = refAA;
        }
    }

    public static class ObjectAA extends BaseObject
    {
        private ObjectAAA refAAA;

        public ObjectAA()
        {
        }

        public ObjectAA(String name)
        {
            super(name);
        }

        public ObjectAAA getRefAAA()
        {
            return refAAA;
        }

        public void setRefAAA(ObjectAAA refAAA)
        {
            this.refAAA = refAAA;
        }
    }

    public static class ObjectAAA extends BaseObject
    {
        private ObjectAAAA refAAAA;
        private ObjectA refA;

        public ObjectAAA()
        {
        }

        public ObjectAAA(String name)
        {
            super(name);
        }

        public ObjectAAAA getRefAAAA()
        {
            return refAAAA;
        }

        public void setRefAAAA(ObjectAAAA refAAAA)
        {
            this.refAAAA = refAAAA;
        }

        public ObjectA getRefA()
        {
            return refA;
        }

        public void setRefA(ObjectA refA)
        {
            this.refA = refA;
        }
    }

    public static class ObjectAAAA extends BaseObject
    {
        private ObjectA refA;

        public ObjectAAAA()
        {
        }

        public ObjectAAAA(String name)
        {
            super(name);
        }

        public ObjectA getRefA()
        {
            return refA;
        }

        public void setRefA(ObjectA refA)
        {
            this.refA = refA;
        }
    }
}
