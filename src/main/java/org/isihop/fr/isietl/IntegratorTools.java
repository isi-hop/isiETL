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

import org.isihop.fr.isietl.entities.Fields;
import org.isihop.fr.isietl.entities.Job;
import org.isihop.fr.isietl.entities.Features;
import org.isihop.fr.isietl.tools.DBTools;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


/**
 *
 * @author tondeur-h
 */


class IntegratorTools 
{
    //variables globales
    String fileIntegratorPath="";
    boolean displayParameters=false;
    
    //logs
       private static final  Logger logger = Logger.getLogger(IntegratorTools.class.getName());
       
    /*********************************
     * Lire les properties du fichier
     * properties local.
     * @param programName
     *********************************/
    public void lire_properties(String programName)
    {
        //recuperer le chemin local et le nom de l'application
        //le fichier properties doit comporter le même
        //nom que l'application.
        String currentPath=System.getProperty("user.dir");
        FileInputStream is=null;
        
        try {
            is = new FileInputStream(currentPath+"/"+programName+".properties");
            Properties p=new Properties();
            try {
                //charger le fichier properties
                p.load(is);
                //lecture des variables
                fileIntegratorPath=p.getProperty("fileintegratorpath", currentPath+"/integrator.yml"); //par defaut le chemin de l'appli
                displayParameters=Boolean.parseBoolean(p.getProperty("displayparameters","false"));
                
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
        }
    }
    
    
    /************************************
     * lecture et controle du
     * fichier integrator.yml
     * @return 
     ************************************/
    void lire_fichier_integration() {
        InputStream inputStream=null;
        Job jobIntegrator=null;
        try {
            //Yaml yaml=new Yaml();
            Yaml yaml = new Yaml(new Constructor(Job.class, new LoaderOptions()));
            inputStream = new FileInputStream(new File(fileIntegratorPath));
            jobIntegrator = yaml.load(inputStream);
            
            if (displayParameters) {display_integrator(jobIntegrator);}
            
            //detecter le type de connecteur IN       
            check_connector_inbound(jobIntegrator);
            
            //detecter le type de connecteur OUT       
            check_connector_outbound(jobIntegrator);
            
            
            //ok passe les contrôles, on va traiter le job
            //si le job source est un fichier
            //faut il le checker ?
            if (checkInBoundValue(jobIntegrator,"connectortype","file"))
            {
                if (Boolean.parseBoolean(getInConnectorInBoundMap(jobIntegrator, "checkfiles"))==true)
                {
                    //verifier les fichiers sources
                }
            }
            //traiter l'integration...
            
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE,"Erreur parsing!", ex);
            System.exit(1); //sortie erreur 1 fichier yaml incorrect..
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
               logger.log(Level.SEVERE, null, ex);
            }
        }
        //return integrator;
    }

    /*****************************
     * Calculer un code de hashage 
     * en SHA256
     * @return 
     *****************************/
    private String calcul_code_Hashage(String chaine) {
        StringBuilder hash=new StringBuilder();
        //input concaténé
        String input=chaine;
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes());
        for (byte b : hashBytes) 
        {
            hash.append(String.format("%02x", b));
        }
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
        
        return hash.toString();
    }

    
    /***************************************
     * Exploitation du fichier inegrator.yml
     * @param integratorGlob
     ***************************************/
    
    public void display_integrator(Job integratorGlob) 
    {      
        System.out.println("------------------JOB-------------------");
        System.out.println("-------------INFORMATIONS---------------");
        System.out.println(integratorGlob.getJobName());
        System.out.println("\t\t---");
        System.out.println(integratorGlob.getJobDescription());
        System.out.println("");
        
        System.out.println("----------CONNECTOR INBOUND-------------");
        for (Map.Entry<String, Features> entry : integratorGlob.getConnectorInbound().entrySet()) 
        {
            System.out.println(entry.getKey()+"="+entry.getValue().value);
        }
        System.out.println("");

        System.out.println("---------------FIELDS IN---------------");
        for (Map.Entry<String, Fields> entry : integratorGlob.getFieldsIn().entrySet()) 
        {   System.out.println(entry.getKey());
            System.out.println("\tName="+entry.getValue().getName());
            System.out.println("\tDefaultValue="+entry.getValue().getDefaultValue());
            System.out.println("\tType="+entry.getValue().getType());
            System.out.println("\tSize="+entry.getValue().getSize());
            System.out.println("");
        }

        
        System.out.println("---------CONNECTOR OUTBOUND------------");
        for (Map.Entry<String, Features> entry : integratorGlob.getConnectorOutbound().entrySet()) 
        {
            System.out.println(entry.getKey()+"="+entry.getValue().value);
        }
        System.out.println("");

        System.out.println("---------------FIELDS OUT--------------");
        for (Map.Entry<String, Fields> entry : integratorGlob.getFieldsOut().entrySet()) 
        {   System.out.println(entry.getKey());
            System.out.println("\tName="+entry.getValue().getName());
            System.out.println("\tDefaultValue="+entry.getValue().getDefaultValue());
            System.out.println("\tType="+entry.getValue().getType());
            System.out.println("\tSize="+entry.getValue().getSize());
            System.out.println("");
        }

    }
    
       
    /*******************************************
     * Verifier présence des variables
     * obligatoire selon le type de connecteur.
     * @param detect_connector 
     *******************************************/
    private void check_connector_inbound(Job integrator) 
    {
        //si non défini, arrêt
        String connector=getInConnectorInBoundMap(integrator,"connectortype");
        
        if (connector.isBlank() || connector.isEmpty() || connector==null) {logger.log(Level.SEVERE, "Connector In n'est pas défini?");System.exit(2);}
        //typage du connecteur
        switch (connector.toUpperCase()) 
        {
            case "FILE" -> check_file_connector(integrator);
            case "DATABASE" -> check_database_connector_inbound(integrator);
            default -> {
                logger.log(Level.SEVERE,"Type de connecteur Entrant inconnu ?");
                System.out.println("Type de connecteur entrant inconnu?");
                System.exit(3);
            }
        }
    }
    
    /*********************************************
     * Rechercher une entree dans la Map Inbound
     * @param connectorInbound
     * @param entrySearch
     * @return 
     *********************************************/
    private String getInConnectorInBoundMap(Job jobInteger, String entrySearch) 
    {
        for (Map.Entry<String, Features> entry : jobInteger.getConnectorInbound().entrySet()) 
        {
            if (entry.getKey().compareToIgnoreCase(entrySearch)==0) {return entry.getValue().value;}
        }
            //par defaut
            return "empty";
    }

    
    /*******************************************
     * Verifier que la valeur de la clé corresponds
     * a une valeur donnée
     * @param integrator
     * @param key
     * @param value
     * @return 
     *******************************************/
    private boolean checkInBoundValue(Job integrator,String key,String value)
    {
       if (getInConnectorInBoundMap(integrator,key).compareToIgnoreCase(value)==0)
       {
           return true;
       }
       return false;
    }


   /*******************************************
     * Verifier que la valeur de la clé corresponds
     * a une valeur donnée
     * @param integrator
     * @param key
     * @param value
     * @return 
     *******************************************/
    private boolean checkOutBoundValue(Job integrator,String key,String value)
    {
       if (getInConnectorOutBoundMap(integrator,key).compareToIgnoreCase(value)==0)
       {
           return true;
       }
       return false;
    }
    

    /*********************************************
     * Rechercher une entree dans la Map Outbound
     * @param connectorOutbound
     * @param entrySearch
     * @return 
     *********************************************/
    private String getInConnectorOutBoundMap(Job jobInteger, String entrySearch) 
    {
        for (Map.Entry<String, Features> entry : jobInteger.getConnectorOutbound().entrySet()) 
        {
            if (entry.getKey().compareToIgnoreCase(entrySearch)==0) {return entry.getValue().value;}
        }
            //par defaut
            return "empty";
    }

    
    /************************************
     * Verifier la presence des valeurs
     * nécessaires pour un connecteur 
     * entrant de type file.
     ************************************/
    private void check_file_connector(Job integrator) 
    {
        System.out.println("Connecteur type fichier...");
        /*
        doit contenir les 4 paramètres suivants...
        correctement enregistré et avec une valeur corrrecte...
        filespath: "/home/tondeur-h/dev/isiETL/test"
        checkfiles: "true"
        destination: "/home/tondeur-h/dev/isiETL/test"
        exttype: "csv"
        */
        //lire le filespath.
        String filespath=getInConnectorInBoundMap(integrator,"filespath");
        if (test_a_path(filespath)==false) {logger.log(Level.SEVERE, "Le chemin {0} n''existe pas ou n''est pas lisible!", filespath);System.exit(4);} //chemin inexistant...
        
        //lire le chemin de destination
        String destination=getInConnectorInBoundMap(integrator,"destination");
        if (test_a_path(destination)==false) {logger.log(Level.SEVERE, "Le chemin {0} n''existe pas ou n''est pas lisible!", destination);System.exit(4);} //chemin inexistant...
        
        //lire et tester un boolean
        String checkfiles=getInConnectorInBoundMap(integrator,"checkfiles");
        if (test_boolean(checkfiles)==false) {logger.log(Level.SEVERE, "La valeur checkfiles n''est pas définie!");System.exit(4);} 
        
        //lire l'extension.
        String exttype=getInConnectorInBoundMap(integrator,"exttype");
        ArrayList<String> lstexttype =new ArrayList<>();
        lstexttype.add("csv"); //liste des extension prise en charge actuellement.
        if (test_string(exttype,lstexttype)==false) {logger.log(Level.SEVERE, "L''extension {0} n''est pas prise en charge!", exttype);System.exit(4);} //extension no prise en charge
        
        System.out.println("Test des variables OK");
    }

    
    /*********************************
     * Check Database Connexion...
     * @param integrator 
     *********************************/
    private void check_database_connector_inbound(Job integrator) 
    {
        // verifier que la database se connecte...
        //recuperer les éléments de connections.
        
        //tester la connection...
        DBTools dbt=new DBTools();
        String dbdriver=getInConnectorInBoundMap(integrator, "dbdriver");
        String dburl=getInConnectorInBoundMap(integrator, "dburl");
        String dblogin=getInConnectorInBoundMap(integrator, "dblogin");
        String dbpassword=getInConnectorInBoundMap(integrator, "dbpassword");
        
        if (!dbt.connect_db(dbdriver, dburl, dblogin, dbpassword))
        {
            System.out.println("connection à la database impossible!");
            System.exit(5); //database not connected...
        }
        else
        {
            dbt.close_db();
            System.out.println("Database Inbound OK.");
        }
    }


    /*********************************
     * Check Database Connexion...
     * @param integrator 
     *********************************/
    private void check_database_connector_outbound(Job integrator) 
    {
        // verifier que la database se connecte...
        //recuperer les éléments de connections.
        
        //tester la connection...
        DBTools dbt=new DBTools();
        String dbdriver=getInConnectorOutBoundMap(integrator, "dbdriver");
        String dburl=getInConnectorOutBoundMap(integrator, "dburl");
        String dblogin=getInConnectorOutBoundMap(integrator, "dblogin");
        String dbpassword=getInConnectorInBoundMap(integrator, "dbpassword");
        
        if (!dbt.connect_db(dbdriver, dburl, dblogin, dbpassword))
        {
            System.out.println("connection à la database sortante impossible!");
            System.exit(5); //database not connected...
        }
        else
        {
            dbt.close_db();
            System.out.println("Database Outbound OK.");
        }
    }

    
    /**********************************
     * Tester la validité d'un chemin
     * @param filespath
     * @return 
     *********************************/
    private boolean test_a_path(String filespath) 
    {
        File f=new File(filespath);
        if (f.exists() && f.canRead()) {return true;}
        //valeur par defaut
        return false;
    }

    /***********************************
     * Tester la valeur d'une chaine
     * dans une liste
     * @param exttype
     * @return 
     ***********************************/
    private boolean test_string(String exttype,ArrayList<String> lstChaines) 
    {
        for (String chaine:lstChaines)
        {
            if (chaine.compareToIgnoreCase(exttype)==0) {return true;}
        }
        //par defaut
        return false;
    }
    
    /**********************************
     * Tester validité du boolean
     * @param checkfiles
     * @return 
     **********************************/
    private boolean test_boolean(String checkfiles) 
    {
        //par defaut c'est ok.
        return ((checkfiles.toUpperCase().compareToIgnoreCase("TRUE")==0) || (checkfiles.toUpperCase().compareToIgnoreCase("FALSE")==0));
    }
    
    
    /*******************************************
     * Verifier présence des variables
     * obligatoire selon le type de connecteur.
     * @param detect_connector 
     *******************************************/
    private void check_connector_outbound(Job integrator) 
    {
        //si non défini, arrêt
        String connector=getInConnectorOutBoundMap(integrator,"connectortype");
        
        if (connector.isBlank() || connector.isEmpty() || connector==null) {logger.log(Level.SEVERE, "Connector Out n'est pas défini?");System.exit(2);}
        //typage du connecteur
        switch (connector.toUpperCase()) 
        {
            case "FILE" -> check_file_connector(integrator);
            case "DATABASE" -> check_database_connector_outbound(integrator);
            default -> {
                logger.log(Level.SEVERE,"Type de connecteur sortant inconnu ?");
                System.out.println("Type de connecteur sortant inconnu?");
                System.exit(3);
            }
        }
    }
    
}