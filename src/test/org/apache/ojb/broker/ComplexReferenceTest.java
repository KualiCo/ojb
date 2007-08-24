package org.apache.ojb.broker;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.ArrayList;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryFactory;

/**
 * Test case for checking the management of references.
 *
 * TODO: Implement useful test cases
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ComplexReferenceTest.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class ComplexReferenceTest extends TestCase
{
    /**
     * simple prefix, used un tests
     */
    private static String PRE = "ComplexReferenceTest";
    private PersistenceBroker broker;

    public static void main(String[] args)
    {
        String[] arr = {ComplexReferenceTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void setUp() throws PBFactoryException
    {
        // called for each test method invocation
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
    }

    public void tearDown()
    {
        try
        {
            if(broker != null) broker.close();
        }
        catch (PersistenceBrokerException e)
        {
        }
    }

    public void testCreateDeleteProject() throws Exception
    {
        String prefix = getStamp() + "testCreateDeleteProject";
        String projectName = prefix + "project";

        Project project = createProject(projectName, null, null);
        try
        {
            broker.beginTransaction();
            broker.store(project);
            broker.commitTransaction();
            Collection result = getProjectsByName(broker, projectName);
            assertNotNull(result);
            assertEquals(1, result.size());

            broker.beginTransaction();
            broker.delete(project);
            broker.commitTransaction();
            result = getProjectsByName(broker, projectName);
            assertNotNull(result);
            assertEquals(0, result.size());
        }
        finally
        {
            if(broker != null) broker.close();
        }
    }

    public void testCreateDeleteProjectWithSubProjects() throws Exception
    {
        String prefix = getStamp() + "testCreateDeleteProjectWithSubProjects";
        String projectName = prefix + "_project";
        String subName = prefix + "_subproject";

        Project project = createProject(projectName, null, null);
        SubProject sub_1 = createSubProject(subName, project, null);
        SubProject sub_2 = createSubProject(subName, project, null);
        ArrayList subs = new ArrayList();
        subs.add(sub_1);
        subs.add(sub_2);
        project.setSubProjects(subs);

        try
        {
            broker.beginTransaction();
            broker.store(project);
            broker.commitTransaction();
            Collection result = getProjectsByName(broker, projectName);
            assertNotNull(result);
            assertEquals(1, result.size());
            Collection subProj = getSubProjectsByName(broker, subName);
            assertNotNull(subProj);
            assertEquals(2, subProj.size());

            broker.beginTransaction();
            broker.delete(project);
            broker.commitTransaction();
            result = getProjectsByName(broker, projectName);
            assertNotNull(result);
            assertEquals(0, result.size());
            subProj = getSubProjectsByName(broker, subName);
            assertNotNull(subProj);
            assertEquals(0, subProj.size());
        }
        finally
        {
            if(broker != null) broker.close();
        }
    }


    //=====================================================================
    // Helper methods
    //=====================================================================
    private Collection getProjectsByName(PersistenceBroker pb, String name)
    {
        return getObjectsByField(pb, Project.class, "name", name);
    }

    private Collection getSubProjectsByName(PersistenceBroker pb, String name)
    {
        return getObjectsByField(pb, SubProject.class, "name", name);
    }

    private Collection getRolesByDescription(PersistenceBroker pb, String description)
    {
        return getObjectsByField(pb, Role.class, "description", description);
    }

    private Collection getAllRoles(PersistenceBroker pb)
    {
        return getObjectsByField(pb, Role.class, null, null);
    }

    private Collection getAllTeamMembers(PersistenceBroker pb)
    {
        return getObjectsByField(pb, TeamMember.class, null, null);
    }

    private Collection getAllProjectEngineers(PersistenceBroker pb)
    {
        return getObjectsByField(pb, ProjectEngineer.class, null, null);
    }

    private Employee getEmployeeByName(PersistenceBroker pb, String name)
    {
        return (Employee)getSingleObjectByField(pb, Employee.class, "name", name);
    }

    private Collection getObjectsByField(PersistenceBroker pb, Class target, String fieldName, String match)
    {
        Criteria crit = new Criteria();
        if(fieldName != null) crit.addEqualTo(fieldName, match);
        Query query = QueryFactory.newQuery(target, crit);
        return pb.getCollectionByQuery(query);
    }

    private Object getSingleObjectByField(PersistenceBroker pb, Class target, String fieldName, String match)
    {
        Criteria crit = new Criteria();
        crit.addEqualTo(fieldName, match);
        Query query = QueryFactory.newQuery(target, crit);
        return pb.getObjectByQuery(query);
    }

    private String getStamp()
    {
        return PRE + "_" + System.currentTimeMillis();
    }

    private Project createProject(String name, ProjectEngineer leader, Collection subProjects)
    {
        Project p = new Project();
        p.setName(name);
        p.setLeader(leader);
        p.setSubProjects(subProjects);
        return p;
    }

    private SubProject createSubProject(String name, Project project, TeamMember tutor)
    {
        SubProject s = new SubProject();
        s.setName(name);
        s.setProject(project);
        s.setTutor(tutor);
        return s;
    }

    private Employee createEmployee(String name, Collection roles)
    {
        Employee e = new Employee();
        e.setName(name);
        e.setRoles(roles);
        return e;
    }

    private ProjectEngineer createEngineer(String description, Employee employee, Collection projects)
    {
        ProjectEngineer result = new ProjectEngineer();
        result.setDescription(description);
        result.setEmployee(employee);
        result.setProjects(projects);
        return result;
    }

    private TeamMember createMember(String description, Employee employee, Collection subProjects)
    {
        TeamMember result = new TeamMember();
        result.setDescription(description);
        result.setEmployee(employee);
        result.setSubProjects(subProjects);
        return result;
    }

    //=====================================================================
    // used persistence capable objects
    //=====================================================================
    public static class Employee
    {
        Integer id;
        String name;
        Collection roles = new ArrayList();

        public Collection getRoles()
        {
            return roles;
        }

        public void setRoles(Collection roles)
        {
            this.roles = roles;
        }

        public void addRole(Role role)
        {
            ArrayList list = new ArrayList(getRoles());
            list.add(role);
            setRoles(list);
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
    }

    public static interface Role
    {
        public Employee getEmployee();

        public void setEmployee(Employee employee);

        public Integer getId();

        public void setId(Integer id);

        public String getDescription();

        public void setDescription(String description);
    }

    public static class AbstractRole implements Role
    {
        Integer id;
        String description;
        Employee employee;

        public Employee getEmployee()
        {
            return employee;
        }

        public void setEmployee(Employee employee)
        {
            this.employee = employee;
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    public static class TeamMember extends AbstractRole
    {
        Collection subProjects;

        public Collection getSubProjects()
        {
            return subProjects;
        }

        public void setSubProjects(Collection subProjects)
        {
            this.subProjects = subProjects;
        }
    }

    public static class ProjectEngineer extends AbstractRole
    {
        Collection projects;

        public Collection getProjects()
        {
            return projects;
        }

        public void setProjects(Collection projects)
        {
            this.projects = projects;
        }
    }

    public static class Project
    {
        Integer id;
        String name;
        ProjectEngineer leader;
        Collection subProjects;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public ProjectEngineer getLeader()
        {
            return leader;
        }

        public void setLeader(ProjectEngineer leader)
        {
            this.leader = leader;
        }

        public Collection getSubProjects()
        {
            return subProjects;
        }

        public void setSubProjects(Collection subProjects)
        {
            this.subProjects = subProjects;
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

    public static class SubProject
    {
        Integer id;
        String name;
        Project project;
        TeamMember tutor;

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

        public Project getProject()
        {
            return project;
        }

        public void setProject(Project project)
        {
            this.project = project;
        }

        public TeamMember getTutor()
        {
            return tutor;
        }

        public void setTutor(TeamMember tutor)
        {
            this.tutor = tutor;
        }
    }
}
