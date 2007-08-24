package org.apache.ojb.broker.metadata.torque;

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
import org.apache.ojb.broker.metadata.RepositoryPersistor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public class TorqueRepositoryGenerator
{

    private TorqueTablePreprocessor tablePreprocessor;
    private TorqueTableGenerator tableGenerator;

    public TorqueRepositoryGenerator(String xmlInputFile, boolean ignoreAutoIncrement) throws Exception
    {
        RepositoryPersistor persistor = new RepositoryPersistor();
        DescriptorRepository repository = persistor.readDescriptorRepository(xmlInputFile);

        this.tablePreprocessor = new TorqueTablePreprocessor(repository);
        this.tablePreprocessor.buildStandardTables();
        this.tableGenerator = new TorqueTableGenerator(repository, ignoreAutoIncrement);
    }

    public void generateTorqueRepository(String xmlOutputFile, String databaseName, String indexTablespaceName) throws Exception
    {
        Iterator tableDescriptorIterators = this.tablePreprocessor.getStandardTables().values().iterator();
        StringBuffer buffer = new StringBuffer(4096);

        generateDatabaseHeader(databaseName, indexTablespaceName, buffer);

        while (tableDescriptorIterators.hasNext())
        {
            this.tableGenerator.generateStandardTable((TableDescriptor) tableDescriptorIterators.next(), buffer, indexTablespaceName);
        }

        this.tableGenerator.generateMappingTables(buffer, indexTablespaceName);
        buffer.append("</database>\n");
        generateOutputFile(xmlOutputFile, buffer);
    }

    private void generateDatabaseHeader(String databaseName, String indexTablespaceName, StringBuffer buffer)
    {
        buffer.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\" ?>\n");
        buffer.append("<!DOCTYPE database>\n\n");
        buffer.append("<database name=\"");
        buffer.append(databaseName);
        buffer.append("\" indexTablespace=\"");
        buffer.append(indexTablespaceName);
        buffer.append("\">\n");
    }

    private void generateOutputFile(String xmlOutputFile, StringBuffer buffer) throws IOException
    {
        FileWriter writer = new FileWriter(xmlOutputFile);
        writer.write(buffer.toString());
        writer.flush();
        writer.close();
    }

    public static void main(String args[]) throws Exception
    {
        if (args.length >= 4 && args.length <= 5)
        {
            boolean ignoreAutoIncrement = (args.length == 5 && args[4].equalsIgnoreCase("true"));
            TorqueRepositoryGenerator generator = new TorqueRepositoryGenerator(args[0], ignoreAutoIncrement);
            generator.generateTorqueRepository(args[1], args[2], args[3]);
        }
        else
        {
            System.out.println("[TorqueRepositoryGenerator] Usage: inputFile outputFile databaseName indexTablespaceName (ignoreAutoIncrement)");
        }
    }

}
