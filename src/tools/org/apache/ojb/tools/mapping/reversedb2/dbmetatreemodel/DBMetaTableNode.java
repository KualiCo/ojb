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
 * This class represents a table within a database.
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBMetaTableNode.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class DBMetaTableNode extends ReverseDbTreeNode
    implements java.io.Serializable
{
	static final long serialVersionUID = 7091783312165332145L; 
    /** Key for accessing the table name in the attributes Map */    
    public static final String ATT_TABLE_NAME = "Table Name";
    
    /** Creates a new instance of DBMetaSchemaNode 
     * @param pdbMeta DatabaseMetaData implementation where this node gets its data from.
     * @param pdbMetaTreeModel The TreeModel this node is associated to.
     * @param pschemaNode The parent node for this node.
     * @param pstrTableName The name of the table this node is representing. 
     */
    public DBMetaTableNode(java.sql.DatabaseMetaData pdbMeta, 
                           DatabaseMetaDataTreeModel pdbMetaTreeModel, 
                           DBMetaSchemaNode pschemaNode, 
                           String pstrTableName)
    {
    	super(pdbMeta, pdbMetaTreeModel, pschemaNode);
    	this.setAttribute(ATT_TABLE_NAME, pstrTableName);
    }
    
    /**
     * @see ReverseDbTreeNode#isLeaf()
     */       
    public boolean getAllowsChildren()
    {
        return true;
    }
        
    /**
     * @see ReverseDbTreeNode#getAllowsChildren()
     */    
    public boolean isLeaf()
    {
        return false;
    }
    
    /**
     * Convenience access method for the table name. Accesses the
     * attributes HashMap to retrieve the value.
     */       
    public String getTableName()
    {
        return (String)this.getAttribute(ATT_TABLE_NAME);
    }
    
    /**
     * @see Object#toString()
     */     
    public String toString()
    {
        return (String)getAttribute(ATT_TABLE_NAME);
    }
    
    /**
     * Convenience access method to the schema this table is associated to.
     */    
    public DBMetaSchemaNode getSchema()
    {
    	return (DBMetaSchemaNode)this.getParent();
    }

    public Class getPropertyEditorClass()
    {
        return org.apache.ojb.tools.mapping.reversedb2.propertyEditors.JPnlPropertyEditorDBMetaTable.class;
    }    
    
    
    /**
     * Loads the columns for this table into the alChildren list.
     */
    protected boolean _load ()
    {
        java.sql.ResultSet rs = null;
        try
        {
            // This synchronization is necessary for Oracle JDBC drivers 8.1.7, 9.0.1, 9.2.0.1
            // The documentation says synchronization is done within the driver, but they
            // must have overlooked something. Without the lock we'd get mysterious error
            // messages.            
            synchronized(getDbMeta())
            {
                getDbMetaTreeModel().setStatusBarMessage("Reading columns for table " + getSchema().getCatalog().getCatalogName() + "." + getSchema().getSchemaName() + "." + getTableName());
                rs = getDbMeta().getColumns(getSchema().getCatalog().getCatalogName(), 
                                                          getSchema().getSchemaName(),
                                                          getTableName(), "%");
                final java.util.ArrayList alNew = new java.util.ArrayList();
                while (rs.next())
                {
                    alNew.add(new DBMetaColumnNode(getDbMeta(), getDbMetaTreeModel(), DBMetaTableNode.this, rs.getString("COLUMN_NAME")));
                }
                alChildren = alNew;            
                javax.swing.SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        getDbMetaTreeModel().nodeStructureChanged(DBMetaTableNode.this);
                    }
                });
                rs.close();
            }
        }
        catch (java.sql.SQLException sqlEx)
        {
            this.getDbMetaTreeModel().reportSqlError("Error retrieving columns", sqlEx);
            try
            {
                if (rs != null) rs.close ();
            }
            catch (java.sql.SQLException sqlEx2)
            {
                this.getDbMetaTreeModel().reportSqlError("Error retrieving columns", sqlEx2);
            }
            return false;
        }
        return true;
    }

}
