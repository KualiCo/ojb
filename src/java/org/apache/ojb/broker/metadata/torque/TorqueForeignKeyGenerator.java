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
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class TorqueForeignKeyGenerator {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DescriptorRepository repository;
    private HashMap mappingTables = new HashMap();
    private HashMap foreignKeyVectors = new HashMap();

    public TorqueForeignKeyGenerator(DescriptorRepository repository) {
        this.repository = repository;
    }

    public void buildConstraintsMap() {
        Iterator classDescriptorIterators = this.repository.iterator();
        while (classDescriptorIterators.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) classDescriptorIterators.next();
            if(cd.isAbstract() || cd.isInterface())
            {
				logger.debug( "Skip constraint build for abstract class/ interface " + cd.getClassNameOfObject() );
            }
            else
            {
                buildConstraints(cd);
                buildOneToOneConstraints(cd);
            }
        }
    }

    public Vector getForeignKeysForTable(String tableName) {
        return (Vector) this.foreignKeyVectors.get(tableName);
    }

    public HashMap getMappingTables() {
        return this.mappingTables;
    }

    private void buildTableFieldDescriptors(FieldDescriptor fieldDescriptors[], TableDescriptor tableDescriptor) {
        for (int i = 0; i < fieldDescriptors.length; i++) {
            tableDescriptor.addColumn(fieldDescriptors[i]);
        }
    }

    private void buildConstraints(ClassDescriptor cd) {
        Vector collectionDescriptors = cd.getCollectionDescriptors();
        for (int i = 0; i < collectionDescriptors.size(); i++) {
            CollectionDescriptor collectionDescriptor = (CollectionDescriptor) collectionDescriptors.get(i);
            if (collectionDescriptor.isMtoNRelation()) {
                buildManyToManyConstraints(cd, collectionDescriptor);
            } else {
                buildOneToManyReferences(cd, collectionDescriptor);
            }
        }
    }

    private void buildManyToManyConstraints(ClassDescriptor cd, CollectionDescriptor collectionDescriptor) {
        Vector columns = new Vector();

        ClassDescriptor itemDescriptor = this.repository.getDescriptorFor(collectionDescriptor.getItemClass());
        buildManyToManyReferences(cd, collectionDescriptor, collectionDescriptor.getFksToThisClass(), columns);
        buildManyToManyReferences(itemDescriptor, collectionDescriptor, collectionDescriptor.getFksToItemClass(), columns);

        if (isImplicitlyMapped(collectionDescriptor.getIndirectionTable())) {
            TableDescriptor mappingTable = new TableDescriptor();
            buildTableFieldDescriptors((FieldDescriptor[]) columns.toArray(new FieldDescriptor[0]), mappingTable);
            mappingTable.setName(collectionDescriptor.getIndirectionTable());
            this.mappingTables.put(mappingTable.getName(), mappingTable);
        }
    }

    private void buildManyToManyReferences(ClassDescriptor cd, CollectionDescriptor collectionDescriptor,
                                           Object keys[], Vector columns)
    {
        if(cd.isAbstract() || cd.isInterface())
        {
			logger.debug( "Skip foreign key build for MtoM, found abstract base class or interface " + cd.getClassNameOfObject() );
            return;
        }
        StringBuffer buffer = new StringBuffer(256);
        buildForeignKeyHeader(cd.getFullTableName(), buffer);

        for (int i = 0; i < keys.length; i++) {
            String columnName = (String) keys[i];

            FieldDescriptor foreignColumn = cd.getPkFields()[i];
            String foreignColumnName = foreignColumn.getPersistentField().getName();
            buildReferenceForColumn(buffer, columnName, foreignColumnName);
            FieldDescriptor fieldDescriptor = (FieldDescriptor)foreignColumn.clone();
            fieldDescriptor.setColumnName(columnName);
            columns.add(fieldDescriptor);
        }
        buffer.append("        </foreign-key>\n");

        addReferenceToTable(collectionDescriptor.getIndirectionTable(), buffer.toString());
    }

    private void buildOneToManyReferences(ClassDescriptor cd, CollectionDescriptor collectionDescriptor) {
        Vector foreignKeyIndices = collectionDescriptor.getForeignKeyFields();
        ClassDescriptor foreignKeyClassDescriptor = this.repository.getDescriptorFor(collectionDescriptor.getItemClass());
        buildForeignKey(cd, foreignKeyIndices, foreignKeyClassDescriptor);
    }


    private void buildOneToOneConstraints(ClassDescriptor classDescriptor) {
        Vector referenceDescriptors = classDescriptor.getObjectReferenceDescriptors();
        for (int i = 0; i < referenceDescriptors.size(); i++) {
            ObjectReferenceDescriptor ord = (ObjectReferenceDescriptor) referenceDescriptors.get(i);

            Vector foreignKeyIndices = ord.getForeignKeyFields();
            ClassDescriptor foreignClassDescriptor = this.repository.getDescriptorFor(ord.getItemClass());
            buildForeignKey(foreignClassDescriptor, foreignKeyIndices, classDescriptor);
        }
    }

    private void buildForeignKey(ClassDescriptor foreignClassDescriptor, Vector foreignKeyIndices, ClassDescriptor classDescriptor) {

        if(classDescriptor.isAbstract() || classDescriptor.isInterface())
        {
			logger.debug( "Skip foreign key build, found abstract base class or interface " + classDescriptor.getClassNameOfObject() );
            return;
        }
        StringBuffer buffer = new StringBuffer(256);
        buildForeignKeyHeader(foreignClassDescriptor.getFullTableName(), buffer);

        for (int i = 0; i < foreignKeyIndices.size(); i++) {
            String columnName = null;
            Object obj = foreignKeyIndices.get(i);

            if (obj instanceof Integer)
            {
                int foreignKeyIndex = ((Integer) obj).intValue();
                columnName = classDescriptor.getFieldDescriptorByIndex(foreignKeyIndex).getColumnName();
            }
            else
            {
                    FieldDescriptor fld = classDescriptor.getFieldDescriptorByName((String) obj);
                    if(fld == null)
                    {
						logger.debug( "FieldDescriptor for foreign key parameter \n" + obj + " was not found in ClassDescriptor \n" + classDescriptor );
                    }
                    else columnName = fld.getColumnName();
            }

            FieldDescriptor foreignColumn = foreignClassDescriptor.getPkFields()[i];
            String foreignColumnName = foreignColumn.getColumnName();
            buildReferenceForColumn(buffer, columnName, foreignColumnName);
        }
        buffer.append("        </foreign-key>\n");
        addReferenceToTable(classDescriptor.getFullTableName(), buffer.toString());
    }

    private void buildForeignKeyHeader(String foreignClassName, StringBuffer buffer) {
        buffer.append("        <foreign-key foreignTable=\"");
        buffer.append(foreignClassName);
        buffer.append("\">\n");
    }

    private void buildReferenceForColumn(StringBuffer buffer, String columnName, String foreignColumnName) {
        buffer.append("            <reference local=\"");
        buffer.append(columnName);
        buffer.append("\" foreign=\"");
        buffer.append(foreignColumnName);
        buffer.append("\"/>\n");
    }


    private boolean isImplicitlyMapped(String tableName) {
        Iterator classDescriptorIterator = repository.iterator();

        while (classDescriptorIterator.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) classDescriptorIterator.next();
            if (tableName.equals(cd.getFullTableName())) {
                return false;
            }
        }

        return true;
    }

    private void addReferenceToTable(String tableName, String reference) {
        Vector tableReferences = (Vector) this.foreignKeyVectors.get(tableName);
        if (tableReferences == null) {
            tableReferences = new Vector();
            this.foreignKeyVectors.put(tableName, tableReferences);

        }
        if (!tableReferences.contains(reference)) {
            tableReferences.add(reference);
        }
    }
}
