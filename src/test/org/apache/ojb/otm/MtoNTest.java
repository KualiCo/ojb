/**
 * User: om
 */

package org.apache.ojb.otm;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.util.collections.ManageableArrayList;
import org.apache.ojb.broker.util.collections.RemovalAwareCollection;
import org.apache.ojb.broker.*;
import org.apache.ojb.otm.lock.LockingException;
import org.apache.ojb.otm.core.Transaction;

/**
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @version $Id: MtoNTest.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class MtoNTest extends TestCase
{
	private static Class CLASS = MtoNTest.class;
	private TestKit _kit;
	private OTMConnection _conn;
	private static final int COUNT = 1;
	private static final long TIME = System.currentTimeMillis();

	public void setUp() throws LockingException
	{
		_kit = TestKit.getTestInstance();
		_conn = _kit.acquireConnection(PersistenceBrokerFactory.getDefaultKey());
	}

	public void tearDown() throws LockingException
	{
		_conn.close();
		_conn = null;
	};

	public static void main(String[] args)
	{
		String[] arr = {CLASS.getName()};
		junit.textui.TestRunner.main(arr);
	}

	private Paper createPaper() throws LockingException
	{
		String now = new Date().toString();
		Paper paper = new Paper();
		paper.setAuthor("Jonny Myers");
		paper.setDate(now);

		Qualifier qual1 = new Topic();
		qual1.setName("qual1 " + now);
		Qualifier qual2 = new Topic();
		qual2.setName("qual2 " + now);

		List qualifiers = new Vector();
		qualifiers.add(qual1);
		qualifiers.add(qual2);
		paper.setQualifiers(qualifiers);
		Transaction trans = _kit.getTransaction(_conn);
		trans.begin();
		_conn.makePersistent(qual1);
		_conn.makePersistent(qual2);
		_conn.makePersistent(paper);
		Identity paperId = _conn.getIdentity(paper);
		trans.commit();

		// sanity check
		trans = _kit.getTransaction(_conn);
		trans.begin();
		Paper retPaper = (Paper) _conn.getObjectByIdentity(paperId);
		qualifiers = retPaper.getQualifiers();

		assertEquals(2, qualifiers.size());
		trans.commit();

		return retPaper;
	}

	public void testCreate() throws Exception
	{
		Paper paper = createPaper();
	}

	public void testStoringWithAutoUpdateFalse() throws Exception
	{
		ClassDescriptor cld = _conn.getDescriptorFor(Paper.class);
		CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
		boolean autoUpdate = cod.getCascadeStore();
		cod.setCascadeStore(false);
		try
		{
			String now = new Date().toString();
			Paper paper = new Paper();
			paper.setAuthor("Jonny Myers");
			paper.setDate(now);
			Qualifier qual = new Topic();
			qual.setName("qual " + now);
			paper.setQualifiers(Arrays.asList(new Qualifier[]{qual}));
			Transaction trans = _kit.getTransaction(_conn);
			trans.begin();
			_conn.makePersistent(paper);        // store Paper and intermediary table only
			Identity paperId = _conn.getIdentity(paper);
			trans.commit();

		//	broker.clearCache();
			trans = _kit.getTransaction(_conn);
			trans.begin();
			Paper retPaper = (Paper) _conn.getObjectByIdentity(paperId);
			assertEquals(0, retPaper.getQualifiers().size());
			trans.commit();
			;
		}
		finally
		{
			cod.setCascadeStore(autoUpdate);
		}
	}

	public void testStoringWithAutoUpdateTrue() throws Exception
	{
		ClassDescriptor cld = _conn.getDescriptorFor(Paper.class);
		CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
		boolean autoUpdate = cod.getCascadeStore();

		cod.setCascadeStore(true);

		try
		{
			String now = new Date().toString();
			Paper paper = new Paper();
			paper.setAuthor("Jonny Myers");
			paper.setDate(now);
			Qualifier qual = new Topic();
			qual.setName("qual " + now);
			paper.setQualifiers(Arrays.asList(new Qualifier[]{qual}));
			Transaction trans = _kit.getTransaction(_conn);
			trans.begin();
			_conn.makePersistent(paper);        // store Paper, intermediary and Qualifier
			Identity paperId = _conn.getIdentity(paper);
			trans.commit();
			//broker.clearCache();
			trans = _kit.getTransaction(_conn);
			trans.begin();
			Paper retPaper = (Paper) _conn.getObjectByIdentity(paperId);
			assertEquals(1, retPaper.getQualifiers().size());
			trans.commit();
			;
		}
		finally
		{
			cod.setCascadeStore(autoUpdate);
		}
	}


	// delete from intermediary table only when collection NOT removal aware
	public void testDelete_NonRemovalAware() throws Exception
	{
		ClassDescriptor cld = _conn.getDescriptorFor(Paper.class);
		CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
		Class collectionClass = cod.getCollectionClass();

		cod.setCollectionClass(ManageableArrayList.class);

		try
		{
			Paper paper = createPaper();
			Identity paperId = _conn.getIdentity(paper);
			List qualifiers = paper.getQualifiers();
			Qualifier qual1 = (Qualifier) qualifiers.get(0);
			Qualifier qual2 = (Qualifier) qualifiers.get(1);

			// remove first object
			qualifiers.remove(0);
			Transaction trans = _kit.getTransaction(_conn);
			trans.begin();
			_conn.makePersistent(paper);
			trans.commit();
			;

			//broker.clearCache();
			trans = _kit.getTransaction(_conn);
			trans.begin();
			Paper retPaper = (Paper) _conn.getObjectByIdentity(paperId);
			assertEquals(1, retPaper.getQualifiers().size());
			// target object qual1 should NOT be deleted
			Qualifier retQual1 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual1));
			Qualifier retQual2 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual2));

			assertNotNull(retQual1);
			assertNotNull(retQual2);

			trans.commit();
			;
		}
		finally
		{
			cod.setCollectionClass(collectionClass);
		}

	}

	// delete from intermediary AND target-table when collection removal aware
	public void testDelete_RemovalAware() throws Exception
	{
		ClassDescriptor cld = _conn.getDescriptorFor(Paper.class);
		CollectionDescriptor cod = cld.getCollectionDescriptorByName("qualifiers");
		Class collectionClass = cod.getCollectionClass();

		cod.setCollectionClass(RemovalAwareCollection.class);

		try
		{
			Paper paper = createPaper();
			List qualifiers = paper.getQualifiers();
			Qualifier qual1 = (Qualifier) qualifiers.get(0);
			Qualifier qual2 = (Qualifier) qualifiers.get(1);
			Identity paperId = _conn.getIdentity(paper);

			// remove first object
			qualifiers.remove(0);
			Transaction trans = _kit.getTransaction(_conn);
			trans.begin();
			_conn.makePersistent(paper);
			trans.commit();
			;

		//	broker.clearCache();
			trans = _kit.getTransaction(_conn);
			trans.begin();
			Paper retPaper = (Paper) _conn.getObjectByIdentity(paperId);
			assertEquals(1, retPaper.getQualifiers().size());

			// target object qual1 should be deleted
			Qualifier retQual1 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual1));
			Qualifier retQual2 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual2));

			assertNull(retQual1);
			assertNotNull(retQual2);

			trans.commit();
			;
		}
		finally
		{
			cod.setCollectionClass(collectionClass);
		}
	}

	public void testDeletionFromIntermediaryTableWithNullList() throws Exception
	{
		Paper paper = createPaper();
		Identity paperId = _conn.getIdentity(paper);
		List qualifiers = paper.getQualifiers();
		Qualifier qual1 = (Qualifier) qualifiers.get(0);
		Qualifier qual2 = (Qualifier) qualifiers.get(1);

		// now set collection to null and check if changes get persisted
		paper.setQualifiers(null);
		Transaction trans = _kit.getTransaction(_conn);
		trans.begin();
		_conn.makePersistent(paper);
		trans.commit();
		;

		//broker.clearCache();
		trans = _kit.getTransaction(_conn);
		trans.begin();
		Paper retPaper = (Paper) _conn.getObjectByIdentity(paperId);
		assertEquals(0, retPaper.getQualifiers().size());

		// target objects should NOT be deleted
		Qualifier retQual1 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual1));
		Qualifier retQual2 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual2));

		assertNotNull(retQual1);
		assertNotNull(retQual2);

		trans.commit();
		;
	}

	public void testDeletionWithClearedList() throws Exception
	{
		Paper paper = createPaper();
		Identity paperId = _conn.getIdentity(paper);
		List qualifiers = paper.getQualifiers();
		Qualifier qual1 = (Qualifier) qualifiers.get(0);
		Qualifier qual2 = (Qualifier) qualifiers.get(1);

		// now clear collection
		paper.getQualifiers().clear();
		Transaction trans = _kit.getTransaction(_conn);
		trans.begin();
		_conn.makePersistent(paper);
		trans.commit();
		;

		//broker.clearCache();
		trans = _kit.getTransaction(_conn);
		trans.begin();
		Paper retPaper = (Paper) _conn.getObjectByIdentity(paperId);
		assertEquals(0, retPaper.getQualifiers().size());

		// target objects should be deleted
		Qualifier retQual1 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual1));
		Qualifier retQual2 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual2));

		assertNull(retQual1);
		assertNull(retQual2);

		trans.commit();
		;
	}

	public void testDeletionFromIntermediaryTableWithEmptyList() throws Exception
	{
		Paper paper = createPaper();
		Identity paperId = _conn.getIdentity(paper);
		List qualifiers = paper.getQualifiers();
		Qualifier qual1 = (Qualifier) qualifiers.get(0);
		Qualifier qual2 = (Qualifier) qualifiers.get(1);

		// now empty collection and check if changes get persisted
		paper.setQualifiers(new RemovalAwareCollection());
		Transaction trans = _kit.getTransaction(_conn);
		trans.begin();
		_conn.makePersistent(paper);
		trans.commit();
		;

	//	broker.clearCache();
		trans = _kit.getTransaction(_conn);
		trans.begin();
		Paper retPaper = (Paper) _conn.getObjectByIdentity(paperId);
		assertEquals(0, retPaper.getQualifiers().size());

		// target objects should NOT be deleted
		Qualifier retQual1 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual1));
		Qualifier retQual2 = (Qualifier) _conn.getObjectByIdentity(_conn.getIdentity(qual2));

		assertNotNull(retQual1);
		assertNotNull(retQual2);

		trans.commit();
		;
	}

}
