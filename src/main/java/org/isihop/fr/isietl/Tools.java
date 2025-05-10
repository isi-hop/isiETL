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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


/**
 *
 * @author tondeur-h
 */


class Tools {
    //variables globales
    String fileIntegratorPath="";
    boolean displayParameters=false;
    
    //logs
       private static final  Logger logger = Logger.getLogger(Tools.class.getName());

    //enumerations
       enum CONNECTOR{FILE,DATABASE,UNKNOWN}
       
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
        Integrator integrator=null;
        try {
            //Yaml yaml=new Yaml();
            Yaml yaml = new Yaml(new Constructor(Integrator.class, new LoaderOptions()));
            inputStream = new FileInputStream(new File(fileIntegratorPath));
            integrator = yaml.load(inputStream);
            
            if (displayParameters) {display_integrator(integrator);}
            
            //detecter le type de connecteur IN       
            check_connector_variables(detect_connector(integrator.getConnector()));
            
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
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
    public void display_integrator(Integrator integratorGlob) 
    {      
        System.out.println("-------------CONNECTOR IN---------------");
        System.out.println("Checkfile : "+integratorGlob.getCheckfiles());
        System.out.println("Connector : "+integratorGlob.getConnector());
        System.out.println("Filespath : "+integratorGlob.getFilespath());
        System.out.println("ExtType : "+integratorGlob.getExttype());
        System.out.println("Destination : "+integratorGlob.getDestination());
        System.out.println("-------------DATABASE OUT-----------");
        System.out.println("DBDriver : "+integratorGlob.getDbdriver());
        System.out.println("DBLogin : "+integratorGlob.getDblogin());
        System.out.println("DBPassword : "+integratorGlob.getDbpassword());
        System.out.println("-------------LOAD-----------");
        System.out.println("SQLInsert : "+integratorGlob.getSqlinsert());
        System.out.println("SQLUpdate : "+integratorGlob.getSqlupdate());
        System.out.println("-------------FIELDS-----------");
        System.out.println("FiledsIn : "+integratorGlob.getFieldsIn());
    }
    
    
    /************************
     * Ecrire fichier YAML a 
     * partir d'un pojo
     ************************/
        public void ecrire_fichier_integration() {

        PrintWriter writer=null;
        try {
            Integrator integrator=new Integrator();
            integrator.setConnector("file");
            integrator.setExttype("csv");
            integrator.setCheckfiles("true");
            integrator.setFilespath("/home/tondeur-h/dev/isiETL/test");
            integrator.setDestination("/home/tondeur-h/dev/isiETL/test");
            integrator.setDbdriver("jdbc.postgresql.org");
            integrator.setDblogin("postgres");
            integrator.setDbpassword("admin");
            integrator.setSqlinsert("");
            integrator.setSqlupdate("");

            Fields f=new Fields();
            f.setName("identifiant");
            f.setSize("20");
            f.setType("varchar");
            f.setDefaultValue("vide");

            Map<String,Fields> fieldsIn=new HashMap<>();
            fieldsIn.put("identifiant", f);
            integrator.setFieldsIn(fieldsIn);


            DumperOptions options = new DumperOptions();
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setVersion(DumperOptions.Version.V1_0);
            Yaml yaml = new Yaml(options);

            writer = new PrintWriter(new File(fileIntegratorPath));
            yaml.dump(integrator, writer);

        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }

    /**********************************
     * Tester le modéle de connecteur
     * @param connector
     * @return 
     *********************************/
    private CONNECTOR detect_connector(String connector) 
    {
        //si non défini, arrêt
        if (connector.isBlank() || connector.isEmpty() || connector==null) {logger.log(Level.SEVERE, "Connector In n'est pas défini?");System.exit(2);}
        //typage du connecteur
        return switch (connector.toUpperCase()) {
            case "FILE" -> CONNECTOR.FILE;
            case "DATABASE" -> CONNECTOR.DATABASE;
            default -> CONNECTOR.UNKNOWN;
        };         
    }

    
    /*******************************************
     * Verifier présence des variables
     * obligatoire selon le type de connecteur.
     * @param detect_connector 
     *******************************************/
    private void check_connector_variables(CONNECTOR detect_connector) {
        switch (detect_connector) {
            case CONNECTOR.UNKNOWN:
                logger.log(Level.SEVERE,"Type de connecteur Entrant inconnu ?");
                System.out.println("Type de connecteur entrant inconnu?");
                System.exit(3);
                break;
            case CONNECTOR.FILE:
                check_file_connector();
                break;
            case CONNECTOR.DATABASE:
                check_database_connector();
                break;
            default:
                //non possible
        }
    }
    
    
    
    private void check_file_connector() {
        System.out.println("Connecteur type fichier...");
    }

    private void check_database_connector() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    
}