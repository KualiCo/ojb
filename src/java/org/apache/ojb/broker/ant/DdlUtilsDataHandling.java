package org.apache.ojb.broker.ant;

/* Copyright 2004-2005 The Apache Software Foundation
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ExtendedBaseRules;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Provides data input and output via DdlUtils.
 * 
 * @author Thomas Dudziak
 */
public class DdlUtilsDataHandling
{
    private class DynaFactoryCreateRule extends Rule
    {
        /**
         * {@inheritDoc}
         */
        public void begin(String namespace, String name, Attributes attributes) throws Exception
        {
            DynaBean bean = _preparedModel.createBeanFor(name);

            if (bean == null)
            {
                throw new DataTaskException("Unknown element "+name);
            }

            for (int idx = 0; idx < attributes.getLength(); idx++)
            {
                String attrName  = attributes.getLocalName(idx);
                String attrValue = attributes.getValue(idx);
                Column column    = _preparedModel.getColumnFor(name, attrName);

                if (column == null)
                {
                    throw new DataTaskException("Unknown attribute "+attrName+" of element "+name);
                }
                bean.set(column.getName(), attrValue);
            }
            DdlUtilsDataHandling.this._digester.push(bean);
        }

        /**
         * {@inheritDoc}
         */
        public void end(String namespace, String name) throws Exception
        {
            DynaBean bean = (DynaBean)DdlUtilsDataHandling.this._digester.pop();

            ((DataSet)DdlUtilsDataHandling.this._digester.peek()).add(bean);
        }
    }

    public class DataRuleSet extends RuleSetBase
    {
        /**
         * {@inheritDoc}
         */
        public void addRuleInstances(Digester digester)
        {
            digester.addObjectCreate("dataset", DataSet.class);
            digester.addRule("*/dataset/*", new DynaFactoryCreateRule());
        }
    }

    /** The database model */
    private Database _dbModel;
    /** The platform */
    private Platform _platform;
    /** The prepared model */
    private PreparedModel _preparedModel;
    /** The digester for parsing the XML */
    private Digester _digester;

    /**
     * Creates a new data handling object.
     */
    public DdlUtilsDataHandling()
    {
        _digester = new Digester();
        _digester.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId)
            {
                // we don't care about the DTD for data files
                return new InputSource(new StringReader(""));
            }

        });
        _digester.setNamespaceAware(true);
        _digester.setValidating(false);
        _digester.setUseContextClassLoader(true);
        _digester.setRules(new ExtendedBaseRules());
        _digester.addRuleSet(new DataRuleSet());
    }

    /**
     * Sets the model that the handling works on.
     * 
     * @param databaseModel The database model
     * @param objModel      The object model
     */
    public void setModel(Database databaseModel, DescriptorRepository objModel)
    {
        _dbModel       = databaseModel;
        _preparedModel = new PreparedModel(objModel, databaseModel);
    }

    /**
     * Sets the (connected) database platform to work against.
     * 
     * @param platform The platform
     */
    public void setPlatform(Platform platform)
    {
        _platform = platform;
    }

    /**
     * Writes a DTD that can be used for data XML files matching the current model to the given writer. 
     * 
     * @param output The writer to write the DTD to
     */
    public void getDataDTD(Writer output) throws DataTaskException
    {
        try
        {
            output.write("<!ELEMENT dataset (\n");
            for (Iterator it = _preparedModel.getElementNames(); it.hasNext();)
            {
                String elementName = (String)it.next();

                output.write("    ");
                output.write(elementName);
                output.write("*");
                output.write(it.hasNext() ? " |\n" : "\n");
            }
            output.write(")>\n<!ATTLIST dataset\n    name CDATA #REQUIRED\n>\n");
            for (Iterator it = _preparedModel.getElementNames(); it.hasNext();)
            {
                String elementName = (String)it.next();
                List   classDescs  = _preparedModel.getClassDescriptorsMappingTo(elementName);

                if (classDescs == null)
                {
                    output.write("\n<!-- Indirection table");
                }
                else
                {
                    output.write("\n<!-- Mapped to : ");
                    for (Iterator classDescIt = classDescs.iterator(); classDescIt.hasNext();)
                    {
                        ClassDescriptor classDesc = (ClassDescriptor)classDescIt.next();
    
                        output.write(classDesc.getClassNameOfObject());
                        if (classDescIt.hasNext())
                        {
                            output.write("\n                 ");
                        }
                    }
                }
                output.write(" -->\n<!ELEMENT ");
                output.write(elementName);
                output.write(" EMPTY>\n<!ATTLIST ");
                output.write(elementName);
                output.write("\n");

                for (Iterator attrIt = _preparedModel.getAttributeNames(elementName); attrIt.hasNext();)
                {
                    String attrName = (String)attrIt.next();

                    output.write("    ");
                    output.write(attrName);
                    output.write(" CDATA #");
                    output.write(_preparedModel.isRequired(elementName, attrName) ? "REQUIRED" : "IMPLIED");
                    output.write("\n");
                }
                output.write(">\n");
            }
        }
        catch (IOException ex)
        {
            throw new DataTaskException(ex);
        }
    }

    /**
     * Returns the sql necessary to add the data XML contained in the given input stream. 
     * Note that the data is expected to match the repository metadata (not the table schema).
     * Also note that you should not use the reader after passing it to this method except closing
     * it (which is not done automatically).
     * 
     * @param input  A reader returning the content of the data file
     * @param output The writer to write the sql to
     */
    public void getInsertDataSql(Reader input, Writer output) throws DataTaskException
    {
        try
        {
            DataSet set = (DataSet)_digester.parse(input);

            set.createInsertionSql(_dbModel, _platform, output);
        }
        catch (Exception ex)
        {
            if (ex instanceof DataTaskException)
            {
                // is not declared by digester, but may be thrown
                throw (DataTaskException)ex;
            }
            else
            {
                throw new DataTaskException(ex);
            }
        }
    }

    /**
     * Returns the sql necessary to add the data XML contained in the given input stream. 
     * Note that the data is expected to match the repository metadata (not the table schema).
     * Also note that you should not use the reader after passing it to this method except closing
     * it (which is not done automatically).
     * 
     * @param input     A reader returning the content of the data file
     * @param batchSize The batch size; use 1 for not batch insertion
     */
    public void insertData(Reader input, int batchSize) throws DataTaskException
    {
        try
        {
            DataSet set = (DataSet)_digester.parse(input);
            
            set.insert(_platform, _dbModel, batchSize);
        }
        catch (Exception ex)
        {
            if (ex instanceof DataTaskException)
            {
                // is not declared by digester, but may be thrown
                throw (DataTaskException)ex;
            }
            else
            {
                throw new DataTaskException(ex);
            }
        }
    }
}
