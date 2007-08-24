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

import java.sql.Types;

public class TorqueFieldGenerator {

    private boolean ignoreAutoIncrement;

    public TorqueFieldGenerator(boolean ignoreAutoIncrement) {
        this.ignoreAutoIncrement = ignoreAutoIncrement;
    }

    public void generateFieldDescriptors(FieldDescriptor[] descriptors, StringBuffer buffer) {
        if (descriptors != null) {
            for (int i = 0; i < descriptors.length; i++) {
                FieldDescriptor descriptor = descriptors[i];
                generateFieldDescriptor(descriptor, descriptor.getColumnName(), descriptor.isRequired(), descriptor.isAutoIncrement(), descriptor.isPrimaryKey(), buffer);
            }
        }
    }

    public void generateMappingFieldDescriptors(FieldDescriptor[] descriptors, StringBuffer buffer) {
        if (descriptors != null) {
            for (int i = 0; i < descriptors.length; i++) {
                FieldDescriptor descriptor = descriptors[i];
                StringBuffer fieldBuffer = new StringBuffer();
                generateFieldDescriptor(descriptor, descriptor.getColumnName(), true, false, true, fieldBuffer);
                buffer.append(fieldBuffer);
            }
        }
    }

    private void generateFieldDescriptor(FieldDescriptor descriptor, String fieldName, boolean isRequired, boolean isAutoIncrement, boolean isPrimaryKey, StringBuffer buffer) {
        buffer.append("        <column name=\"");
        buffer.append(fieldName);
        buffer.append("\" required=\"");
        buffer.append(isRequired || isPrimaryKey);
        if (!this.ignoreAutoIncrement) {
            buffer.append("\" autoIncrement=\"");
            buffer.append(isAutoIncrement);
        }
        buffer.append("\" primaryKey=\"");
        buffer.append(isPrimaryKey);
        buffer.append("\" type=\"");
        buffer.append(descriptor.getColumnType());

        generateConstraints(descriptor, buffer);

        buffer.append("\"/>\n");
    }

    private void generateConstraints(FieldDescriptor descriptor, StringBuffer buffer) {
        int jdbcType = descriptor.getJdbcType().getType();

        switch (jdbcType) {
            case Types.BIGINT:
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.FLOAT:
//            case Types.INTEGER:
            case Types.NUMERIC:
            case Types.REAL:
            case Types.SMALLINT:
            case Types.TINYINT:
                if (descriptor.isPrecisionSpecified()) {
                    buffer.append("\" precision=\"");
                    buffer.append(descriptor.getPrecision());
                }
                if (descriptor.isScaleSpecified()) {
                    buffer.append("\" scale=\"");
                    buffer.append(descriptor.getScale());
                }
                break;
            default:
                if (descriptor.isLengthSpecified()) {
                    buffer.append("\" size=\"");
                    buffer.append(descriptor.getLength());
                }
        }
    }

}
