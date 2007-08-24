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

import org.apache.ojb.broker.metadata.FieldDescriptor;

import java.util.TreeMap;
import java.util.Vector;

public class TableDescriptor {

    private TreeMap columnsMap = new TreeMap();
    private Vector columns = new Vector();
    private Vector indices = new Vector();
    private Vector references = new Vector();
    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vector getColumns() {
        return this.columns;
    }

    public void addColumn(FieldDescriptor column) {
        if (!this.columnsMap.containsKey(column.getColumnName())) {
            this.columnsMap.put(column.getColumnName(), column);
            this.columns.add(column);
        }
    }

    public Vector getIndices() {
        return this.indices;
    }

    public void setIndices(Vector indices) {
        this.indices = indices;
    }

    public Vector getReferences() {
        return this.references;
    }

    public void setReferences(Vector references) {
        this.references = references;
    }

}
