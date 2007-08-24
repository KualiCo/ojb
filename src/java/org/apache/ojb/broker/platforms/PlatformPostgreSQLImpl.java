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

import org.apache.ojb.broker.query.LikeCriteria;
import org.apache.ojb.broker.util.sequence.SequenceManagerHelper;

/**
 * This class extends <code>PlatformDefaultImpl</code> and defines specific
 * behavior for the PostgreSQL platform.
 *
 * <p/>
 * Many of the database sequence specific properties can be specified using
 * <em>custom attributes</em> within the <em>sequence-manager</em> element.
 * <br/>
 * The database sequence specific properties are generally speaking, see database user guide
 * for detailed description.
 *
 * <p>
 * Implementation configuration properties:
 * </p>
 *
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 *     <td><strong>Property Key</strong></td>
 *     <td><strong>Property Values</strong></td>
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
 * @version $Id: PlatformPostgreSQLImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */

public class PlatformPostgreSQLImpl extends PlatformDefaultImpl
{
    public void setObjectForStatement(PreparedStatement ps, int index, Object value, int sqlType) throws SQLException
    {
        if ((value instanceof byte[]) && (sqlType == Types.LONGVARBINARY))
        {
            ps.setBytes(index, (byte[]) value);
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
        CREATE [ TEMPORARY | TEMP ] SEQUENCE seqname [ INCREMENT increment ]
            [ MINVALUE minvalue ] [ MAXVALUE maxvalue ]
            [ START start ] [ CACHE cache ] [ CYCLE ]
        */
        StringBuffer query = new StringBuffer(createSequenceQuery(sequenceName));
        if(prop != null)
        {
            Boolean b;
            Long value;

            value = SequenceManagerHelper.getSeqIncrementBy(prop);
            if(value != null)
            {
                query.append(" INCREMENT ").append(value.longValue());
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

            value = SequenceManagerHelper.getSeqStart(prop);
            if(value != null)
            {
                query.append(" START ").append(value.longValue());
            }

            value = SequenceManagerHelper.getSeqCacheValue(prop);
            if(value != null)
            {
                query.append(" CACHE ").append(value.longValue());
            }

            b = SequenceManagerHelper.getSeqCycleValue(prop);
            if(b != null)
            {
                if(b.booleanValue()) query.append(" CYCLE");
            }
        }
        return query.toString();
    }

    public String nextSequenceQuery(String sequenceName)
    {
        return "select nextval('" + sequenceName + "')";
    }

    public String dropSequenceQuery(String sequenceName)
    {
        return "drop sequence " + sequenceName;
    }

    /* (non-Javadoc)
    * @see org.apache.ojb.broker.platforms.Platform#addPagingSql(java.lang.StringBuffer)
    */
    public void addPagingSql(StringBuffer anSqlString)
    {
        anSqlString.append(" LIMIT ? OFFSET ?");
    }

    /* (non-Javadoc)
    * @see org.apache.ojb.broker.platforms.Platform#supportsPaging()
    */
    public boolean supportsPaging()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.platforms.Platform#bindPagingParameters(java.sql.PreparedStatement, int, int, int)
     */
    public int bindPagingParameters(PreparedStatement ps, int index, int startAt, int endAt) throws SQLException
    {
        ps.setInt(index, endAt - (startAt - 1));    // number of rows to fetch
        index++;
        ps.setInt(index, startAt - 1);              // zero based start
        index++;
        return index;
    }

    /**
     * @see org.apache.ojb.broker.platforms.Platform#getEscapeClause(org.apache.ojb.broker.query.LikeCriteria)
     */
    public String getEscapeClause(LikeCriteria aCriteria)
    {
        if (LikeCriteria.getEscapeCharacter() != LikeCriteria.DEFAULT_ESCPAPE_CHARACTER)
        {
            // the default escape character is \, so there's no need for an escape clause
            return super.getEscapeClause(aCriteria);
        }
        else
        {
            return "";
        }
    }
}
