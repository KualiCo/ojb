package org.apache.ojb.tools.mapping.reversedb2.actions;
import javax.swing.JFileChooser;

import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.RepositoryPersistor;
import org.apache.ojb.tools.mapping.reversedb2.Main;
import org.apache.ojb.tools.mapping.reversedb2.gui.JIFrmOJBRepository;
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
 * Opens a new JIFrmOBJRepository in the specified frame.
 * @author  Administrator
 */
public class ActionOpenOJBRepository extends javax.swing.AbstractAction
{
    private javax.swing.JFrame containingFrame;
    /** Creates a new instance of ActionOpenOJBRepository */
    public ActionOpenOJBRepository(javax.swing.JFrame pcontainingFrame)
    {
        this.containingFrame = pcontainingFrame;
        putValue(NAME, "Open OJB Repository");
        putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_O));
    }

    public void actionPerformed (java.awt.event.ActionEvent actionEvent)
    {
		String lastPath = Main.getProperties().getProperty("lastFileChooserPosition");
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser (lastPath);
        if (fileChooser.showOpenDialog(containingFrame)==JFileChooser.APPROVE_OPTION)
        {
            final java.io.File selectedFile = fileChooser.getSelectedFile();
			Main.getProperties().setProperty("lastFileChooserPosition", selectedFile.getParentFile().getAbsolutePath());
            Main.getProperties().storeProperties("");
            javax.swing.SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        RepositoryPersistor persistor = new RepositoryPersistor ();
                        DescriptorRepository repository = persistor.readDescriptorRepository(selectedFile.getCanonicalPath());
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
        }
    }
}
