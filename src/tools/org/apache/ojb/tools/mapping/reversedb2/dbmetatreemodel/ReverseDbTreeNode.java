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

package org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel;

import javax.swing.tree.TreeNode;
import org.apache.ojb.tools.mapping.reversedb2.propertyEditors.EditableTreeNodeWithProperties;


/**
 * Abstract implementation of a treenode representing an metadata object
 * in a database. It implements loading the children of the node in a
 * separate thread, thus not blocking the user interface. 
 * @author  Administrator
 */
public abstract class ReverseDbTreeNode extends EditableTreeNodeWithProperties
    implements java.io.Serializable
{
    transient private java.sql.DatabaseMetaData dbMeta;
    private DatabaseMetaDataTreeModel dbMetaTreeModel;
    
    /**
     * List of children of this treenode.
     */ 
    protected java.util.ArrayList alChildren = new java.util.ArrayList();
    
    private boolean isFilled = false;
    
    transient private Object  populationLock = new Object();
    private boolean populationInProgress = false;
    
    private ReverseDbTreeNode parent = null;    
    
    public ReverseDbTreeNode(
        java.sql.DatabaseMetaData pdbMeta,
        DatabaseMetaDataTreeModel pdbMetaTreeModel,
        ReverseDbTreeNode pparent)
    {
        this.dbMeta = pdbMeta;
        this.dbMetaTreeModel = pdbMetaTreeModel;
        this.parent = pparent;
        alChildren.add(new javax.swing.tree.DefaultMutableTreeNode("..."));
    }
        
    /**
     * @see TreeNode#getChildAt(int)
     */
    public TreeNode getChildAt(int index)
    {
        if (!this.isFilled) this.load(false, false, true);
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
        if (!this.isFilled) this.load(false, false, true);
        return java.util.Collections.enumeration(this.alChildren);
    }
    
    /**
     * Loads the children of this TreeNode. If another Thread is already active on this node the method returns
     * without doing anything (if a separate Thread is started the method returns anyway, but the Thread might
     * do nothing).
     * @param recursive If true, all children down to the leaf node are retrieved
     * @param replace If true the children are loaded unconditionally. If false the
     * retrieval is only done if the node has not been populated before.
     * @param inNewThread if true the load is done in a new thread.
     */
    public void load(final boolean recursive, final boolean replace, final boolean inNewThread)
    {
        if (inNewThread)
        {
            new Thread()
            {
                public void run()
                {
                    load(recursive, replace, false);
                }
            }.start();
            return;
        }
        if (!populationInProgress)
        {
            synchronized (this.populationLock)
            {
                this.populationInProgress = true;
                if (replace || !this.isFilled)
                {
                    this.isFilled = _load();
                }
                this.populationInProgress = false;
            }
            if (!recursive)
                this.getDbMetaTreeModel ().setStatusBarMessage ("Done");
        }
        if (recursive)
        {
            java.util.Enumeration e = this.children();
            while (e.hasMoreElements())
            {
                Object o = e.nextElement();
                if (o instanceof ReverseDbTreeNode)
                    ((ReverseDbTreeNode) o).load(recursive, replace, false);
            }
            this.getDbMetaTreeModel ().setStatusBarMessage ("Done");
        }
    }
    
    /**
     * Loads the children of this TreeNode. If the node is already populated, this method returns without action. If
     * there is already a Thread populating this treenode the method waits until the other Thread has finished
     * @param recursive if true, all children down to the leaf node are retrieved.
     * @param replace if false and the list of children is already populated, return without action. If recursive
     * is true, all children down to the leaf of the tree are checked.
     */
    public void loadWait(
        final boolean recursive,
        final boolean replace,
        final boolean inNewThread)
    {
        if (inNewThread)
        {
            new Thread()
            {
                public void run()
                {
                    loadWait(recursive, replace, false);
                }
            }.start();
            return;
        }
        
        synchronized (this.populationLock)
        {
            this.populationInProgress = true;
            if (replace || !this.isFilled)
            {
                this.isFilled = _load();
            }
            if (recursive)
            {
                java.util.Enumeration e = this.children();
                while (e.hasMoreElements())
                {
                    Object o = e.nextElement();
                    if (o instanceof ReverseDbTreeNode)
                        ((ReverseDbTreeNode)o).loadWait(recursive, replace, false);
                }
            }
            this.populationInProgress = false;
        }
        this.getDbMetaTreeModel ().setStatusBarMessage("Done");        
    }
    
    /**
     * Access method for the DatabaseMetaData object of this tree model
     */
    protected java.sql.DatabaseMetaData getDbMeta()
    {
        return dbMeta;
    }
    
    /**
     * Access method for the TreeModel this node is associated to.
     */
    protected DatabaseMetaDataTreeModel getDbMetaTreeModel()
    {
        return dbMetaTreeModel;
    }
    
    /**
     * Purpose of this method is to fill the children of the node. It should
     * replace all children in alChildren (the arraylist containing the children)
     * of this node and notify the TreeModel that a change has occurred.
     */
    protected abstract boolean _load();
    
}
