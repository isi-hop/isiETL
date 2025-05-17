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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.isihop.fr.isietl.tools.FSTools;
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
    private String fileIntegratorPath="integrator.yml";
    private boolean displayParameters=false;
    
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
            fileIntegratorPath=currentPath+"/integrator.yml";
        } finally {
            try {
                if (is!=null) is.close();
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
    public void lire_fichier_jobs() 
    {
        InputStream inputStream=null;
        Job jobIntegrator;
        try {
            //lecture du YAML
            System.out.println("Lecture du fichier job : "+fileIntegratorPath);
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
                    System.out.println("Check fichiers inbound...");
                    FSTools fst=new FSTools();
                    fst.check_files_format(jobIntegrator);
                }
            }
            
            //traiter l'integration...
            traiter_integration(jobIntegrator);
            System.out.println("Fin des jobs...");
            
            
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE,"Erreur parsing!, Fichier Job non trouvé!", ex);
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
    private String calcul_code_Hashage(String chaine) 
    {
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
        String valeur;
        try {valeur=jobInteger.getConnectorInbound().get(entrySearch).getValue();} catch (Exception e) {valeur="";}        
        return valeur;
    }

    
    /*******************************************
     * Verifier que la valeur de la clé corresponds
     * a une valeur donnée en paramètre
     * @param integrator
     * @param key
     * @param value
     * @return 
     *******************************************/
    private boolean checkInBoundValue(Job integrator,String key,String value)
    {
       return getInConnectorInBoundMap(integrator,key).compareToIgnoreCase(value)==0;
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
       return getInConnectorOutBoundMap(integrator,key).compareToIgnoreCase(value)==0;
    }
    

    /*********************************************
     * Rechercher une entree dans la Map Outbound
     * @param connectorOutbound
     * @param entrySearch
     * @return 
     *********************************************/
    private String getInConnectorOutBoundMap(Job jobInteger, String entrySearch) 
    {
        String valeur;
        try {valeur=jobInteger.getConnectorOutbound().get(entrySearch).getValue();} catch (Exception e) {valeur="";}        
        return valeur;
    }

    
    /************************************
     * Verifier la presence des valeurs
     * nécessaires pour un connecteur 
     * entrant de type file.
     ************************************/
    private void check_file_connector(Job integrator) 
    {
        System.out.println("Connecteur InBound, type fichier...");
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
        
        System.out.println("Test du connecteur fichier OK");
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

    
    /**********************************************
     * TRaiter l'intégration des données
     * @param jobIntegrator 
     **********************************************/
    private void traiter_integration(Job jobIntegrator) 
    {
        try {
            //se connecter à la database_outbound
            System.out.println("Connection DataBase OutBound.");
            DBTools dbt=new DBTools();
            dbt.connect_db(
                    getInConnectorOutBoundMap(jobIntegrator, "dbdriver"),
                    getInConnectorOutBoundMap(jobIntegrator, "dburl"),
                    getInConnectorOutBoundMap(jobIntegrator, "dblogin"),
                    getInConnectorOutBoundMap(jobIntegrator, "dbpassword"));
            System.out.println("Connection DataBase OutBound : PASS");
            
            //tester présence de la table sinon la construire
            String sql="select count(*) from "+getInConnectorOutBoundMap(jobIntegrator, "targetTable");
            
            try{
                System.out.println("Controle disponibilité table OutBound");
                ResultSet rs=dbt.getStmt().executeQuery(sql);
                System.out.println("Table "+getInConnectorOutBoundMap(jobIntegrator, "targetTable")+" disponible.");
            } catch (SQLException ex) 
            {
                //creer la table car manquante...
                System.out.println("Création de la table "+getInConnectorOutBoundMap(jobIntegrator, "targetTable"));
                
                //creer_create_Table(jobIntegrator);
                sql=creer_create_Table(jobIntegrator);
                
                dbt.getStmt().executeUpdate(sql);
                System.out.println("Creation de la table : PASS");
            }
                        
            //construire le template de la requête UPSERT
            System.out.println("Préparation du template UPSERT");
            String sqlTemplate=creer_create_Template_UPSERT(jobIntegrator);
            
            //traiter les fichiers
            int nbLignes;
            FSTools fst=new FSTools();
            List<String> lstfile=fst.lister_les_fichiers(getInConnectorInBoundMap(jobIntegrator, "filespath"), getInConnectorInBoundMap(jobIntegrator, "exttype"));
            for (String fichier:lstfile)
            {
                System.out.println("Début du job d'intégration du fichier : "+fichier);
                nbLignes=1;
                fst.ouvrir_fichier(fichier);
                while(fst.lecture_statut())
                {
                    String[] col=fst.lecture_ligne().split(";");
                    
                    String hashCode=calcul_code_Hashage(concatenate_col(col));
                    
                    sql=replace_template_UPSERT_Value(sqlTemplate,hashCode,col,jobIntegrator);
                    
                    //UPSERT_DATA
                    dbt.getStmt().executeUpdate(sql);
                    nbLignes++;
                }
                fst.fermer_fichier();
                System.out.println("Traitement de "+nbLignes+" lignes.");
                System.out.println("Fin du job d'intégration du fichier : "+fichier);
            }
            
            //se deconnecter de la database outobound
            dbt.close_db();
        } catch (SQLException ex) {
            Logger.getLogger(IntegratorTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    
    /************************
     * Concatene un tableau
     * de chaine
     * @param col
     * @return 
     ************************/
    private String concatenate_col(String[] col) 
    {
        String resultat="";
       for (String c:col) {resultat=resultat+col;}
       return resultat;
    }

    /*****************************************
     * Creer la table automatique a partir des
     * donnees de descriptions qui se trouvent
     * dans la description "fieldsOut"
     * @param jobIntegrator
     * @return 
     ****************************************/
    private String creer_create_Table(Job jobIntegrator) 
    {
        String sqlCreateTable="";
        
        String NomTable=getInConnectorOutBoundMap(jobIntegrator, "targetTable");
        sqlCreateTable="CREATE TABLE " + NomTable+" (";
        for (Map.Entry<String, Fields> entry : jobIntegrator.getFieldsOut().entrySet()) 
        {
            //si varchar présent et taille >0 => varchar(x)
            if (entry.getValue().getType().toUpperCase().compareTo("VARCHAR")==0 && Integer.parseInt(entry.getValue().getSize(),10)>0)
            {
                sqlCreateTable=sqlCreateTable+entry.getKey().toLowerCase()+" "+entry.getValue().getType().toLowerCase()+"("+entry.getValue().getSize()+"),";
            }
            else
            {
                sqlCreateTable=sqlCreateTable+entry.getKey()+" "+entry.getValue().getType().toLowerCase()+",";
            } 
        }
        //ajouter le hascode
        sqlCreateTable=sqlCreateTable+"hashcode varchar,";
        
        //ajouter la contraite
        sqlCreateTable=sqlCreateTable+"CONSTRAINT "+NomTable+"_unique UNIQUE (hashcode))";

        
        return sqlCreateTable;
    }

    /********************************
     * Creer le template de l'UPSERT
     * @param jobIntegrator
     * @return 
     ********************************/
    private String creer_create_Template_UPSERT(Job jobIntegrator) {
        String template="";
        
        template="INSERT INTO "+getInConnectorOutBoundMap(jobIntegrator, "targetTable")+" (";
        
        for (Map.Entry<String, Fields> entry : jobIntegrator.getFieldsOut().entrySet()) 
            {template=template+entry.getKey().toLowerCase()+",";}
        
        template=template+"hashcode) VALUES(";
        
        for (Map.Entry<String, Fields> entry : jobIntegrator.getFieldsOut().entrySet()) 
        {template=template+"'%"+entry.getKey().toLowerCase()+"%',";}
        template=template+"'%hashcode%'";
        
        template=template+") ON CONFLICT(hashcode) DO UPDATE SET ";
        
        for (Map.Entry<String, Fields> entry : jobIntegrator.getFieldsOut().entrySet()) 
        {template=template+entry.getKey().toLowerCase()+"='%"+entry.getKey().toLowerCase()+"%',";}
        
        return template.substring(0, template.length()-1); //retirer la dernière virgule
    }

    /******************************
     * Implementer la requete
     * @param sqlTemplate
     * @return 
     ******************************/
    private String replace_template_UPSERT_Value(String sqlTemplate,String hashCode,String[] col, Job jobIntegrator) 
    {
        String sqlReplace=sqlTemplate;
        //chercher le nom de la valeur
        String champ="";
        int num=0;
        for (Map.Entry<String, Fields> entry : jobIntegrator.getFieldsOut().entrySet()) 
        {
            champ="%"+entry.getKey().toLowerCase()+"%";
            sqlReplace=sqlReplace.replaceAll(champ, col[num]);
            num++;
        }
        
        //remplacer le hashcode
        sqlReplace=sqlReplace.replaceAll("%hashcode%", hashCode);
        
        return sqlReplace;
    }
    
    
}