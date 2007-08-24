package org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.tools.mapping.reversedb2.events.StatusMessageListener;

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
 * TreeModel representing the metadata of the database. Root element of this
 * model is a DBMetaRootNode.
 *
 *
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a> 
 * @version $Id: OjbMetaDataTreeModel.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */

public class OjbMetaDataTreeModel extends javax.swing.tree.DefaultTreeModel
{
    private DescriptorRepository ojbMetaData;
    
    /** Creates a new instance of DatabaseMetaDataTreeModel. The
     * model represents the metadata specified by pdbMetadata
     * @param pdbMetadata the metadata this model represents.
     * @param pStatusBar a JTextComponent that takes status messages
     * of this model. This model sometimes needs
     * some time to finish a request, the status
     * bar indicates what the model is doing.
     */
    public OjbMetaDataTreeModel (DescriptorRepository pOjbMetaData)
    {
        super(new javax.swing.tree.DefaultMutableTreeNode("dummy"));
        this.ojbMetaData = pOjbMetaData;      
        OjbMetaRootNode rootNode = new OjbMetaRootNode(ojbMetaData, this);
        super.setRoot(rootNode);                
        rootNode.load();
    }
    
    public DescriptorRepository getRepository()
    {
        return ojbMetaData;
    }
    
    
    /** Set a status message in the JTextComponent passed to this
     * model.
     * @param message The message that should be displayed.
     */    
    public void setStatusBarMessage(final String message)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==StatusMessageListener.class) 
            {
                ((StatusMessageListener)listeners[i+1]).statusMessageReceived(message);
            }          
        }        
    }
    
    /** Add a listener that receives status messages from
     * this model.
     * @param listener The listener that should receive the status messsages
     */    
    public void addStatusMessageListener(StatusMessageListener listener)
    {
        listenerList.add(StatusMessageListener.class, listener);
    }
    
    /** Remove a listener that receives status messages from
     * this model.
     * @param listener The listener that shall be removed
     */    
    public void removeStatusMessageListener(StatusMessageListener listener)
    {
        listenerList.remove(StatusMessageListener.class, listener);
    }    
    
    /** Method for reporting SQLException. This is used by
     * the treenodes if retrieving information for a node
     * is not successful.
     * @param message The message describing where the error occurred
     * @param sqlEx The exception to be reported.
     */    
    public void reportSqlError(String message, java.sql.SQLException sqlEx)
    {
        StringBuffer strBufMessages = new StringBuffer();
        java.sql.SQLException currentSqlEx = sqlEx;
        do
        {
            strBufMessages.append("\n" + sqlEx.getErrorCode() + ":" + sqlEx.getMessage());
            currentSqlEx = currentSqlEx.getNextException();
        } while (currentSqlEx != null);        
        System.err.println(message + strBufMessages.toString());
        sqlEx.printStackTrace();
    }
    
    public OjbMetaClassDescriptorNode getClassDescriptorNodeForClassDescriptor(ClassDescriptor cld)
    {
    	return ((OjbMetaRootNode)this.getRoot()).getClassDescriptorNodeForClassDescriptor(cld);
    }
}
