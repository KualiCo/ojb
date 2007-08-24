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
import org.apache.ojb.broker.metadata.FieldDescriptor;

import java.util.Iterator;
import java.util.Vector;

public class TorqueTableGenerator {

    private TorqueFieldGenerator fieldGenerator;
    private TorqueForeignKeyGenerator foreignKeyPreprocessor;
    private TorqueIndexGenerator indexGenerator;

    public TorqueTableGenerator(DescriptorRepository repository, boolean ignoreAutoIncrement) {
        this.fieldGenerator = new TorqueFieldGenerator(ignoreAutoIncrement);
        this.foreignKeyPreprocessor = new TorqueForeignKeyGenerator(repository);
        this.foreignKeyPreprocessor.buildConstraintsMap();
        this.indexGenerator = new TorqueIndexGenerator();
    }

    public void generateMappingTables(StringBuffer buffer, String indexTablespaceName) {
        Iterator mappingTables = this.foreignKeyPreprocessor.getMappingTables().values().iterator();
        while (mappingTables.hasNext()) {
            TableDescriptor mappingTable = (TableDescriptor) mappingTables.next();
            FieldDescriptor descriptors[] = (FieldDescriptor[]) (mappingTable.getColumns().toArray(new FieldDescriptor[0]));
            if (descriptors != null && descriptors.length > 0) {
                generateTableHeader(buffer, mappingTable, indexTablespaceName);
                this.fieldGenerator.generateMappingFieldDescriptors(descriptors, buffer);
                generateForeignKeys(mappingTable, buffer);
                buffer.append("    </table>\n");
            }
        }
    }

    public void generateStandardTable(TableDescriptor tableDescriptor, StringBuffer buffer, String indexTablespaceName) {
        generateTableHeader(buffer, tableDescriptor, indexTablespaceName);

        this.fieldGenerator.generateFieldDescriptors((FieldDescriptor[]) tableDescriptor.getColumns().toArray(new FieldDescriptor[0]), buffer);
        this.indexGenerator.generateIndices(tableDescriptor.getIndices(), buffer);
        generateForeignKeys(tableDescriptor, buffer);

        buffer.append("    </table>\n\n");
    }

    private void generateTableHeader(StringBuffer buffer, TableDescriptor tableDescriptor, String indexTablespaceName) {
        buffer.append("    <table name=\"");
        buffer.append(tableDescriptor.getName());
        buffer.append("\" indexTablespace=\"");
        buffer.append(indexTablespaceName);
        buffer.append("\">\n");
    }

    private void generateForeignKeys(TableDescriptor tableDescriptor, StringBuffer buffer) {
        Vector foreignKeys = this.foreignKeyPreprocessor.getForeignKeysForTable(tableDescriptor.getName());
        if (foreignKeys != null) {
            Iterator foreignKeyIterator = foreignKeys.iterator();
            while (foreignKeyIterator.hasNext()) {
                buffer.append(foreignKeyIterator.next());
            }
        }
    }
}
