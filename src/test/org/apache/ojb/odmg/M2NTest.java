package org.apache.ojb.odmg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.ODMGTestCase;
import org.odmg.OQLQuery;
import org.odmg.QueryException;
import org.odmg.Transaction;

/**
 * Test m:n relation handling with the odmg-api. The mandatory auto-update/auto-delete
 * setting are 'false' in that case equals to 'LINK'.
 * <p/>
 * TODO: we need more tests doing delete/update operations on M:N relations
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: M2NTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class M2NTest extends ODMGTestCase
{
    static final int NONE = ObjectReferenceDescriptor.CASCADE_NONE;
    static final int LINK = ObjectReferenceDescriptor.CASCADE_LINK;
    static final int OBJECT = ObjectReferenceDescriptor.CASCADE_OBJECT;
    boolean oldImpliciteWriteLock;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[]{M2NTest.class.getName()});
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        oldImpliciteWriteLock = odmg.isImpliciteWriteLocks();
    }

    protected void tearDown() throws Exception
    {
        odmg.setImpliciteWriteLocks(oldImpliciteWriteLock);
        super.tearDown();
    }

    public void testOnetoNViaIndirectionTable() throws Exception
    {
        String name = "testOnetoNViaIndirectionTable_" + System.currentTimeMillis();
        int opId = (int) (Math.random() * Integer.MAX_VALUE);
        String cId = "m2n_" + opId;

        // explicit set false
        odmg.setImpliciteWriteLocks(false);

        // Make a profile and County
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        OfficeProfile profile = new OfficeProfileImpl();
        profile.setId(opId);
        County c = new County();
        c.setName(name);
        c.setId(cId);
        odmg.getDatabase(null).makePersistent(profile);
        odmg.getDatabase(null).makePersistent(c);
        tx.commit();

        tx.begin();
        PersistenceBroker pb = tx.getBroker();
        pb.clearCache();
        // use PB-api to lookup object
        Criteria crit = new Criteria();
        crit.addLike("name", name);
        Query q = QueryFactory.newQuery(County.class, crit);
        County county = (County) pb.getObjectByQuery(q);
        tx.commit();

        // Add a county to it
        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.lock(profile, Transaction.WRITE);
        tx.lock(county, Transaction.READ);
        // add already persisted County object
        profile.addCounty(county);
        // add a new County object
        County c2 = new County();
        c2.setId(cId + "_new");
        c2.setName(name + "_new");
        profile.addCounty(c2);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select all from " + OfficeProfile.class.getName() + " where id=$1");
        query.bind(new Integer(opId));
        List result = (List) query.execute();
        assertEquals(1, result.size());
        tx.commit();
        OfficeProfile newProfile = (OfficeProfile) result.get(0);
        assertTrue(profile.equals(newProfile));

        tx.begin();
        tx.lock(newProfile, Transaction.WRITE);
        List counties = newProfile.getCounties();
        for(int i = 0; i < counties.size(); i++)
        {
            County tmp =  (County) counties.get(i);
            tmp.setName(tmp.getName() + "_updated");
        }
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select all from " + County.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (List) query.execute();
        assertEquals(2, result.size());
        tx.commit();
        for(int i = 0; i < counties.size(); i++)
        {
            County tmp = (County) counties.get(i);
            assertTrue(StringUtils.contains(tmp.getName(), "_updated"));
        }

        tx.begin();
        tx.lock(newProfile, Transaction.WRITE);
        counties = newProfile.getCounties();
        // this only remove the indirection table entry
        County removed = (County) counties.remove(0);
        // update the "unlinked" object
        removed.setName(removed.getName() + "_unlinked");
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select all from " + County.class.getName() + " where name=$1");
        query.bind(removed.getName());
        result = (List) query.execute();
        assertEquals(1, result.size());
        tx.commit();

        tx.begin();
        tx.setCascadingDelete(OfficeProfileImpl.class, true);
        database.deletePersistent(newProfile);
        tx.commit();

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select all from " + County.class.getName() + " where name like $1");
        query.bind(name + "%");
        result = (List) query.execute();
        // expect to find the unlinked County object
        assertEquals(1, result.size());
        tx.commit();
    }

    public void testTwoMtoNRelationsInOneClass() throws Exception
    {
        String postfixId = "testTwoMtoNRelationsInOneClass_" + System.currentTimeMillis();
        Movie m = new MovieImpl(postfixId, "Movie_" + postfixId, "none");

        Actor a1 = new Actor("Actor_1_" + postfixId);
        m.addActors(a1);

        Actor a2a = new Actor("Actor_2a_" + postfixId);
        m.addActors2(a2a);
        Actor a2b = new Actor("Actor_2b_" + postfixId);
        m.addActors2(a2b);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(m);
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();

        OQLQuery queryMovie = movieQuery(postfixId);
        Collection resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        Movie newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(1, newMovie.getActors().size());
        assertEquals(2, newMovie.getActors2().size());

        OQLQuery queryActor = actorQuery(postfixId);
        Collection resultActor = (Collection) queryActor.execute();
        assertEquals(1 + 2, resultActor.size());

        OQLQuery queryRole = roleQuery(null, m);
        Collection resultRole = (Collection) queryRole.execute();
        assertEquals(1, resultRole.size());

        // remove both Actors from relation and delete one Actor
        // instance completely, the other Actor should still in DB
        Object removed1 = newMovie.getActors2().remove(0);
        Actor removed2 = (Actor) newMovie.getActors2().remove(0);
        database.deletePersistent(removed1);
        // update the Actor unlinked from relation
        tx.lock(removed2, Transaction.WRITE);
        String newName = removed2.getName() + "_updated";
        removed2.setName(newName);
        tx.commit();

        queryMovie = movieQuery(postfixId);
        resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(1, newMovie.getActors().size());
        assertEquals(0, newMovie.getActors2().size());

        queryActor = actorQuery(postfixId);
        resultActor = (Collection) queryActor.execute();
        assertEquals(1 + 1, resultActor.size());

        queryActor = actorQuery(newName);
        resultActor = (Collection) queryActor.execute();
        assertEquals(1, resultActor.size());

        queryRole = roleQuery(null, m);
        resultRole = (Collection) queryRole.execute();
        assertEquals(1, resultRole.size());
   }

    public void testStore() throws Exception
    {
        // arminw: fixed
        // TODO: Seems that the order of objects is not valid to insert M:N
        //if(ojbSkipKnownIssueProblem()) return;
        changeMovieCollectionDescriptorTo(true, NONE, NONE, false);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStore();
    }

    public void testStoreWithProxy() throws Exception
    {
        // arminw: fixed
        // TODO: Seems that the order of objects is not valid to insert M:N
        // if(ojbSkipKnownIssueProblem()) return;
        changeMovieCollectionDescriptorTo(true, NONE, NONE, true);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStore();
    }

    public void doTestStore() throws Exception
    {
        String postfix = "doTestStore_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActors(postfix);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        database.makePersistent(movie);
        tx.commit();

        OQLQuery queryMovie = movieQuery(postfix);
        Collection resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        Movie newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(3, newMovie.getActors().size());
        assertEquals(2, newMovie.getActors2().size());

        OQLQuery queryActor = actorQuery(postfix);
        Collection resultActor = (Collection) queryActor.execute();
        assertEquals(3 + 2, resultActor.size());

        OQLQuery queryRole = roleQuery(null, movie);
        Collection resultRole = (Collection) queryRole.execute();
        assertEquals(3, resultRole.size());
   }

    public void testStore_2() throws Exception
    {
        changeMovieCollectionDescriptorTo(true, NONE, NONE, false);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStore_2();
    }

// arminw: this test will not work, because we use a user defined ManageableCollection
// and the collection proxy is not compatible. Maybe this test will work in future versions
// when it's possible set a CollectionProxy impl in metadate.    
//    public void testStore_2WithProxy() throws Exception
//    {
//        prepareAutoUpdateDeleteSettings(true);
//        doTestStore_2();
//    }

    public void doTestStore_2() throws Exception
    {
        String postfix = "doTestStore_2_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActors(postfix);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        Iterator it = movie.getActors().iterator();
        while(it.hasNext())
        {
            database.makePersistent(it.next());
        }
        database.makePersistent(movie);
        tx.commit();

        OQLQuery queryMovie = movieQuery(postfix);
        Collection resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        Movie newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(3, newMovie.getActors().size());

        OQLQuery queryActor = actorQuery(postfix);
        Collection resultActor = (Collection) queryActor.execute();
        assertEquals(5, resultActor.size());

        OQLQuery queryRole = roleQuery(null, movie);
        Collection resultRole = (Collection) queryRole.execute();
        assertEquals(3, resultRole.size());
    }

    public void testStoreComplex() throws Exception
    {
        // arminw: fixed
        // TODO: Seems that the order of objects is not valid to insert M:N
        // if(ojbSkipKnownIssueProblem()) return;
        changeMovieCollectionDescriptorTo(true, NONE, NONE, false);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStoreComplex();
    }

    public void testStoreComplexWithProxy() throws Exception
    {
        // arminw: fixed
        // TODO: Seems that the order of objects is not valid to insert M:N
        // if(ojbSkipKnownIssueProblem()) return;
        changeMovieCollectionDescriptorTo(true, NONE, NONE, true);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStoreComplex();
    }


    public void doTestStoreComplex() throws Exception
    {
        String postfix = "doTestStoreComplex_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        database.makePersistent(movie);
        tx.commit();

        OQLQuery queryMovie = movieQuery(postfix);
        Collection resultMovie = (Collection) queryMovie.execute();
        assertEquals(3, resultMovie.size());
        Iterator it = resultMovie.iterator();
        boolean matchActors = false;
        while(it.hasNext())
        {
            Movie m = (Movie) it.next();
            if(m.getActors() != null)
            {
                matchActors = m.getActors().size() == 3;
                if(matchActors) break;
            }
        }
        assertTrue(matchActors);

        OQLQuery queryActor = actorQuery(postfix);
        Collection resultActor = (Collection) queryActor.execute();
        assertEquals(3, resultActor.size());
        it = resultActor.iterator();
        boolean matchMovies = false;
        while(it.hasNext())
        {
            Actor a = (Actor) it.next();
            if(a.getMovies() != null)
            {
                matchMovies = a.getMovies().size() == 3;
                if(matchMovies) break;
            }
        }
        assertTrue(matchMovies);

        OQLQuery queryRole = odmg.newOQLQuery();
        queryRole.create("select obj from " + Role.class.getName() + " where movieStrId=$3");
        queryRole.bind(postfix);
        Collection resultRole = (Collection) queryRole.execute();
        assertEquals(5, resultRole.size());
    }

    public void testStoreComplex_2() throws Exception
    {
        // arminw: fixed
        // TODO: Seems that the order of objects is not valid to insert M:N
        // if(ojbSkipKnownIssueProblem()) return;
        changeMovieCollectionDescriptorTo(true, NONE, NONE, false);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStoreComplex_2();
    }

    public void testStoreComplex_2_WithProxy() throws Exception
    {
        // arminw: fixed
        // TODO: Seems that the order of objects is not valid to insert M:N
        // if(ojbSkipKnownIssueProblem()) return;
        changeMovieCollectionDescriptorTo(true, NONE, NONE, true);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStoreComplex_2();
    }

    public void doTestStoreComplex_2() throws Exception
    {
        String postfix = "doTestStoreComplex_2_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);

        Transaction tx = odmg.newTransaction();
        tx.begin();
        Iterator it = movie.getActors().iterator();
        while(it.hasNext())
        {
            database.makePersistent(it.next());
        }
        database.makePersistent(movie);
        tx.commit();

        OQLQuery queryMovie = movieQuery(postfix);
        Collection resultMovie = (Collection) queryMovie.execute();
        assertEquals(3, resultMovie.size());
        it = resultMovie.iterator();
        boolean matchActors = false;
        while(it.hasNext())
        {
            Movie m = (Movie) it.next();
            if(m.getActors() != null)
            {
                matchActors = m.getActors().size() == 3;
                if(matchActors) break;
            }
        }

        OQLQuery queryActor = actorQuery(postfix);
        Collection resultActor = (Collection) queryActor.execute();
        assertEquals(3, resultActor.size());
        it = resultActor.iterator();
        boolean matchMovies = false;
        while(it.hasNext())
        {
            Actor a = (Actor) it.next();
            if(a.getMovies() != null)
            {
                matchMovies = a.getMovies().size() == 3;
                if(matchMovies) break;
            }
        }
        assertTrue(matchMovies);

        OQLQuery queryRole = odmg.newOQLQuery();
        queryRole.create("select obj from " + Role.class.getName() + " where movieStrId=$3");
        queryRole.bind(postfix);
        Collection resultRole = (Collection) queryRole.execute();
        assertEquals(5, resultRole.size());
    }


    public void testStoreDelete() throws Exception
    {
        changeMovieCollectionDescriptorTo(true, NONE, NONE, false);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStoreDelete();
    }

    public void testStoreDeleteWithProxy() throws Exception
    {
        changeMovieCollectionDescriptorTo(true, NONE, NONE, true);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestStoreDelete();
    }

    public void doTestStoreDelete() throws Exception
    {
        String postfix = "doTestStoreDelete_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActors(postfix);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        Iterator it = movie.getActors().iterator();
        while(it.hasNext())
        {
            database.makePersistent(it.next());
        }
        database.makePersistent(movie);
        tx.commit();

        OQLQuery queryMovie = movieQuery(postfix);
        Collection resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        Movie newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(3, newMovie.getActors().size());

        OQLQuery queryActor = actorQuery(postfix);
        Collection resultActor = (Collection) queryActor.execute();
        assertEquals(5, resultActor.size());

        OQLQuery queryRole = roleQuery(null, movie);
        Collection resultRole = (Collection) queryRole.execute();
        assertEquals(3, resultRole.size());

        tx.begin();
        tx.getBroker().clearCache();
        queryMovie = movieQuery(postfix);
        resultMovie = (Collection) queryMovie.execute();
        Movie m = (Movie) resultMovie.iterator().next();
        assertNotNull(m);
        //**********************************************
        tx.lock(m, Transaction.WRITE);
        it = m.getActors().iterator();
        Actor actor = (Actor) it.next();
        /*
        we expect that the entry in indirection table was removed
        AND the Actor object itself will be deleted
        */
        database.deletePersistent(actor);
        //*********************************************
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        //tx.commit();

        queryMovie = movieQuery(postfix);
        resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(2, newMovie.getActors().size());

        queryActor = actorQuery(postfix);
        resultActor = (Collection) queryActor.execute();
        assertEquals(4, resultActor.size());

        queryRole = roleQuery(null, movie);
        resultRole = (Collection) queryRole.execute();
        assertEquals(2, resultRole.size());

        tx.commit();
    }

    /**
     * Use auto-delete="object" to enable cascading delete.
     */ 
    public void testStoreDeleteCascade() throws Exception
    {
        String postfix = "doTestStoreDeleteCascade_" + System.currentTimeMillis();

        changeMovieCollectionDescriptorTo(true, NONE, OBJECT, true);
        changeActorCollectionDescriptorTo(true, NONE, OBJECT, false);

        Movie movie = buildMovieWithActors(postfix);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(movie);
        tx.commit();

        OQLQuery queryMovie = movieQuery(postfix);
        Collection resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        Movie newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(3, newMovie.getActors().size());

        OQLQuery queryActor = actorQuery(postfix);
        Collection resultActor = (Collection) queryActor.execute();
        assertEquals(5, resultActor.size());

        OQLQuery queryRole = roleQuery(null, movie);
        Collection resultRole = (Collection) queryRole.execute();
        assertEquals(3, resultRole.size());

        tx.begin();
        tx.getBroker().clearCache();
        queryMovie = movieQuery(postfix);
        resultMovie = (Collection) queryMovie.execute();
        Movie m = (Movie) resultMovie.iterator().next();
        //**********************************************
        database.deletePersistent(m);
        //*********************************************
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();

        queryMovie = movieQuery(postfix);
        resultMovie = (Collection) queryMovie.execute();
        assertEquals(0, resultMovie.size());

        queryActor = actorQuery(postfix);
        resultActor = (Collection) queryActor.execute();
        assertEquals(0, resultActor.size());

        queryRole = roleQuery(null, movie);
        resultRole = (Collection) queryRole.execute();
        assertEquals(0, resultRole.size());

        tx.commit();
    }

    public void testRemoveAssociation() throws Exception
    {
        changeMovieCollectionDescriptorTo(true, NONE, NONE, false);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestRemoveAssociation();
    }

    public void testRemoveAssociationWithProxy() throws Exception
    {
        changeMovieCollectionDescriptorTo(true, NONE, NONE, true);
        changeActorCollectionDescriptorTo(true, NONE, NONE, false);
        doTestRemoveAssociation();
    }

    public void doTestRemoveAssociation() throws Exception
    {
        String postfix = "doTestRemoveAssociation_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActors(postfix);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        Iterator it = movie.getActors().iterator();
        while(it.hasNext())
        {
            database.makePersistent(it.next());
        }
        database.makePersistent(movie);
        tx.commit();

        OQLQuery queryMovie = movieQuery(postfix);
        Collection resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        Movie newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(3, newMovie.getActors().size());

        OQLQuery queryActor = actorQuery(postfix);
        Collection resultActor = (Collection) queryActor.execute();
        assertEquals(5, resultActor.size());

        OQLQuery queryRole = roleQuery(null, movie);
        Collection resultRole = (Collection) queryRole.execute();
        assertEquals(3, resultRole.size());

        tx.begin();
        tx.getBroker().clearCache();

        queryMovie = movieQuery(postfix);
        resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        Movie m = (Movie) resultMovie.iterator().next();
        assertNotNull(m);
        Collection actors = m.getActors();
        assertEquals(3, actors.size());
        //***************************************
        tx.lock(m, Transaction.WRITE);
        /*
        now remove an association between Movie ans Actor
        we expect that the entry in indirection class will be removed,
        but the Actor entry should be still there
        */
        Iterator iter = actors.iterator();
        iter.next();
        iter.remove();
        //***************************************
        assertEquals(2, m.getActors().size());
        tx.commit();

        tx.begin();
        tx.getBroker().clearCache();
        //tx.commit();

        queryMovie = movieQuery(postfix);
        resultMovie = (Collection) queryMovie.execute();
        assertEquals(1, resultMovie.size());
        newMovie = (Movie) resultMovie.iterator().next();
        assertNotNull(newMovie.getActors());
        assertEquals(2, newMovie.getActors().size());

        queryRole = roleQuery(null, movie);
        resultRole = (Collection) queryRole.execute();
        assertEquals(2, resultRole.size());

        // we only remove the association
        queryActor = actorQuery(postfix);
        resultActor = (Collection) queryActor.execute();
        assertEquals(5, resultActor.size());

        tx.commit();
    }

    //=======================================================================
    // helper methods
    //=======================================================================

    OQLQuery movieQuery(String postfix) throws QueryException
    {
        OQLQuery query = odmg.newOQLQuery();
        query.create("select obj from " + Movie.class.getName() + " where idStr like $1");
        query.bind("%" + postfix + "%");
        return query;
    }

    OQLQuery actorQuery(String postfix) throws QueryException
    {
        OQLQuery query = odmg.newOQLQuery();
        query.create("select obj from " + Actor.class.getName() + " where name like $1");
        query.bind("%" + postfix + "%");
        return query;
    }

    OQLQuery roleQuery(Actor actor, Movie movie) throws QueryException
    {
        OQLQuery query = odmg.newOQLQuery();
        String select = "select obj from " + Role.class.getName() + " where";
        boolean needsAnd = false;
        if(actor != null)
        {
            select = select + " actorId=$1 and actorId2=$2";
            needsAnd = true;
        }
        if(movie != null)
        {
            if(needsAnd) select = select + " and";
            select = select + " movieIntId=$3 and movieIntId2=$4 and movieStrId=$5";
        }
        query.create(select);
        if(actor != null)
        {
            query.bind(actor.getId());
            query.bind(actor.getId2());

        }
        if(movie != null)
        {
            query.bind(movie.getIdInt());
            query.bind(movie.getIdInt2());
            query.bind(movie.getIdStr());
        }
        return query;
    }

    /**
     * Returns 1 movie object with 3 actor objects in actors-collection
     * and 2 actor objects in actors2-collection
     */
    Movie buildMovieWithActors(String postfixId)
    {
        Movie m = new MovieImpl(postfixId,
                "Dr. Strangelove or: How I Learned to Stop Worrying and Love the Bomb " + postfixId,
                "An insane general starts a process to nuclear holocaust that a war" +
                " room of politicians and generals frantically try to stop. " + postfixId);
        
        Actor a1 = new Actor("Peter Sellers " + postfixId);
        Actor a2 = new Actor("George C. Scott " + postfixId);
        Actor a3 = new Actor("Sterling Hayden " + postfixId);
        ArrayList list = new ArrayList();
        list.add(a1);
        list.add(a2);
        list.add(a3);
        m.setActors(list);

        Actor a4 = new Actor("Actor 2 A " + postfixId);
        Actor a5 = new Actor("Actor 2 B " + postfixId);
        ArrayList list2 = new ArrayList();
        list2.add(a4);
        list2.add(a5);
        m.setActors2(list2);

        return m;
    }

    /**
     * Returns 1 movie object m1 with 3 actor objects and one actor object with
     * back-reference to movie object m1 + 2 new movies
     */
    Movie buildMovieWithActorsAndBackReferences(String postfixId)
    {
        Movie m = new MovieImpl(postfixId,
                "Dr. Strangelove or: How I Learned to Stop Worrying and Love the Bomb " + postfixId,
                "An insane general starts a process to nuclear holocaust that a war" +
                " room of politicians and generals frantically try to stop. " + postfixId);
        Actor a1 = new Actor("Peter Sellers " + postfixId);
        Actor a2 = new Actor("George C. Scott " + postfixId);
        Actor a3 = new Actor("Sterling Hayden " + postfixId);
        ArrayList list = new ArrayList();
        list.add(a1);
        list.add(a2);
        list.add(a3);
        m.setActors(list);

        Movie m1 = new MovieImpl(postfixId + "", "A Shot in the Dark",
                "As murder follows murder, beautiful Maria is the obvious suspect...");
        Movie m2 = new MovieImpl(postfixId + "", "The Pink Panther",
                " In the first movie starring Peter Sellers as the bumbling Inspector Clouseau...");

        MovieManageableCollection mlist1 = new MovieManageableCollectionImpl();
        mlist1.add(m);
        mlist1.add(m1);
        mlist1.add(m2);
        MovieManageableCollection mlist2 = new MovieManageableCollectionImpl();
        MovieManageableCollection mlist3 = new MovieManageableCollectionImpl();
        a1.setMovies(mlist1);
        a2.setMovies(mlist2);
        a3.setMovies(mlist3);

        return m;
    }

    Actor buildActorWithMovies(String postfixId)
    {
        Actor a = new Actor("John Cusack" + postfixId);
        MovieManageableCollection list = new MovieManageableCollectionImpl();
        list.add(new MovieImpl("a_" + postfixId, "High Fidelity", "A comedy about fear of commitment, hating your job..." + postfixId));
        list.add(new MovieImpl("b_" + postfixId, "Identity", "When a nasty storm hits a hotel, ten strangers are stranded within ..." + postfixId));
        list.add(new MovieImpl("c_" + postfixId, "Grosse Pointe Blank", "Martin Blank is a professional assassin. He is sent on a mission to a small Detroit ..." + postfixId));
        a.setMovies(list);
        return a;
    }

    void changeActorCollectionDescriptorTo(boolean autoRetrieve, int autoUpdate, int autoDelete, boolean proxy)
    {
        ojbChangeReferenceSetting(Actor.class, "movies", autoRetrieve, autoUpdate, autoDelete, proxy);
    }

    void changeMovieCollectionDescriptorTo(boolean autoRetrieve, int autoUpdate, int autoDelete, boolean proxy)
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", autoRetrieve, autoUpdate, autoDelete, proxy);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", autoRetrieve, autoUpdate, autoDelete, proxy);
    }


    //=======================================================================
    // Inner classes, persistence capable  test classes
    //=======================================================================

    public static interface MovieManageableCollection extends ManageableCollection
    {
        public Iterator iterator();

        public int size();

        public boolean isEmpty();

        public void clear();

        public boolean add(Movie movie);

        public boolean remove(Movie movie);

        public boolean contains(Movie movie);

        public Movie get(int index);
    }

    public static class MovieManageableCollectionImpl implements MovieManageableCollection
    {
        private ArrayList list = new ArrayList();

        public void ojbAdd(Object anObject)
        {
            list.add(anObject);
        }

        public void ojbAddAll(ManageableCollection otherCollection)
        {
            Iterator it = otherCollection.ojbIterator();
            while(it.hasNext())
            {
                list.add(it.next());
            }
        }

        public Iterator ojbIterator()
        {
            return list.iterator();
        }

        public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
        {
        }

        public Iterator iterator()
        {
            return list.iterator();
        }

        public int size()
        {
            return list.size();
        }

        public boolean isEmpty()
        {
            return list.isEmpty();
        }

        public void clear()
        {
            list.clear();
        }

        public boolean add(Movie movie)
        {
            return list.add(movie);
        }

        public boolean remove(Movie movie)
        {
            return list.remove(movie);
        }

        public boolean contains(Movie movie)
        {
            return list.contains(movie);
        }

        public Movie get(int index)
        {
            return (Movie) list.get(index);
        }
    }

    public static class Actor
    {
        private Integer id;
        private Integer id2;
        private String name;
        private MovieManageableCollection movies;

        public Actor()
        {
        }

        public Actor(String name)
        {
            this.name = name;
        }

        public MovieManageableCollection getMovies()
        {
            return movies;
        }

        public void setMovies(MovieManageableCollection movies)
        {
            this.movies = movies;
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public Integer getId2()
        {
            return id2;
        }

        public void setId2(Integer id2)
        {
            this.id2 = id2;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this).toString();
        }
    }

    public static interface Movie
    {
        public void addActors(Actor actor);
        public void addActors2(Actor actor);

        public Collection getActors();
        public void setActors(Collection actors);

        public List getActors2();
        public void setActors2(List actors);

        public Integer getIdInt2();
        public Integer getIdInt();

        public void setIdInt2(Integer id2Int);
        public void setIdInt(Integer idInt);

        public String getIdStr();
        public void setIdStr(String idStr);

        public String getTitle();
        public void setTitle(String title);

        public String getDescription();
        public void setDescription(String description);
    }

    public static class MovieImpl implements Movie
    {
        private Integer idInt;
        private Integer idInt2;
        private String idStr;
        private String title;
        private String description;
        private Collection actors;
        private List actors2;

        public MovieImpl()
        {
        }

        public MovieImpl(String idStr, String title, String description)
        {
            this.idStr = idStr;
            this.title = title;
            this.description = description;
        }

        public void addActors(Actor actor)
        {
            if(actors == null)
            {
                actors = new ArrayList();
            }
            if(!actors.contains(actor)) actors.add(actor);
            else throw new OJBRuntimeException("Can't add same object twice");
        }

        public void addActors2(Actor actor)
        {
            if(actors2 == null)
            {
                actors2 = new ArrayList();
            }
            if(!actors2.contains(actor)) actors2.add(actor);
            else throw new OJBRuntimeException("Can't add same object twice");
        }

        public Collection getActors()
        {
            return actors;
        }

        public void setActors(Collection actors)
        {
            this.actors = actors;
        }

       public List getActors2()
        {
            return actors2;
        }

        public void setActors2(List actors)
        {
            this.actors2 = actors;
        }
        
        public Integer getIdInt()
        {
            return idInt;
        }

        public void setIdInt(Integer idInt)
        {
            this.idInt = idInt;
        }

        public Integer getIdInt2()
        {
            return idInt2;
        }

        public void setIdInt2(Integer idInt2)
        {
            this.idInt2 = idInt2;
        }

        public String getIdStr()
        {
            return idStr;
        }

        public void setIdStr(String idStr)
        {
            this.idStr = idStr;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this).toString();
        }
    }

    public static class Role
    {
        private Integer actorId;
        private Integer actorId2;
        private Integer movieIntId;
        private Integer movieIntId2;
        private String movieStrId;

        public Role()
        {
        }

        public Integer getActorId()
        {
            return actorId;
        }

        public void setActorId(Integer actorId)
        {
            this.actorId = actorId;
        }

        public Integer getMovieIntId()
        {
            return movieIntId;
        }

        public Integer getMovieIntId2()
        {
            return movieIntId2;
        }

        public void setMovieIntId2(Integer movieIntId2)
        {
            this.movieIntId2 = movieIntId2;
        }

        public Integer getActorId2()
        {
            return actorId2;
        }

        public void setActorId2(Integer actorId2)
        {
            this.actorId2 = actorId2;
        }

        public void setMovieIntId(Integer movieIntId)
        {
            this.movieIntId = movieIntId;
        }

        public String getMovieStrId()
        {
            return movieStrId;
        }

        public void setMovieStrId(String movieStrId)
        {
            this.movieStrId = movieStrId;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this).toString();
        }
    }



    public static class County
    {
        private String id;
        private String name;

        public County()
        {
        }

        /**
         * Compares the given objects on the basis of their
         * toString() methods. Returns <code>false</code>
         * if the given object is not of type County.
         */
        public boolean equals(Object obj)
        {
            if(!(obj instanceof County))
            {
                return false;
            }
            County other = (County) obj;
            return new EqualsBuilder().append(getId(), other.getId())
                    .append(getName(), other.getName())
                    .isEquals();
        }

        public String getId()
        {
            return id;
        }

        public void setId(String id)
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

    public static interface OfficeProfile
    {
        public int getId();
        public void setId(int officeId);
        public void setName(String name);
        public String getName();
        public List getCounties();
        public void setCounties(List list);
        public void clearCounties();
        public void addCounty(County county);
        public void removeCounty(County county);
    }

    public static class OfficeProfileImpl implements OfficeProfile
    {
        private int id;
        private String name;
        private List counties;

        public OfficeProfileImpl()
        {
        }

        public boolean equals(Object obj)
        {
            if(!(obj instanceof OfficeProfile))
            {
                return false;
            }
            OfficeProfile other = (OfficeProfile) obj;
            return new EqualsBuilder().append(getId(), other.getId())
                    .append(getName(), other.getName())
                    .append(getCounties(), other.getCounties())
                    .isEquals();
        }

        public int getId()
        {
            return id;
        }

        public void setId(int officeId)
        {
            id = officeId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List getCounties()
        {
            return counties;
        }

        public void setCounties(List list)
        {
            counties = list;
        }

        public void clearCounties()
        {
            if(counties != null)
            {
                counties.clear();
            }
        }

        public void addCounty(County county)
        {
            if(counties == null)
            {
                counties = new LinkedList();
            }
            counties.add(county);
        }

        public void removeCounty(County county)
        {
            if(counties != null)
            {
                counties.remove(county);
            }
        }
    }
}
