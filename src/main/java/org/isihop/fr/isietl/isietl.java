/*
 * Copyright (C) 2025 tondeur-h
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.isihop.fr.isietl;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 *
 * @author tondeur-h
 */


public class isietl {
    //variables globales
    Integrator integratorGlob=null;
    
    //logs
       private static final  Logger logger = Logger.getLogger(isietl.class.getName());
    

    public static void main(String[] args) {
        isietl ietl = new isietl();
    }

    /****************
     * Constructeur 
     ****************/
    public isietl() 
    {   
        //mise en place des logs isietl basé sur logback
        try 
            {
                logger.setUseParentHandlers(false);
                FileHandler fh = new FileHandler("isietl%g.log", 0, 1, true);
                fh.setLevel(Level.ALL);
                fh.setFormatter(new SimpleFormatter());
                logger.addHandler(fh);
            } catch (IOException ioe) {
                Logger.getLogger(isietl.class.getName()).log(Level.SEVERE, null, ioe);
            }
        
        //au travail
        worker();
    }

    
    /*****************************
     * Executer du processus
     *****************************/
    public void worker()
    {
       tools to=new tools();  //instancier les tools
                
        //lire les properties du système isietl
        String programName=this.getClass().getSimpleName();
        to.lire_properties(programName);
        
        //lire le fichier d'integration.
        integratorGlob=to.lire_fichier_integration();
        
        //exploiter_integrator
        to.exploit_integrator(integratorGlob); 
    }
    
}