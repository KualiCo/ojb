package org.apache.ojb.broker;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.junit.PBTestCase;

/**
 * This TestClass tests OJB facilities to work with polymorphism.
 */
public class AutomaticForeignKeys extends PBTestCase
{
	public static void main(String[] args)
	{
		String[] arr = {AutomaticForeignKeys.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

    public AutomaticForeignKeys(String name)

	{
		super(name);
	}

	public void setUp() throws Exception
	{
		super.setUp();
		ClassDescriptor cld = broker.getClassDescriptor(Article.class);
		ObjectReferenceDescriptor ord =
			(ObjectReferenceDescriptor) cld.getObjectReferenceDescriptors().get(0);
		ord.setCascadeStore(true);
	}

	/** test automatic assignment of foreign keys  for 1:1 reference. */
	public void testOneOneReference()
	{
		try
		{
			Article art = new Article();
			art.setArticleName("OJB O/R mapping power");
			ProductGroup pg = new ProductGroup();
			pg.setName("Software");
			art.setProductGroup(pg);
			Identity artOID = new Identity(art, broker);
			broker.beginTransaction();
			broker.store(art);
			broker.store(pg);
			broker.commitTransaction();
			broker.clearCache();
			InterfaceArticle readInArt = (Article) broker.getObjectByIdentity(artOID);
			InterfaceProductGroup readInPg = readInArt.getProductGroup();
			assertEquals(art.getArticleName(), readInArt.getArticleName());
			assertEquals(pg.getName(), readInPg.getName());
		}
		catch (Throwable t)
		{
			System.out.println(t.getMessage());
			t.printStackTrace();
			fail(t.getMessage());
		}
	}

	/** test automatic assignment of foreign keys  for 1:n reference. */
	public void testOneManyReference()
	{
		try
		{
            ProductGroup pg = new ProductGroup();
            pg.setName("O/R mapping tools");

			Article art1 = new Article();
			art1.setArticleName("TOPLink");
            art1.setProductGroup(pg);

			Article art2 = new Article();
			art2.setArticleName("OJB");
            art2.setProductGroup(pg);

			Article art3 = new Article();
			art3.setArticleName("CASTOR");
            art3.setProductGroup(pg);

			pg.add(art1);
			pg.add(art2);
			pg.add(art3);

			Identity pgOID = new Identity(pg, broker);
            broker.beginTransaction();
			broker.store(pg);
            broker.commitTransaction();
			broker.clearCache();
			InterfaceProductGroup readInPG =
				(InterfaceProductGroup) broker.getObjectByIdentity(pgOID);
			assertEquals(pg.getName(), readInPG.getName());
			assertEquals(pg.getAllArticles().size(), readInPG.getAllArticles().size());
			InterfaceArticle art1a = (InterfaceArticle) pg.getAllArticles().get(0);
			InterfaceArticle art2a = (InterfaceArticle) pg.getAllArticles().get(1);
			InterfaceArticle art3a = (InterfaceArticle) pg.getAllArticles().get(2);
			assertEquals(art1.getArticleName(), art1a.getArticleName());
			assertEquals(art2.getArticleName(), art2a.getArticleName());
			assertEquals(art3.getArticleName(), art3a.getArticleName());
		}
		catch (Throwable t)
		{
			System.out.println(t.getMessage());
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
