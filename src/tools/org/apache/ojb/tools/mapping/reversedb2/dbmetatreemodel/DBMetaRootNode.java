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
 * This is the root node for the DatabaseMetaTreeModel. Its children are
 * DBMetaCatalogNode objects.
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: DBMetaRootNode.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class DBMetaRootNode extends ReverseDbTreeNode
    implements java.io.Serializable
{
	static final long serialVersionUID = 5002948511759554049L;    /** Creates a new instance of DBMetaRootNode 
     *  @param pdbMeta DatabaseMetaData implementation where this node gets its data from.
     *  @param pdbMetaTreeModel The TreeModel this node is associated to.
     */
    public DBMetaRootNode(java.sql.DatabaseMetaData pdbMeta, 
                          DatabaseMetaDataTreeModel pdbMetaTreeModel)
    {
    	super(pdbMeta, pdbMetaTreeModel, null);
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
     * @see Object#toString()
     */
    public String toString()
    {
        return "Root";
    }
    
    public Class getPropertyEditorClass()
    {
        return null;
    }    
    
    /**
     * Loads the catalogs of this database.
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
                getDbMetaTreeModel().setStatusBarMessage("Started reading catalogs");
                rs = getDbMeta().getCatalogs();
                final java.util.ArrayList alNew = new java.util.ArrayList();
                int count = 0;
                while (rs.next())
                {
                    getDbMetaTreeModel().setStatusBarMessage("Reading catalog " + rs.getString("TABLE_CAT"));
                    alNew.add(new DBMetaCatalogNode(getDbMeta(), getDbMetaTreeModel(), 
                            DBMetaRootNode.this, rs.getString("TABLE_CAT")));
                    count++;
                }
                if (count == 0) 
                    alNew.add(new DBMetaCatalogNode(getDbMeta(), getDbMetaTreeModel(), 
                            DBMetaRootNode.this, null));
                alChildren = alNew;            
                javax.swing.SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        getDbMetaTreeModel().nodeStructureChanged(DBMetaRootNode.this);
                    }
                });
                rs.close();
            }
        }
        catch (java.sql.SQLException sqlEx)
        {
            getDbMetaTreeModel().reportSqlError("Error retrieving catalogs", sqlEx);
            try
            {
                if (rs != null) rs.close ();
            }
            catch (java.sql.SQLException sqlEx2)
            {
                this.getDbMetaTreeModel().reportSqlError("Error retrieving catalogs", sqlEx2);
            }            
            return false;
        }    	
        return true;
    }    
}
