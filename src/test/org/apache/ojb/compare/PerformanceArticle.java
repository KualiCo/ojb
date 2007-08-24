package org.apache.ojb.compare;

import java.io.Serializable;

public class PerformanceArticle implements Serializable
{
    /** maps to db-column "Artikel-Nr";INT;PrimaryKey*/
    protected Integer articleId;
    /** maps to db-column Artikelname;CHAR*/
    protected String articleName;
    
    /** maps to db-column Mindestbestand;INT*/
    protected int minimumStock;
    /** maps to db-column BestellteEinheiten;INT*/
    protected int orderedUnits;
    /** maps to db-column Einzelpreis;DECIMAL*/
    protected double price;
    
    /** maps to db-column Kategorie-Nr;INT*/
    protected int productGroupId;
    /** maps to db-column Lagerbestand;INT*/
    protected int stock;
    /** maps to db-column Lieferanten-Nr;INT*/
    protected int supplierId;
    /** maps to db-column Liefereinheit;CHAR*/
    protected String unit;

    /** increase the amount of articles in stock by diff*/
    public void addToStock(int diff)
    {
        stock += diff;
    }

    /**
     * return an articles unique id.
     * @return int the articles unique id
     */
    public Integer getArticleId()
    {
        return articleId;
    }

    /**
     * return an articles name.
     * @return java.lang.String
     */
    public String getArticleName()
    {
        return articleName;
    }

    /** return an articles ProductGroup*/

    public double getStockValue()
    {
        return price * stock;
    }

    /**
     * set an articles unique id.
     * @param newArticleId int
     */
    public void setArticleId(Integer newArticleId)
    {
        articleId = newArticleId;
    }

    /**
     * set an articles name.
     * @param newArticleName java.lang.String
     */
    public void setArticleName(java.lang.String newArticleName)
    {
        articleName = newArticleName;
    }

    /**
     * Insert the method's description here.
     * Creation date: (05.01.2001 19:31:04)
     */
    public PerformanceArticle()
    {
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getMinimumStock()
    {
        return minimumStock;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getOrderedUnits()
    {
        return orderedUnits;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return double
     */
    public double getPrice()
    {
        return price;
    }


    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getProductGroupId()
    {
        return productGroupId;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getStock()
    {
        return stock;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return int
     */
    public int getSupplierId()
    {
        return supplierId;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @return java.lang.String
     */
    public java.lang.String getUnit()
    {
        return unit;
    }



    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newMinimumStock int
     */
    public void setMinimumStock(int newMinimumStock)
    {
        minimumStock = newMinimumStock;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newOrderedUnits int
     */
    public void setOrderedUnits(int newOrderedUnits)
    {
        orderedUnits = newOrderedUnits;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newPrice double
     */
    public void setPrice(double newPrice)
    {
        price = newPrice;
    }


    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newProductGroupId int
     */
    public void setProductGroupId(int newProductGroupId)
    {
        productGroupId = newProductGroupId;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newStock int
     */
    public void setStock(int newStock)
    {
        stock = newStock;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newSupplierId int
     */
    public void setSupplierId(int newSupplierId)
    {
        supplierId = newSupplierId;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10.12.2000 14:40:04)
     * @param newUnit java.lang.String
     */
    public void setUnit(java.lang.String newUnit)
    {
        unit = newUnit;
    }
}
