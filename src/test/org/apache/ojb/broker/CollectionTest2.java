package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

/**
 * Test case for collection handling.
 *
 * IMPORTANT NOTE: The global runtime metadata changes made by this test case
 * are NOT recommended in multithreaded environments, because they are global
 * and each thread will be affected.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: CollectionTest2.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class CollectionTest2 extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {CollectionTest2.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void tearDown() throws Exception
    {
        if(broker != null)
        {
            changeAutoSetting(Project.class, "subProjects", false, false, false, false);
            changeAutoSetting(Project.class, "developers", false, false, false, false);
            changeAutoSetting(SubProject.class, "project", false, false, false, false);
            broker.close();
        }
        super.tearDown();
    }

    public void testAutoUpdateDeleteSettings()
    {
        changeAutoSetting(Project.class, "subProjects", false, false, false, false);
        CollectionDescriptor ord = broker.getClassDescriptor(Project.class)
                .getCollectionDescriptorByName("subProjects");
        assertEquals(CollectionDescriptor.CASCADE_LINK, ord.getCascadingStore());
        assertEquals(CollectionDescriptor.CASCADE_NONE, ord.getCascadingDelete());
        assertEquals(false, ord.getCascadeStore());
        assertEquals(false, ord.getCascadeDelete());

        changeAutoSetting(Project.class, "subProjects", false, true, true, false);
        ord = broker.getClassDescriptor(Project.class)
                .getCollectionDescriptorByName("subProjects");
        assertEquals(ObjectReferenceDescriptor.CASCADE_OBJECT, ord.getCascadingStore());
        assertEquals(ObjectReferenceDescriptor.CASCADE_OBJECT, ord.getCascadingDelete());
        assertEquals(true, ord.getCascadeStore());
        assertEquals(true, ord.getCascadeDelete());
    }

    public void testStoreUpdateDelete_1a()
    {
        changeAutoSetting(Project.class, "subProjects", false, false, false, false);
        changeAutoSetting(Project.class, "developers", false, false, false, false);
        changeAutoSetting(SubProject.class, "project", false, false, false, false);
        doTestStoreUpdateDelete_1();
    }

    public void testStoreUpdateDelete_1b()
    {
        changeAutoSetting(Project.class, "subProjects", false, false, false, false);
        changeAutoSetting(Project.class, "developers", false, false, false, false);
        changeAutoSetting(SubProject.class, "project", false, false, false, false);
        doTestStoreUpdateDelete_1();
    }

    public void doTestStoreUpdateDelete_1()
    {
        String name = "testStoreUpdateDelete_1" + System.currentTimeMillis();
        changeAutoSetting(Project.class, "subProjects", false, false, false, false);
        changeAutoSetting(Project.class, "developers", false, false, false, false);
        changeAutoSetting(SubProject.class, "project", false, false, false, false);

        Developer dev1 = new Developer(name);
        Developer dev2 = new Developer(name);
        ArrayList devList = new ArrayList();
        devList.add(dev1);
        devList.add(dev2);

        SubProject sub1 = new SubProject(name, null);
        SubProject sub2 = new SubProject(name, null);
        ArrayList subList = new ArrayList();
        subList.add(sub1);
        subList.add(sub2);

        Project pro = new Project(name, subList, devList);
        sub1.setProject(pro);
        sub2.setProject(pro);
        pro.setDevelopers(devList);
        pro.setSubProjects(subList);
        sub1.setProject(pro);
        sub2.setProject(pro);

        Query queryProject = createQueryFor(Project.class, "name", name);
        Query querySubProject = createQueryFor(SubProject.class, "name", name);
        Query queryDeveloper = createQueryFor(Developer.class, "name", name);

        //*****************************************
        // insert
        //*****************************************
        broker.beginTransaction();
        broker.store(pro);
        broker.commitTransaction();

        broker.clearCache();
        Collection result = broker.getCollectionByQuery(queryProject);
        assertEquals(1, result.size());
        assertNotNull(result.iterator().next());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(0, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(0, result.size());

        broker.beginTransaction();
        broker.store(sub1);
        broker.store(sub2);
        broker.store(dev1);
        broker.store(dev2);
        broker.commitTransaction();

        broker.clearCache();
        result = broker.getCollectionByQuery(queryProject);
        assertEquals(1, result.size());
        assertNotNull(result.iterator().next());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(2, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(2, result.size());

        Identity proOid = new Identity(pro, broker);
        Project newPro = (Project) broker.getObjectByIdentity(proOid);
        assertNull(newPro.getDevelopers());
        assertNull(newPro.getSubProjects());
        //assertEquals(0, newPro.getDevelopers().size());
        //assertEquals(0, newPro.getSubProjects().size());
        broker.retrieveAllReferences(newPro);
        assertNotNull(newPro.getDevelopers());
        assertNotNull(newPro.getSubProjects());
        assertEquals(2, newPro.getDevelopers().size());
        assertEquals(2, newPro.getSubProjects().size());
        SubProject sub = (SubProject) newPro.getSubProjects().iterator().next();
        assertNotNull(sub);
        assertNull(sub.getProject());
        broker.retrieveAllReferences(sub);
        assertNotNull(sub.getProject());
        assertEquals(sub.getProject().getId(), newPro.getId());

        //*****************************************
        // update
        //*****************************************
        broker.clearCache();
        newPro = (Project) broker.getObjectByIdentity(proOid);
        newPro.setName("updated_" + name);
        broker.retrieveAllReferences(newPro);

        broker.beginTransaction();
        broker.store(newPro);
        Iterator it = newPro.getSubProjects().iterator();
        while(it.hasNext())
        {
            SubProject subProject = (SubProject) it.next();
            broker.retrieveAllReferences(subProject);
            subProject.setName("updated_" + name);
            broker.store(subProject);
        }
        broker.commitTransaction();

        broker.clearCache();
        newPro = (Project) broker.getObjectByIdentity(proOid);
        assertEquals("updated_" + name, newPro.getName());
        assertNull(newPro.getDevelopers());
        assertNull(newPro.getSubProjects());
        broker.retrieveAllReferences(newPro);
        assertNotNull(newPro.getDevelopers());
        assertNotNull(newPro.getSubProjects());
        assertEquals(2, newPro.getDevelopers().size());
        assertEquals(2, newPro.getSubProjects().size());
        it = newPro.getSubProjects().iterator();
        while(it.hasNext())
        {
            SubProject subProject = (SubProject) it.next();
            assertEquals("updated_" + name, subProject.getName());
        }


        //*****************************************
        // delete
        //*****************************************
        broker.clearCache();
        broker.beginTransaction();
        broker.delete(sub1);
        broker.delete(sub2);
        broker.delete(dev1);
        broker.delete(dev2);
        broker.delete(pro);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(queryProject);
        assertEquals(0, result.size());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(0, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(0, result.size());
    }

    public void testStoreUpdateDelete_2a()
    {
        changeAutoSetting(Project.class, "subProjects", true, true, true, false);
        changeAutoSetting(Project.class, "developers", true, true, true, false);
        changeAutoSetting(SubProject.class, "project", true, true, true, false);
        doTestStoreUpdateDelete_2();
    }

    public void testStoreUpdateDelete_2b()
    {
        changeAutoSetting(Project.class, "subProjects", true, true, true, true);
        changeAutoSetting(Project.class, "developers", true, true, true, true);
        changeAutoSetting(SubProject.class, "project", true, true, true, true);
        doTestStoreUpdateDelete_2();
    }

    public void doTestStoreUpdateDelete_2()
    {
        String name = "testStoreUpdateDelete_2" + System.currentTimeMillis();

        Developer dev1 = new Developer(name);
        Developer dev2 = new Developer(name);
        ArrayList devList = new ArrayList();
        devList.add(dev1);
        devList.add(dev2);

        SubProject sub1 = new SubProject(name, null);
        SubProject sub2 = new SubProject(name, null);
        ArrayList subList = new ArrayList();
        subList.add(sub1);
        subList.add(sub2);

        Project pro = new Project(name, subList, devList);
        sub1.setProject(pro);
        sub2.setProject(pro);

        sub1.setProject(pro);
        sub2.setProject(pro);
        pro.setSubProjects(subList);
        pro.setDevelopers(devList);

        Query queryProject = createQueryFor(Project.class, "name", name);
        Query querySubProject = createQueryFor(SubProject.class, "name", name);
        Query queryDeveloper = createQueryFor(Developer.class, "name", name);

        //*****************************************
        // insert
        //*****************************************
        broker.beginTransaction();
        broker.store(pro);
        broker.commitTransaction();

        broker.clearCache();
        Collection result = broker.getCollectionByQuery(queryProject);
        assertEquals(1, result.size());
        assertNotNull(result.iterator().next());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(2, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(2, result.size());

        Identity proOid = new Identity(pro, broker);
        Project newPro = (Project) broker.getObjectByIdentity(proOid);
        assertNotNull(newPro.getDevelopers());
        assertNotNull(newPro.getSubProjects());
        assertEquals(2, newPro.getDevelopers().size());
        assertEquals(2, newPro.getSubProjects().size());
        SubProject sub = (SubProject) newPro.getSubProjects().iterator().next();
        assertNotNull(sub);
        assertNotNull(sub.getProject());
        assertEquals(sub.getProject().getId(), newPro.getId());

        //*****************************************
        // update
        //*****************************************
        broker.clearCache();
        newPro = (Project) broker.getObjectByIdentity(proOid);
        newPro.setName("updated_" + name);
        broker.beginTransaction();
        Iterator it = newPro.getSubProjects().iterator();
        while(it.hasNext())
        {
            SubProject subProject = (SubProject) it.next();
            subProject.setName("updated_" + name);
        }
        broker.store(newPro);
        broker.commitTransaction();

        broker.clearCache();
        newPro = (Project) broker.getObjectByIdentity(proOid);
        assertEquals("updated_" + name, newPro.getName());
        assertNotNull(newPro.getDevelopers());
        assertNotNull(newPro.getSubProjects());
        assertEquals(2, newPro.getDevelopers().size());
        assertEquals(2, newPro.getSubProjects().size());
        it = newPro.getSubProjects().iterator();
        while(it.hasNext())
        {
            SubProject subProject = (SubProject) it.next();
            assertEquals("updated_" + name, subProject.getName());
        }

        //*****************************************
        // delete
        //*****************************************
        broker.clearCache();
        broker.beginTransaction();
        broker.delete(pro);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(queryProject);
        assertEquals(0, result.size());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(0, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(0, result.size());
    }

    public void testStoreUpdateDeleteSerialize_1a()
    {
        changeAutoSetting(Project.class, "subProjects", true, true, true, false);
        changeAutoSetting(Project.class, "developers", true, true, true, false);
        changeAutoSetting(SubProject.class, "project", true, true, true, false);
        doTestStoreUpdateDeleteSerialize();
    }

    public void testStoreUpdateDeleteSerialize_1b()
    {
        changeAutoSetting(Project.class, "subProjects", true, true, true, true);
        changeAutoSetting(Project.class, "developers", true, true, true, true);
        changeAutoSetting(SubProject.class, "project", true, true, true, true);
        doTestStoreUpdateDeleteSerialize();
    }

    public void doTestStoreUpdateDeleteSerialize()
    {
        String name = "testStoreUpdateDelete_2" + System.currentTimeMillis();

        Developer dev1 = new Developer(name);
        Developer dev2 = new Developer(name);
        ArrayList devList = new ArrayList();
        devList.add(dev1);
        devList.add(dev2);

        SubProject sub1 = new SubProject(name, null);
        SubProject sub2 = new SubProject(name, null);
        ArrayList subList = new ArrayList();
        subList.add(sub1);
        subList.add(sub2);

        Project pro = new Project(name, subList, devList);
        sub1.setProject(pro);
        sub2.setProject(pro);

        sub1.setProject(pro);
        sub2.setProject(pro);
        pro.setSubProjects(subList);
        pro.setDevelopers(devList);

        Query queryProject = createQueryFor(Project.class, "name", name);
        Query querySubProject = createQueryFor(SubProject.class, "name", name);
        Query queryDeveloper = createQueryFor(Developer.class, "name", name);

        //*****************************************
        // insert
        //*****************************************
        broker.beginTransaction();
        broker.store(pro);
        broker.commitTransaction();

        broker.clearCache();
        Collection result = broker.getCollectionByQuery(queryProject);
        assertEquals(1, result.size());
        assertNotNull(result.iterator().next());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(2, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(2, result.size());

        Identity proOid = new Identity(pro, broker);
        Project newPro = (Project) broker.getObjectByIdentity(proOid);
        assertNotNull(newPro.getDevelopers());
        assertNotNull(newPro.getSubProjects());
        assertEquals(2, newPro.getDevelopers().size());
        assertEquals(2, newPro.getSubProjects().size());
        SubProject sub = (SubProject) newPro.getSubProjects().iterator().next();
        assertNotNull(sub);
        assertNotNull(sub.getProject());
        assertEquals(sub.getProject().getId(), newPro.getId());

        //*****************************************
        // update
        //*****************************************
        broker.clearCache();
        newPro = (Project) broker.getObjectByIdentity(proOid);
        newPro.setName("updated_" + name);

        //*****************************************
        // de-/serialize object
        //*****************************************
        newPro = (Project) SerializationUtils.deserialize(SerializationUtils.serialize(newPro));


        broker.beginTransaction();
        newPro.getSubProjects().get(0);
        Iterator it = newPro.getSubProjects().iterator();
        while(it.hasNext())
        {
            SubProject subProject = (SubProject) it.next();
            subProject.setName("updated_" + name);
        }
        broker.store(newPro);
        broker.commitTransaction();

        broker.clearCache();
        newPro = (Project) broker.getObjectByIdentity(proOid);
        assertEquals("updated_" + name, newPro.getName());
        assertNotNull(newPro.getDevelopers());
        assertNotNull(newPro.getSubProjects());
        assertEquals(2, newPro.getDevelopers().size());
        assertEquals(2, newPro.getSubProjects().size());
        it = newPro.getSubProjects().iterator();
        while(it.hasNext())
        {
            SubProject subProject = (SubProject) it.next();
            assertEquals("updated_" + name, subProject.getName());
        }

        //*****************************************
        // de-/serialize object
        //*****************************************
        newPro = (Project) SerializationUtils.deserialize(SerializationUtils.serialize(newPro));

        //*****************************************
        // delete
        //*****************************************
        broker.clearCache();
        broker.beginTransaction();
        broker.delete(pro);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(queryProject);
        assertEquals(0, result.size());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(0, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(0, result.size());
    }

    public void testStoreUpdateDelete_3a()
    {
        changeAutoSetting(Project.class, "subProjects", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_LINK, false);
        changeAutoSetting(Project.class, "developers", true,
                CollectionDescriptor.CASCADE_LINK, CollectionDescriptor.CASCADE_OBJECT, false);
        changeAutoSetting(SubProject.class, "project", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_NONE, false);
        doTestStoreUpdateDelete_3();
    }

    public void testStoreUpdateDelete_3b()
    {
        changeAutoSetting(Project.class, "subProjects", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_LINK, true);
        changeAutoSetting(Project.class, "developers", true,
                CollectionDescriptor.CASCADE_LINK, CollectionDescriptor.CASCADE_OBJECT, true);
        changeAutoSetting(SubProject.class, "project", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_NONE, true);
        doTestStoreUpdateDelete_3();
    }

    public void doTestStoreUpdateDelete_3()
    {
        String name = "testStoreUpdateDelete_2" + System.currentTimeMillis();

        Developer dev1 = new Developer(name);
        Developer dev2 = new Developer(name);
        ArrayList devList = new ArrayList();
        devList.add(dev1);
        devList.add(dev2);

        SubProject sub1 = new SubProject(name, null);
        SubProject sub2 = new SubProject(name, null);
        ArrayList subList = new ArrayList();
        subList.add(sub1);
        subList.add(sub2);

        Project pro = new Project(name, subList, devList);
        sub1.setProject(pro);
        sub2.setProject(pro);

        sub1.setProject(pro);
        sub2.setProject(pro);
        pro.setSubProjects(subList);
        pro.setDevelopers(devList);

        Query queryProject = createQueryFor(Project.class, "name", name);
        Query querySubProject = createQueryFor(SubProject.class, "name", name);
        Query queryDeveloper = createQueryFor(Developer.class, "name", name);

        //*****************************************
        // insert
        //*****************************************
        broker.beginTransaction();
        broker.store(pro);
        broker.commitTransaction();

        broker.clearCache();
        Collection result = broker.getCollectionByQuery(queryProject);
        assertEquals(1, result.size());
        assertNotNull(result.iterator().next());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(2, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(0, result.size());

        broker.beginTransaction();
        dev1.setProjectId(pro.getId());
        dev2.setProjectId(pro.getId());
        broker.store(dev1);
        broker.store(dev2);
        broker.commitTransaction();

        broker.clearCache();
        Identity proOid = new Identity(pro, broker);
        Project newPro = (Project) broker.getObjectByIdentity(proOid);
        assertNotNull(newPro.getDevelopers());
        assertNotNull(newPro.getSubProjects());
        assertEquals(2, newPro.getDevelopers().size());
        assertEquals(2, newPro.getSubProjects().size());
        SubProject sub = (SubProject) newPro.getSubProjects().iterator().next();
        assertNotNull(sub);
        assertNotNull(sub.getProject());
        assertEquals(sub.getProject().getId(), newPro.getId());

        //*****************************************
        // update
        //*****************************************
        broker.clearCache();
        newPro = (Project) broker.getObjectByIdentity(proOid);
        newPro.setName("updated_" + name);
        broker.beginTransaction();
        Iterator it = newPro.getSubProjects().iterator();
        while(it.hasNext())
        {
            SubProject subProject = (SubProject) it.next();
            subProject.setName("updated_" + name);
        }
        broker.store(newPro);
        broker.commitTransaction();

        broker.clearCache();
        newPro = (Project) broker.getObjectByIdentity(proOid);
        assertEquals("updated_" + name, newPro.getName());
        assertNotNull(newPro.getDevelopers());
        assertNotNull(newPro.getSubProjects());
        assertEquals(2, newPro.getDevelopers().size());
        assertEquals(2, newPro.getSubProjects().size());
        it = newPro.getSubProjects().iterator();
        while(it.hasNext())
        {
            SubProject subProject = (SubProject) it.next();
            assertEquals("updated_" + name, subProject.getName());
        }

        //*****************************************
        // delete
        //*****************************************
        broker.clearCache();
        broker.beginTransaction();
        broker.serviceBrokerHelper().unlink(sub1, "project");
        broker.serviceBrokerHelper().unlink(sub2, "project");
        broker.store(sub1);
        broker.store(sub2);
        broker.delete(pro);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(queryProject);
        assertEquals(0, result.size());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(2, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(0, result.size());
    }

    public void testUpdate_1()
    {
        changeAutoSetting(Project.class, "subProjects", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_NONE, false);
        changeAutoSetting(Project.class, "developers", true,
                CollectionDescriptor.CASCADE_LINK, CollectionDescriptor.CASCADE_LINK, false);
        changeAutoSetting(SubProject.class, "project", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_NONE, false);
    }

    public void testUpdate_2()
    {
        changeAutoSetting(Project.class, "subProjects", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_NONE, true);
        changeAutoSetting(Project.class, "developers", true,
                CollectionDescriptor.CASCADE_LINK, CollectionDescriptor.CASCADE_LINK, true);
        changeAutoSetting(SubProject.class, "project", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_NONE, true);
    }

    public void doTestUpdate()
    {
        String name = "testStoreUpdateDelete_2" + System.currentTimeMillis();

        changeAutoSetting(Project.class, "subProjects", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_NONE, false);
        changeAutoSetting(Project.class, "developers", true,
                CollectionDescriptor.CASCADE_LINK, CollectionDescriptor.CASCADE_LINK, false);
        changeAutoSetting(SubProject.class, "project", true,
                CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_NONE, false);

        Developer dev1 = new Developer(name);
        Developer dev2 = new Developer(name);
        ArrayList devList = new ArrayList();
        devList.add(dev1);
        devList.add(dev2);

        SubProject sub1 = new SubProject(name, null);
        SubProject sub2 = new SubProject(name, null);
        ArrayList subList = new ArrayList();
        subList.add(sub1);
        subList.add(sub2);

        Project pro = new Project(name, subList, devList);
        sub1.setProject(pro);
        sub2.setProject(pro);

        sub1.setProject(pro);
        sub2.setProject(pro);
        pro.setSubProjects(subList);
        pro.setDevelopers(devList);

        Query queryProject = createQueryFor(Project.class, "name", name);
        Query querySubProject = createQueryFor(SubProject.class, "name", name);
        Query queryDeveloper = createQueryFor(Developer.class, "name", name);

        //*****************************************
        // insert
        //*****************************************
        broker.beginTransaction();
        broker.store(pro);
        broker.commitTransaction();

        broker.clearCache();
        Collection result = broker.getCollectionByQuery(queryProject);
        assertEquals(1, result.size());
        assertNotNull(result.iterator().next());
        result = broker.getCollectionByQuery(querySubProject);
        assertEquals(2, result.size());
        result = broker.getCollectionByQuery(queryDeveloper);
        assertEquals(0, result.size());
    }

    //============================================================================
    // helper methods
    //============================================================================

    void changeAutoSetting(Class clazz, String referenceField, boolean autoRetrieve, boolean autoUpdate, boolean autoDelete, boolean useProxy)
    {
        ClassDescriptor cld = broker.getClassDescriptor(clazz);
        ObjectReferenceDescriptor ref = cld.getCollectionDescriptorByName(referenceField);
        if(ref == null) ref = cld.getObjectReferenceDescriptorByName(referenceField);
        ref.setLazy(useProxy);
        ref.setCascadeRetrieve(autoRetrieve);
        ref.setCascadeStore(autoUpdate);
        ref.setCascadeDelete(autoDelete);
    }

    void changeAutoSetting(Class clazz, String referenceField, boolean autoRetrieve, int autoUpdate, int autoDelete, boolean useProxy)
    {
        ClassDescriptor cld = broker.getClassDescriptor(clazz);
        ObjectReferenceDescriptor ref = cld.getCollectionDescriptorByName(referenceField);
        if(ref == null) ref = cld.getObjectReferenceDescriptorByName(referenceField);
        ref.setLazy(useProxy);
        ref.setCascadeRetrieve(autoRetrieve);
        ref.setCascadingStore(autoUpdate);
        ref.setCascadingDelete(autoDelete);
    }

    Query createQueryFor(Class clazz, String attribute, String value)
    {
        Criteria crit = new Criteria();
        crit.addLike(attribute, "%" + value + "%");
        return QueryFactory.newQuery(clazz, crit);
    }


    //============================================================================
    // inner classes, used test classes
    //============================================================================

    public static class Project implements ProjectIF
    {
        private Integer id;
        private String name;
        private List subProjects;
        private Collection developers;

        public Project()
        {
        }

        public Project(String name, List subProjects, Collection developers)
        {
            this.name = name;
            this.subProjects = subProjects;
            this.developers = developers;
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

        public List getSubProjects()
        {
            return subProjects;
        }

        public void setSubProjects(List subProjects)
        {
            this.subProjects = subProjects;
        }

        public Collection getDevelopers()
        {
            return developers;
        }

        public void setDevelopers(Collection developers)
        {
            this.developers = developers;
        }
    }

    public static interface ProjectIF extends Serializable
    {
        public Integer getId();
        public void setId(Integer id);
        public String getName();
        public void setName(String name);
        public List getSubProjects();
        public void setSubProjects(List subProjects);
        public Collection getDevelopers();
        public void setDevelopers(Collection developers);
    }

    public static class SubProject implements SubProjectIF
    {
        private Integer id;
        private String name;
        private ProjectIF project;

        public SubProject()
        {
        }

        public SubProject(String name, Project project)
        {
            this.name = name;
            this.project = project;
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

        public ProjectIF getProject()
        {
            return project;
        }

        public void setProject(ProjectIF project)
        {
            this.project = project;
        }
    }

    public static interface SubProjectIF extends Serializable
    {
        public Integer getId();
        public void setId(Integer id);
        public String getName();
        public void setName(String name);
        public ProjectIF getProject();
        public void setProject(ProjectIF project);
    }

    public static class Developer implements Serializable
    {
        private Integer id;
        private String name;
        private Integer projectId;

        public Developer()
        {
        }

        public Developer(String name)
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

        public Integer getProjectId()
        {
            return projectId;
        }

        public void setProjectId(Integer projectId)
        {
            this.projectId = projectId;
        }
    }
}
