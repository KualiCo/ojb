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

package org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.IndexDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;


/**
 *
 * @author  Administrator
 */
public class OjbMetaClassDescriptorNode extends OjbMetaTreeNode implements javax.swing.tree.MutableTreeNode
{

    private static java.util.ArrayList supportedActions = new java.util.ArrayList();


    private ClassDescriptor cld;
    /** Creates a new instance of OjbMetaClassDescriptorNode */
    public OjbMetaClassDescriptorNode (DescriptorRepository pRepository,
                                       OjbMetaDataTreeModel pTreeModel,
                                       OjbMetaRootNode pparent,
                                       ClassDescriptor pCld)
    {
        super(pRepository, pTreeModel, pparent);
        this.cld = pCld;
    }

    public boolean getAllowsChildren ()
    {
        return true;
    }

    public Object getAttribute (String key)
    {
        return null;
    }

    public Class getPropertyEditorClass ()
    {
        return null;
    }

    public boolean isLeaf ()
    {
        return false;
    }

    public void setAttribute (String key, Object value)
    {

    }

    /** Purpose of this method is to fill the children of the node. It should
     * replace all children in alChildren (the arraylist containing the children)
     * of this node and notify the TreeModel that a change has occurred.
     */
    protected boolean _load ()
    {
    	java.util.ArrayList newChildren = new java.util.ArrayList();

    	/* @todo make this work */
//    	if (cld.getConnectionDescriptor() != null)
//    	{
//    		newChildren.add(new OjbMetaJdbcConnectionDescriptorNode(
//    			this.getOjbMetaTreeModel().getRepository(),
//    			this.getOjbMetaTreeModel(),
//    			this,
//    			cld.getConnectionDescriptor()));
//    	}
//
    	// Add collection descriptors
    	java.util.Iterator it = cld.getCollectionDescriptors().iterator();
    	while (it.hasNext())
    	{
    		CollectionDescriptor collDesc = (CollectionDescriptor)it.next();
    		newChildren.add(new OjbMetaCollectionDescriptorNode(
    			this.getOjbMetaTreeModel().getRepository(),
    			this.getOjbMetaTreeModel(),
    			this,
    			collDesc));

    	}

    	// Add extent classes Class

        it = cld.getExtentClassNames().iterator();
    	while (it.hasNext())
    	{
    		String extentClassName = (String)it.next();
    		newChildren.add(new OjbMetaExtentClassNode(
    			this.getOjbMetaTreeModel().getRepository(),
    			this.getOjbMetaTreeModel(),
    			this,
    			extentClassName));

    	}

        // Get Field descriptors FieldDescriptor
        if (cld.getFieldDescriptions() != null)
        {
            it = new ArrayIterator(cld.getFieldDescriptions());
            while (it.hasNext())
            {
                FieldDescriptor fieldDesc = (FieldDescriptor)it.next();
                newChildren.add(new OjbMetaFieldDescriptorNode(
                        this.getOjbMetaTreeModel().getRepository(),
                        this.getOjbMetaTreeModel(),
                        this,
                        fieldDesc));
            }
        }
        else
        {
                System.out.println(cld.getClassNameOfObject() + " does not have field descriptors");
        }

        // Get Indices IndexDescriptor
        it = cld.getIndexes().iterator();
    	while (it.hasNext())
    	{
    		IndexDescriptor indexDesc = (IndexDescriptor)it.next();
    		newChildren.add(new OjbMetaIndexDescriptorNode(
    			this.getOjbMetaTreeModel().getRepository(),
    			this.getOjbMetaTreeModel(),
    			this,
    			indexDesc));

    	}

    	// Get references ObjectReferenceDescriptor
    	it = cld.getObjectReferenceDescriptors().iterator();
    	while (it.hasNext())
    	{
    		ObjectReferenceDescriptor objRefDesc = (ObjectReferenceDescriptor)it.next();
    		newChildren.add(new OjbMetaObjectReferenceDescriptorNode(
    			this.getOjbMetaTreeModel().getRepository(),
    			this.getOjbMetaTreeModel(),
    			this,
    			objRefDesc));

    	}
    	// Add
    	this.alChildren = newChildren;
    	this.getOjbMetaTreeModel().nodeStructureChanged(this);
        return true;
    }

    public String toString()
    {
        return "ClassDescriptor:" + this.cld.getClassNameOfObject();
        // return "ClassDescriptor:" + this.cld.getClassOfObject().getName();
    }

    /**
     * @see org.apache.ojb.tools.mapping.reversedb2.ActionTarget#getActions()
     */
    public java.util.Iterator getActions()
    {
        return supportedActions.iterator();
    }

    /**
     * @see org.apache.ojb.tools.mapping.reversedb2.ActionTarget#actionListCachable()
     */
    public boolean actionListCachable()
    {
        return true;
    }

    public boolean actionListStatic()
    {
        return true;
    }

    /** Adds <code>child</code> to the receiver at <code>index</code>.
     * <code>child</code> will be messaged with <code>setParent</code>.
     *
     */
    public void insert(javax.swing.tree.MutableTreeNode child, int index)
    {
    }

    /** Removes <code>node</code> from the receiver. <code>setParent</code>
     * will be messaged on <code>node</code>.
     *
     */
    public void remove(javax.swing.tree.MutableTreeNode node)
    {
    }

    /** Removes the child at <code>index</code> from the receiver.
     *
     */
    public void remove(int index)
    {
    }

    /** Removes the receiver from its parent.
     *
     */
    public void removeFromParent()
    {
    }

    /** Sets the parent of the receiver to <code>newParent</code>.
     *
     */
    public void setParent(javax.swing.tree.MutableTreeNode newParent)
    {
    }

    /** Resets the user object of the receiver to <code>object</code>.
     *
     */
    public void setUserObject(Object object)
    {
    }

    /**
     * Return the descriptor object this node is associated with. E.g. if the
     * node displays a class descriptor, the ClassDescriptor describing the class
     * should be returned. Used for creating a Transferable.
     */
    public Object getAssociatedDescriptor()
    {
        return cld;
    }

}
