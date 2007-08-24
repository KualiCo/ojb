package org.apache.ojb.ejb;

/* Copyright 2003-2005 The Apache Software Foundation
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

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: CategoryVO.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class CategoryVO implements Serializable
{
    private Integer objId;
    private String categoryName;
    private String description;
    private Collection assignedArticles;

    public CategoryVO(Integer categoryId, String categoryName, String description)
    {
        this.objId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
    }

    public CategoryVO()
    {
    }

    public Collection getAssignedArticles()
    {
        return assignedArticles;
    }

    public void setAssignedArticles(Collection assignedArticles)
    {
        this.assignedArticles = assignedArticles;
    }

    public Integer getObjId()
    {
        return objId;
    }

    public void setObjId(Integer objId)
    {
        this.objId = objId;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        buf.append("objId", objId).
                append("categoryName", categoryName).
                append("description", description).
                append("assignedArticles", assignedArticles);
        return buf.toString();
    }
}
