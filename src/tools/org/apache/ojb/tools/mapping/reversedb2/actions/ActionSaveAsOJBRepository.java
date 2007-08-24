package org.apache.ojb.tools.mapping.reversedb2.actions;

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

import javax.swing.JFileChooser;

import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.tools.mapping.reversedb2.Main;

/**
 * Saves the given DescriptorRepository to a file selected by the use
 * @author  Administrator
 */
public class ActionSaveAsOJBRepository extends javax.swing.AbstractAction
{
    DescriptorRepository aRepository;

    /** Creates a new instance of ActionSaveOJBRepository */
    public ActionSaveAsOJBRepository(DescriptorRepository paRepository)
    {
        super("Save As...");
        putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_A));        
        aRepository = paRepository;
    }
    
    public void actionPerformed(java.awt.event.ActionEvent evt)
    {
		String lastPath = Main.getProperties().getProperty("lastFileChooserPosition");
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser (lastPath);
        if (fileChooser.showSaveDialog((java.awt.Component)evt.getSource())==JFileChooser.APPROVE_OPTION)
        {
            final java.io.File selectedFile = fileChooser.getSelectedFile();
			Main.getProperties().setProperty("lastFileChooserPosition", selectedFile.getParentFile().getAbsolutePath());
            Main.getProperties().storeProperties("");

            new ActionSaveOJBRepository(aRepository, selectedFile).actionPerformed(evt);
            
/*            javax.swing.SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        RepositoryPersistor persistor = new RepositoryPersistor ();
                        DescriptorRepository repository = persistor.readFromFile(selectedFile.getCanonicalPath());
                        JIFrmOJBRepository frm = new JIFrmOJBRepository(repository);
                        containingFrame.getContentPane().add(frm);
                        frm.setVisible(true);
                    }
                    catch (Throwable t)
                    {
                        t.printStackTrace();
                    }
                }
            });  
 */      
        }        
    }
}
