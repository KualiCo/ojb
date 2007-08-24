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
 * This node represents a schema of the database. Its children are DBMetaTableNode
 * objects. Not all databases support schemas (e.g. MySQL), so the schema name
 * may be null.
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBMetaSchemaNode.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class DBMetaSchemaNode extends ReverseDbTreeNode
    implements java.io.Serializable
{
	static final long serialVersionUID = 2430983502951445144L;    /** Key for accessing the schema name in the attributes Map */
    public static final String ATT_SCHEMA_NAME = "Schema Name";
    /** Creates a new instance of DBMetaSchemaNode 
     * @param pdbMeta DatabaseMetaData implementation where this node gets its data from.
     * @param pdbMetaTreeModel The TreeModel this node is associated to.
     * @param pcatalogNode The parent node for this node.
     * @param pstrSchemaName The name of the schema this node is representing. Some databases do not support
     * schemas, therefore null values are allowed for this parameter     
     */
    public DBMetaSchemaNode(java.sql.DatabaseMetaData pdbMeta, 
                             DatabaseMetaDataTreeModel pdbMetaTreeModel, 
                             DBMetaCatalogNode pcatalogNode, 
                             String pstrSchemaName)
    {
    	super(pdbMeta, pdbMetaTreeModel, pcatalogNode);
    	this.setAttribute(ATT_SCHEMA_NAME, pstrSchemaName);
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
     * Convenience access method for the schema name. Accesses the
     * attributes HashMap to retrieve the value.
     */    
    public String getSchemaName()
    {
        return (String)this.getAttribute(ATT_SCHEMA_NAME);
    }
    
    /**
     * If the schema name is specified, returns the schema name, 
     * otherwise a constant string indicating that the schema name
     * is emtpy (which is legal for some databases, e.g. MySQL)
     * @see Object#toString()
     */      
    public String toString()
    {
        if (this.getAttribute(ATT_SCHEMA_NAME) == null) return "Schema not specified";
        else return this.getAttribute(ATT_SCHEMA_NAME).toString();
    }
    
    /**
     * Convenience access method to the catalog this schema is associated to.
     */
    public DBMetaCatalogNode getCatalog()
    {
    	return (DBMetaCatalogNode ) getParent();
    }

    public Class getPropertyEditorClass()
    {
        return org.apache.ojb.tools.mapping.reversedb2.propertyEditors.JPnlPropertyEditorDBMetaSchema.class;
    }    
    
    /**
     * Fills the children list with the tables this schema contains.
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
                getDbMetaTreeModel().setStatusBarMessage("Reading tables for schema " + getCatalog().getCatalogName() + "." + getSchemaName());
                rs = getDbMeta().getTables(getCatalog().getCatalogName(), 
                                                         getSchemaName(),
                                                         "%", null);
                final java.util.ArrayList alNew = new java.util.ArrayList();
                while (rs.next())
                {
                    getDbMetaTreeModel().setStatusBarMessage("Creating table " + getCatalog().getCatalogName() + "." + getSchemaName() + "." + rs.getString("TABLE_NAME"));                    
                    alNew.add(new DBMetaTableNode(getDbMeta(),
                                                  getDbMetaTreeModel(),
                                                  DBMetaSchemaNode.this, 
                                                  rs.getString("TABLE_NAME")));
                }
                alChildren = alNew;            
                javax.swing.SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        getDbMetaTreeModel().nodeStructureChanged(DBMetaSchemaNode.this);
                    }
                });
                rs.close();
            }
        }
        catch (java.sql.SQLException sqlEx)
        {
            getDbMetaTreeModel().reportSqlError("Error retrieving tables", sqlEx);
            try
            {
                if (rs != null) rs.close ();
            }
            catch (java.sql.SQLException sqlEx2)
            {
                this.getDbMetaTreeModel().reportSqlError("Error retrieving tables", sqlEx2);
            }                        
            return false;
        }
        return true;
    }
}
