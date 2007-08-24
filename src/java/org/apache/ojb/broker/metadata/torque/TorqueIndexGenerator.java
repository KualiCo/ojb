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

import org.apache.ojb.broker.metadata.IndexDescriptor;

import java.util.Vector;

public class TorqueIndexGenerator {

    public void generateIndices(Vector indexDescriptors, StringBuffer buffer) {
        int numIndexes = indexDescriptors.size();
        for (int i = 0; i < numIndexes; i++) {
            IndexDescriptor index = (IndexDescriptor) indexDescriptors.get(i);
            String indexTag = getIndexTag(index);

            buffer.append("        <");
            buffer.append(indexTag);
            if (index.getName() != null) {
                buffer.append(" name=\"");
                buffer.append(index.getName());
                buffer.append("\"");
            }
            buffer.append(">\n");

            generateStringVector(index.getIndexColumns(), indexTag + "-column", "name", "            ", buffer);
            buffer.append("        </");
            buffer.append(indexTag);
            buffer.append(">\n");
        }
    }

    private String getIndexTag(IndexDescriptor indexDescriptor) {
        if (indexDescriptor.isUnique()) {
            return "unique";
        } else {
            return "index";
        }
    }

    private void generateStringVector(Vector strings, String element, String attribute, String indentation, StringBuffer buffer) {
        for (int i = 0; i < strings.size(); i++) {
            buffer.append(indentation);
            buffer.append("<");
            buffer.append(element);
            buffer.append(" ");
            buffer.append(attribute);
            buffer.append("=\"");
            buffer.append(strings.get(i));
            buffer.append("\"/>\n");
        }
    }

}
