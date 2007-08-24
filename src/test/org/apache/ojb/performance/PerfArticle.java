package org.apache.ojb.performance;

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

import java.io.Serializable;

/**
 * Persistent object interface - describes the persistent object used in performance test.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PerfArticle.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public interface PerfArticle extends Serializable
{
    public Long getArticleId();

    public void setArticleId(Long articleId);

    public String getArticleName();

    public void setArticleName(String articleName);

    public int getMinimumStock();

    public void setMinimumStock(int minimumStock);

    public double getPrice();

    public void setPrice(double price);

    public String getUnit();

    public void setUnit(String unit);

    public int getStock();

    public void setStock(int stock);

    public int getSupplierId();

    public void setSupplierId(int supplierId);

    public int getProductGroupId();

    public void setProductGroupId(int productGroupId);
}
