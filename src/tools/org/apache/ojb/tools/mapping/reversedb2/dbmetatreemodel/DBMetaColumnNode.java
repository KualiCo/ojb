package org.apache.ojb.tools.mapping.reversedb2.dbmetatreemodel;
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
 * This class represents a columns of a table
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBMetaColumnNode.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class DBMetaColumnNode extends ReverseDbTreeNode
    implements java.io.Serializable
{
	static final long serialVersionUID = -7694494988930854647L;
    /** Key for accessing the column name in the attributes Map */       
    public static final String ATT_COLUMN_NAME = "Column Name";   
	
    /** Creates a new instance of DBMetaSchemaNode
     * @param pdbMeta DatabaseMetaData implementation where this node gets its data from.
     * @param pdbMetaTreeModel The TreeModel this node is associated to.
     * @param pschemaNode The parent node for this node.
     * @param pstrColumnName The name of the column this node is representing. 
     */
    public DBMetaColumnNode(java.sql.DatabaseMetaData pdbMeta, 
                            DatabaseMetaDataTreeModel pdbMetaTreeModel, 
                            DBMetaTableNode ptableNode, String pstrColumnName)
    {
    	super(pdbMeta, pdbMetaTreeModel, ptableNode);
        this.setAttribute(ATT_COLUMN_NAME, pstrColumnName);
    }
    
    /**
     * @see ReverseDbTreeNode#isLeaf()
     */         
    public boolean getAllowsChildren()
    {
        return false;
    }
    
    /**
     * @see ReverseDbTreeNode#getAllowsChildren()
     */        
    public boolean isLeaf()
    {
        return true;
    }
    
    
    /**
     * @see Object#toString()
     */      
    public String toString()
    {
        return this.getAttribute(ATT_COLUMN_NAME).toString();
    }

    public Class getPropertyEditorClass()
    {
        return org.apache.ojb.tools.mapping.reversedb2.propertyEditors.JPnlPropertyEditorDBMetaColumn.class;
    }    
    
    
    /**
     * Do nothing as there are no children for a column.
     */
    protected boolean _load ()
    {
        return true;
    }    
}
