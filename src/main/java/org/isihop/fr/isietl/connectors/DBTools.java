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
package org.isihop.fr.isietl.connectors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tondeur-h
 */
public class DBTools 
{
    
    private final boolean debug=false;
    private final String deleteSQL="";
    
    //logs
    private Logger logger;
    
    //database
    private Connection conn;
    private Statement stmt;

    
    /**************************
     * Constructeur
     * @param logs 
     **************************/
    public DBTools(Logger logs) 
    {
        logger=logs;
    }

    
    /****************************
     * Getter pour Connection DB
     * @return 
     ****************************/
    public Connection getConn() 
    {
        return conn;
    }

    /****************************
     * Getter pour Statement
     * @return 
     ****************************/
    public Statement getStmt() 
    {
        return stmt;
    }
    
    
    /***********************************
     * Connecter la DB
     * Si non possible pas de traitement
     * @param dbdriver
     * @param dburl
     * @param dblogin
     * @param dbpassword
     * @return boolean
     **********************************/
    public boolean connect_db(String dbdriver, String dburl, String dblogin, String dbpassword) 
    {
        boolean isconnected=false;
        try {
            Class.forName(dbdriver);
       
                conn = DriverManager.getConnection(dburl,dblogin,dbpassword);
                isconnected=true;
                stmt=conn.createStatement();
                
        } catch (ClassNotFoundException | SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    return isconnected;  
    }

    
    /*********************************
     * Fermer la database si possible.
     *********************************/
    public void close_db() 
    {
        try {
            stmt.close();
            conn.close();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }
    
    
    /*******************************
     * purger des lignes inutiles 
     *******************************/
    public void purger_donnees_non_solicites() 
    {            
        String sql="";
        
        try {
            //Construction de la requête...
            if (debug==true) {System.out.println("Début du traitement de la purge des données non solicitées!");}
            logger.log(Level.INFO, "Début du traitement de la purge des données non solicitées!");
            stmt=conn.createStatement();
            BufferedReader brsql=new BufferedReader(new FileReader(deleteSQL));
            while (brsql.ready())
            {
                sql=brsql.readLine();
                if (debug==true) {System.out.println("Traitement de : "+sql);}
                logger.log(Level.INFO, "Traitement de : {0}", sql);
                stmt.executeUpdate(sql);
            }
            if (debug==true) {System.out.println("Fin du traitement de la purge des données non solicitées!");}
            logger.log(Level.INFO, "Fin du traitement de la purge des données non solicitées!");
        } catch (SQLException ex) 
        {
             if (debug==true) {System.out.println("( KO : "+sql);}
             logger.log(Level.SEVERE, "( KO : {0}", sql);
             logger.log(Level.SEVERE, "{0} ---- {1}", new Object[]{ex.getMessage(), ex.getSQLState()});
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    
    /***************************
     * Convertir date
     * JJ/MM/AA => AAAA-MM-JJ
     * @param string
     * @return 
     **************************/
    private String cvtTopgDate(String strDate) 
    {
        return "20"+strDate.substring(6)+"-"+strDate.substring(3,5)+"-"+strDate.substring(0, 2);
    }
}
