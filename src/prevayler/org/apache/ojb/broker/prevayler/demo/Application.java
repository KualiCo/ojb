package org.apache.ojb.broker.prevayler.demo;

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
 
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.util.ui.AsciiSplash;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;
/**
 * The tutorial application.
 * @author Thomas Mahler
 */
public class Application
{
    private Vector useCases;
    private PersistenceBroker broker;
    /**
     * Application constructor comment.
     */
    public Application()
    {
        broker = null;
        try
        {
            broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        useCases = new Vector();
        useCases.add(new UCListAllProducts(broker));
        useCases.add(new UCEnterNewProduct(broker));
        useCases.add(new UCEditProduct(broker));
        useCases.add(new UCDeleteProduct(broker));
        useCases.add(new UCQuitApplication(broker));
    }
    /**
     * Disply available use cases.
     */
    public void displayUseCases()
    {
        System.out.println();
        for (int i = 0; i < useCases.size(); i++)
        {
            System.out.println("[" + i + "] " + ((UseCase) useCases.get(i)).getDescription());
        }
    }
    /**
     * Insert the method's description here.
     * Creation date: (04.03.2001 10:40:25)
     * @param args java.lang.String[]
     */
    public static void main(String[] args)
    {
        Application app = new Application();
        app.run();
    }
    /**
     * read a single line from stdin and return as String
     */
    private String readLine()
    {
        try
        {
            BufferedReader rin = new BufferedReader(new InputStreamReader(System.in));
            return rin.readLine();
        }
        catch (Exception e)
        {
            return "";
        }
    }
    /**
     * the applications main loop.
     */
    public void run()
    {    	
    	System.out.println(AsciiSplash.getSplashArt());
        System.out.println("Welcome to the OJB PB tutorial application");
        System.out.println();
        // never stop (there is a special use case to quit the application)
        while (true)
        {
            try
            {
                // select a use case and perform it
                UseCase uc = selectUseCase();
                uc.apply();
            }
            catch (Throwable t)
            {
                broker.close();
                System.out.println(t.getMessage());
            }
        }
    }
    /**
     * select a use case.
     */
    public UseCase selectUseCase()
    {
        displayUseCases();
        System.out.println("type in number to select a use case");
        String in = readLine();
        int index = Integer.parseInt(in);
        return (UseCase) useCases.get(index);
    }
    

}
