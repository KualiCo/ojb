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

package org.apache.ojb.tools.mapping.reversedb2.ojbmetatreemodel;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;

import org.apache.ojb.broker.metadata.AttributeDescriptorBase;
/**
 *
 * @author  Florian Bruckner
 */
public class OjbMetadataTransferable implements java.awt.datatransfer.Transferable
{
    
    public static final DataFlavor OJBMETADATA_FLAVOR 
    //    = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + "; class=org.apache.ojb.broker.metadata.AttributeDescriptorBase[]", "OJB Repository metadata objects");
          = new DataFlavor(org.apache.ojb.broker.metadata.AttributeDescriptorBase[].class, "OJB");
    
    private static final DataFlavor[] _flavors = {OJBMETADATA_FLAVOR};
    
    private AttributeDescriptorBase[] selectedDescriptors;
    
    /** Creates a new instance of OjbMetadataTransferable */
    public OjbMetadataTransferable(AttributeDescriptorBase[] pSelectedDescriptors)
    {
        selectedDescriptors = pSelectedDescriptors;
    }
        
    /** Returns an object which represents the data to be transferred.  The class
     * of the object returned is defined by the representation class of the flavor.
     *
     * @param flavor the requested flavor for the data
     * @see DataFlavor#getRepresentationClass
     * @exception IOException                if the data is no longer available
     *              in the requested flavor.
     * @exception UnsupportedFlavorException if the requested data flavor is
     *              not supported.
     *
     */
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, java.io.IOException
    {
        if (flavor.isMimeTypeEqual(OJBMETADATA_FLAVOR))
            return selectedDescriptors;
        else
            throw new UnsupportedFlavorException(flavor);
    }
    
    /** Returns an array of DataFlavor objects indicating the flavors the data
     * can be provided in.  The array should be ordered according to preference
     * for providing the data (from most richly descriptive to least descriptive).
     * @return an array of data flavors in which this data can be transferred
     *
     */
    public DataFlavor[] getTransferDataFlavors()
    {
        return _flavors;
    }
    
    /** Returns whether or not the specified data flavor is supported for
     * this object.
     * @param flavor the requested flavor for the data
     * @return boolean indicating whether or not the data flavor is supported
     *
     */
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
         return java.util.Arrays.asList(_flavors).contains(flavor);
    }
    
}
