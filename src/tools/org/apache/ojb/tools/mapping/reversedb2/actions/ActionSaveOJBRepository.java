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

import org.apache.ojb.broker.metadata.DescriptorRepository;

/**
 * Saves the given DescriptorRepository to a file selected by the use
 * @author  Administrator
 */
public class ActionSaveOJBRepository extends javax.swing.AbstractAction
{
    DescriptorRepository aRepository;
    java.io.File theFile;
    /** Creates a new instance of ActionSaveOJBRepository */
    public ActionSaveOJBRepository(DescriptorRepository paRepository, java.io.File fileSaveTo)
    {
        super("Save");
        putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_S));
        aRepository = paRepository;
        theFile = fileSaveTo;
    }
    
    public void actionPerformed(java.awt.event.ActionEvent evt)
    {
        String xmlString = aRepository.toXML();
        try
        {
            if (!theFile.exists()) theFile.createNewFile();
            java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileOutputStream( theFile ));
            pw.println(xmlString);
            pw.close();
        }
        // catches java.io.IOException and java.io.FileNotFoundException
        catch (Throwable t)
        {
            javax.swing.JOptionPane.showMessageDialog((java.awt.Component)evt.getSource(), t.getMessage(), "Save repository.xml", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
}
