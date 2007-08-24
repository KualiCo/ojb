package org.apache.ojb.performance;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;

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

/**
 * Implementation of the {@link PerfArticle} interface.
 *
 * @version $Id: PerfArticleImpl.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class PerfArticleImpl implements PerfArticle
{
    private Long articleId;
    private String articleName;
    private int minimumStock;
    private double price;
    private String unit;
    private int stock;
    private int supplierId;
    private int productGroupId;

    public PerfArticleImpl()
    {
    }

    public PerfArticleImpl(Long articleId, String articleName, int minimumStock, double price, String unit, int stock, int supplierId, int productGroupId)
    {
        this.articleId = articleId;
        this.articleName = articleName;
        this.minimumStock = minimumStock;
        this.price = price;
        this.unit = unit;
        this.stock = stock;
        this.supplierId = supplierId;
        this.productGroupId = productGroupId;
    }

    public String toString()
    {
        return new ToStringBuilder(this).append("articleId", articleId)
                .append("articleName", articleName)
                .append("minimumStock", minimumStock)
                .append("price", price)
                .append("unit", unit)
                .append("stock", stock)
                .append("supplierId", supplierId)
                .append("productGroupId", productGroupId)
                .toString();
    }

    public Long getArticleId()
    {
        return articleId;
    }

    public void setArticleId(Long articleId)
    {
        this.articleId = articleId;
    }

    public String getArticleName()
    {
        return articleName;
    }

    public void setArticleName(String articleName)
    {
        this.articleName = articleName;
    }

    public int getMinimumStock()
    {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock)
    {
        this.minimumStock = minimumStock;
    }

    public double getPrice()
    {
        return price;
    }

    public void setPrice(double price)
    {
        this.price = price;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public int getStock()
    {
        return stock;
    }

    public void setStock(int stock)
    {
        this.stock = stock;
    }

    public int getSupplierId()
    {
        return supplierId;
    }

    public void setSupplierId(int supplierId)
    {
        this.supplierId = supplierId;
    }

    public int getProductGroupId()
    {
        return productGroupId;
    }

    public void setProductGroupId(int productGroupId)
    {
        this.productGroupId = productGroupId;
    }

    public int hashCode()
    {
        return new HashCodeBuilder().append(articleId).hashCode();
    }

    public boolean equals(Object obj)
    {
        if(obj == this)
        {
            return true;
        }
        if(obj instanceof PerfArticleImpl)
        {
            PerfArticleImpl o = (PerfArticleImpl) obj;
            return new EqualsBuilder()
                    .append(articleId, o.articleId)
                    .append(articleName, o.articleName)
                    .append(minimumStock, o.minimumStock)
                    .append(price, o.price)
                    .append(productGroupId, o.productGroupId)
                    .append(stock, o.stock)
                    .append(supplierId, o.supplierId)
                    .append(unit, o.unit)
                    .isEquals();
        }
        return false;
    }
}
