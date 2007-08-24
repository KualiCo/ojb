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
 * This node represents a catalog of the database. Its children are DBMetaSchemaNode
 * objects.
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBMetaCatalogNode.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class DBMetaCatalogNode extends ReverseDbTreeNode
    implements java.io.Serializable
{
	static final long serialVersionUID = -2455228985120104948L;    /** Attribute key for the accessing the catalog name */
    public static final String ATT_CATALOG_NAME = "Catalog Name";

  
    /** Creates a new instance of DBMetaCatalogNode.
     * @param pdbMeta DatabaseMetaData implementation where this node gets its data from.
     * @param pdbMetaTreeModel The TreeModel this node is associated to.
     * @param prootNode The parent node for this node.
     * @param pstrCatalogName The name of the catalog this node is representing. Some databases do not supports
     * catalogs, therefore null values are allowed for this parameter
     */
    public DBMetaCatalogNode(java.sql.DatabaseMetaData pdbMeta, 
                             DatabaseMetaDataTreeModel pdbMetaTreeModel, 
                             DBMetaRootNode prootNode, 
                             String pstrCatalogName)
    {
    	super(pdbMeta, pdbMetaTreeModel, prootNode);
    	this.setAttribute(ATT_CATALOG_NAME, pstrCatalogName);
    }
    
    /**
     * @see ReverseDbTreeNode#isLeaf()
     */    
    public boolean isLeaf()
    {
        return false;
    }
    
    /**
     * @see ReverseDbTreeNode#getAllowsChildren()
     */
    public boolean getAllowsChildren()
    {
        return false;
    }
    
    /**
     * Convenience access method for the catalog name. Accesses the
     * attributes HashMap to retrieve the value.
     */
    public String getCatalogName()
    {
        return (String)this.getAttribute(ATT_CATALOG_NAME);
    }
    
    /**
     * If the catalog name is specified, returns the catalog name, 
     * otherwise a constant string indicating that the catalog name
     * is emtpy (which is legal for some databases, e.g. Oracle)
     * @see Object#toString()
     */    
    public String toString()
    {
        if (this.getAttribute(ATT_CATALOG_NAME) != null)
            return this.getAttribute(ATT_CATALOG_NAME).toString();
        else return "catalog not specified";
    }
    
    public Class getPropertyEditorClass()
    {
        return org.apache.ojb.tools.mapping.reversedb2.propertyEditors.JPnlPropertyEditorDBMetaCatalog.class;
    }
    
    /**
     * Loads the schemas associated to this catalog.
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
            
                getDbMetaTreeModel().setStatusBarMessage("Reading schemas for catalog " 
                    + this.getAttribute(ATT_CATALOG_NAME));
                rs = getDbMeta().getSchemas();
                final java.util.ArrayList alNew = new java.util.ArrayList();
                int count = 0;
                while (rs.next())
                {
                    getDbMetaTreeModel().setStatusBarMessage("Creating schema " + getCatalogName() + "." + rs.getString("TABLE_SCHEM"));
                    alNew.add(new DBMetaSchemaNode(getDbMeta(),
                                                   getDbMetaTreeModel(),
                                                   DBMetaCatalogNode.this, 
                                                   rs.getString("TABLE_SCHEM")));
                    count++;
                }
                if (count == 0) 
                    alNew.add(new DBMetaSchemaNode(getDbMeta(), 
                                                   getDbMetaTreeModel(),
                                                   DBMetaCatalogNode.this, null));
                alChildren = alNew;            
                javax.swing.SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        getDbMetaTreeModel().nodeStructureChanged(DBMetaCatalogNode.this);
                    }
                });
                rs.close();
            }
        }
        catch (java.sql.SQLException sqlEx)
        {
            getDbMetaTreeModel().reportSqlError("Error retrieving schemas", sqlEx);
            try
            {
                if (rs != null) rs.close ();
            }
            catch (java.sql.SQLException sqlEx2)
            {
                this.getDbMetaTreeModel().reportSqlError("Error retrieving schemas", sqlEx2);
            }                        
            return false;
        }
        return true;
    }

}
