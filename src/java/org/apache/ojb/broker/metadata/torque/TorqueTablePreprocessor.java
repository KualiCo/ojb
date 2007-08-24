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

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;

import java.util.HashMap;
import java.util.Iterator;

public class TorqueTablePreprocessor {

    private DescriptorRepository repository;
    private HashMap standardTables = new HashMap();

    public TorqueTablePreprocessor(DescriptorRepository repository) {
        this.repository = repository;
    }

    public void buildStandardTables() {
        Iterator classDescriptorIterators = this.repository.iterator();
        while (classDescriptorIterators.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) classDescriptorIterators.next();
            if(cd.isAbstract() || cd.isInterface())
            {
                System.out.println("Skip table build for abstract base class / interface called "+cd.getClassNameOfObject());
            }
            else
            {
                buildStandardTable(cd);
            }
        }
    }

    public HashMap getStandardTables() {
        return this.standardTables;
    }

    private void buildStandardTable(ClassDescriptor cd) {
        String tableName = cd.getFullTableName();
        TableDescriptor tableDescriptor = (TableDescriptor) this.standardTables.get(tableName);
        if (tableDescriptor == null) {
            tableDescriptor = new TableDescriptor();
            tableDescriptor.setName(tableName);
            this.standardTables.put(tableName, tableDescriptor);
        }

        buildStandardTableFieldDescriptors(cd.getFieldDescriptions(), tableDescriptor);
        tableDescriptor.getIndices().addAll(cd.getIndexes());
        tableDescriptor.getReferences().addAll(cd.getObjectReferenceDescriptors());
    }

    private void buildStandardTableFieldDescriptors(FieldDescriptor fieldDescriptors[], TableDescriptor tableDescriptor) {
        if (fieldDescriptors != null) {
            for (int i = 0; i < fieldDescriptors.length; i++) {
                tableDescriptor.addColumn(fieldDescriptors[i]);
            }
        }
    }
}
