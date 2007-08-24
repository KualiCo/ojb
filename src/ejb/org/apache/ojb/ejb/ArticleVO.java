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
import java.math.BigDecimal;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ArticleVO.java,v 1.1 2007-08-24 22:17:39 ewestfal Exp $
 */
public class ArticleVO implements Serializable
{
    private Integer articleId;
    private String name;
    private BigDecimal price;
    private String description;
    private Integer categoryId;
    private CategoryVO category;

    public ArticleVO(Integer articleId, String name, String description, BigDecimal price, Integer categoryId)
    {
        this.articleId = articleId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
    }

    public ArticleVO()
    {
    }

    public CategoryVO getCategory()
    {
        return category;
    }

    public void setCategory(CategoryVO category)
    {
        this.category = category;
    }

    public Integer getArticleId()
    {
        return articleId;
    }

    public void setArticleId(Integer articleId)
    {
        this.articleId = articleId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        if(price != null) price.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.price = price;
    }

    public Integer getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId)
    {
        this.categoryId = categoryId;
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this);
        buf.append("articleId", articleId).
                append("name", name).
                append("description", description).
                append("price", price).
                append("categoryId", categoryId).
                append("category", category);
        return buf.toString();
    }
}
