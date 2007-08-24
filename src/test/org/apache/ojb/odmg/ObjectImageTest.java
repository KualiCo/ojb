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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Iterator;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.core.proxy.CollectionProxy;
import org.apache.ojb.broker.core.proxy.CollectionProxyDefaultImpl;
import org.apache.ojb.broker.core.proxy.CollectionProxyListener;
import org.apache.ojb.broker.core.proxy.IndirectionHandler;
import org.apache.ojb.broker.core.proxy.MaterializationListener;
import org.apache.ojb.broker.core.proxy.ProxyHelper;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

/**
 * Check the quality of object image comparison:
 * - new referenced objects
 * - deleted referenced objects
 * - deleted referenced objects
 * ...etc.
 *
 * Test cases for refactored odmg-api implementation. These tests asserts previously outstanding
 * ODMG-issues and proxy object handling in the ODMG API.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: ObjectImageTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ObjectImageTest extends ODMGTestCase
{
    static final int NONE = ObjectReferenceDescriptor.CASCADE_NONE;
    static final int LINK = ObjectReferenceDescriptor.CASCADE_LINK;
    static final int OBJECT = ObjectReferenceDescriptor.CASCADE_OBJECT;
    static final String EOL = SystemUtils.LINE_SEPARATOR;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[]{ObjectImageTest.class.getName()});
    }

    public void testReplaceOneToOneReference() throws Exception
    {
        String prefix = "testReplaceOneToOneReference_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Book.class, "reviews", true, NONE, NONE, false);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        Book book = new Book(prefix, null, null);
        Publisher p_1 = new PublisherImpl(prefix);
        Publisher p_2 = new PublisherImpl(prefix + "_replaced");
        book.setPublisher(p_1);
        database.makePersistent(book);
        database.makePersistent(p_1);
        database.makePersistent(p_2);
        tx.commit();

        Integer book_version = book.getVersion();
        Integer p1_version = p_1.getVersion();
        Integer p2_version = p_2.getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        tx.lock(book.getPublisher(), Transaction.READ);
        tx.lock(p_2, Transaction.READ);
        book.setPublisher(p_2);
        tx.commit();

        assertEquals(book_version.intValue() + 1, book.getVersion().intValue());
        assertEquals(p1_version, p_1.getVersion());
        assertEquals(p2_version, p_2.getVersion());
    }

    public void testAddCollectionObjectToExistingObject() throws Exception
    {
        String prefix = "testAddCollectionObjectToExistingObject_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Book.class, "reviews", true, NONE, NONE, false);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        Book book = new Book(prefix, null, null);
        Review r1 = new Review(prefix + "_1");
        database.makePersistent(book);
        database.makePersistent(r1);
        tx.commit();

        Integer book_version = book.getVersion();
        Integer r_1_version = r1.getVersion();

        Review r2 = new Review(prefix + "_2");
        tx.begin();
        tx.lock(r1, Transaction.WRITE);
        tx.lock(book, Transaction.READ);
        book.addReview(r1);
        book.addReview(r2);
        database.makePersistent(r2);
        tx.commit();

        assertEquals(book_version, book.getVersion());
        assertTrue(book.getId() != null);

        Integer r_2_version = r2.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        Book loadedCopy = (Book) tx.getBroker().getObjectByIdentity(
                tx.getBroker().serviceIdentity().buildIdentity(Book.class, book.getId()));
        assertNotNull(loadedCopy);
        assertNotNull(loadedCopy.getReviews());
        assertEquals(2, loadedCopy.getReviews().size());
        assertEquals(book_version, loadedCopy.getVersion());
        Review new_r1 = (Review) loadedCopy.getReviews().get(0);
        Review new_r2 = (Review) loadedCopy.getReviews().get(1);
        assertEquals(r_1_version.intValue() + 1, new_r1.getVersion().intValue());
        assertEquals(r_2_version, new_r2.getVersion());

        tx.getBroker().clearCache();
        Criteria criteria = new Criteria();
        criteria.addLike("title", prefix);
        Query q = QueryFactory.newQuery(Book.class, criteria);
        Collection books = tx.getBroker().getCollectionByQuery(q);
        assertNotNull(books);
        assertEquals(1, books.size());
        Book new_book = (Book) books.iterator().next();
        tx.commit();
        assertEquals(book_version, new_book.getVersion());

        tx.begin();
        tx.lock(loadedCopy, Transaction.WRITE);
        Review removed = (Review) loadedCopy.getReviews().remove(0);
        Review stayed =  (Review) loadedCopy.getReviews().get(0);
        tx.commit();
        // expect same version, nothing should be changed
        assertEquals(r_2_version, stayed.getVersion());
        //
        //assertEquals(r_1_version, removed.getVersion());

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(prefix);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        tx.commit();

        assertEquals(1, b.getReviews().size());
        Review r = (Review) b.getReviews().get(0);
        if(!r.equals(r1) && !r.equals(r2))
        {
            fail("Wrong object or wrong object version returned. Returned obj was "
                    + EOL + r + " expected object was " + EOL + r1 + " or " + EOL + r2);
        }
    }

    /**
     * test persistence by reachability of collection reference objects
     */
    public void testPersistenceByReachability_1() throws Exception
    {
        String name = "testPersistenceByReachability_1_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Book.class, "reviews", true, NONE, NONE, true);

        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        Review r2 = new Review(name);
        Review r3 = new Review(name + "_not_persistent");
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        reviews.add(r2);
        reviews.add(r3);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        Review r4 = new Review(name + "_new_added");
        // add a new review after make persistent main object
        book.addReview(r4);
        tx.setCascadingDelete(Book.class, true);
        // remove object after make persistent main object
        book.removeReview(r3);
        tx.commit();
        // System.err.println("## Insert main object with 3 referecnes");

        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());

        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name + "_new_added");
        result = (Collection) query.execute();
        // we expect the delayed added Review object
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name + "_not_persistent");
        result = (Collection) query.execute();
        // we expect the removed Review object wasn't persistet
        assertEquals(0, result.size());
        tx.commit();
    }

    /**
     * test persistence by reachability of collection reference objects
     */
    public void testPersistenceByReachability_2() throws Exception
    {
        String name = "testPersistenceByReachability_2_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Book.class, "reviews", true, NONE, NONE, true);

        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        Review r2 = new Review(name);
        Review r3 = new Review(name + "_not_persistent");
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        reviews.add(r2);
        reviews.add(r3);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        Review r4 = new Review(name + "_new_added");
        // add a new review after make persistent main object
        book.addReview(r4);
        tx.setCascadingDelete(Book.class, true);
        // remove object after make persistent main object
        book.removeReview(r3);
        tx.checkpoint();

        //tx.begin();
        tx.getBroker().clearCache();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());

        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name + "_new_added%");
        result = (Collection) query.execute();
        // we expect the delayed added Review object
        assertEquals(1, result.size());

        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name + "_not_persistent");
        result = (Collection) query.execute();
        // we expect the removed Review object wasn't persistet
        assertEquals(0, result.size());

        b.setTitle(name + "_updated");
        tx.commit();

        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name + "_updated");
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b_updated = (Book) result.iterator().next();
        assertNotNull(b_updated.getReviews());
        assertEquals(3, b_updated.getReviews().size());
        assertEquals(name+"_updated", b_updated.getTitle());
    }

    public void testAddPersistentObjectTo1toN() throws Exception
    {
        String name = "testAddPersistentObjectTo1toN_" + System.currentTimeMillis();
        Review review = new Review(name);
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(review);
        tx.commit();

        Integer versionReview = review.getVersion();

        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        // tx.lock(review, Transaction.WRITE);
        database.makePersistent(book);
        book.addReview(review);
        tx.commit();

        // the Review object has to be linked
        assertEquals("expect that this object was updated", versionReview.intValue() + 1, review.getVersion().intValue());

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertNotNull(b.getReviews());
        assertEquals(1, b.getReviews().size());
        tx.commit();
    }

    public void testAddPersistentObjectToMtoN() throws Exception
    {
        String name = "testAddPersistentObjectToMtoN_" + System.currentTimeMillis();
        Author author = new Author(name, null);
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(author);
        tx.commit();

        Integer versionReview = author.getVersion();

        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        book.addAuthor(author);
        author.addBook(book);
        tx.commit();

        // the Review object has to be linked
        assertEquals("expect that this object wasn't updated", versionReview.intValue(), author.getVersion().intValue());

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertNotNull(b.getAuthors());
        assertEquals(1, b.getAuthors().size());
        tx.commit();
    }

    /**
     * only lock object, no changes made
     */
    public void testChangeMainFields() throws Exception
    {
        String name = "testChangeMainFields_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer version = book.getVersion();
        // System.err.println("### 1. commit, insert new object");

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        tx.commit();

        // System.err.println("### 2. commit, no changes");
        assertEquals(version, book.getVersion());

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        // we set the same date, so no reason to update
        book.setPublicationDate(new Date(date.getTime()));
        tx.commit();

        // System.err.println("### 3. commit, replace with same date");
        assertEquals(version, book.getVersion());

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        // now we change the date
        Date d = new Date(1111);
        book.setPublicationDate(d);
        tx.commit();
        // System.err.println("### 4. commit, changed date");
        assertFalse(date.equals(book.getPublicationDate()));
        assertFalse(version.equals(book.getVersion()));
    }

    /**
     * modify field of main object
     */
    public void testChangeMainFields_2() throws Exception
    {
        String name = "testChangeMainFields_2_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer version = book.getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        book.setCover(new byte[]{2,3,4,5,6,7,8,8});
        tx.commit();

        assertFalse(version.equals(book.getVersion()));
        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertFalse(version.equals(b.getVersion()));
        tx.commit();
    }

    /**
     * lock object and lock serialized unmodified version again
     */
    public void testChangeMainFields_3() throws Exception
    {
        String name = "testChangeMainFields_3_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer version = book.getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        book = (Book) SerializationUtils.clone(book);
        tx.lock(book, Transaction.WRITE);
        tx.commit();

        assertEquals(version, book.getVersion());
    }

    /**
     * Double lock object with reference
     */
    public void testChangeOneToOneReference_1() throws Exception
    {
        String name = "testChangeOneToOneReference_1_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("### 1. commit, insert new object");

        Integer versionBook = book.getVersion();
        Integer versionPublisher = book.getPublisher().getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        tx.lock(book, Transaction.WRITE);
        tx.commit();
        // System.err.println("### 2. commit, double lock call, no changes");

        assertEquals(versionBook, book.getVersion());
        assertEquals(versionPublisher, book.getVersion());
    }

    /**
     * Double lock object with reference and proxy reference
     */
    public void testChangeOneToOneReference_1b() throws Exception
    {
        String name = "testChangeOneToOneReference_1b_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Book.class, "publisher", true, NONE, NONE, true);

        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("### 1. commit, insert new object");

        Integer versionBook = book.getVersion();
        Integer versionPublisher = book.getPublisher().getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        tx.lock(book, Transaction.WRITE);
        tx.commit();
        // System.err.println("### 2. commit, double lock call, no changes");

        assertEquals(versionBook, book.getVersion());
        assertEquals(versionPublisher, book.getVersion());
    }

    /**
     * lock object with reference and lock serialized version again
     */
    public void testChangeOneToOneReference_2() throws Exception
    {
        String name = "testChangeOneToOneReference_2_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("### 1. commit, insert new object");

        Integer versionBook = book.getVersion();
        Integer versionPublisher = book.getPublisher().getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        // nothing changed, so no need to update objects
        book = (Book) SerializationUtils.clone(book);
        tx.lock(book, Transaction.WRITE);
        tx.commit();

        assertEquals(versionBook, book.getVersion());
        assertEquals(versionPublisher, book.getVersion());
    }

    /**
     * lock object with reference, change reference only
     */
    public void testChangeOneToOneReference_3() throws Exception
    {
        String name = "testChangeOneToOneReference_2_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("### 1. commit, insert new object");

        Integer versionBook = book.getVersion();
        Integer versionPublisher = book.getPublisher().getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        // nothing changed, so no need to update objects
        book = (Book) SerializationUtils.clone(book);
        Publisher p = book.getPublisher();
        p.setName(name + "_updated");
        // not needed to re-lock, because nothing changed, but
        // if we lock Book no update should be done, because nothing changed
        tx.lock(book, Transaction.WRITE);
        // we have to re-lock the changed objects, because it was serialized
        tx.lock(p, Transaction.WRITE);
        tx.commit();

        // no changes made in Book
        assertEquals(versionBook, book.getVersion());
        // publisher should be updated
        assertEquals(new Integer(versionPublisher.intValue() + 1), p.getVersion());
    }

    /**
     * lock object with reference, replace reference only
     */
    public void testReplaceOneToOneReference_1() throws Exception
    {
        String name = "testChangeOneToOneReference_2_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("### 1. commit, insert new object");

        Integer versionBook = book.getVersion();
        Integer versionPublisher = book.getPublisher().getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        Publisher p = new PublisherImpl(name + "_new");
        // set new Publisher instance
        book.setPublisher(p);
        tx.lock(p, Transaction.WRITE);
        tx.commit();

        // changes made in Book
        assertEquals(new Integer(versionBook.intValue() + 1), book.getVersion());
        // publisher should not be updated, because it was replaced
        assertEquals(versionPublisher, p.getVersion());
    }

    /**
     * lock object with reference, replace reference only
     */
    public void testReplaceOneToOneReference_2() throws Exception
    {
        String name = "testChangeOneToOneReference_2_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("### 1. commit, insert new object");

        Integer versionBook = book.getVersion();
        Integer versionPublisher = book.getPublisher().getVersion();

        tx.begin();
        tx.lock(book, Transaction.WRITE);
        book = (Book) SerializationUtils.clone(book);
        Publisher p = new PublisherImpl(name + "_new");
        // set new Publisher instance
        book.setPublisher(p);
        // not needed to re-lock, because nothing changed, but
        // if we lock Book no update should be done, because nothing changed
        tx.lock(book, Transaction.WRITE);
        // we have to re-lock the changed objects, because it was serialized
        tx.lock(p, Transaction.WRITE);
        tx.commit();

        // changes made in Book
        assertEquals(new Integer(versionBook.intValue() + 1), book.getVersion());
        // publisher should not be updated, because it was replaced
        assertEquals(versionPublisher, p.getVersion());
    }

    /**
     * check materialization of proxy object
     */
    public void testChangeOneToOneReference_4() throws Exception
    {
        ojbChangeReferenceSetting(Book.class, "publisher", true, NONE, NONE, true);
        String name = "testChangeOneToOneReference_4_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        IndirectionHandler handler = ProxyHelper.getIndirectionHandler(b.getPublisher());
        assertNotNull(handler);
        assertFalse(handler.alreadyMaterialized());
        handler.addListener(
                new MaterializationListener()
                {
                    public void beforeMaterialization(IndirectionHandler handler, Identity oid)
                    {
                        fail("Reference shall not materialize while locking");
                    }

                    public void afterMaterialization(IndirectionHandler handler, Object materializedObject)
                    {
                    }
                }
        );
        tx.lock(b, Transaction.WRITE);
        tx.commit();

        assertEquals(versionBook, b.getVersion());
    }

    /**
     * replace proxy reference by new reference object
     */
    public void testChangeOneToOneReference_5() throws Exception
    {
        ojbChangeReferenceSetting(Book.class, "publisher", true, NONE, NONE, true);
        String name = "testChangeOneToOneReference_5_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("### 1. commit, insert new object");


        Integer versionBook = book.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        IndirectionHandler handler = ProxyHelper.getIndirectionHandler(b.getPublisher());
        assertNotNull(handler);
        assertFalse(handler.alreadyMaterialized());
        handler.addListener(
                new MaterializationListener()
                {
                    public void beforeMaterialization(IndirectionHandler handler, Identity oid)
                    {
                        fail("Reference shall not materialize while locking");
                    }

                    public void afterMaterialization(IndirectionHandler handler, Object materializedObject)
                    {
                    }
                }
        );
        // no need to lock with default settings, because lock is done when query object
        tx.lock(b, Transaction.WRITE);
        // replace 1:1 reference
        Publisher p = new PublisherImpl(name+"_new");
        b.setPublisher(p);
        //tx.lock(p, Transaction.WRITE);
        tx.commit();

        // we expect increased version, because Book object needs update - changed FK
        assertEquals(new Integer(versionBook.intValue() + 1), b.getVersion());
        // should should differ, because new reference should be stored
        assertFalse(publisher.getName().equals(p.getName()));
        assertNotNull(p.getVersion());

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();
        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        Publisher newP = b.getPublisher();
        assertNotNull(newP);
        assertEquals(name+"_new", newP.getName());
    }

    /**
     * update referenced object
     */
    public void testChangeOneToOneReference_6() throws Exception
    {
        String name = "testChangeOneToOneReference_6_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();
        Integer versionPublisher = book.getPublisher().getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        tx.lock(b, Transaction.WRITE);
        b.getPublisher().setName("updated_" + b.getPublisher().getName());
        tx.commit();

        // nothing changed
        assertEquals(versionBook, b.getVersion());
        // version should should differ, because reference should be updated
        assertFalse(versionPublisher.equals(b.getPublisher().getVersion()));
        assertEquals("updated_" + name, b.getPublisher().getName());
    }

    /**
     * add new reference
     */
    public void testChangeOneToOneReference_7() throws Exception
    {
        String name = "testChangeOneToOneReference_7_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();

        tx.lock(b, Transaction.WRITE);
        assertNull(b.getPublisher());
        Publisher publisher = new PublisherImpl(name);
        b.setPublisher(publisher);
        // tx.lock(publisher, Transaction.WRITE);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        tx.commit();

        // we expect increased version, because Book object needs update
        assertEquals(new Integer(versionBook.intValue() + 1), b.getVersion());
        // version should differ from null, because new reference should be stored
        assertNotNull(b.getPublisher());
        assertNotNull(b.getPublisher().getVersion());
        assertEquals(name, b.getPublisher().getName());
    }

    /**
     * remove reference
     */
    public void testChangeOneToOneReference_8() throws Exception
    {
        String name = "testChangeOneToOneReference_8_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();
        Integer versionPublisher = book.getPublisher().getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();

        tx.lock(b, Transaction.WRITE);
        b.setPublisher(null);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        tx.commit();

        // we expect increased version, because Book object needs update
        assertEquals(new Integer(versionBook.intValue() + 1), b.getVersion());
        assertNull(b.getPublisher());

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select publishers from " + Publisher.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        // we don't remove the reference object
        assertEquals(1, result.size());
        Publisher p = (Publisher) result.iterator().next();
        assertEquals(name, p.getName());
        // removed 1:1 reference, expect unchanged version
        assertEquals(versionPublisher, p.getVersion());
        tx.commit();
    }

    /**
     * delete reference
     */
    public void testChangeOneToOneReference_8b() throws Exception
    {
        String name = "testChangeOneToOneReference_8b_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Publisher publisher = new PublisherImpl(name);
        book.setPublisher(publisher);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();

        tx.lock(b, Transaction.WRITE);
        database.deletePersistent(b.getPublisher());
        b.setPublisher(null);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        tx.commit();

        // we expect increased version, because Book object needs update
        assertEquals(new Integer(versionBook.intValue() + 1), b.getVersion());
        assertNull(b.getPublisher());

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select publishers from " + Publisher.class.getName() + " where name like $1");
        query.bind(name);
        result = (Collection) query.execute();
        // we don't remove the reference object
        assertEquals(0, result.size());
        tx.commit();
    }

    /**
     * check materialzation of collection reference
     */
    public void testCollectionReference_1() throws Exception
    {
        String name = "testCollectionReference_1_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(Book.class, "reviews", true, NONE, NONE, true);

        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        Review r2 = new Review(name);
        Review r3 = new Review(name);
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        reviews.add(r2);
        reviews.add(r3);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("## Insert main object with 3 referecnes");

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        CollectionProxy handler = ProxyHelper.getCollectionProxy(b.getReviews());
        assertFalse("Don't expect an materialized collection proxy", handler.isLoaded());
        handler.addListener(new CollectionProxyListener()
        {
            public void beforeLoading(CollectionProxyDefaultImpl colProxy)
            {
                fail("Collection proxy shouldn't be materialized");
            }

            public void afterLoading(CollectionProxyDefaultImpl colProxy)
            {
            }
        });
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());

        tx.lock(b, Transaction.WRITE);
        tx.commit();
    }

    /**
     * update collection reference object
     */
    public void testCollectionReference_2a() throws Exception
    {
        String name = "testCollectionReference_2a_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        Review r2 = new Review(name);
        Review r3 = new Review(name);
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        reviews.add(r2);
        reviews.add(r3);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("## Insert main object with 3 referecnes");

        Integer versionBook = book.getVersion();
        Integer versionR1 = ((Review) book.getReviews().get(0)).getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());

        tx.lock(b, Transaction.WRITE);
        Review newR2 = (Review) b.getReviews().get(1);
        newR2.setSummary("updated" + name);
        final int newR2id = newR2.getId().intValue();
        tx.commit();

        assertEquals(versionBook, b.getVersion());
        // this referenced object was not updated
        Integer versionR1New = ((Review) b.getReviews().get(0)).getVersion();
        assertEquals(versionR1, versionR1New);

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());

        // Search for the updated R2:
        final List updatedReviews = b.getReviews();
        for (int i = 0; i < updatedReviews.size(); i++)
        {
            newR2 = (Review) updatedReviews.get(i);
            if (newR2id == newR2.getId().intValue()) {
                break;
            }
        }
        assertEquals("Could not find the updated review in the returned results",
                newR2id, newR2.getId().intValue());
        assertEquals("updated" + name, newR2.getSummary());
        assertEquals(versionBook, b.getVersion());
        // this referenced object was not updated
        versionR1New = ((Review) b.getReviews().get(0)).getVersion();
        assertEquals(versionR1, versionR1New);
    }

    /**
     * update proxy collection reference object
     */
    public void testCollectionReference_2b() throws Exception
    {
        String name = "testCollectionReference_2b_" + System.currentTimeMillis();

        ojbChangeReferenceSetting(Book.class, "reviews", true, NONE, NONE, true);

        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        Review r2 = new Review(name);
        Review r3 = new Review(name);
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        reviews.add(r2);
        reviews.add(r3);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();
        // System.err.println("## Insert main object with 3 referecnes");

        Integer versionBook = book.getVersion();
        Integer versionR0 = ((Review) book.getReviews().get(0)).getVersion();
        Integer versionR1 = ((Review) book.getReviews().get(1)).getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        // System.err.println("## Started new tx");
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());
        assertEquals(versionR0, ((Review) b.getReviews().get(0)).getVersion());
        assertEquals(versionR1, ((Review) b.getReviews().get(1)).getVersion());

        // System.err.println("## Query done");
        tx.lock(b, Transaction.WRITE);
        // System.err.println("## Lock object again");
        Review newR1 = (Review) b.getReviews().get(1);
        newR1.setSummary("updated" + name);
        // System.err.println("## Before commit");
        tx.commit();
        // System.err.println("## Commit Book with updated Review");

        assertEquals(versionBook, b.getVersion());
        assertEquals(3, b.getReviews().size());

        // the updated one
        Integer versionR1New = ((Review) b.getReviews().get(1)).getVersion();
        assertEquals(new Integer(versionR1.intValue() + 1), versionR1New);
        // this referenced object was not updated
        Integer versionR0New = ((Review) b.getReviews().get(0)).getVersion();
        assertEquals(versionR0, versionR0New);

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());
        tx.commit();

        // query for updated Review
        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind("updated" + name);
        result = (Collection) query.execute();
        tx.commit();
        // the update Review object
        assertEquals(1, result.size());
        // query for unchanged Review objects
        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(2, result.size());
        assertEquals(versionR0, ((Review) new ArrayList(result).get(0)).getVersion());

    }

    /**
     * remove collection reference object
     * this test expects that removed 1:n referenced objects only
     * be "unlinked" instead of deleted.
     */
    public void testCollectionReference_3() throws Exception
    {
        String name = "testCollectionReference_3_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();
        Integer versionR1 = ((Review) book.getReviews().get(0)).getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        assertNotNull(b.getReviews());
        assertEquals(1, b.getReviews().size());

        //***********************************
        tx.lock(b, Transaction.WRITE);
        // remove from collection, but do not explicit delete
        tx.setCascadingDelete(Book.class, "reviews", false);
        Review newR1 = (Review) b.getReviews().remove(0);
        tx.commit();
        //***********************************

        // only the removed reference has changed
        assertEquals(versionBook, b.getVersion());
        // expect an "unlinked" new version
        assertEquals(new Integer(versionR1.intValue() + 1), newR1.getVersion());

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();

        // we don't delete the Review object, only remove from reference collection
        assertEquals(1, result.size());
        Review r = (Review) result.iterator().next();
        // expect new object version, because the FK to main object was set null
        assertEquals(new Integer(versionR1.intValue() + 1), r.getVersion());

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        // we have removed the Review object
        assertEquals(0, b.getReviews().size());
    }

    /**
     * remove collection reference object and explicit delete it
     */
    public void testCollectionReference_3b() throws Exception
    {
        String name = "testCollectionReference_3b_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        assertNotNull(b.getReviews());
        assertEquals(1, b.getReviews().size());

        tx.lock(b, Transaction.WRITE);
        // remove from collection and delete
        Review newR1 = (Review) b.getReviews().remove(0);
        database.deletePersistent(newR1);
        tx.commit();

        // only the removed reference has changed
        assertEquals(versionBook, b.getVersion());

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(0, result.size());

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        assertEquals(0, b.getReviews().size());
    }

    /**
     * remove collection reference object with enabled auto-delete
     */
    public void testCollectionReference_3c() throws Exception
    {
        String name = "testCollectionReference_3c_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        assertNotNull(b.getReviews());
        assertEquals(1, b.getReviews().size());

        tx.lock(b, Transaction.WRITE);
        tx.setCascadingDelete(Book.class, "reviews", true);
        // remove from collection with cascading delete
        b.getReviews().remove(0);
        tx.commit();

        // only the removed reference has changed
        assertEquals(versionBook, b.getVersion());

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();

        // cascading delete was used, so we don't expect an unlinked
        // version of the Review class
        assertEquals(0, result.size());


        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();

        assertEquals(1, result.size());
        b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        // we have removed the Review object
        assertEquals(0, b.getReviews().size());
    }

    /**
     * delete Book object with existing references
     */
    public void testCollectionReference_4a() throws Exception
    {
        String name = "testCollectionReference_4_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        Review r2 = new Review(name);
        Review r3 = new Review(name);
        Author a1 = new Author(name, null);
        Author a2 = new Author(name, null);
        r1.setAuthor(a1);
        r2.setAuthor(a1);
        r3.setAuthor(a2);
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        reviews.add(r2);
        reviews.add(r3);
        book.setReviews(reviews);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());
        assertNotNull(((Review) b.getReviews().get(0)).getAuthor());
        assertNotNull(((Review) b.getReviews().get(1)).getAuthor());
        assertNotNull(((Review) b.getReviews().get(2)).getAuthor());

        // Book instance should be already locked
        // now mark Book for delete and disable cascading delete
        // for the 1:n relation to Review class
        tx.setCascadingDelete(Book.class, "reviews", false);
        database.deletePersistent(b);
        tx.commit();
        // System.out.println("## After commit");

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();
        // we auto-delete the Review object, only remove from reference collection
        assertEquals(3, result.size());
        List list = new ArrayList(result);
        assertNotNull(((Review) list.get(0)).getAuthor());
        assertNotNull(((Review) list.get(1)).getAuthor());
        assertNotNull(((Review) list.get(2)).getAuthor());
        Review newR1 = (Review) list.get(0);
        // book was deleted so we expect unlink of FK
        assertNull(newR1.getFkBook());


        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();
        assertEquals(0, result.size());
    }

    /**
     * delete Book object with existing references and circular referencing objects
     */
    public void testCollectionReference_4b() throws Exception
    {
        String name = "testCollectionReference_4_" + System.currentTimeMillis();
        Date date = new Date();
        byte[] cover = new byte[]{2,3,4,5,6,7,8,9};
        Book book = new Book(name, date, cover);
        Review r1 = new Review(name);
        Review r2 = new Review(name);
        Review r3 = new Review(name);
        Author a1 = new Author(name, null);
        Author a2 = new Author(name, null);
        r1.setAuthor(a1);
        r2.setAuthor(a1);
        r3.setAuthor(a2);
        ArrayList reviews = new ArrayList();
        reviews.add(r1);
        reviews.add(r2);
        reviews.add(r3);
        book.setReviews(reviews);
        ArrayList authors = new ArrayList();
        authors.add(a1);
        authors.add(a2);
        book.setAuthors(authors);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(book);
        tx.commit();

        Integer versionBook = book.getVersion();

        tx.begin();
        tx.getBroker().clearCache();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        Collection result = (Collection) query.execute();
        assertEquals(1, result.size());
        Book b = (Book) result.iterator().next();
        assertEquals(versionBook, b.getVersion());
        assertNotNull(b.getReviews());
        assertEquals(3, b.getReviews().size());
        assertNotNull(((Review) b.getReviews().get(0)).getAuthor());
        assertNotNull(((Review) b.getReviews().get(1)).getAuthor());
        assertNotNull(((Review) b.getReviews().get(2)).getAuthor());
        assertNotNull(b.getAuthors());
        assertEquals(2, b.getAuthors().size());
        Author newA = (Author) b.getAuthors().get(0);
        boolean failed = true;
        for(Iterator iterator = b.getReviews().iterator(); iterator.hasNext();)
        {
            Review review =  (Review) iterator.next();
            if(newA.equals(review.getAuthor()))
            {
                // as we have circular references we expect the same object instance
                assertSame(newA, review.getAuthor());
                failed = false;
            }
        }
        if(failed) fail("Expect the same object instance, but not found for " + newA);

        // Book instance should be already locked
        // now mark Book for delete and disable cascading delete
        // for the 1:n relation to Review class
        tx.setCascadingDelete(Book.class, "reviews", false);
        database.deletePersistent(b);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        query = odmg.newOQLQuery();
        query.create("select reviews from " + Review.class.getName() + " where summary like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();
        // we auto-delete the Review object, only remove from reference collection
        assertEquals(3, result.size());
        List list = new ArrayList(result);
        assertNotNull(((Review) list.get(0)).getAuthor());
        assertNotNull(((Review) list.get(1)).getAuthor());
        assertNotNull(((Review) list.get(2)).getAuthor());
        Review newR1 = (Review) list.get(0);
        // book was deleted so we expect unlink of FK
        assertNull(newR1.getFkBook());
        newA = ((Review) list.get(0)).getAuthor();
        assertEquals(0, newA.getBooks().size());


        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select books from " + Book.class.getName() + " where title like $1");
        query.bind(name);
        result = (Collection) query.execute();
        tx.commit();
        assertEquals(0, result.size());
    }

    //=======================================================
    // inner test classes
    //=======================================================
    public static final class Book implements Serializable
    {
        private Integer id;
        private String title;
        private Date publicationDate;
        private byte[] cover;
        private Integer version;

        private List authors;
        private List reviews;
        private Publisher publisher;

        public Book()
        {
        }

        public Book(String title, Date publicationDate, byte[] cover)
        {
            this.title = title;
            this.publicationDate = publicationDate;
            this.cover = cover;
        }

        public void addAuthor(Author author)
        {
            if(authors == null)
            {
                authors = new ArrayList();
            }
            authors.add(author);
        }

        public void addReview(Review review)
        {
            if(reviews == null)
            {
                reviews = new ArrayList();
            }
            reviews.add(review);
        }

        public boolean removeReview(Review review)
        {
            if(reviews != null) return reviews.remove(review);
            else return false;
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public Date getPublicationDate()
        {
            return publicationDate;
        }

        public void setPublicationDate(Date publicationDate)
        {
            this.publicationDate = publicationDate;
        }

        public byte[] getCover()
        {
            return cover;
        }

        public void setCover(byte[] cover)
        {
            this.cover = cover;
        }

        public Integer getVersion()
        {
            return version;
        }

        public void setVersion(Integer version)
        {
            this.version = version;
        }

        public List getAuthors()
        {
            return authors;
        }

        public void setAuthors(List authors)
        {
            this.authors = authors;
        }

        public List getReviews()
        {
            return reviews;
        }

        public void setReviews(List reviews)
        {
            this.reviews = reviews;
        }

        public Publisher getPublisher()
        {
            return publisher;
        }

        public void setPublisher(Publisher publisher)
        {
            this.publisher = publisher;
        }
    }

    public static final class Author implements Serializable
    {
        private Integer id;
        private String name;
        private List books;
        private Integer version;

        public Author()
        {
        }

        public Author(String name, List books)
        {
            this.name = name;
            this.books = books;
        }

        public void addBook(Book book)
        {
            if(books == null)
            {
                books = new ArrayList();
            }
            books.add(book);
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

        public List getBooks()
        {
            return books;
        }

        public void setBooks(List books)
        {
            this.books = books;
        }

        public Integer getVersion()
        {
            return version;
        }

        public void setVersion(Integer version)
        {
            this.version = version;
        }
    }

    public static interface Publisher extends Serializable
    {
        public Integer getId();
        public void setId(Integer id);
        public String getName();
        public void setName(String name);
        public Integer getVersion();
        public void setVersion(Integer version);
    }

    public static final class PublisherImpl implements Publisher
    {
        private Integer id;
        private String name;
        private Integer version;

        public PublisherImpl()
        {
        }

        public PublisherImpl(String name)
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

        public Integer getVersion()
        {
            return version;
        }

        public void setVersion(Integer version)
        {
            this.version = version;
        }
    }

    public static final class Review implements Serializable
    {
        private Integer id;
        private String summary;
        private Integer fkBook;
        private Integer version;
        private Author author;

        public Review()
        {
        }

        public Review(String summary)
        {
            this.summary = summary;
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public Integer getFkBook()
        {
            return fkBook;
        }

        public void setFkBook(Integer fkBook)
        {
            this.fkBook = fkBook;
        }

        public String getSummary()
        {
            return summary;
        }

        public void setSummary(String summary)
        {
            this.summary = summary;
        }

        public Integer getVersion()
        {
            return version;
        }

        public void setVersion(Integer version)
        {
            this.version = version;
        }

        public Author getAuthor()
        {
            return author;
        }

        public void setAuthor(Author author)
        {
            this.author = author;
        }

        public boolean equals(Object obj)
        {
            boolean result = false;
            if(obj instanceof Review)
            {
                Review other = (Review) obj;
                result = new EqualsBuilder()
                        .append(id, other.id)
                        .append(summary, other.summary)
                        .append(version, other.version)
                        .append(fkBook, other.fkBook)
                        .append(author, other.author)
                        .isEquals();
            }
            return result;
        }
    }

}
