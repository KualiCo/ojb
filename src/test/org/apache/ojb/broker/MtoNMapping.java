package org.apache.ojb.broker;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.junit.PBTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/** This TestClass tests OJB facilities to work with polymorphism.
 */
public class MtoNMapping extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {MtoNMapping.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public MtoNMapping(String name)
    {
        super(name);
    }

    /**
     * this tests if polymorph collections (i.e. collections of objects
     * implementing a common interface) are treated correctly
     */
    public void testPolymorphMToN()
    {
        Gourmet james = new Gourmet("james");
        Identity jamesId = new Identity(james, broker);
        Gourmet doris = new Gourmet("doris");
        Identity dorisId = new Identity(doris, broker);

        Fish tuna = new Fish("tuna", 242, "salt");
        Fish trout = new Fish("trout", 52, "fresh water");

        Salad radiccio = new Salad("Radiccio", 7, "red");
        Salad lolloverde = new Salad("Lollo verde", 7, "green");

        james.addFavoriteFood(tuna);
        james.addFavoriteFood(radiccio);

        doris.addFavoriteFood(tuna);
        doris.addFavoriteFood(trout);
        doris.addFavoriteFood(lolloverde);

        broker.beginTransaction();
        broker.store(james);
        broker.store(doris);
        broker.commitTransaction();

        broker.clearCache();

        Gourmet loadedJames = (Gourmet) broker.getObjectByIdentity(jamesId);
        List favFood = loadedJames.getFavoriteFood();
        assertEquals(2, favFood.size());

        Gourmet loadedDoris = (Gourmet) broker.getObjectByIdentity(dorisId);
        favFood = loadedDoris.getFavoriteFood();
        assertEquals(3, favFood.size());
    }

    public void testPolymorphMToNUpdate()
    {
        long timestamp = System.currentTimeMillis();
        Gourmet james = new Gourmet("james");
        Identity jamesId = new Identity(james, broker);
        Gourmet doris = new Gourmet("doris");
        Identity dorisId = new Identity(doris, broker);

        Fish tuna = new Fish("tuna", 242, "salt");
        Fish trout = new Fish("trout", 52, "fresh water");
        Fish goldfish = new Fish("goldfish_"+timestamp, 10, "brackish water");

        Salad radiccio = new Salad("Radiccio", 7, "red");
        Salad lolloverde = new Salad("Lollo verde", 7, "green");

        james.addFavoriteFood(tuna);
        james.addFavoriteFood(radiccio);

        doris.addFavoriteFood(tuna);
        doris.addFavoriteFood(trout);
        doris.addFavoriteFood(lolloverde);

        broker.beginTransaction();
        broker.store(james);
        broker.store(doris);
        broker.commitTransaction();

        broker.clearCache();

        Gourmet loadedJames = (Gourmet) broker.getObjectByIdentity(jamesId);
        List favFood = loadedJames.getFavoriteFood();
        assertEquals(2, favFood.size());

        Gourmet loadedDoris = (Gourmet) broker.getObjectByIdentity(dorisId);
        favFood = loadedDoris.getFavoriteFood();
        assertEquals(3, favFood.size());

        /*
        add new reference object
        */
        loadedDoris.addFavoriteFood(goldfish);
        // update main object
        broker.beginTransaction();
        broker.store(loadedDoris);
        broker.commitTransaction();

        broker.clearCache();
        // query main object
        loadedDoris = (Gourmet) broker.getObjectByIdentity(dorisId);
        assertEquals(4, loadedDoris.getFavoriteFood().size());
    }

    public void testPolymorphMToNDelete()
    {
        long timestamp = System.currentTimeMillis();
        Gourmet james = new Gourmet("james_" + timestamp);
        Identity jamesId = new Identity(james, broker);
        Gourmet doris = new Gourmet("doris_" + timestamp);
        Identity dorisId = new Identity(doris, broker);

        Fish tuna = new Fish("tuna_" + timestamp, 242, "salt");
        Fish trout = new Fish("trout_" + timestamp, 52, "fresh water");
        Fish goldfish = new Fish("goldfish_" + timestamp, 10, "brackish water");

        Salad radiccio = new Salad("Radiccio_" + timestamp, 7, "red");
        Salad lolloverde = new Salad("Lollo verde_" + timestamp, 7, "green");

        james.addFavoriteFood(tuna);
        james.addFavoriteFood(radiccio);

        doris.addFavoriteFood(tuna);
        doris.addFavoriteFood(trout);
        doris.addFavoriteFood(lolloverde);
        doris.addFavoriteFood(goldfish);

        broker.beginTransaction();
        broker.store(james);
        broker.store(doris);

        broker.commitTransaction();

        broker.clearCache();

        Gourmet loadedJames = (Gourmet) broker.getObjectByIdentity(jamesId);
        List favFood = loadedJames.getFavoriteFood();
        assertEquals(2, favFood.size());

        Gourmet loadedDoris = (Gourmet) broker.getObjectByIdentity(dorisId);
        favFood = loadedDoris.getFavoriteFood();
        assertEquals(4, favFood.size());

        Criteria c = new Criteria();
        c.addLike("name", "%"+timestamp);
        Query q = QueryFactory.newQuery(InterfaceFood.class, c);
        Collection result = broker.getCollectionByQuery(q);
        int foodBeforeRemove = result.size();
        assertEquals("Wrong number of InterfaceFood objects", 5, foodBeforeRemove);


        List foodList = loadedDoris.getFavoriteFood();
        foodList.remove(0);
        loadedDoris.setFavoriteFood(foodList);
        // update main object
        broker.beginTransaction();
        broker.store(loadedDoris);
        broker.commitTransaction();

        broker.clearCache();
        // query main object
        loadedDoris = (Gourmet) broker.getObjectByIdentity(dorisId);
        assertEquals(3, loadedDoris.getFavoriteFood().size());
        result = broker.getCollectionByQuery(q);
        assertEquals("n-side object shouldn't be removed", foodBeforeRemove, result.size());
    }

    /** test loading of m:n mapped object nets*/
    public void testMNLoading() throws Exception
    {
        broker.clearCache();

        Person p = new Person();
        p.setId(1);
        Query q = QueryFactory.newQuery(p);
        p = (Person) broker.getObjectByQuery(q);
        assertNotNull(p);
        Collection projects = p.getProjects();
        assertNotNull(projects);
        assertTrue(projects.size() > 0);

        projects.toArray(new Project[0]); // load it

        Criteria c = null;
        q = QueryFactory.newQuery(Project.class, c);
        Collection col = broker.getCollectionByQuery(q);
        assertNotNull(col);
    }

    /** test loading of m:n mapped object nets with prefetch*/
    public void testMNLoadingPrefetch() throws Exception
    {
        Criteria crit;
        QueryByCriteria qry;
        Collection col1, col2;

        broker.clearCache();

        crit = new Criteria();
        crit.addLessThan("id",new Integer(6));
        qry = QueryFactory.newQuery(Project.class, crit);
        qry.addOrderByAscending("id");
        col1 = broker.getCollectionByQuery(qry);
        assertNotNull(col1);

        broker.clearCache();

        crit = new Criteria();
        crit.addLessThan("id",new Integer(6));
        qry = QueryFactory.newQuery(Project.class, crit);
        qry.addOrderByAscending("id");
        qry.addPrefetchedRelationship("persons");
        col2 = broker.getCollectionByQuery(qry);
        assertNotNull(col2);

        assertEquals("Same size",col1.size(), col2.size());

        Iterator it1 = col1.iterator();
        Iterator it2 = col2.iterator();

        while (it1.hasNext() && it2.hasNext())
        {
            Project p1 = (Project)it1.next();
            Project p2 = (Project)it2.next();

            assertEquals("Same Title", p1.getTitle(), p2.getTitle());
            assertEquals("Same Number of Persons", p1.getPersons().size(), p2.getPersons().size());
            assertEquals("Same toString", p1.toString(), p2.toString());
        }
    }


    /** test loading of m:n unidirectionally mapped objects*/
    public void testMNLoadingUnidirectional() throws Exception
    {
        broker.clearCache();

        PersonUnidirectional p = new PersonUnidirectional();
        p.setId(1);
        Query q = QueryFactory.newQuery(p);
        p = (PersonUnidirectional) broker.getObjectByQuery(q);
        Collection projects = p.getProjects();
        assertNotNull(projects);
        assertTrue(projects.size() > 0);
        projects.toArray(new ProjectUnidirectional[0]); // load it
    }

    /** test a manually build association via the association class Role*/
    public void testLoadingWithAssociationClass() throws Exception
    {
        Person p = new Person();
        p.setId(1);
        Query q = QueryFactory.newQuery(p);
        p = (Person) broker.getObjectByQuery(q);

        Vector roles = (Vector) p.getRoles();
        assertNotNull(roles);
        //System.out.println(roles);

        Criteria c = null;
        q = QueryFactory.newQuery(Project.class, c);
        Collection col = broker.getCollectionByQuery(q);
        assertNotNull(col);

        Iterator iter = col.iterator();
        while (iter.hasNext())
        {
            iter.next();
            //System.out.println(proj.getRoles());
        }
    }

    /** test inserting new objects to m:n association*/
    public void testInsertion() throws Exception
    {
        Person p = new Person();
        p.setId(1);
        Query q = QueryFactory.newQuery(p);
        p = (Person) broker.getObjectByQuery(q);
        assertNotNull("We should found a 'person' for id 1 - check db script", p);
        Collection projects = p.getProjects();
        assertNotNull(projects);
        projects.toArray(new Project[0]); // load it
        assertNotNull("Person should have some projects - check db script", projects);
        int count = projects.size();

        Project proj = new Project();
        proj.setPersons(new ArrayList());
        proj.setTitle("MARS");
        proj.setDescription("colonization of planet Mars");

        p.getProjects().add(proj);
        proj.getPersons().add(p);
        assertEquals(count + 1, p.getProjects().size());

        broker.beginTransaction();
        broker.store(p);
        broker.commitTransaction();

        broker.clearCache();

        p = (Person) broker.getObjectByQuery(q);
        assertEquals(count + 1, p.getProjects().size());
    }

    /** Add a new Project, delete an existing Project */
    public void testInsertAndDelete() throws Exception
    {
        Person pers = new Person();
        pers.setId(7);
        Query query = QueryFactory.newQuery(pers);
        pers = (Person) broker.getObjectByQuery(query);
        Collection projects = pers.getProjects();
        Project[] projectArray = (Project[]) projects.toArray(new Project[0]);
        Project oldProj, newProj;
        int count = projects.size();

        oldProj = projectArray[0];
        projects.remove(oldProj);

        newProj = new Project();
        newProj.setTitle("Test Project1 for Person 7");
        newProj.setDescription("This is a Test Project1 for Person 7");
        projects.add(newProj);

        newProj = new Project();
        newProj.setTitle("Test Project2 for Person 7");
        newProj.setDescription("This is a Test Project2 for Person 7");
        projects.add(newProj);

        broker.beginTransaction();
        broker.store(pers);
        broker.commitTransaction();

        broker.clearCache();

        pers = (Person) broker.getObjectByQuery(query);
        assertEquals(count + 1 , pers.getProjects().size());

    }

    /**
     * Create a project with two persons
     * @param title
     * @return
     * @throws Exception
     */
    private Project createProjectWithAssignedPersons_1(String title) throws Exception
    {
        /*
        the order of store statements is crucial because the relationship
        pointing from Project to Person has auto-update=false
        */
        // create new project
        Project project = new Project();
        project.setTitle(title);

        // create two persons and assign project
        // and assign persons with project
        Person p1 = new Person();
        p1.setFirstname(title);
        broker.beginTransaction();

        broker.store(p1);

        List projects_1 = new ArrayList();
        projects_1.add(project);
        p1.setProjects(projects_1); // connect project to person

        Person p2 = new Person();
        p2.setFirstname(title);
        broker.store(p2);

        List projects_2 = new ArrayList();
        projects_2.add(project);
        p2.setProjects(projects_2); // connect project to person

        ArrayList persons = new ArrayList();
        persons.add(p1);
        persons.add(p2);
        project.setPersons(persons);    // connect persons to project

        broker.store(project);
        broker.commitTransaction();

        return project;
    }

    /**
     * Create a project with two persons
     * both relationships are set to auto-update=true
     * @param title
     * @return
     * @throws Exception
     */
    private Project createProjectWithAssignedPersons_2(String title) throws Exception
    {
        ClassDescriptor cldProject = broker.getClassDescriptor(Project.class);
        CollectionDescriptor codPersons =cldProject.getCollectionDescriptorByName("persons");
        boolean cascadeStorePersons = codPersons.getCascadeStore();

        ClassDescriptor cldPerson = broker.getClassDescriptor(Person.class);
        CollectionDescriptor codProjects =cldPerson.getCollectionDescriptorByName("projects");
        boolean cascadeStoreProjects = codProjects.getCascadeStore();

        // temporarily set auto-update = true
        codPersons.setCascadeStore(true);
        codProjects.setCascadeStore(true);

        // create new project
        Project project = new Project();
        project.setTitle(title);

        // create two persons and assign project
        // and assign persons with project
        Person p1 = new Person();
        p1.setFirstname(title);

        List projects_1 = new ArrayList();
        projects_1.add(project);
        p1.setProjects(projects_1); // connect project to person

        Person p2 = new Person();
        p2.setFirstname(title);

        List projects_2 = new ArrayList();
        projects_2.add(project);
        p2.setProjects(projects_2); // connect project to person

        ArrayList persons = new ArrayList();
        persons.add(p1);
        persons.add(p2);
        project.setPersons(persons);    // connect persons to project

        broker.beginTransaction();
        broker.store(project);
        broker.commitTransaction();

        // reset original value
        codPersons.setCascadeStore(cascadeStorePersons);
        codProjects.setCascadeStore(cascadeStoreProjects);

        return project;
    }

    /**
     * Add two new persons and one new project. Assign persons with the
     * new project and vice versa.
     */
    public void testInsertWithIndirectionTable_1() throws Exception
    {
        String title = "testInsertWithIndirectionTable_1_" + System.currentTimeMillis();

        Project project = createProjectWithAssignedPersons_1(title);

        verifyProjectWithAssignedPersons(title, project);
    }

    /**
     * Add two new persons and one new project. Assign persons with the
     * new project and vice versa.
     * both relationships are set to auto-update=true
     */
    public void testInsertWithIndirectionTable_2() throws Exception
    {
        String title = "testInsertWithIndirectionTable_2_" + System.currentTimeMillis();

        Project project = createProjectWithAssignedPersons_2(title);

        verifyProjectWithAssignedPersons(title, project);
    }

    private void verifyProjectWithAssignedPersons(String title, Project project)
    {
        /*
        Now I expect two entries in PERSON_PROJECT table
        with same project id, two new Person and one Project
        entries in PERSON/PROJECT table
        */
        broker.clearCache();
        Criteria crit = new Criteria();
        crit.addEqualTo("firstname", title);
        Query query = new QueryByCriteria(Person.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("We expect 2 person instances", 2, result.size());

        crit = new Criteria();
        crit.addEqualTo("id", new Integer(project.getId()));
        query = new QueryByCriteria(Project.class, crit);
        result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("We expect 1 project instance", 1, result.size());
        Project newProject = (Project) result.iterator().next();
        assertNotNull(newProject.getRoles());
        assertEquals("We expect 2 Role objects", 2, newProject.getRoles().size());

        // query for role objects representing PERSON_PROJECT entries
        crit = new Criteria();
        crit.addEqualTo("project_id", new Integer(project.getId()));
        query = new QueryByCriteria(Role.class, crit);
        result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("We expect 2 role instances", 2, result.size());
    }

    /**
     * Add two new persons to existing project. Assign persons with the
     * existing project and vice versa.
     */
    public void testInsertWithIndirectionTable_3() throws Exception
    {
        String title = "testInsertWithIndirectionTable_3_" + System.currentTimeMillis();

        // first we create an project with assigned persons
        // create new project with two assigned persons
        Project tempProject = createProjectWithAssignedPersons_1(title);

        // now the real update test begins
        broker.clearCache();
        Criteria critProject = new Criteria();
        critProject.addEqualTo("id", new Integer(tempProject.getId()));
        Query projectQuery = new QueryByCriteria(Project.class, critProject);

        Criteria critPerson = new Criteria();
        critPerson.addEqualTo("firstname", title);
        Query personQuery = new QueryByCriteria(Person.class, critPerson);

        broker.clearCache();

        // first we lookup roles for existing project
        // query for role objects representing PERSON_PROJECT entries
        Criteria crit = new Criteria();
        crit.addEqualTo("project_id", new Integer(tempProject.getId()));
        Query query = new QueryByCriteria(Role.class, crit);
        Collection result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertTrue("test needs existing roles for given id", result.size() > 0);
        int roleCount = result.size();

        // lookup the existing project
        Project project = (Project) broker.getObjectByQuery(projectQuery);
        assertNotNull(project);

        // create two persons and assign project
        Person p1 = new Person();
        p1.setFirstname(title);
        broker.beginTransaction();
        broker.store(p1);

        List projects_1 = new ArrayList();
        projects_1.add(project);
        p1.setProjects(projects_1);

        Person p2 = new Person();
        p2.setFirstname(title);
        broker.store(p2);

        List projects_2 = new ArrayList();
        projects_2.add(project);
        p2.setProjects(projects_2);

        // connect persons to project
        project.getPersons().add(p1);
        project.getPersons().add(p2);

        broker.store(project);
        broker.commitTransaction();

        result = broker.getCollectionByQuery(personQuery);
        assertNotNull(result);
        assertEquals("We expect 2 new person instances", 2+2, result.size());

        /*
        Now I expect two new entries in PERSON_PROJECT table
        with same project id, two new Person entries
        */
        broker.clearCache();

        result = broker.getCollectionByQuery(personQuery);
        assertNotNull(result);
        assertEquals("We expect 2 new person instances", 2+2, result.size());

        crit = new Criteria();
        crit.addEqualTo("id", new Integer(project.getId()));
        query = new QueryByCriteria(Project.class, crit);
        result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("We expect 1 project instance", 1, result.size());
        Project newProject = (Project) result.iterator().next();
        assertNotNull(newProject.getRoles());
        assertEquals("We expect 2 new Role objects", roleCount + 2, newProject.getRoles().size());

        // query for role objects representing PERSON_PROJECT entries
        crit = new Criteria();
        crit.addEqualTo("project_id", new Integer(project.getId()));
        query = new QueryByCriteria(Role.class, crit);
        result = broker.getCollectionByQuery(query);
        assertNotNull(result);
        assertEquals("We expect 2 role instances", roleCount + 2, result.size());
    }

    /** test deleting objects from an m:n association*/
    public void testDeletion() throws Exception
    {
        Person pers = new Person();
        pers.setId(1);
        Query query = QueryFactory.newQuery(pers);
        pers = (Person) broker.getObjectByQuery(query);
        Collection projects = pers.getProjects();
        Project[] projectArray = (Project[]) projects.toArray(new Project[0]); // load it
        assertNotNull(projects);
        int count = projects.size();

        Project proj = projectArray[0];

        Criteria crit = new Criteria();
        crit.addEqualTo("person_id", new Integer(pers.getId()));
        crit.addEqualTo("project_id", new Integer(proj.getId()));
        Query roleQuery = QueryFactory.newQuery(Role.class, crit);

        Role role = (Role) broker.getObjectByQuery(roleQuery);
        assertNotNull(role);
        //System.out.println(role.toString());

        broker.beginTransaction();
        broker.delete(proj);
        broker.commitTransaction();

        broker.clearCache();

        pers = (Person) broker.getObjectByQuery(query);
        assertEquals(count - 1, pers.getProjects().size());
        role = (Role) broker.getObjectByQuery(roleQuery);
        assertNull(role);
    }

    /**
     * delete all projects of a person
     */
    public void testDeleteUnidirectional() throws Exception
    {
        PersonUnidirectional p = new PersonUnidirectional();
        p.setId(1);
        Query q = QueryFactory.newQuery(p);
        p = (PersonUnidirectional) broker.getObjectByQuery(q);
        Collection projects = p.getProjects();
        Collection originalProjects;
        projects.toArray(new ProjectUnidirectional[0]); // load it
        originalProjects = new Vector();
        originalProjects.addAll(projects);

        assertNotNull(projects);
        int count = projects.size();

        ProjectUnidirectional proj = new ProjectUnidirectional();
        proj.setTitle("GALVIN");
        proj.setDescription("galvins project");

        p.getProjects().add(proj);
        broker.beginTransaction();
        broker.store(p);
        broker.commitTransaction();

        broker.clearCache();

        p = (PersonUnidirectional) broker.getObjectByQuery(q);

        assertEquals(count + 1, p.getProjects().size());

        broker.beginTransaction();

        projects = p.getProjects();
        projects.clear();
        p.setProjects(projects);

        broker.store(p);
        broker.commitTransaction();

        broker.clearCache();

        p = (PersonUnidirectional) broker.getObjectByQuery(q);
        assertEquals(0, p.getProjects().size());

        // restore originals
        broker.beginTransaction();
        p.setProjects(originalProjects);
        broker.store(p);
        broker.delete(proj);
        broker.commitTransaction();
    }
}
