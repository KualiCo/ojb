package org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.commons.collections.iterators.ArrayIterator;

public class OjbMetaCollectionDescriptorNode extends OjbMetaTreeNode
{
    private static java.util.ArrayList supportedActions = new java.util.ArrayList();
    

    private CollectionDescriptor collectionDescriptor;
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
    public OjbMetaCollectionDescriptorNode(
        DescriptorRepository pRepository,
        OjbMetaDataTreeModel pTreeModel,
        OjbMetaTreeNode pparent,
		CollectionDescriptor pCollectionDescriptor)
    {
        super(pRepository, pTreeModel, pparent);
        this.collectionDescriptor = pCollectionDescriptor;
    }

    /**
     * @see OjbMetaTreeNode#_load()
     */
    protected boolean _load()
    {
    	java.util.ArrayList newChildren = new java.util.ArrayList();
    	ClassDescriptor itemClass = this.getRepository().getDescriptorFor(this.collectionDescriptor.getItemClassName());
    	newChildren.add(getOjbMetaTreeModel().getClassDescriptorNodeForClassDescriptor(itemClass));
    	
		// Foreign Key fields retrieved here map to FieldDescriptors of the table in the collection
		System.err.println(toString());
    	java.util.Iterator it;
    	try
    	{
    		it = new ArrayIterator(collectionDescriptor.getFksToThisClass());
    		while (it.hasNext())
    			newChildren.add(new javax.swing.tree.DefaultMutableTreeNode("FksToThisClass: " + it.next().toString()));
    		it = new ArrayIterator(collectionDescriptor.getFksToItemClass());
    		while (it.hasNext())
    			newChildren.add(new javax.swing.tree.DefaultMutableTreeNode("FksToItemClass: " + it.next().toString()));
    		
    	}
    	catch (NullPointerException npe)
    	{
    	}
		try
		{
    	
	    	it = collectionDescriptor.getForeignKeyFields().iterator();
	    	while (it.hasNext()) 
	    		newChildren.add(new javax.swing.tree.DefaultMutableTreeNode("FkFields: " + it.next().toString()));
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}    		
    	this.alChildren = newChildren;
    	this.getOjbMetaTreeModel().nodeStructureChanged(this);
        return true;
    }
    
    /**
     * Override load() of superClass to prevent recursive loading which would lead to an endless recursion
     * because of OjbClassDescriptorNodes being children of this node
     */
    public boolean load()
    {
    	return _load();
    }

    /**
     * @see OjbMetaTreeNode#isLeaf()
     */
    public boolean isLeaf()
    {
        return false;
    }

    /**
     * @see OjbMetaTreeNode#getAllowsChildren()
     */
    public boolean getAllowsChildren()
    {
        return false;
    }

    /**
     * @see OjbMetaTreeNode#setAttribute(String, Object)
     */
    public void setAttribute(String strKey, Object value)
    {
    }

    /**
     * @see OjbMetaTreeNode#getAttribute(String)
     */
    public Object getAttribute(String strKey)
    {
        return null;
    }

    /**
     * @see org.apache.ojb.tools.mapping.reversedb2.propertyEditors.PropertyEditorTarget#getPropertyEditorClass()
     */
    public Class getPropertyEditorClass()
    {
        return null;
    }
    
    public String toString()
    {
//    	System.out.println(collectionDescriptor.toXML());
    	if (collectionDescriptor.getItemClassName() == null)
    		return "CollectionDescriptor.getItemClass() == null";
		else return "CollectionDescriptor: " + collectionDescriptor.getItemClassName();
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
    
    /**
     * Return the descriptor object this node is associated with. E.g. if the 
     * node displays a class descriptor, the ClassDescriptor describing the class
     * should be returned. Used for creating a Transferable.
     */    
    public Object getAssociatedDescriptor()
    {
        return collectionDescriptor;
    }
    
}

