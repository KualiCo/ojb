package org.apache.ojb.broker;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

import java.util.Collection;
import java.util.Iterator;

/**
 * Testing graph structure, which mean two relations between two classes.
 */
public class GraphTest extends PBTestCase
{
	public static void main(String[] args)
	{
		String[] arr = {GraphTest.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

	public GraphTest(String name)
	{
		super(name);
	}

	private void clearDatabase()
	{
		Criteria crit = new Criteria();
		Query q;
		Iterator iter;

		q = QueryFactory.newQuery(GraphNode.class, crit);
		iter = broker.getIteratorByQuery(q);
        broker.beginTransaction();
		while (iter.hasNext())
		{
			broker.delete(iter.next());
		}
        broker.commitTransaction();

		q = QueryFactory.newQuery(GraphEdge.class, crit);
		iter = broker.getIteratorByQuery(q);
		broker.beginTransaction();
        while (iter.hasNext())
		{
			broker.delete(iter.next());
		}
        broker.commitTransaction();
	}


	public void testCreate()
	{
		clearDatabase();    // Clear database before inserting new data

		GraphNode a = new GraphNode("A");
		GraphNode b = new GraphNode("B");
		GraphNode c = new GraphNode("C");
		Identity oid = new Identity(a, broker);
		new Identity(b, broker);
		new Identity(c, broker);

		GraphEdge aa = new GraphEdge(a, a);
		GraphEdge ab = new GraphEdge(a, b);
		GraphEdge bc = new GraphEdge(b, c);
		GraphEdge ac = new GraphEdge(a, c);
		new Identity(aa, broker);
		new Identity(ab, broker);
		new Identity(bc, broker);
		new Identity(ac, broker);

		Point locA = new Point(0, 0);
		Point locB = new Point(1, 0);
		Point locC = new Point(1, 1);

		new Identity(locA, broker);
		new Identity(locB, broker);
		new Identity(locC, broker);

        broker.beginTransaction();
		broker.store(locA);
		broker.store(locB);
		broker.store(locC);
        broker.commitTransaction();

		a.setLocation(locA);
		b.setLocation(locB);
		c.setLocation(locC);

        broker.beginTransaction();
		broker.store(a);
		broker.store(b);
		broker.store(c);
        broker.commitTransaction();

		broker.clearCache();

		GraphNode retrieved = (GraphNode) broker.getObjectByIdentity(oid);
		assertEquals("check graph structure", "A [(A -> A), (A -> B [(B -> C [])]), (A -> C [])]", retrieved.toString());
	}

	public void testEqualToFieldQuery()
	{
		Criteria crit;
		Query q;
		Collection results;

		crit = new Criteria();
		crit.addEqualToField("name", "outgoingEdges.sink.name");
		q = QueryFactory.newQuery(GraphNode.class, crit);
		results = broker.getCollectionByQuery(q);
		//System.out.println(results);
		assertNotNull(results);
		assertEquals(results.size(), 1); // only "bc" conforms

	}


	public void testSingleJoin()
	{
		Criteria crit;
		Query q;
		Collection results;
		crit = new Criteria();
		crit.addEqualTo("source.name", "A");
		q = QueryFactory.newQuery(GraphEdge.class, crit);
		results = broker.getCollectionByQuery(q);
		assertNotNull(results);
		assertEquals(results.size(), 3); // only "bc" conforms
	}

	public void testNestedJoin()
	{
		Criteria crit;
		Query q;
		Collection results;
		crit = new Criteria();
		crit.addGreaterThan("source.location.x", new Integer(0));
		crit.addEqualTo("source.location.y", new Integer(0));
		q = QueryFactory.newQuery(GraphEdge.class, crit);
		results = broker.getCollectionByQuery(q);
		assertNotNull(results);
		assertEquals(results.size(), 1); // only "bc" conforms
	}

	public void testMultiNonNestedJoin()
	{
		Criteria crit;
		Query q;
		Collection results;
		crit = new Criteria();
		crit.addEqualTo("source.name", "A");
		crit.addEqualTo("sink.name", "B");
		q = QueryFactory.newQuery(GraphEdge.class, crit);
		results = broker.getCollectionByQuery(q);
		assertNotNull(results);
		assertEquals(results.size(), 1); // only "bc" conforms
	}
}
