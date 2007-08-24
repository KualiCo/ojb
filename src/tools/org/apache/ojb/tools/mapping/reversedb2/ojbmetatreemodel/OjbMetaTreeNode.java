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

import org.apache.ojb.tools.mapping.reversedb2.propertyEditors.EditableTreeNodeWithProperties;
import javax.swing.tree.TreeNode;



/**
 * Abstract implementation of a treenode representing a metadata object
 * in a repository. 
 *
 * @author  <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: OjbMetaTreeNode.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 *
 */
public abstract class OjbMetaTreeNode extends EditableTreeNodeWithProperties
    implements Comparable, 
               org.apache.ojb.tools.mapping.reversedb2.ActionTarget
{
    private OjbMetaTreeNode parent;
    private org.apache.ojb.broker.metadata.DescriptorRepository repository;
    private OjbMetaDataTreeModel treeModel;
    protected java.util.ArrayList alChildren = new java.util.ArrayList();
        
    public OjbMetaTreeNode(org.apache.ojb.broker.metadata.DescriptorRepository pRepository, OjbMetaDataTreeModel pTreeModel, OjbMetaTreeNode pparent)
    {
        this.parent = pparent;
        this.repository = pRepository;
        this.treeModel = pTreeModel;
    }
    
    public org.apache.ojb.broker.metadata.DescriptorRepository getRepository()
    {
    	return this.repository;
    }
        
    /**
     * @see TreeNode#getChildAt(int)
     */
    public TreeNode getChildAt(int index)
    {
        return (TreeNode)this.alChildren.get(index);
    }
    
    /**
     * @see TreeNode#getChildCount()
     */
    public int getChildCount()
    {
        return this.alChildren.size();
    }
    
    /**
     * @see TreeNode#getParent()
     */
    public TreeNode getParent()
    {
        return this.parent;
    }
    
    /**
     * @see TreeNode#getIndex(TreeNode)
     */
    public int getIndex(TreeNode o)
    {
        return this.alChildren.indexOf(o);
    }
    
    /**
     * @see TreeNode#getAllowsChildren()
     */
    public abstract boolean getAllowsChildren();
    
    /**
     * @see TreeNode#isLeaf()
     */
    public abstract boolean isLeaf();
    
    /**
     * @see TreeNode#children()
     */
    public java.util.Enumeration children ()
    {
        return java.util.Collections.enumeration(this.alChildren);
    }
    
    /**
     * Access method for the TreeModel this node is associated to.
     */
    protected OjbMetaDataTreeModel getOjbMetaTreeModel()
    {
        return treeModel;
    }
    
    /**
     * Purpose of this method is to fill the children of the node. It should
     * replace all children in alChildren (the arraylist containing the children)
     * of this node and notify the TreeModel that a change has occurred.
     */
    protected abstract boolean _load();    
    
    /**
     * Recursively loads the metadata for this node
     */
    public boolean load()
    {
    	_load();
    	java.util.Iterator it = this.alChildren.iterator();
    	while (it.hasNext())
    	{
    		Object o = it.next();
    		if (o instanceof OjbMetaTreeNode) ((OjbMetaTreeNode)o).load();
    	}
    	return true;
    }
    
    /**
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(Object arg0)
    {
        return this.toString().compareTo(arg0.toString());
    }    
    
    /**
     * Return the descriptor object this node is associated with. E.g. if the 
     * node displays a class descriptor, the ClassDescriptor describing the class
     * should be returned. Used for creating a Transferable.
     */
    public abstract Object getAssociatedDescriptor();
    
}
