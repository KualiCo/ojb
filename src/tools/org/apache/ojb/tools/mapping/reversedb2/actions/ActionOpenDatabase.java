package org.apache.ojb.tools.mapping.reversedb2.actions;
import org.apache.ojb.tools.mapping.reversedb2.gui.JDlgDBConnection;
import org.apache.ojb.tools.mapping.reversedb2.gui.JIFrmDatabase;
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



/** First opens a JDlgDBConnection to get a JDBC connection and if this
 * was successful, a JIFrmDatabase is created with this connection and
 * added to the parentFrame specified in the constructor.
 * @author <a href="mailto:bfl@florianbruckner.com">Florian Bruckner</a>
 * @version $Id: ActionOpenDatabase.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */

public class ActionOpenDatabase extends javax.swing.AbstractAction
{
    
    javax.swing.JFrame containingFrame;
    /** Creates a new instance of ActionOpenDatabase, pcontainingFrame
     * is the parent frame for the dialog and the containing frame
     * for the internal frame created.
     * @param pcontainingFrame parent frame for the dialog, containing frame for the IFrame
     */
    public ActionOpenDatabase(javax.swing.JFrame pcontainingFrame) 
    {
        this.containingFrame = pcontainingFrame;
        putValue(NAME, "Open Database");
        putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_C));
    }
    
    /** Called to execute this action.
     * @param actionEvent
     */    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent)
    {
        new Thread()
        {
            public void run()
            {
                final java.sql.Connection conn = new JDlgDBConnection(containingFrame, false).showAndReturnConnection();
                if (conn != null)
                {
                    javax.swing.SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            JIFrmDatabase frm = new JIFrmDatabase(conn);
                            containingFrame.getContentPane().add(frm);
                            frm.setVisible(true);
                        }
                    });
                }
            }
        }.start();
    }
}
