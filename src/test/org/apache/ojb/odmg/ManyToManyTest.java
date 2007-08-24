package org.apache.ojb.odmg;

import java.util.List;

import org.apache.ojb.broker.Fish;
import org.apache.ojb.broker.Salad;
import org.apache.ojb.junit.ODMGTestCase;
import org.apache.ojb.odmg.shared.ODMGGourmet;
import org.odmg.OQLQuery;
import org.odmg.Transaction;

public class ManyToManyTest extends ODMGTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {ManyToManyTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public ManyToManyTest(String name)
    {
        super(name);
    }

    /**
     * this tests if polymorph collections (i.e. collections of objects
     * implementing a common interface) are treated correctly
     */
    public void testPolymorphMToN() throws Exception
    {
        String postfix = "testPolymorphMToN_" + System.currentTimeMillis();
        ODMGGourmet james = new ODMGGourmet(postfix + "_james");
        ODMGGourmet doris = new ODMGGourmet(postfix + "_doris");

        Fish tuna = new Fish(postfix + "_tuna", 242, "salt");
        Fish trout = new Fish(postfix + "_trout", 52, "fresh water");

        Salad radiccio = new Salad(postfix + "_Radiccio", 7, "red");
        Salad lolloverde = new Salad(postfix + "_Lollo verde", 7, "green");

        james.addFavoriteFood(tuna);
        james.addFavoriteFood(radiccio);

        doris.addFavoriteFood(tuna);
        doris.addFavoriteFood(trout);
        doris.addFavoriteFood(lolloverde);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(james);
        database.makePersistent(doris);
        tx.commit();

        int dorisId = doris.getGourmetId();
        int jamesId = james.getGourmetId();

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        tx.getBroker().clearCache();
        
        OQLQuery query = odmg.newOQLQuery();
        query.create("select gourmets from " + ODMGGourmet.class.getName() +
                " where gourmetId=$1");
        query.bind(new Integer(dorisId));
        List gourmets = (List) query.execute();
        tx.commit();
        assertEquals(1, gourmets.size());
        ODMGGourmet loadedDoris = (ODMGGourmet) gourmets.get(0);
        //System.err.println(loadedDoris);
        assertEquals(3, loadedDoris.getFavoriteFood().size());

        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select gourmets from " + ODMGGourmet.class.getName() +
                " where gourmetId=$1");
        query.bind(new Integer(jamesId));
        gourmets = (List) query.execute();
        tx.commit();
        assertEquals(1, gourmets.size());
        ODMGGourmet loadedJames = (ODMGGourmet) gourmets.get(0);
        //System.err.println(loadedJames);
        assertEquals(2, loadedJames.getFavoriteFood().size());
    }

//    /**
//     * Store the objects and return the result of the query
//     * "select gourmets from " + ODMGGourmet.class.getName() + " where gourmetId=$1"
//     */
//    private int store(Implementation odmg, Database db, ODMGGourmet gourmet) throws Exception
//    {
//        Transaction tx = odmg.newTransaction();
//        tx.begin();
//        db.makePersistent(gourmet);
//        tx.commit();
//
//        tx.begin();
//        OQLQuery query = odmg.newOQLQuery();
//        query = odmg.newOQLQuery();
//        query.create("select gourmets from " + ODMGGourmet.class.getName() +
//                " where gourmetId=$1");
//        query.bind(new Integer(gourmet.getGourmetId()));
//        List gourmets = (List) query.execute();
//        tx.commit();
//        return gourmets.size();
//    }

    public void testMtoNSeparate_I() throws Exception
    {
        ODMGGourmet paula = new ODMGGourmet("a_testMtoNSeparate_I");
        ODMGGourmet candy = new ODMGGourmet("b_testMtoNSeparate_I");

        long timestamp = System.currentTimeMillis();
        Fish tuna = new Fish("tuna_" + timestamp, 242, "salt");
        Fish trout = new Fish("trout_" + timestamp, 52, "fresh water");

        paula.addFavoriteFood(trout);
        candy.addFavoriteFood(tuna);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(paula);
        database.makePersistent(candy);
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select fishs from " + Fish.class.getName() +
                " where (name=$1 or name=$2)");
        query.bind(tuna.getName());
        query.bind(trout.getName());
        List fishs = (List) query.execute();
        /*
        we expect 2 created 'fish'
        */
        assertEquals(2, fishs.size());
    }

    public void testMtoNSeparate_II() throws Exception
    {
        ODMGGourmet james = new ODMGGourmet("a_testMtoNSeparate_II");
        ODMGGourmet doris = new ODMGGourmet("b_testMtoNSeparate_II");

        long timestamp = System.currentTimeMillis();
        Fish tuna = new Fish("tuna_" + timestamp, 242, "salt");
        Fish trout = new Fish("trout_" + timestamp, 52, "fresh water");

        james.addFavoriteFood(tuna);

        doris.addFavoriteFood(tuna);
        doris.addFavoriteFood(trout);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(james);
        database.makePersistent(doris);
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select fishs from " + Fish.class.getName() +
                " where (name=$1 or name=$2)");
        query.bind(tuna.getName());
        query.bind(trout.getName());
        List fishs = (List) query.execute();
        /*
        we expect 2 created 'fish'
        */
        assertEquals(2, fishs.size());
    }

    public void testMtoNTogether() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Fish tuna = new Fish("tuna_" + timestamp, 242, "salt");
        Fish trout = new Fish("trout_" + timestamp, 52, "fresh water");

        ODMGGourmet paula = new ODMGGourmet("a_testMtoNTogether");
        ODMGGourmet candy = new ODMGGourmet("b_testMtoNTogether");
        ODMGGourmet james = new ODMGGourmet("c_testMtoNTogether");
        ODMGGourmet doris = new ODMGGourmet("d_testMtoNTogether");

        paula.addFavoriteFood(trout);
        candy.addFavoriteFood(tuna);
        james.addFavoriteFood(tuna);
        doris.addFavoriteFood(tuna);
        doris.addFavoriteFood(trout);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(james);
        database.makePersistent(doris);
        database.makePersistent(candy);
        database.makePersistent(paula);
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select fishs from " + Fish.class.getName() +
                " where (name=$1 or name=$2)");
        query.bind(tuna.getName());
        query.bind(trout.getName());
        List fishs = (List) query.execute();
        /*
        we expect 2 created 'fish'
        */
        assertEquals(2, fishs.size());
    }

    /**
     * main object gourment has list of food objects, this
     * test check if we add one food object to the list
     * and lock the main object, do get an updated list
     */
    public void testMtoNPolymorphUpdate() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        Fish tuna = new Fish("tuna_" + timestamp, 242, "salt");
        Fish trout = new Fish("trout_" + timestamp, 52, "fresh water");
        Fish goldfish = new Fish("goldfish_" + timestamp, 10, "brackish water");

        ODMGGourmet paula = new ODMGGourmet("a_testMtoNTogether"+ timestamp);
        ODMGGourmet candy = new ODMGGourmet("b_testMtoNTogether"+ timestamp);
        ODMGGourmet james = new ODMGGourmet("c_testMtoNTogether"+ timestamp);
        ODMGGourmet doris = new ODMGGourmet("d_testMtoNTogether"+ timestamp);

        paula.addFavoriteFood(trout);
        candy.addFavoriteFood(tuna);
        james.addFavoriteFood(tuna);
        doris.addFavoriteFood(tuna);
        doris.addFavoriteFood(trout);

        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(james);
        database.makePersistent(doris);
        database.makePersistent(candy);
        database.makePersistent(paula);
        tx.commit();

        OQLQuery query = odmg.newOQLQuery();
        query.create("select fishs from " + Fish.class.getName() +
                " where (name=$1 or name=$2)");
        query.bind(tuna.getName());
        query.bind(trout.getName());
        List fishs = (List) query.execute();
        /*
        we expect 2 created 'fish'
        */
        assertEquals(2, fishs.size());

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select gourmets from " + ODMGGourmet.class.getName() +
                " where name=$1");
        query.bind(doris.getName());
        List result = (List) query.execute();
        assertEquals("We should found a gourmet", 1, result.size());
        ODMGGourmet gourmet = (ODMGGourmet)result.get(0);

        /*
        now we lock main object and add a new reference object
        */
        tx.lock(gourmet, Transaction.WRITE);
        gourmet.addFavoriteFood(goldfish);
        tx.commit();

        query = odmg.newOQLQuery();
        query.create("select fishs from " + Fish.class.getName() +
                " where (name=$1 or name=$2 or name=$3)");
        query.bind(tuna.getName());
        query.bind(trout.getName());
        query.bind(goldfish.getName());

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        fishs = (List) query.execute();
        tx.commit();
        assertEquals("seems referenced object was not added (if found <3) ",3, fishs.size());
    }

    /**
     * main object gourment has list of food objects, this
     * test check if we delete one food object from the list
     * and lock the main object, do get an updated list
     */
    public void testMtoNPolymorphDelete() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        String name = "testMtoNPolymorphDelete_" + timestamp;
        Fish tuna = new Fish(name + "_tuna", 242, "salt");
        Fish trout = new Fish(name + "_trout", 52, "fresh water");
        Fish goldfish = new Fish(name + "_goldfish", 10, "brackish water");

        ODMGGourmet paula = new ODMGGourmet(name + "_paula");
        ODMGGourmet candy = new ODMGGourmet(name + "_candy");
        ODMGGourmet james = new ODMGGourmet(name + "_james");
        ODMGGourmet doris = new ODMGGourmet(name + "_doris");

        paula.addFavoriteFood(trout);
        candy.addFavoriteFood(tuna);
        james.addFavoriteFood(tuna);
        doris.addFavoriteFood(tuna);
        doris.addFavoriteFood(trout);
        doris.addFavoriteFood(goldfish);

        /*
        we expect one created 'gourment' per store
        */
        TransactionExt tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        database.makePersistent(paula);
        database.makePersistent(james);
        database.makePersistent(candy);
        database.makePersistent(doris);
        tx.commit();

        tx.begin();
        OQLQuery query = odmg.newOQLQuery();
        query.create("select fishs from " + Fish.class.getName() +
                " where (name=$1 or name=$2 or name=$3)");
        query.bind(tuna.getName());
        query.bind(trout.getName());
        query.bind(goldfish.getName());

        List fishs = (List) query.execute();
        tx.commit();
        /*
        we expect 3 created 'fish'
        */
        assertEquals(3, fishs.size());

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        query = odmg.newOQLQuery();
        query.create("select gourmets from " + ODMGGourmet.class.getName() +
                " where name=$1");
        query.bind(doris.getName());
        List result = (List) query.execute();
        assertEquals("We should found a gourmet_doris", 1, result.size());
        ODMGGourmet gourmet_doris = (ODMGGourmet)result.get(0);
        assertEquals(name + "_doris", gourmet_doris.getName());
        assertEquals(3, gourmet_doris.getFavoriteFood().size());

        /*
        now we lock main object and add remove a reference object
        */
        tx.lock(gourmet_doris, Transaction.WRITE);
        List foodList = gourmet_doris.getFavoriteFood();
        foodList.remove(0);
        //gourmet_doris.setFavoriteFood(foodList);
        tx.commit();

        query = odmg.newOQLQuery();
        query.create("select gourmets from " + ODMGGourmet.class.getName() +
                " where name=$1");
        query.bind(doris.getName());

        tx = (TransactionExt) odmg.newTransaction();
        tx.begin();
        result = (List) query.execute();
        assertEquals("We should found a gourmet_doris", 1, result.size());
        gourmet_doris = (ODMGGourmet)result.get(0);
        tx.commit();
        assertEquals(
        	"We removed one fish, so doris should only have two entries left",
        	2, gourmet_doris.getFavoriteFood().size());
    }
}
