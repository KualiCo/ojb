package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Helper class centralize test classes.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ObjectRepository.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class ObjectRepository
{
    public static class Component
            implements ComponentIF
    {
        Integer id;
        int type;
        String name;
        Group group;
        ComponentIF parentComponent;
        Collection childComponents;

        public boolean equals(Object obj)
        {
            boolean result = false;
            if(obj instanceof ComponentIF)
            {
                ComponentIF other = (ComponentIF) obj;
                result = new EqualsBuilder()
                 .append(this.getId(), other.getId())
                 .append(this.getName(), other.getName())
                 .append(this.getType(), other.getType())
                 .isEquals();
            }
            return result;
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public Group getGroup()
        {
            return group;
        }

        public void setGroup(Group group)
        {
            this.group = group;
        }

        public ComponentIF getParentComponent()
        {
            return parentComponent;
        }

        public void setParentComponent(ComponentIF parentComponent)
        {
            this.parentComponent = parentComponent;
        }

        public Collection getChildComponents()
        {
            return childComponents;
        }

        public void setChildComponents(Collection childComponents)
        {
            this.childComponents = childComponents;
        }

        public int getType()
        {
            return type;
        }

        public void setType(int type)
        {
            this.type = type;
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

    public static interface ComponentIF extends Serializable
    {
        Integer getId();
        void setId(Integer id);
        ObjectRepository.Group getGroup();
        void setGroup(ObjectRepository.Group group);
        ComponentIF getParentComponent();
        void setParentComponent(ComponentIF parentComponent);
        Collection getChildComponents();
        void setChildComponents(Collection childComponents);
        int getType();
        void setType(int type);
        String getName();
        void setName(String name);
    }

    public static class Group implements Serializable
    {
        Integer id;
        String name;

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

    public static abstract class AB implements Serializable
    {
        private int id;
        /**
         * This special attribute telling OJB which concrete class this Object has.
         * NOTE: this attribute MUST be called ojbConcreteClass
         */
        private String ojbConcreteClass;

        protected AB()
        {
            // this guarantee that always the correct class name will be set
            this.ojbConcreteClass = this.getClass().getName();
        }

        protected AB(int id)
        {
            this();
            this.id = id;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }

    public static interface AAlone
    {
    }

    public static class A extends AB implements AAlone
    {
        private int someValue;
        private String someAField;

        public A()
        {
            super();
        }

        public A(int pId, int pSomeValue)
        {
            super(pId);
            this.someValue = pSomeValue;
        }

        public String getSomeAField()
        {
            return someAField;
        }

        public void setSomeAField(String someAField)
        {
            this.someAField = someAField;
        }

        public int getSomeValue()
        {
            return someValue;
        }

        public void setSomeValue(int someValue)
        {
            this.someValue = someValue;
        }
    }

    public static class B extends AB
    {
        private int someValue;
        private String someBField;

        public B()
        {
            super();
        }

        public B(int pId, int pSomeValue)
        {
            super(pId);
            this.someValue = pSomeValue;
        }

        public String getSomeBField()
        {
            return someBField;
        }

        public void setSomeBField(String someBField)
        {
            this.someBField = someBField;
        }

        public int getSomeValue()
        {
            return someValue;
        }

        public void setSomeValue(int someValue)
        {
            this.someValue = someValue;
        }
    }

    public static class B1 extends B
    {
        public B1()
        {
            super();
        }

        public B1(int pId, int pSomeValue)
        {
            super(pId, pSomeValue);
        }
    }

    public static class C implements Serializable
    {
        private String ojbConcreteClass;
        private int id;
        private int someValue;

        public C()
        {
            ojbConcreteClass = this.getClass().getName();
        }

        public C(int pId, int pSomeValue)
        {
            this();
            this.id = pId;
            this.someValue = pSomeValue;
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }

        public int getSomeValue()
        {
            return someValue;
        }

        public void setSomeValue(int someValue)
        {
            this.someValue = someValue;
        }
    }

    public static class D extends C
    {
        public D()
        {
            super();
        }

        public D(int pId, int pSomeValue)
        {
            super(pId, pSomeValue);
        }
    }

    public static class E implements Serializable
    {
        private Integer id;
        private int someSuperValue;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public int getSomeSuperValue()
        {
            return someSuperValue;
        }

        public void setSomeSuperValue(int someSuperValue)
        {
            this.someSuperValue = someSuperValue;
        }
    }

    /**
     * important note:
     * This class uses an anonymous field to hold the foreign key referencing to the parent table.
     * thus there is no attribute holding the FK in the class.
     * There is also no additional coding required to populate the inherited attributes
     * on loading or persiting an instance of this class.
     */
    public static class F extends E implements Serializable
    {
        int someValue;

        public int getSomeValue()
        {
            return someValue;
        }

        public void setSomeValue(int someValue)
        {
            this.someValue = someValue;
        }
    }

    public static class G extends F implements Serializable
    {
        int someSubValue;

        public int getSomeSubValue()
        {
            return someSubValue;
        }

        public void setSomeSubValue(int someSubValue)
        {
            this.someSubValue = someSubValue;
        }
    }

    public static class F1 extends E implements Serializable
    {
        int someValue;

        public int getSomeValue()
        {
            return someValue;
        }

        public void setSomeValue(int someValue)
        {
            this.someValue = someValue;
        }
    }

    public static class G1 extends F1 implements Serializable
    {
        int someSubValue;

        public int getSomeSubValue()
        {
            return someSubValue;
        }

        public void setSomeSubValue(int someSubValue)
        {
            this.someSubValue = someSubValue;
        }
    }
}

