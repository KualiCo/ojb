package org.apache.ojb.broker;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;

import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.MetadataManager;

/**
 * @author Matthew.Baird
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class IdentityPerformanceTest extends TestCase
{
    PersistenceBroker broker;
    private static Class CLASS = IdentityPerformanceTest.class;
    private static final int ITERATIONS = 10000;

    public static void main(String[] args)
    {
        String[] arr = {CLASS.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void testIdentityIterations()
    {
        Article art = new Article();
        art.setArticleName("OJB O/R mapping power");
        ProductGroup pg = new ProductGroup();
        pg.setName("Software");
        art.setProductGroup(pg);
        // prime the pump.
        Identity artOID = new Identity(art,broker);
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++)
        {
            artOID = new Identity(art,broker);
        }
        long stop = System.currentTimeMillis();
        System.out.println("total time to build " + ITERATIONS + " identities " + (stop - start) + " ms.");
        System.out.println("time to build one Identity " + ((stop - start) / ITERATIONS) + " ms.");
    }
    public void testMapIterations()
    {
        long start = System.currentTimeMillis();
        Map temp;

        for (int i = 0; i < ITERATIONS; i++)
        {
            temp = new HashMap();
        }
        long stop = System.currentTimeMillis();
        System.out.println("total time to build " + ITERATIONS + " HashMaps " + (stop - start) + " ms.");
        System.out.println("time to build one HashMaps " + ((stop - start) / ITERATIONS) + " ms.");
    }
    public void testDescriptorRepositoryGetDescriptorForIterations()
    {
        long start = System.currentTimeMillis();
        DescriptorRepository descriptorRepository = MetadataManager.getInstance().getRepository();
        for (int i = 0; i < ITERATIONS; i++)
        {
            descriptorRepository.getDescriptorFor(Article.class);
        }
        long stop = System.currentTimeMillis();
        System.out.println("total time to getDescriptorFor " + ITERATIONS + " times " + (stop - start) + " ms.");
        System.out.println("time to call one getDescriptorFor " + ((stop - start) / ITERATIONS) + " ms.");
    }
}
