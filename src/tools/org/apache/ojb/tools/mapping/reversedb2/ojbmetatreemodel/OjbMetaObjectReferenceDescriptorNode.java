package org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;

public class OjbMetaObjectReferenceDescriptorNode extends OjbMetaTreeNode
{

    private static java.util.ArrayList supportedActions = new java.util.ArrayList();
    
    private ObjectReferenceDescriptor objRefDescriptor;
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
    public OjbMetaObjectReferenceDescriptorNode(
        DescriptorRepository pRepository,
        OjbMetaDataTreeModel pTreeModel,
        OjbMetaTreeNode pparent,
		ObjectReferenceDescriptor pObjRefDescriptor)
    {
        super(pRepository, pTreeModel, pparent);
        this.objRefDescriptor = pObjRefDescriptor;
    }

    /**
     * @see OjbMetaTreeNode#_load()
     */
    protected boolean _load()
    {
    	java.util.ArrayList newChildren = new java.util.ArrayList();
    	ClassDescriptor itemClass = this.getRepository().getDescriptorFor(this.objRefDescriptor.getItemClassName());
    	newChildren.add(getOjbMetaTreeModel().getClassDescriptorNodeForClassDescriptor(itemClass));
    	this.alChildren = newChildren;
    	
    	java.util.Iterator it;
	    	
		// FK field indices are the the indices of the attributes in the class containing the reference descriptor.
		try
		{
    	
	    	it = objRefDescriptor.getForeignKeyFields().iterator();
	    	while (it.hasNext()) 
	    		newChildren.add(new javax.swing.tree.DefaultMutableTreeNode("FkFields: " + it.next().toString()));
		}
		catch (NullPointerException npe)
		{
			npe.printStackTrace();
		}      	
    	
    	
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
     * @see PropertyEditorTarget#getPropertyEditorClass()
     */
    public Class getPropertyEditorClass()
    {
        return null;
    }
    
    public String toString()
    {
    	if (objRefDescriptor.getItemClassName() != null)
	    	return "ObjectReferenceDescriptor: " + objRefDescriptor.getItemClassName();
	   	else
	   		return "ObjectReference: .getItemClass() == null";
    }
    
    /**
     * @see ActionTarget#getActions()
     */    
    public java.util.Iterator getActions() 
    {
        return supportedActions.iterator();
    }      
    
    /**
     * @see ActionTarget#actionListCacheable()
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
        return objRefDescriptor;
    }    

}

