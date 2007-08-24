package org.apache.ojb.ejb;

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


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple helper class.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 */
public class VOHelper
{
    public static List createNewArticleList(int number)
    {
        ArrayList list = new ArrayList();
        for (int i = 0; i < number; i++)
        {
            list.add(createNewArticle(i));
        }
        return list;
    }

    public static List createNewPersonList(int number)
    {
        ArrayList list = new ArrayList();
        for (int i = 0; i < number; i++)
        {
            list.add(createNewPerson(i));
        }
        return list;
    }

    public static ArticleVO createNewArticle(int counter)
    {
        return createNewArticle("A simple test article ", counter);
    }

    public static ArticleVO createNewArticle(String name, int counter)
    {
        ArticleVO a = new ArticleVO();
        a.setName(name);
        a.setPrice(new BigDecimal(0.45d * counter));
        a.setDescription("test article description " + counter);
        return a;
    }

    public static CategoryVO createNewCategory(String name)
    {
        return new CategoryVO(null, name, "this is a test category");
    }

    public static PersonVO createNewPerson(int counter)
    {
        PersonVO p = new PersonVO();
        p.setFirstName("firstname " + counter);
        p.setLastName("lastname " + counter);
        p.setGrade("grade" + counter);
        return p;
    }
}
