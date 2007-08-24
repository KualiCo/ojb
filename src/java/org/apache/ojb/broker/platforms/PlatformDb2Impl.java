package org.apache.ojb.broker.platforms;

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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.apache.ojb.broker.util.sequence.SequenceManagerHelper;

/**
 * This class extends <code>PlatformDefaultImpl</code> and defines specific
 * behavior for the DB2 platform.
 *
 * <p/>
 * Many of the database sequence specific properties can be specified using
 * <em>custom attributes</em> within the <em>sequence-manager</em> element.
 * <br/>
 * The database sequence specific properties are generally speaking, see database user guide
 * for detailed description.
 *
 * <p>
 * Supported properties on sequence creation:
 * </p>
 *
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 *     <td><strong>Property Key</strong></td>
 *     <td><strong>Property Values</strong></td>
 * </tr>
 * <tr>
 *     <td>seq.as</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Specifies the datatype used for the sequence.
 *          Allowed: all numeric datatypes? e.g. <em>INTEGER</em>
 *    </td>
 * </tr>
 * <tr>
 *     <td>sequenceStart</td>
 *     <td>
 *          DEPRECATED. Database sequence specific property.<br/>
 *          Specifies the first sequence number to be
 *          generated. Allowed: <em>1</em> or greater.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.start</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Specifies the first sequence number to be
 *          generated. Allowed: <em>1</em> or greater.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.incrementBy</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Specifies the interval between sequence numbers.
 *          This value can be any positive or negative
 *          integer, but it cannot be 0.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.maxValue</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Set max value for sequence numbers.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.minValue</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Set min value for sequence numbers.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.cycle</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          If <em>true</em>, specifies that the sequence continues to generate
 *          values after reaching either its maximum or minimum value.
 *          <br/>
 *          If <em>false</em>, specifies that the sequence cannot generate more values after
 *          reaching its maximum or minimum value.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.cache</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          Specifies how many values of the sequence Oracle
 *          preallocates and keeps in memory for faster access.
 *          Allowed values: <em>2</em> or greater. If set <em>0</em>,
 *          an explicite <em>nocache</em> expression will be set.
 *    </td>
 * </tr>
 * <tr>
 *     <td>seq.order</td>
 *     <td>
 *          Database sequence specific property.<br/>
 *          If set <em>true</em>, guarantees that sequence numbers
 *          are generated in order of request.
 *          <br/>
 *          If <em>false</em>, a <em>no order</em> expression will be set.
 *    </td>
 * </tr>
 * </table>
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: PlatformDb2Impl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PlatformDb2Impl extends PlatformDefaultImpl
{
    /**
     * Patch provided by Avril Kotzen (hi001@webmail.co.za)
     * DB2 handles TINYINT (for mapping a byte).
     */
    public void setObjectForStatement(PreparedStatement ps, int index,
                                      Object value, int sqlType) throws SQLException
    {
        if (sqlType == Types.TINYINT)
        {
            ps.setByte(index, ((Byte) value).byteValue());
        }
        else
        {
            super.setObjectForStatement(ps, index, value, sqlType);
        }
    }

    public String createSequenceQuery(String sequenceName)
    {
        return "create sequence " + sequenceName;
    }

    public String createSequenceQuery(String sequenceName, Properties prop)
    {
        /*
        Read syntax diagramSkip visual syntax diagram
                                              .-AS INTEGER----.
        >>-CREATE SEQUENCE--sequence-name--*--+---------------+--*------>
                                              '-AS--data-type-'

        >--+------------------------------+--*-------------------------->
           '-START WITH--numeric-constant-'

           .-INCREMENT BY 1-----------------.
        >--+--------------------------------+--*------------------------>
           '-INCREMENT BY--numeric-constant-'

           .-NO MINVALUE----------------.
        >--+----------------------------+--*---------------------------->
           '-MINVALUE--numeric-constant-'

           .-NO MAXVALUE----------------.     .-NO CYCLE-.
        >--+----------------------------+--*--+----------+--*----------->
           '-MAXVALUE--numeric-constant-'     '-CYCLE----'

           .-CACHE 20----------------.     .-NO ORDER-.
        >--+-------------------------+--*--+----------+--*-------------><
           +-CACHE--integer-constant-+     '-ORDER----'
           '-NO CACHE----------------'
        */
        StringBuffer query = new StringBuffer(createSequenceQuery(sequenceName));
        if(prop != null)
        {
            Boolean b;
            Long value;
            String str;

            str = SequenceManagerHelper.getSeqAsValue(prop);
            if(str != null)
            {
                query.append(" AS ").append(str);
            }

            value = SequenceManagerHelper.getSeqStart(prop);
            if(value != null)
            {
                query.append(" START WITH ").append(value.longValue());
            }

            value = SequenceManagerHelper.getSeqIncrementBy(prop);
            if(value != null)
            {
                query.append(" INCREMENT BY ").append(value.longValue());
            }

            value = SequenceManagerHelper.getSeqMinValue(prop);
            if(value != null)
            {
                query.append(" MINVALUE ").append(value.longValue());
            }

            value = SequenceManagerHelper.getSeqMaxValue(prop);
            if(value != null)
            {
                query.append(" MAXVALUE ").append(value.longValue());
            }

            b = SequenceManagerHelper.getSeqCycleValue(prop);
            if(b != null)
            {
                if(b.booleanValue()) query.append(" CYCLE");
                else query.append(" NO CYCLE");
            }

            value = SequenceManagerHelper.getSeqCacheValue(prop);
            if(value != null)
            {
                query.append(" CACHE ").append(value.longValue());
            }

            b = SequenceManagerHelper.getSeqOrderValue(prop);
            if(b != null)
            {
                if(b.booleanValue()) query.append(" ORDER");
                else query.append(" NO ORDER");
            }
        }
        return query.toString();
    }

    public String nextSequenceQuery(String sequenceName)
    {
        return "values nextval for "+ sequenceName;
    }

    public String dropSequenceQuery(String sequenceName)
    {
        return "drop sequence " + sequenceName;
    }

    public String getLastInsertIdentityQuery(String tableName)
    {
        // matthias.roth@impart.ch
        // the function is used by the org.apache.ojb.broker.util.sequence.SequenceManagerNativeImpl
		// this call must be made before commit the insert command, so you
        // must turn off autocommit by seting the useAutoCommit="2"
        // or use useAutoCommit="1" or use a connection with autoCommit set false
        // by default (e.g. in managed environments)
        // transaction demarcation is mandatory
        return "values IDENTITY_VAL_LOCAL()";
    }
}
