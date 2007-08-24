package org.apache.ojb.broker;

import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.junit.PBTestCase;

import java.util.Vector;

/**
 * Testing selfjoins and tree structures
 */
public class TreeTest extends PBTestCase
{
	public static void main(String[] args)
	{
		String[] arr = {TreeTest.class.getName()};
		junit.textui.TestRunner.main(arr);
	}

	public TreeTest(String name)

	{
		super(name);
	}

	/**
	 * Insert the method's description here.
	 * Creation date: (13.02.2001 18:50:29)
	 * @return TestThreadsNLocks.org.apache.ojb.broker.Tree
	 */
	public TreeGroup createTreeGroup() throws Exception
	{
		TreeGroup result = new TreeGroup();

		FieldDescriptor idFld = broker.getClassDescriptor(Tree.class).getFieldDescriptorByName("id");
		Integer idVal = (Integer) broker.serviceSequenceManager().getUniqueValue(idFld);

        result.setId(idVal.intValue());
		result.setData("" + result.getId());
		result.setChilds(new Vector());
		result.setMembers(new Vector());
		return result;
	}

	/**
	 * Insert the method's description here.
	 * Creation date: (13.02.2001 18:50:29)
	 * @return TestThreadsNLocks.org.apache.ojb.broker.Tree
	 * @param parent TestThreadsNLocks.org.apache.ojb.broker.Tree
	 */
	public Tree createTreeNodeWithParent(Tree parent) throws Exception
	{

		Tree result = new Tree();
		try
		{
			FieldDescriptor idFld = broker.getClassDescriptor(Tree.class).getFieldDescriptorByName("id");
			Integer idVal = (Integer) broker.serviceSequenceManager().getUniqueValue(idFld);

			result.setId(idVal.intValue());
		}
		catch (PersistenceBrokerException e)

			{
		}
		result.setData(parent.getId() + "-" + result.getId());
		result.setParentId(parent.getId());
		result.setChilds(new Vector());

		return result;
	}

	/**
	 */
	public void testCreate()
	{
		try
		{

			Tree root = new Tree();
			try
			{
				FieldDescriptor idFld = broker.getClassDescriptor(Tree.class).getFieldDescriptorByName("id");
				Integer idVal = (Integer) broker.serviceSequenceManager().getUniqueValue(idFld);

				root.setId(idVal.intValue());
			}
			catch (PersistenceBrokerException e)

				{
			}
			root.setData("a brand new root: " + root.getId());

			root.addChild(createTreeNodeWithParent(root));
			root.addChild(createTreeNodeWithParent(root));
			root.addChild(createTreeNodeWithParent(root));
			root.addChild(createTreeNodeWithParent(root));
			root.addChild(createTreeNodeWithParent(root));

			Tree child = root.getChild(0);
			child.addChild(createTreeNodeWithParent(child));
			child.addChild(createTreeNodeWithParent(child));
			child.addChild(createTreeNodeWithParent(child));

			child = child.getChild(1);
			child.addChild(createTreeNodeWithParent(child));
			child.addChild(createTreeNodeWithParent(child));

			//System.out.println("original tree:");
			//System.out.println(root);
            broker.beginTransaction();
			broker.store(root);
            broker.commitTransaction();

			Identity oid = new Identity(root, broker);

			broker.clearCache();

			Tree retrieved = (Tree) broker.getObjectByIdentity(oid);

			//System.out.println("tree after reading from db:");
			//System.out.println(retrieved);

			assertEquals(
				"tree should have same size after retrival",
				root.size(),
				retrieved.size());

		}
		catch (Throwable t)
		{
			fail(t.getMessage());
		}
	}

	/**
     *
	 */
	public void testTreeGroup() throws Exception
	{
        TreeGroup root = createTreeGroup();
        root.setData("The Tree Group root: " + root.getId());
        TreeGroup green = createTreeGroup();
        green.setData("the GREEN group " + green.getId());
        TreeGroup red = createTreeGroup();
        red.setData("the RED group " + red.getId());

        TreeGroup child;
        for (int i = 0; i < 3; i++)
        {
            child = createTreeGroup();
            root.addChild(child);
            green.addMember(child);
            child = createTreeGroup();
            root.addChild(child);
            red.addMember(child);
        }

        for (int i = 0; i < 6; i++)
        {
            child = root.getChild(i);
            child.addChild(createTreeGroup());
        }

        //System.out.println("original TreeGroup:");
        //System.out.println(root);
        //System.out.println("GREEN TreeGroup:");
        //System.out.println(green);
        //System.out.println("RED TreeGroup:");
        //System.out.println(red);
        broker.beginTransaction();
        broker.store(root);
        broker.store(green);
        broker.store(red);
        broker.commitTransaction();

        Identity oid = new Identity(root, broker);

        broker.clearCache();

        TreeGroup root_r = (TreeGroup) broker.getObjectByIdentity(oid);
        //System.out.println("tree after reading from db:");
        //System.out.println(root_r);
        assertNotNull(root_r);
        assertEquals(
            "tree should have same size after retrival",
            root.size(),
            root_r.size());

        oid = new Identity(green, broker);
        TreeGroup green_r = (TreeGroup) broker.getObjectByIdentity(oid);
        //System.out.println("tree after reading from db:");
        //System.out.println(green_r);
        assertEquals(
            "tree should have same size after retrival",
            green.size(),
            green_r.size());

        oid = new Identity(red, broker);
        TreeGroup red_r = (TreeGroup) broker.getObjectByIdentity(oid);
        //System.out.println("tree after reading from db:");
        //System.out.println(red_r);
        assertEquals(
            "tree should have same size after retrival",
            red.size(),
            red_r.size());

	}



    
    //*****************************************************************
    // inner classes - test objects
    //*****************************************************************

    /**
     * Tree is  recursive type: a Tree element contains some data
     * and a Vector of child Tree elements.
     * This sample demonstrates what is needed to map such a data
     * structure on a DB table
     * @author Thomas Mahler
     */
    public static class Tree implements java.io.Serializable
    {
        private int id;
        private String data;
        private int parentId;
        private Vector childs;

        /**
         * Tree constructor comment.
         */
        public Tree()
        {
            super();
        }

        /**
         * Tree constructor comment.
         */
        public Tree(int id, String data, int parentid)
        {
            this.id = id;
            this.data = data;
            this.parentId = parentid;
        }

        public void addChild(Tree newChild)
        {
            if (childs == null) childs = new Vector();

            childs.add(newChild);

        }

        public Tree getChild(int index)
        {
            return (Tree) childs.get(index);

        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return java.util.Vector
         */
        public java.util.Vector getChilds()
        {
            return childs;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return java.lang.String
         */
        public java.lang.String getData()
        {
            return data;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return int
         */
        public int getId()
        {
            return id;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return int
         */
        public int getParentId()
        {
            return parentId;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @param newChilds java.util.Vector
         */
        public void setChilds(java.util.Vector newChilds)
        {
            childs = newChilds;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @param newData java.lang.String
         */
        public void setData(java.lang.String newData)
        {
            data = newData;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @param newId int
         */
        public void setId(int newId)
        {
            id = newId;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @param newParentId int
         */
        public void setParentId(int newParentId)
        {
            parentId = newParentId;
        }

        /**
         * Insert the method's description here.
         * Creation date: (14.02.2001 19:51:23)
         * @return int
         */
        public int size()
        {
            int result = 1;
            for (int i = 0; i < childs.size(); i++)
            {
                result += ((Tree) childs.get(i)).size();
            }
            return result;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:41)
         * @return java.lang.String
         */
        public String toString()
        {

            return data + ((childs == null) ? "" : childs.toString());
        }
    }

    /**
     * Tree is  recursive type: a Tree element contains some data
     * and a Vector of child Tree elements.
     * This sample demonstrates what is needed to map such a data
     * structure on a DB table
     * @author Thomas Mahler
     */
    public static class TreeGroup implements java.io.Serializable
    {
        private int id;
        private String data;

        private int parentId;
        private Vector children;
        private TreeGroup myParent;

        private int groupId;
        private Vector groupMembers;
        private TreeGroup myGroup;


        /**
         * Tree constructor comment.
         */
        public TreeGroup()
        {
            super();
        }

        /**
         * Tree constructor comment.
         */
        public TreeGroup(int id, String data, int parentid, int groupid)
        {
            this.id = id;
            this.data = data;
            this.parentId = parentid;
            this.groupId = groupid;
        }

        public void addChild(TreeGroup newChild)
        {
            if (children == null) children = new Vector();

            children.add(newChild);
            newChild.setParentId(this.getId());

        }

        public void addMember(TreeGroup newMember)
        {
            if (groupMembers == null) groupMembers = new Vector();

            groupMembers.add(newMember);
            newMember.setGroupId(this.getId());

        }

        public TreeGroup getChild(int index)
        {
            return (TreeGroup) children.get(index);

        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return java.util.Vector
         */
        public Vector getChilds()
        {
            return children;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return java.lang.String
         */
        public String getData()
        {
            return data;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return int
         */
        public int getGroupId()
        {
            return groupId;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return int
         */
        public int getId()
        {
            return id;
        }

        public TreeGroup getMember(int index)
        {
            return (TreeGroup) groupMembers.get(index);

        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return java.util.Vector
         */
        public Vector getMembers()
        {
            return groupMembers;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @return int
         */
        public int getParentId()
        {
            return parentId;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @param newChilds java.util.Vector
         */
        public void setChilds(java.util.Vector newChilds)
        {
            children = newChilds;
        }

        /**
         * Sets the data.
         * @param data The data to set
         */
        public void setData(String data)
        {
            this.data = data;
        }

        /**
         * Sets the groupId.
         * @param groupId The groupId to set
         */
        public void setGroupId(int groupId)
        {
            this.groupId = groupId;
        }

        /**
         * Sets the id.
         * @param id The id to set
         */
        public void setId(int id)
        {
            this.id = id;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:02)
         * @param newMembers java.util.Vector
         */
        public void setMembers(java.util.Vector newMembers)
        {
            groupMembers = newMembers;
        }

        /**
         * Sets the parentId.
         * @param parentId The parentId to set
         */
        public void setParentId(int parentId)
        {
            this.parentId = parentId;
        }

        /**
         * Insert the method's description here.
         * Creation date: (14.02.2001 19:51:23)
         * @return int
         */
        public int size()
        {
            int result = 1;
            if(children == null) return result;
            for (int i = 0; i < children.size(); i++)
            {
                result += ((TreeGroup) children.get(i)).size();
            }
            return result;
        }

        /**
         * Insert the method's description here.
         * Creation date: (13.02.2001 18:33:41)
         * @return java.lang.String
         */
        public String toString()
        {
            return data
                    + ((children == null || (children.size() == 0)) ? "" : " children: " + children.toString())
                    + ((groupMembers == null || (groupMembers.size() == 0)) ? "" : " members: " + groupMembers.toString());
        }
        /**
         * Gets the children.
         * @return Returns a Vector
         */
        public Vector getChildren()
        {
            return children;
        }

        /**
         * Sets the children.
         * @param children The children to set
         */
        public void setChildren(Vector children)
        {
            this.children = children;
        }

        /**
         * Gets the groupMembers.
         * @return Returns a Vector
         */
        public Vector getGroupMembers()
        {
            return groupMembers;
        }

        /**
         * Sets the groupMembers.
         * @param groupMembers The groupMembers to set
         */
        public void setGroupMembers(Vector groupMembers)
        {
            this.groupMembers = groupMembers;
        }

        /**
         * Gets the myGroup.
         * @return Returns a TreeGroup
         */
        public TreeGroup getMyGroup()
        {
            return myGroup;
        }

        /**
         * Sets the myGroup.
         * @param myGroup The myGroup to set
         */
        public void setMyGroup(TreeGroup myGroup)
        {
            this.myGroup = myGroup;
        }

        /**
         * Gets the myParent.
         * @return Returns a TreeGroup
         */
        public TreeGroup getMyParent()
        {
            return myParent;
        }

        /**
         * Sets the myParent.
         * @param myParent The myParent to set
         */
        public void setMyParent(TreeGroup myParent)
        {
            this.myParent = myParent;
        }

    }


}
