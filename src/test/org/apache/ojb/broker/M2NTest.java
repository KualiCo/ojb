package org.apache.ojb.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.time.StopWatch;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.junit.PBTestCase;

/**
 * Test (non-decomposed) M:N relations.
 *
 * IMPORTANT NOTE: The global runtime metadata changes made by this test case
 * are NOT recommended in multithreaded environments, because they are global
 * and each thread will be affected.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: M2NTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class M2NTest extends PBTestCase
{
    static final int NONE = ObjectReferenceDescriptor.CASCADE_NONE;
    static final int LINK = ObjectReferenceDescriptor.CASCADE_LINK;
    static final int OBJECT = ObjectReferenceDescriptor.CASCADE_OBJECT;

    int actorCount = 200;
    int movieCount = 100;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[]{M2NTest.class.getName()});
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Test for OJB-76
     */
    public void testStoreWithSharedIndirectionTableColumn()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "producers", true, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(Producer.class, "movies", true, OBJECT, OBJECT, false);
        String timestamp = "" + System.currentTimeMillis();
        String postfix = "testStoreWithSharedIndirectionTableColumn_" + timestamp;

        Movie m_1 = new MovieImpl(postfix, postfix + "_1", null);
        Movie m_2 = new MovieImpl(postfix, postfix + "_2", null);
        Producer p_1 = new Producer(postfix, "producer_" + timestamp);
        m_1.addProducer(p_1);
        p_1.addMovie(m_1);
        broker.beginTransaction();
        broker.store(p_1, ObjectModification.INSERT);
        broker.commitTransaction();

        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addLike("title", postfix + "%");
        Query q = QueryFactory.newQuery(Movie.class, crit);
        Movie new_m_1 = (Movie) broker.getObjectByQuery(q);
        assertNotNull(new_m_1);
        assertNotNull(new_m_1.getProducers());
        assertEquals(1, new_m_1.getProducers().size());

        broker.beginTransaction();
        p_1.addMovie(m_2);
        m_2.addProducer(p_1);
        broker.store(p_1, ObjectModification.UPDATE);
        // or (but this will cause more DB traffic)
        // broker.store(m_2, ObjectModification.INSERT);
        broker.commitTransaction();

        broker.clearCache();
        new_m_1 = (Movie) broker.getObjectByQuery(q);
        assertNotNull(new_m_1);
        assertNotNull(new_m_1.getProducers());
        assertEquals(1, new_m_1.getProducers().size());
        Producer new_p_1 = (Producer) new_m_1.getProducers().get(0);
        assertNotNull(new_p_1);
        assertNotNull(new_p_1.getMovies());
        assertEquals(2, new_p_1.getMovies().size());


        broker.beginTransaction();
        broker.delete(p_1);
        broker.commitTransaction();

        new_m_1 = (Movie) broker.getObjectByQuery(q);
        assertNull(new_m_1);

        crit = new Criteria();
        crit.addEqualTo("name", "producer_" + timestamp);
        q = QueryFactory.newQuery(Producer.class, crit);
        new_p_1 = (Producer) broker.getObjectByQuery(q);
        assertNull(new_p_1);
    }

    public void testSimpleStore_1()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, OBJECT, false);
        String postfix = "testSimpleStore_1_" + System.currentTimeMillis();
        Movie m = new MovieImpl(postfix, postfix, null);
        Actor a = new Actor(postfix);

        broker.beginTransaction();
        broker.store(m);
        broker.store(a);

        m.addActor(a);
        broker.store(m);
        broker.commitTransaction();

        broker.retrieveAllReferences(a);

        assertNotNull(a.getMovies());
        assertEquals(1, a.getMovies().size());
    }

    public void testSimpleStore_2()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", false, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", false, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(Actor.class, "movies", false, OBJECT, OBJECT, false);
        String postfix = "testSimpleStore_2_" + System.currentTimeMillis();
        Movie m = new MovieImpl(postfix, postfix, null);
        Actor a = new Actor(postfix);

        broker.beginTransaction();
        broker.store(m);
        broker.store(a);

        m.addActor(a);
        broker.store(m);
        broker.commitTransaction();

        // needed, because autoretrieve is set false
        broker.retrieveAllReferences(a);

        assertNotNull(a.getMovies());
        assertEquals(1, a.getMovies().size());
    }

    /**
     * Test deprecated auto Settings
     */
    public void testAutoUpdateDeleteSettings()
    {
        ojbChangeReferenceSetting(Actor.class, "movies", false, false, false, false);
        CollectionDescriptor ord = broker.getClassDescriptor(Actor.class)
                .getCollectionDescriptorByName("movies");
        assertEquals(LINK, ord.getCascadingStore());
        assertEquals(LINK, ord.getCascadingDelete());
        assertEquals(false, ord.getCascadeStore());
        assertEquals(false, ord.getCascadeDelete());

        ojbChangeReferenceSetting(Actor.class, "movies", false, true, true, false);
        ord = broker.getClassDescriptor(Actor.class)
                .getCollectionDescriptorByName("movies");
        assertEquals(OBJECT, ord.getCascadingStore());
        assertEquals(OBJECT, ord.getCascadingDelete());
        assertEquals(true, ord.getCascadeStore());
        assertEquals(true, ord.getCascadeDelete());
    }

    public void testMassStoreUpdateAutomatic()
    {

        long testPeriod = 0;

        String postfix = "testMassStoreUpdateAutomatic_" + System.currentTimeMillis();
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, OBJECT, false);

        Movie movie = buildMovieWithActors(postfix, actorCount);
        Actor actor = buildActorWithMovies(postfix, movieCount);

        List actors = new ArrayList(movie.getActors());
        actors.add(actor);
        movie.setActors(actors);

        MovieManageableCollection movies = actor.getMovies();
        movies.add(movie);

        StopWatch watch = new StopWatch();
        watch.start();
        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateAutomatic] Time to store "+(actorCount + movieCount)+" m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();

        watch.reset();
        watch.start();
        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(movieCount + 1, resultMovie.size());
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateAutomatic] Time to query "+movieCount+" m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();

        watch.reset();
        watch.start();
        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(actorCount + 1, resultActor.size());
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateAutomatic] Time to query "+actorCount+" m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();

        Query queryRole = roleQueryActorOrMovieMatch(actor, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(actorCount + movieCount + 1, resultRole.size());

        //*****************************
        // update movie
        movie.setActors(new ArrayList());
        watch.reset();
        watch.start();
        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateAutomatic] Time to update object with "+actorCount + " m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();
        //*****************************

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(movie);
        movie = (Movie) broker.getObjectByIdentity(oid);

        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(movieCount + 1, resultMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(actorCount + 1, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(movieCount, resultRole.size());

        assertNotNull(movie);
        assertEquals(0, movie.getActors().size());

        //*****************************
        // remove actor
        movie.setActors(new ArrayList());
        watch.reset();
        watch.start();
        broker.beginTransaction();
        broker.delete(actor);
        broker.commitTransaction();
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateAutomatic] Time to remove object with "+ movieCount + " m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();
        //*****************************

        broker.clearCache();
        oid = broker.serviceIdentity().buildIdentity(actor);
        actor = (Actor) broker.getObjectByIdentity(oid);

        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(0, resultMovie.size());

        // we never delete these actor objects
        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(actorCount, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(0, resultRole.size());

        assertNull(actor);

        broker.beginTransaction();
        broker.delete(movie);
        broker.commitTransaction();

        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(0, resultMovie.size());
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateAutomatic] Total test time is "+ testPeriod+" ms");
        System.out.println("");
    }


    public void testMassStoreUpdateLinking()
    {
        long testPeriod = 0;

        String postfix = "testMassStoreUpdateLinking" + System.currentTimeMillis();
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, NONE, OBJECT, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, NONE, OBJECT, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, NONE, OBJECT, false);

        Movie movie = buildMovieWithActors(postfix, actorCount);
        Actor actor = buildActorWithMovies(postfix, movieCount);

        List actors = new ArrayList(movie.getActors());
        actors.add(actor);
        movie.setActors(actors);

        MovieManageableCollection movies = actor.getMovies();
        movies.add(movie);

        StopWatch watch = new StopWatch();
        watch.start();
        broker.beginTransaction();
        broker.store(movie);
        for(int i = 0; i < actors.size(); i++)
        {
            broker.store(actors.get(i));
        }
        MovieManageableCollection actorMovies = actor.getMovies();
        for(int i = 0; i < actorMovies.size(); i++)
        {
             broker.store(actorMovies.get(i));
        }
        broker.serviceBrokerHelper().link(movie, true);
        broker.serviceBrokerHelper().link(actor, true);
        broker.commitTransaction();
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateLinking] Time to store "+(actorCount + movieCount)+" m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();

        watch.reset();
        watch.start();
        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(movieCount + 1, resultMovie.size());
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateLinking] Time to query "+movieCount+" m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();

        watch.reset();
        watch.start();
        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(actorCount + 1, resultActor.size());
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateLinking] Time to query "+actorCount+" m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();

        Query queryRole = roleQueryActorOrMovieMatch(actor, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(actorCount + movieCount + 1, resultRole.size());

        //*****************************
        // update movie
        movie.setActors(new ArrayList());
        watch.reset();
        watch.start();
        broker.beginTransaction();
        broker.serviceBrokerHelper().unlink(movie);
        broker.store(movie);
        broker.serviceBrokerHelper().link(movie, false);
        broker.commitTransaction();
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateLinking] Time to update object with "+actorCount + " m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();
        //*****************************

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(movie);
        movie = (Movie) broker.getObjectByIdentity(oid);

        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(movieCount + 1, resultMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(actorCount + 1, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(movieCount, resultRole.size());

        assertNotNull(movie);
        assertEquals(0, movie.getActors().size());

        //*****************************
        // remove actor
        movie.setActors(new ArrayList());
        watch.reset();
        watch.start();
        broker.beginTransaction();
        broker.delete(actor);
        broker.commitTransaction();
        watch.stop();
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateLinking] Time to remove object with "+ movieCount + " m:n objects=" + watch.getTime());
        testPeriod += watch.getTime();
        //*****************************

        broker.clearCache();
        oid = broker.serviceIdentity().buildIdentity(actor);
        actor = (Actor) broker.getObjectByIdentity(oid);

        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(0, resultMovie.size());

        // we never delete these actor objects
        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(actorCount, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(0, resultRole.size());

        assertNull(actor);

        broker.beginTransaction();
        broker.delete(movie);
        broker.commitTransaction();

        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(0, resultMovie.size());
        System.out.println("[" + ClassUtils.getShortClassName(this.getClass())
                + "#testMassStoreUpdateLinking] Total test time is "+ testPeriod+" ms");
        System.out.println("");
    }

    //============================================================
    // auto-retrieve true / auto-update/auto-delete LINK
    //============================================================
    // test needs refactoring, LINK settings doesn't allow to store
    // an object graph
    public void YYYtestStoreAddUpdateDeleteTLLF()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, LINK, LINK, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, LINK, LINK, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, LINK, LINK, false);
        String postfix = "_testStoreTLLF_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);
        doTestStoreAddUpdateDeleteTLLX(movie, postfix);
    }

    public void YYYtestStoreAddUpdateDeleteTLLT()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, LINK, LINK, true);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, LINK, LINK, true);
        ojbChangeReferenceSetting(Actor.class, "movies", true, LINK, LINK, false);
        String postfix = "_testStoreTLLF_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);
        doTestStoreAddUpdateDeleteTLLX(movie, postfix);
    }


    public void doTestStoreAddUpdateDeleteTLLX(Movie movie, String postfix)
    {
        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();
        Identity movieOID = broker.serviceIdentity().buildIdentity(movie);

        /*
        auto-update setting is LINK, but it behaves same way as OBJECT
        */
        broker.clearCache();
        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(3, resultMovie.size());

        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3, resultActor.size());

        // first created actor
        Actor actor = (Actor) movie.getActors().iterator().next();
        Query queryRole = queryRole(actor, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(5, resultRole.size());

        broker.clearCache();
        broker.beginTransaction();
        Movie newMovie = (Movie) broker.getObjectByIdentity(movieOID);
        newMovie.setTitle("updated_title_" + postfix);
        Iterator it = newMovie.getActors().iterator();
        while(it.hasNext())
        {
            Actor a = (Actor) it.next();
            a.setName("updated_name_" + postfix);
        }
        ArrayList list = new ArrayList(newMovie.getActors());
        list.add(new Actor("updated_name_" + postfix));
        newMovie.setActors(list);
        broker.store(newMovie);
        broker.commitTransaction();

        broker.clearCache();
        queryMovie = queryMovie(postfix);
        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(3, resultMovie.size());

        queryActor = queryActor(postfix);
        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(4, resultActor.size());

        // first created actor
        actor = (Actor) movie.getActors().iterator().next();
        queryRole = queryRole(actor, movie);
        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(6, resultRole.size());

        newMovie = (Movie) broker.getObjectByIdentity(movieOID);
        assertEquals("updated_title_"+postfix, newMovie.getTitle());
        it = newMovie.getActors().iterator();
        while(it.hasNext())
        {
            Actor a = (Actor) it.next();
            assertEquals("updated_name_" + postfix, a.getName());
        }

        // now we delete the movie object
        broker.beginTransaction();
        broker.delete(newMovie);
        broker.commitTransaction();

        broker.clearCache();
        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(2, resultMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(4, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(2, resultRole.size());
    }

    //============================================================
    // auto-update / auto-retrieve false
    //============================================================

    /**
     * All auto-xxx settings are false, thus expect that all thing must be
     * done by hand
     */
    public void testStoreFFFF()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", false, NONE, NONE, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", false, NONE, NONE, false);
        ojbChangeReferenceSetting(Actor.class, "movies", false, NONE, NONE, false);
        String postfix = "" + System.currentTimeMillis();
        Movie movie = buildMovieWithActors(postfix);
        doTestStoreFFFX(movie, postfix);
    }

    /**
     * All auto-xxx settings are false, thus expect that all thing must be
     * done by hand, proxy true should not affect anything
     */
    public void testStoreFFFT()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", false, NONE, NONE, true);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", false, NONE, NONE, true);
        ojbChangeReferenceSetting(Actor.class, "movies", false, NONE, NONE, false);
        String postfix = "" + System.currentTimeMillis();
        Movie movie = buildMovieWithActors(postfix);
        doTestStoreFFFX(movie, postfix);
    }

    /**
     * All auto-xxx settings are false, thus expect that all thing must be
     * done by hand. Actors has a collection of movies too - should be ignored
     * in this case.
     */
    public void testStoreFFFF_2()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", false, NONE, NONE, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", false, NONE, NONE, false);
        ojbChangeReferenceSetting(Actor.class, "movies", false, NONE, NONE, false);
        String postfix = "" + System.currentTimeMillis();
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);
        doTestStoreFFFX(movie, postfix);
    }

    /**
     * All auto-xxx settings are false, thus expect that all thing must be
     * done by hand, proxy true should not affect anything. Actors has a
     * collection of movies too - should be ignored in this case.
     */
    public void testStoreFFFT_2()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", false, NONE, NONE, true);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", false, NONE, NONE, true);
        ojbChangeReferenceSetting(Actor.class, "movies", false, NONE, NONE, false);
        String postfix = "" + System.currentTimeMillis();
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);
        doTestStoreFFFX(movie, postfix);
    }

    public void doTestStoreFFFX(Movie movie, String postfix)
    {
        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();

        /*
        all auto-xxx settings are false, so only the movie object should be
        stored - no other inserts!
        */
        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, resultMovie.size());

        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        // auto-update is false, thus we don't expect an Actor object
        assertEquals(0, resultActor.size());

        Query queryRole = queryRole(null, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        // auto-update is false, thus we don't expect Role objects
        assertEquals(0, resultRole.size());

        broker.beginTransaction();
        /*
        now we store the right-side objects and the intermediary entries
        */
        Iterator it = movie.getActors().iterator();
        while(it.hasNext())
        {
            Object actor = it.next();
            broker.store(actor);
        }
        // now both side exist and we can link the references
        broker.serviceBrokerHelper().link(movie, "actors", true);
        /*
        alternative call
        broker.serviceBrokerHelper().link(movie, true);
        */
        broker.commitTransaction();

        /*
        now we expect all stored objects
        */
        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, resultMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(3, resultRole.size());

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(movie);
        Movie readMovie = (Movie) broker.getObjectByIdentity(oid);
        assertNotNull(readMovie);
        // auto-retrieve false
        assertTrue(readMovie.getActors() == null || readMovie.getActors().size() == 0);

        broker.retrieveAllReferences(readMovie);
        assertEquals(3, readMovie.getActors().size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3, resultActor.size());
        Actor readActor = (Actor) resultActor.iterator().next();
        // auto-retrieve false
        assertTrue(readActor.getMovies() == null || readActor.getMovies().size() == 0);
        broker.retrieveAllReferences(readActor);
        assertEquals(1, readActor.getMovies().size());

        // We try to delete all objects
        // first do unlink the m:n references
        broker.beginTransaction();
        broker.serviceBrokerHelper().unlink(readMovie, "actors");
        /*
        alternative call
        broker.serviceBrokerHelper().unlink(readMovie);
        */
        broker.commitTransaction();

        broker.clearCache();
        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, resultMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(0, resultRole.size());

        // now we delete the n- and m-side objects
        broker.beginTransaction();
        Iterator iter = movie.getActors().iterator();
        while(iter.hasNext())
        {
            broker.delete(iter.next());
        }
        broker.delete(movie);
        broker.commitTransaction();

        broker.clearCache();
        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(0, resultMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(0, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(0, resultRole.size());
    }

    //============================================================
    // auto-retrieve true / auto-update/auto-delete false
    //============================================================
    /**
     * auto-retrieve is true, other false. proxy true/false should not affect
     * anything.
     */
    public void testStoreTFFF()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, NONE, NONE, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, NONE, NONE, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, NONE, NONE, false);
        doTestStoreTFFX();
    }

    /**
     * auto-retrieve is true, other false. proxy true/false should not affect
     * anything.
     */
    public void testStoreTFFT()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, NONE, NONE, true);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, NONE, NONE, true);
        ojbChangeReferenceSetting(Actor.class, "movies", true, NONE, NONE, false);
        doTestStoreTFFX();
    }

    public void doTestStoreTFFX()
    {
        String postfix = "" + System.currentTimeMillis();
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);

        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();
        broker.clearCache();

        /*
        auto-update settings is false, so only the movie object should be
        stored - no other inserts!
        */
        Query queryMovie = queryMovie(postfix);
        Collection collMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, collMovie.size());

        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        // auto-update is false, thus we don't expect an Actor object
        assertEquals(0, resultActor.size());

        Query queryRole = queryRole(null, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        // auto-update is false, thus we don't expect Role objects
        assertEquals(0, resultRole.size());

        broker.beginTransaction();
        /*
        now we store the right-side objects and the intermediary entries
        */
        Iterator it = movie.getActors().iterator();
        while(it.hasNext())
        {
            Object actor = it.next();
            broker.store(actor);
        }
        // now both side exist and we can link the references
        broker.serviceBrokerHelper().link(movie, "actors", true);
        /*
        alternative call
        broker.serviceBrokerHelper().link(movie, true);
        */
        broker.commitTransaction();

        /*
        now we expect all stored objects
        */
        collMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, collMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(3, resultRole.size());

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(movie);
        Movie readMovie = (Movie) broker.getObjectByIdentity(oid);
        assertNotNull(readMovie);
        // auto-retrieve true
        assertTrue(readMovie.getActors() != null);
        assertEquals(3, readMovie.getActors().size());

        // Now we want to add new objects
        Actor a1 = new Actor(postfix);
        Actor a2 = new Actor(postfix);
        readMovie.addActor(a1);
        readMovie.addActor(a2);
        broker.beginTransaction();
        broker.store(a1);
        broker.store(a2);
        broker.serviceBrokerHelper().unlink(readMovie, "actors");
        broker.serviceBrokerHelper().link(readMovie, "actors", true);
        broker.commitTransaction();

        collMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, collMovie.size());
        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(5, resultActor.size());
        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(5, resultRole.size());
        broker.clearCache();
        oid = broker.serviceIdentity().buildIdentity(movie);
        readMovie = (Movie) broker.getObjectByIdentity(oid);
        assertNotNull(readMovie);
        // auto-retrieve true
        assertTrue(readMovie.getActors() != null);
        assertEquals(5, readMovie.getActors().size());

        // We try to delete all objects
        // first do unlink the m:n references
        broker.beginTransaction();
        broker.serviceBrokerHelper().unlink(readMovie, "actors");
        /*
        alternative call
        broker.serviceBrokerHelper().unlink(readMovie);
        */
        broker.commitTransaction();

        broker.clearCache();
        // TODO: replace this with query below (when prefetching bug was solved)
        //Movie movieLookup = (Movie) broker.getObjectByIdentity(broker.serviceIdentity().buildIdentity(movie));
        //assertNotNull(movieLookup);
        collMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, collMovie.size());
        readMovie = (Movie) collMovie.iterator().next();
        assertEquals(0, readMovie.getActors().size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(5, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(0, resultRole.size());

        // now we delete the n- and m-side objects
        broker.beginTransaction();
        Iterator iter = resultActor.iterator();
        while(iter.hasNext())
        {
            broker.delete(iter.next());
        }
        broker.delete(readMovie);
        broker.commitTransaction();

        // broker.clearCache();
        collMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(0, collMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(0, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(0, resultRole.size());
    }




    //============================================================
    // auto-update / auto-retrieve true
    //============================================================

    public void testStoreTTFF()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, NONE, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, NONE, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, NONE, false);
        doTestStoreTTXX();
    }

    public void testStoreTTFT()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, NONE, true);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, NONE, true);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, NONE, false);
        doTestStoreTTXX();
    }

    public void doTestStoreTTXX()
    {
        String postfix = "" + System.currentTimeMillis();
        Movie movie = buildMovieWithActors(postfix);

        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();
        broker.clearCache();

        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, resultMovie.size());

        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3 + 2, resultActor.size());

        Query queryRole = queryRole(null, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(3, resultRole.size());

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(movie);
        Movie readMovie = (Movie) broker.getObjectByIdentity(oid);
        assertNotNull(readMovie);
        assertEquals(3, readMovie.getActors().size());
        assertEquals(2, readMovie.getActors2().size());
    }

    /**
     * movies with back-references
     * auto-update = OBJECT
     */
    public void testStoreTTFT_2()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, NONE, true);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, NONE, true);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, NONE, false);
        doTestStoreTTXX_2();
    }

    public void doTestStoreTTXX_2()
    {
        String postfix = "" + System.currentTimeMillis();
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);

        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();
        broker.clearCache();

        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(3, resultMovie.size());

        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3, resultActor.size());

        Query queryRole = queryRole(null, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(3, resultRole.size());

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(movie);
        Movie readMovie = (Movie) broker.getObjectByIdentity(oid);
        assertNotNull(readMovie);
        assertEquals(3, readMovie.getActors().size());
    }

    public void testStoreUpdateTTFF()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, NONE, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, NONE, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, NONE, false);
        doTestStoreUpdateTTXX();
    }

    public void testStoreUpdateTTFF_2()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, NONE, true);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, NONE, true);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, NONE, false);
        doTestStoreUpdateTTXX();
    }

    public void doTestStoreUpdateTTXX()
    {
        String postfix = "doTestStoreUpdateTTXX_" + System.currentTimeMillis();
        Movie movie = buildMovieWithActors(postfix);

        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();
        broker.clearCache();

        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, resultMovie.size());

        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3 + 2, resultActor.size());

        Query queryRole = queryRole(null, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(3, resultRole.size());

        //*****************************
        // remove all actors
        movie.setActors(new ArrayList());
        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();
        //*****************************

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(movie);
        movie = (Movie) broker.getObjectByIdentity(oid);

        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(1, resultMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(3 + 2, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(0, resultRole.size());

        assertNotNull(movie);
        assertEquals(0, movie.getActors().size());
    }

    public void testStoreUpdateActorTTFF()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, NONE, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, NONE, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, NONE, false);

        String postfix = "" + System.currentTimeMillis();
        Actor actor = buildActorWithMovies(postfix);

        broker.beginTransaction();
        broker.store(actor);
        broker.commitTransaction();

        broker.clearCache();
        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(3, resultMovie.size());

        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(1, resultActor.size());

        Query queryRole = queryRole(actor, null);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(3, resultRole.size());

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(actor);
        Actor loadedActor = (Actor) broker.getObjectByIdentity(oid);
        assertNotNull(loadedActor);
        MovieManageableCollection col = loadedActor.getMovies();
        assertNotNull(col);
        col.get(0);
    }

    public void testAddNewEntriesTTTF()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, OBJECT, false);
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, OBJECT, false);
        doTestAddNewEntries();
    }

    public void testAddNewEntriesTTTT()
    {
        ojbChangeReferenceSetting(MovieImpl.class, "actors", true, OBJECT, OBJECT, true);
        ojbChangeReferenceSetting(MovieImpl.class, "actors2", true, OBJECT, OBJECT, true);
        // default proxy does not work for user defined collection
        ojbChangeReferenceSetting(Actor.class, "movies", true, OBJECT, OBJECT, false);
        doTestAddNewEntries();
    }

    public void doTestAddNewEntries()
    {
        String postfix = "doTestAddNewEntries_" + System.currentTimeMillis();

        /*
        Returns 1 movie object with 3 actor objects and one actor object with
        back-reference to movie object + 2 new movies
        */
        Movie movie = buildMovieWithActorsAndBackReferences(postfix);
        Actor a_1 = new Actor("testAddNewEntries_"+postfix);
        Actor a_2 = new Actor("testAddNewEntries_"+postfix);
        Actor a_3 = new Actor("testAddNewEntries_"+postfix);
        Actor a_4 = new Actor("testAddNewEntries_"+postfix);
        /*
        all in all we expect 3 movie, 6 actor, 3 role entries after first
        store.
        */

        broker.beginTransaction();
        broker.store(movie);
        broker.store(a_1);
        broker.store(a_2);
        broker.store(a_3);
        broker.commitTransaction();

        broker.clearCache();

        Query queryMovie = queryMovie(postfix);
        Collection resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(3, resultMovie.size());

        Query queryActor = queryActor(postfix);
        Collection resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(6, resultActor.size());

        Query queryRole = queryRole(null, movie);
        Collection resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(3, resultRole.size());

        broker.clearCache();
        Identity oid = broker.serviceIdentity().buildIdentity(movie);
        Movie readMovie = (Movie) broker.getObjectByIdentity(oid);
        assertNotNull(readMovie);
        assertEquals(3, readMovie.getActors().size());
        assertEquals(0, readMovie.getActors2().size());

        /*
        we add 2 existing actor an movie object, thus we expect
        3 movie, 6 actor, 5 role entries after store.
        And next lookup of movie we expect 5 dependend actor objects
        */
        movie.getActors().add(a_1);
        movie.getActors().add(a_2);
        // add new actor object
        movie.getActors().add(a_4);
        broker.beginTransaction();
        broker.store(movie);
        broker.commitTransaction();

        broker.clearCache();

        queryMovie = queryMovie(postfix);
        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(3, resultMovie.size());

        queryActor = queryActor(postfix);
        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(7, resultActor.size());

        queryRole = queryRole(null, movie);
        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(6, resultRole.size());

        broker.clearCache();
        oid = broker.serviceIdentity().buildIdentity(movie);
        readMovie = (Movie) broker.getObjectByIdentity(oid);
        assertNotNull(readMovie);
        assertEquals(6, readMovie.getActors().size());

        /*
        on delete we expect that all entries are deleted except the single
        actor which have no references to any movie object
        */
        broker.beginTransaction();
        broker.delete(movie);
        broker.commitTransaction();

        broker.clearCache();
        resultMovie = broker.getCollectionByQuery(queryMovie);
        assertEquals(0, resultMovie.size());

        resultActor = broker.getCollectionByQuery(queryActor);
        assertEquals(1, resultActor.size());

        resultRole = broker.getCollectionByQuery(queryRole);
        assertEquals(0, resultRole.size());
    }

    //=======================================================================
    // helper methods
    //=======================================================================
    Query queryMovie(String postfix)
    {
        Criteria c = new Criteria();
        c.addLike("idStr", "%" + postfix + "%");
        return QueryFactory.newQuery(Movie.class, c);
    }

    Query queryActor(String postfix)
    {
        Criteria c = new Criteria();
        c.addLike("name", "%" + postfix + "%");
        return QueryFactory.newQuery(Actor.class, c);
    }

    Query queryRole(Actor actor, Movie movie)
    {
        Criteria c = new Criteria();
        if(actor != null) c.addEqualTo("actorId", actor.getId());
        if(movie != null && actor != null)
        {
            Criteria c2 = new Criteria();
            c2.addEqualTo("movieIntId", movie.getIdInt());
            c2.addEqualTo("movieStrId", movie.getIdStr());
            c.addOrCriteria(c2);
        }
        else if(movie != null)
        {
            c.addEqualTo("movieIntId", movie.getIdInt());
            c.addEqualTo("movieStrId", movie.getIdStr());
        }
        return QueryFactory.newQuery(Role.class, c);
    }

    Query roleQueryActorOrMovieMatch(Actor actor, Movie movie)
    {
        Criteria c_1 = new Criteria();
        Criteria c_2 = new Criteria();
        if(actor != null) c_1.addEqualTo("actorId", actor.getId());
        if(movie != null)
        {
            c_2.addEqualTo("movieIntId", movie.getIdInt());
            c_2.addEqualTo("movieStrId", movie.getIdStr());
        }
        if(actor != null)
        {
            c_2.addOrCriteria(c_1);
        }
        else
        {
            c_2 = c_1;
        }
        return QueryFactory.newQuery(Role.class, c_2);
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
     * Returns 1 movie object with 3 actor objects
     */
    Movie buildMovieWithActors(String postfixId, int actorCount)
    {
        Movie m = new MovieImpl(postfixId, "Movie with "+ actorCount+" actors_" + postfixId, "none");

        ArrayList list = new ArrayList();
        for(int i = 0; i < actorCount; i++)
        {
            Actor a = new Actor("A bad actor_" + postfixId);
            list.add(a);
        }
        m.setActors(list);
        return m;
    }

    /**
     * Returns 1 movie object with 3 actor objects
     */
    Actor buildActorWithMovies(String postfixId, int movieCount)
    {
        Actor a = new Actor(postfixId+"_Actor play in "+ movieCount+" movies");

        MovieManageableCollection list = new MovieManageableCollection();
        for(int i = 0; i < movieCount; i++)
        {
            Movie m  = new MovieImpl(postfixId, "A bad movie_" + postfixId, "none");
            list.add(m);
        }
        a.setMovies(list);
        return a;
    }

    /**
     * Returns 1 movie object with 3 actor objects and one actor object with
     * back-reference to movie object + 2 new movies
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

        MovieManageableCollection mlist1 = new MovieManageableCollection();
        mlist1.add(m);
        mlist1.add(m1);
        mlist1.add(m2);
        MovieManageableCollection mlist2 = new MovieManageableCollection();
        MovieManageableCollection mlist3 = new MovieManageableCollection();
        a1.setMovies(mlist1);
        a2.setMovies(mlist2);
        a3.setMovies(mlist3);

        return m;
    }

    Actor buildActorWithMovies(String postfixId)
    {
        Actor a = new Actor("John Cusack" + postfixId);
        MovieManageableCollection list = new MovieManageableCollection();
        list.add(new MovieImpl("a_" + postfixId, "High Fidelity", "A comedy about fear of commitment, hating your job..." + postfixId));
        list.add(new MovieImpl("b_" + postfixId, "Identity", "When a nasty storm hits a hotel, ten strangers are stranded within ..." + postfixId));
        list.add(new MovieImpl("c_" + postfixId, "Grosse Pointe Blank", "Martin Blank is a professional assassin. He is sent on a mission to a small Detroit ..." + postfixId));
        a.setMovies(list);
        return a;
    }



    //=======================================================================
    // Inner classes, persistence capable  test classes
    //=======================================================================
    public static class MovieManageableCollection implements ManageableCollection
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

    //===================================================================
    // inner class
    //===================================================================
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

        public void addMovie(Movie m)
        {
            if(movies == null)
            {
                movies = new MovieManageableCollection();
            }
            movies.add(m);
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
            return ToStringBuilder.reflectionToString(this);
        }
    }

    //===================================================================
    // inner class
    //===================================================================
    public static interface Movie
    {
        public Collection getActors();
        public void setActors(Collection actors);
        public void addActor(Actor a);

        public Collection getActors2();
        public void setActors2(Collection actors);

        public List getProducers();
        public void setProducers(List producers);
        public void addProducer(Producer p);

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

    //===================================================================
    // inner class
    //===================================================================
    public static class MovieImpl implements Movie
    {
        private Integer idInt;
        private Integer idInt2;
        private String idStr;

        private String title;
        private String description;
        private Collection actors;
        private Collection actors2;
        private List producers;

        public MovieImpl()
        {
        }

        public MovieImpl(String idStr, String title, String description)
        {
            this.idStr = idStr;
            this.title = title;
            this.description = description;
        }

        public List getProducers()
        {
            return producers;
        }

        public void setProducers(List producers)
        {
            this.producers = producers;
        }

        public void addProducer(Producer p)
        {
            if(producers == null)
            {
                producers = new ArrayList();
            }
            producers.add(p);
            if(p.getMovies() == null || !p.getMovies().contains(this))
            {
                p.addMovie(this);
            }
        }

        public Collection getActors()
        {
            return actors;
        }

        public void setActors(Collection actors)
        {
            this.actors = actors;
        }

        public void addActor(Actor a)
        {
            if(actors == null)
            {
                actors = new ArrayList();
            }
            actors.add(a);
        }

        public Collection getActors2()
        {
            return actors2;
        }

        public void setActors2(Collection actors)
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

        public int hashCode()
        {
            return new HashCodeBuilder().append(idInt).append(idInt2).append(idStr).hashCode();
        }

        public boolean equals(Object obj)
        {
            boolean result = false;
            if(obj instanceof MovieImpl)
            {
                MovieImpl other = (MovieImpl) obj;
                result = new EqualsBuilder().append(idInt, other.idInt).append(idInt2, other.idInt2).append(idStr, other.idStr).isEquals();
            }
            return result;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    //===================================================================
    // inner class
    //===================================================================
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
            return ToStringBuilder.reflectionToString(this);
        }
    }

    //===================================================================
    // inner class
    //===================================================================
    /**
     * This class has a m:n relation with Movie and also use a composite
     * key.
     */
    public static class Producer
    {
        private Integer id;
        private String idStr;
        private String name;
        private List movies;

        public Producer()
        {
        }

        public Producer(String idStr, String name)
        {
            this.idStr = idStr;
            this.name = name;
        }

        public List getMovies()
        {
            return movies;
        }

        public void setMovies(List movies)
        {
            this.movies = movies;
        }

        public void addMovie(Movie movie)
        {
            if(movies == null)
            {
                movies = new ArrayList();
            }
            movies.add(movie);
            if(movie.getProducers() == null || !movie.getProducers().contains(this))
            {
                movie.addProducer(this);
            }
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getIdStr()
        {
            return idStr;
        }

        public void setIdStr(String idStr)
        {
            this.idStr = idStr;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public int hashCode()
        {
            return new HashCodeBuilder().append(id).append(idStr).hashCode();
        }

        public boolean equals(Object obj)
        {
            boolean result = false;
            if(obj instanceof Producer)
            {
                Producer other = (Producer) obj;
                result = new EqualsBuilder().append(id, other.id).append(idStr, other.idStr).isEquals();
            }
            return result;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}