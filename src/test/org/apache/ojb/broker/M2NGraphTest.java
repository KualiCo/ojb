package org.apache.ojb.broker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.junit.PBTestCase;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Realize object tree via m:n relation.
 *
 * IMPORTANT NOTE: The global runtime metadata changes made by this test case
 * are NOT recommended in multithreaded environments, because they are global
 * and each thread will be affected.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: M2NGraphTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class M2NGraphTest extends PBTestCase
{
    public static void main(String[] args)
    {
        String[] arr = {M2NGraphTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void tearDown() throws Exception
    {
        changeRelationMetadata("children", true, CollectionDescriptor.CASCADE_NONE, CollectionDescriptor.CASCADE_NONE, false);
        changeRelationMetadata("parents", true, CollectionDescriptor.CASCADE_NONE, CollectionDescriptor.CASCADE_NONE, false);
        super.tearDown();
    }

    /**
     * All stuff is done by OJB
     */
    public void testAddNewChild_1() throws Exception
    {
        changeRelationMetadata("children", true, CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_OBJECT, false);
        changeRelationMetadata("parents", true, CollectionDescriptor.CASCADE_OBJECT, CollectionDescriptor.CASCADE_OBJECT, false);


        String postfix = "_testAddNewChild_1_" + System.currentTimeMillis();

        Node nodeA = new Node("nodeA" + postfix);
        Node nodeB = new Node("nodeB" + postfix);
        Node nodeB2 = new Node("nodeB2" + postfix);
        Node nodeC = new Node("nodeC" + postfix);

        //===============================================
        broker.beginTransaction();
        nodeA.addChildNode(nodeB);
        nodeA.addChildNode(nodeB2);
        nodeB.addChildNode(nodeC);
        broker.store(nodeA);
        broker.commitTransaction();
        //===============================================

        Identity oidA = new Identity(nodeA, broker);
        Identity oidB = new Identity(nodeB, broker);
        Identity oidC = new Identity(nodeC, broker);

        broker.clearCache();

        // get the stored stuff
        Node retrievednodeB = (Node) broker.getObjectByIdentity(oidB);
        assertNotNull("Verifying the nodeB was retrieved", retrievednodeB);
        assertEquals("verify the retrieved nodeB has 1 parent (the nodeA)",
                1, retrievednodeB.getParents().size());
        Node retrievednodeA = (Node) retrievednodeB.getParents().get(0);
        assertEquals("verify the nodeA was stored", nodeA.getName(),
                retrievednodeA.getName());
        assertNotNull(retrievednodeA.getChildren());
        assertEquals("verify the retrieved nodeA has 2 childs (the nodeB)", 2,
                retrievednodeA.getChildren().size());
        for (Iterator i = retrievednodeA.getChildren().iterator(); i.hasNext();)
        {
            Node b = (Node) i.next();
            //this next test fails because the child is null
            assertNotNull("Verifying nodeA's children are not null", b);
            assertTrue("verify, child is the nodeBx",
                    nodeB.getName().equals(b.getName()) || nodeB2.getName().equals(b.getName()));
        }
        assertNotNull(retrievednodeB.getChildren());
        assertEquals(1, retrievednodeB.getChildren().size());
        assertNotNull(nodeC.getName(), ((Node) retrievednodeB.getChildren().get(0)).getName());

        // get the stored stuff vice versa
        retrievednodeA = (Node) broker.getObjectByIdentity(oidA);
        assertNotNull(retrievednodeA);
        assertNotNull(retrievednodeA.getChildren());
        assertEquals(2, retrievednodeA.getChildren().size());
        retrievednodeB = (Node) retrievednodeA.getChildren().get(0);
        assertNotNull(retrievednodeB);
        assertNotNull(retrievednodeB.getParents());
        assertEquals(1, retrievednodeB.getParents().size());

        broker.clearCache();
        retrievednodeB = (Node) broker.getObjectByIdentity(oidB);
        //===============================================
        broker.beginTransaction();
        broker.delete(nodeB);
        broker.commitTransaction();
        //===============================================

        retrievednodeA = (Node) broker.getObjectByIdentity(oidA);
        retrievednodeB = (Node) broker.getObjectByIdentity(oidB);
        Node retrievednodeC = (Node) broker.getObjectByIdentity(oidC);

        // complete hierachy will be deleted
        assertNull(retrievednodeA);
        assertNull(retrievednodeB);
        assertNull(retrievednodeC);
    }

    /**
     * Autoretrieve is true, update/delete is done by hand
     */
    public void testAddNewChild_2() throws Exception
    {
        changeRelationMetadata("children", true, CollectionDescriptor.CASCADE_NONE, CollectionDescriptor.CASCADE_NONE, false);
        changeRelationMetadata("parents", true, CollectionDescriptor.CASCADE_NONE, CollectionDescriptor.CASCADE_NONE, false);


        String postfix = "_testAddNewChild_2_" + System.currentTimeMillis();

        Node nodeA = new Node("nodeA" + postfix);
        Node nodeB = new Node("nodeB" + postfix);
        Node nodeB2 = new Node("nodeB2" + postfix);
        Node nodeC = new Node("nodeC" + postfix);

        //===============================================
        broker.beginTransaction();
        nodeA.addChildNode(nodeB);
        nodeA.addChildNode(nodeB2);
        nodeB.addChildNode(nodeC);

        //store nodeA first
        broker.store(nodeA);
        //then store the nodeB
        broker.store(nodeB);
        broker.store(nodeB2);
        broker.store(nodeC);
        //make A the parent of B because they are not linked in the code automatically
        broker.serviceBrokerHelper().link(nodeA, true);
        // broker.serviceBrokerHelper().link(nodeB, true);
        // this will not work, because nodeB already linked by nodeA
        // so better specify the field to be link
        broker.serviceBrokerHelper().link(nodeB, "children", true);
        broker.commitTransaction();
        //===============================================

        Identity oidA = new Identity(nodeA, broker);
        Identity oidB = new Identity(nodeB, broker);
        Identity oidC = new Identity(nodeC, broker);

        broker.clearCache();

        // get the stored stuff
        Node retrievednodeB = (Node) broker.getObjectByIdentity(oidB);

        assertNotNull("Verifying the nodeB was retrieved", retrievednodeB);
        assertEquals("verify the retrieved nodeB has 1 parent (the nodeA)",
                1, retrievednodeB.getParents().size());

        Node retrievednodeA = (Node) retrievednodeB.getParents().get(0);

        assertEquals("verify the nodeA was stored", nodeA.getName(),
                retrievednodeA.getName());
        assertNotNull(retrievednodeA.getChildren());
        assertEquals("verify the retrieved nodeA has 2 childs (the nodeB)", 2,
                retrievednodeA.getChildren().size());
        for (Iterator i = retrievednodeA.getChildren().iterator(); i.hasNext();)
        {
            Node b = (Node) i.next();
            //this next test fails because the child is null
            assertNotNull("Verifying nodeA's children are not null", b);
            assertTrue("verify, child is the nodeBx",
                    nodeB.getName().equals(b.getName()) || nodeB2.getName().equals(b.getName()));
        }
        assertNotNull(retrievednodeB.getChildren());
        assertEquals(1, retrievednodeB.getChildren().size());
        assertNotNull(nodeC.getName(), ((Node) retrievednodeB.getChildren().get(0)).getName());

        // get the stored stuff vice versa
        retrievednodeA = (Node) broker.getObjectByIdentity(oidA);
        assertNotNull(retrievednodeA);
        assertNotNull(retrievednodeA.getChildren());
        assertEquals(2, retrievednodeA.getChildren().size());
        retrievednodeB = (Node) retrievednodeA.getChildren().get(0);
        assertNotNull(retrievednodeB);
        assertNotNull(retrievednodeB.getParents());
        assertEquals(1, retrievednodeB.getParents().size());

        broker.clearCache();
        retrievednodeB = (Node) broker.getObjectByIdentity(oidB);
        // cascade delete is not set and we only want to delete node B
        //===============================================
        broker.beginTransaction();
        broker.serviceBrokerHelper().unlink(nodeB);
        broker.delete(nodeB);
        broker.commitTransaction();
        //===============================================

        retrievednodeA = (Node) broker.getObjectByIdentity(oidA);
        retrievednodeB = (Node) broker.getObjectByIdentity(oidB);
        Node retrievednodeC = (Node) broker.getObjectByIdentity(oidC);

        // only nodeB should be deleted
        assertNotNull(retrievednodeA);
        assertNull(retrievednodeB);
        assertNotNull(retrievednodeC);
    }

    /**
     * Do all work manually
     */
    public void testAddNewChild_3() throws Exception
    {
        changeRelationMetadata("children", false, CollectionDescriptor.CASCADE_NONE, CollectionDescriptor.CASCADE_NONE, false);
        changeRelationMetadata("parents", false, CollectionDescriptor.CASCADE_NONE, CollectionDescriptor.CASCADE_NONE, false);

        String postfix = "_testAddNewChild_3_" + System.currentTimeMillis();

        Node nodeA = new Node("nodeA" + postfix);
        Node nodeB = new Node("nodeB" + postfix);
        Node nodeC = new Node("nodeC" + postfix);

        //===============================================
        broker.beginTransaction();
        nodeA.addChildNode(nodeB);
        nodeB.addChildNode(nodeC);
        //store nodeA first
        broker.store(nodeA);
        //then store the nodeB
        broker.store(nodeB);
        broker.store(nodeC);
        //make A the parent of B because they are not linked in the code automatically
        broker.serviceBrokerHelper().link(nodeA, "children", true);
        broker.serviceBrokerHelper().link(nodeB, "children", true);
        broker.commitTransaction();
        //===============================================

        Identity oidA = new Identity(nodeA, broker);
        Identity oidB = new Identity(nodeB, broker);

        broker.clearCache();

        // get the stored stuff
        Node retrievednodeB = (Node) broker.getObjectByIdentity(oidB);
        assertNotNull("Verifying the nodeB was retrieved", retrievednodeB);
        assertEquals(nodeB.getName(), retrievednodeB.getName());
        //===============================================
        broker.retrieveReference(retrievednodeB, "parents");
        assertEquals("verify the retrieved nodeB has 1 parent (the nodeA)",
                1, retrievednodeB.getParents().size());
        Node retrievednodeA = (Node) retrievednodeB.getParents().get(0);
        //===============================================
        broker.retrieveReference(retrievednodeA, "children");
        assertEquals("verify the nodeA was stored", nodeA.getName(),
                retrievednodeA.getName());
        assertNotNull(retrievednodeA.getChildren());
        assertEquals("verify the retrieved nodeA has 1 child (the nodeB)", 1,
                retrievednodeA.getChildren().size());
        for (Iterator i = retrievednodeA.getChildren().iterator(); i.hasNext();)
        {
            Node b = (Node) i.next();
            //this next test fails because the child is null
            assertNotNull("Verifying nodeA's children are not null", b);
            assertEquals("verify, using hashcode, that the nodeAs child is the nodeB",
                    retrievednodeB.getName(), b.getName());
        }
        //===============================================
        broker.retrieveReference(retrievednodeB, "children");
        assertNotNull(retrievednodeB.getChildren());
        assertEquals(1, retrievednodeB.getChildren().size());
        assertNotNull(nodeC.getName(), ((Node) retrievednodeB.getChildren().get(0)).getName());

        // get the stored stuff vice versa
        retrievednodeA = (Node) broker.getObjectByIdentity(oidA);
        broker.retrieveReference(retrievednodeA, "children");
        assertNotNull(retrievednodeA);
        assertNotNull(retrievednodeA.getChildren());
        assertEquals(1, retrievednodeA.getChildren().size());
        retrievednodeB = (Node) retrievednodeA.getChildren().get(0);
        assertNotNull(retrievednodeB);
        broker.retrieveReference(retrievednodeB, "parents");
        assertNotNull(retrievednodeB.getParents());
        assertEquals(1, retrievednodeB.getParents().size());
    }

    void changeRelationMetadata(String field, boolean autoRetrieve, int autoUpdate, int autoDelete, boolean proxy)
    {
        ClassDescriptor cld = broker.getClassDescriptor(Node.class);
        CollectionDescriptor cod = cld.getCollectionDescriptorByName(field);
        cod.setLazy(proxy);
        cod.setCascadeRetrieve(autoRetrieve);
        cod.setCascadingStore(autoUpdate);
        cod.setCascadingDelete(autoDelete);
    }

    //=========================================
    // test classes
    //=========================================
    public static class Node
    {
        private Integer id;
        private String name;
        private List children;
        private List parents;

        public Node()
        {
        }

        public Node(String name)
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

        public void addChildNode(Node child)
        {
            // Cannot add same child twice
            if (!getChildren().contains(child))
            {
                getChildren().add(child);
                if (!child.getParents().contains(this))
                {
                    child.getParents().add(this);
                }
            }
        }

//        public void addParentNode(Node parent)
//        {
//            if(! getParents().contains(parent))
//            {
//                getParents().add(parent);
//                if(!parent.getChildren().contains(this))
//                {
//                    parent.getChildren().add(this);
//                }
//            }
//        }

        public void removeChild(Node child)
        {
            getChildren().remove(child);
            child.getParents().remove(this);
        }

        public List getParents()
        {
            if (parents == null)
            {
                parents = new ArrayList();
            }
            return parents;
        }

        public void setParents(List parents)
        {
            this.parents = parents;
        }

        public List getChildren()
        {
            if (children == null)
            {
                children = new ArrayList();
            }
            return children;
        }

        public void setChildren(List children)
        {
            this.children = children;
        }

        public String toString()
        {
            return new ToStringBuilder(this)
                    .append("id", id)
                    .append("name", name)
                    .append("children", children != null ? children.size() : 0)
                    .append("parents", parents != null ? parents.size() : 0)
                    .toString();
        }

        public boolean equals(Object ref)
        {
            if (ref == null || !(ref instanceof Node))
            {
                return false;
            }
            else
            {
                return (getId() != null ? getId().equals(((Node) ref).getId()) : false);
            }
        }
    }
}
