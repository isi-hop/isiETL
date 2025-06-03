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
package org.isihop.fr.isietl.jobs;

import org.isihop.fr.isietl.entities.Fields;
import org.isihop.fr.isietl.entities.Job;
import org.isihop.fr.isietl.entities.Features;
import org.isihop.fr.isietl.connectors.DBTools;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.isihop.fr.isietl.connectors.FSTools;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


/**
 *
 * @author tondeur-h
 */


public class IntegratorTools 
{
    //variables globales
    private String fileIntegratorPath;
    private boolean displayParameters;
    private long startIntegrationTime;
    private long endIntegrationTime;
    
    //logs
    private Logger logger;

       
    /************************************
     * lecture et controle du
     * fichier integrator.yml
     * @param fileymlpath
     * @param dp
     * @param logs
     ************************************/
    public void read_job_file(String fileymlpath, boolean dp,Logger logs) 
    {
        //réassocier les paramètre en loca
        fileIntegratorPath=fileymlpath;
        displayParameters=dp;
        logger=logs;
        
        
        InputStream inputStream=null;
        Job jobIntegrator;
        try {
            //lecture du YAML
            System.out.println("Reading the job file : "+fileIntegratorPath);
            logger.log(Level.INFO, "Reading the job file : {0}", fileIntegratorPath);
            
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
                if (safeParseBool(getInConnectorInBoundMap(jobIntegrator, "checkfiles"),false)==true)
                {
                    //verifier les fichiers sources
                    System.out.println("Check inbound file...");
                    logger.log(Level.INFO,"Check inbound file...");
                    
                    FSTools fst=new FSTools(logger);
                    fst.check_files_format(jobIntegrator);
                }
            }
            
            //traiter l'integration...
            //---------------le job se fait ici-------------------
            process_integration_file_to_db(jobIntegrator);
            System.out.println("End of jobs...");
            logger.log(Level.INFO,"End of jobs...");
            //----------------------------------------------------
            
        } catch (FileNotFoundException ex) {
            System.out.println("Parsing Error!, Job file not found!");
            System.out.println(ex.getMessage());
            logger.log(Level.SEVERE,"Parsing Error!, Job file not found!", ex.getMessage());
            System.exit(1); //sortie erreur 1 fichier yaml incorrect..
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
               logger.log(Level.SEVERE, ex.getMessage());
            }
        }
        //return integrator;
    }

    
    /*****************************
     * Calculer un code de hashage 
     * en SHA256
     * @return 
     *****************************/
    private String hash_code_calculate(String chaine) 
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
        System.out.println("-----------------HEADER-----------------");
        System.out.println("Job Name :");
        System.out.println(integratorGlob.getJobName());
        System.out.println("Job description :");
        System.out.println(integratorGlob.getJobDescription());
        System.out.println("Job Date Time :");
        System.out.println(integratorGlob.getJobDateTime());
        System.out.println("--- Batch Size Mode :");
        System.out.println("BatchSize="+integratorGlob.getJobBatchSize());
        System.out.println("ForceIntermediateCommit="+integratorGlob.getForceIntermediateCommit());
        
        System.out.println("");
        
        System.out.println("----------CONNECTOR INBOUND-------------");
        for (Map.Entry<String, Features> entry : integratorGlob.getConnectorInbound().entrySet()) 
        {
            System.out.println(entry.getKey()+"="+entry.getValue().value);
        }
        System.out.println("");
        
        System.out.println("---------CONNECTOR OUTBOUND------------");
        for (Map.Entry<String, Features> entry : integratorGlob.getConnectorOutbound().entrySet()) 
        {
            System.out.println(entry.getKey()+"="+entry.getValue().value);
        }
        System.out.println("");

        System.out.println("---------------FIELDS OUT--------------");
        for (Map.Entry<String, Fields> entry : integratorGlob.getFieldsOut().entrySet()) 
        {   System.out.println(entry.getKey());
            System.out.println("\tDefaultValue="+entry.getValue().getDefaultValue());
            System.out.println("\tType="+entry.getValue().getType());
            System.out.println("\tSize="+entry.getValue().getSize());
            System.out.println("");
        }

        System.out.println("-------------FMT PROCESSING------------");
        String FPS=integratorGlob.getFilteringScript().isEmpty()?"not defined":integratorGlob.getFilteringScript();
        System.out.println("filteringScript="+FPS);
        String MPS=integratorGlob.getMappingScript().isEmpty()?"not defined":integratorGlob.getMappingScript();
        System.out.println("mappingScript="+MPS);
        String TPS=integratorGlob.getTransformerScript().isEmpty()?"not defined":integratorGlob.getTransformerScript();
        System.out.println("transformerScript="+TPS);
        System.out.println("---------------------------------------");
        System.out.println("");

        
        System.out.println("-------------POSTPROCESSING------------");
        String SQLPP=integratorGlob.getSQLPostProcessing().isEmpty()?"not defined":integratorGlob.getSQLPostProcessing();
        System.out.println("SQLPostProcessing="+SQLPP);
        System.out.println("---------------------------------------");
        System.out.println("");
        System.out.println("");
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
        
        if (connector.isBlank() || connector.isEmpty() || connector==null) {logger.log(Level.SEVERE, "Connector In, is not defined?");System.exit(2);}
        //typage du connecteur
        switch (connector.toUpperCase()) 
        {
            case "FILE" -> check_file_connector(integrator);
            case "DATABASE" -> check_database_connector_inbound(integrator);
            default -> {
                logger.log(Level.SEVERE,"Incoming connector type is unknown !");
                System.out.println("Incoming connector type is unknown !");
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
        System.out.println("InBound connector, is a file type...");
        logger.log(Level.INFO,"InBound connector, is a file type...");
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
        if (test_a_path(filespath)==false) {logger.log(Level.SEVERE, "The path {0} does''nt existe or is not readable!", filespath);System.out.println("The path "+filespath+" does''nt existe or is not readable!");System.exit(4);} //chemin inexistant...
        
        //lire le chemin de destination
        String destination=getInConnectorInBoundMap(integrator,"backupdestinationpath");
        if (test_a_path(destination)==false) {logger.log(Level.SEVERE, "The path {0} does''nt existe or is not readable!", destination);System.out.println("The path "+filespath+" does''nt existe or is not readable!");System.exit(4);} //chemin inexistant...
        
        //lire et tester un boolean
        String checkfiles=getInConnectorInBoundMap(integrator,"checkfiles");
        if (test_boolean(checkfiles)==false) {logger.log(Level.SEVERE, "The variable ''checkfiles'' is not defined!");System.exit(4);} 
        
        //lire l'extension.
        String exttype=getInConnectorInBoundMap(integrator,"exttype");
        ArrayList<String> lstexttype =new ArrayList<>();
        lstexttype.add("csv"); //liste des extension prise en charge actuellement.
        if (test_string(exttype,lstexttype)==false) {logger.log(Level.SEVERE, "the extension {0} is not supported!", exttype);System.exit(4);} //extension no prise en charge
        
        System.out.println("Test file connector OK");
        System.out.println("Test variables OK");
        logger.log(Level.INFO,"Test file connector OK");
        logger.log(Level.INFO,"Test variables OK");
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
        DBTools dbt=new DBTools(logger);
        String dbdriver=getInConnectorInBoundMap(integrator, "dbdriver");
        String dburl=getInConnectorInBoundMap(integrator, "dburl");
        String dblogin=getInConnectorInBoundMap(integrator, "dblogin");
        String dbpassword=getInConnectorInBoundMap(integrator, "dbpassword");
        
        if (!dbt.connect_db(dbdriver, dburl, dblogin, dbpassword))
        {
            System.out.println("Unable to connect to database!");
            logger.log(Level.SEVERE,"Unable to connect to database!");
            System.exit(5); //database not connected...
        }
        else
        {
            dbt.close_db();
            System.out.println("Database Inbound OK.");
            logger.log(Level.INFO,"Database Inbound OK.");
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
        DBTools dbt=new DBTools(logger);
        String dbdriver=getInConnectorOutBoundMap(integrator, "dbdriver");
        String dburl=getInConnectorOutBoundMap(integrator, "dburl");
        String dblogin=getInConnectorOutBoundMap(integrator, "dblogin");
        String dbpassword=getInConnectorOutBoundMap(integrator, "dbpassword");
        
        if (!dbt.connect_db(dbdriver, dburl, dblogin, dbpassword))
        {
            System.out.println("Unable to connect to outgoing database !");
            logger.log(Level.SEVERE,"Unable to connect to the outgoing database!");
            System.exit(5); //database not connected...
        }
        else
        {
            dbt.close_db();
            System.out.println("Database Outbound OK.");
            logger.log(Level.INFO,"Database Outbound OK.");
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
        
        if (connector.isBlank() || connector.isEmpty() || connector==null) {logger.log(Level.SEVERE, "Connector Out is not define !");System.exit(2);}
        //typage du connecteur
        switch (connector.toUpperCase()) 
        {
            case "FILE" -> check_file_connector(integrator);
            case "DATABASE" -> check_database_connector_outbound(integrator);
            default -> {
                logger.log(Level.SEVERE,"Outgoing connector type is unknown ?");
                System.out.println("Outgoing connector type is unknown ?");
                System.exit(3);
            }
        }
    }

    
    /**********************************************
     * TRaiter l'intégration des données
     * @param jobIntegrator 
     **********************************************/
    private void process_integration_file_to_db(Job jobIntegrator) 
    {
        try {
            //se connecter à la database_outbound
            System.out.println("Connection DataBase OutBound.");
            logger.log(Level.INFO,"Connection DataBase OutBound.");
            
            DBTools dbt=new DBTools(logger);
            dbt.connect_db(
                    getInConnectorOutBoundMap(jobIntegrator, "dbdriver"),
                    getInConnectorOutBoundMap(jobIntegrator, "dburl"),
                    getInConnectorOutBoundMap(jobIntegrator, "dblogin"),
                    getInConnectorOutBoundMap(jobIntegrator, "dbpassword"));
            System.out.println("Connection DataBase OutBound : PASS");
            logger.log(Level.INFO,"Connection DataBase OutBound : PASS");
            
            //tester présence de la table sinon la construire
            String sql="select count(*) from "+getInConnectorOutBoundMap(jobIntegrator, "targetTable");
            
            try{
                System.out.println("Check table OutBound availability");
                logger.log(Level.INFO,"Check table OutBound availability");
                
                ResultSet rs=dbt.getStmt().executeQuery(sql);
                System.out.println("Table "+getInConnectorOutBoundMap(jobIntegrator, "targetTable")+" available.");
                logger.log(Level.INFO, "Table {0} available.", getInConnectorOutBoundMap(jobIntegrator, "targetTable"));
                
            } catch (SQLException ex) 
            {
                //ex.printStackTrace();
                //creer la table car manquante...
                System.out.println("Creation of the table "+getInConnectorOutBoundMap(jobIntegrator, "targetTable"));
                logger.log(Level.INFO, "Creation of the table {0}", getInConnectorOutBoundMap(jobIntegrator, "targetTable"));
                
                //creer_create_Table(jobIntegrator);
                sql=create_destination_table(jobIntegrator);
                
                dbt.getStmt().executeUpdate(sql);
                System.out.println("Table Create : PASS");
                logger.log(Level.INFO,"Table Create : PASS");
            }
                        
            //construire le template de la requête UPSERT
            System.out.println("Preparing the UPSERT template");
            logger.log(Level.INFO,"Preparing the UPSERT template");
            
            String sqlTemplate=create_template_UPSERT(jobIntegrator);
            
            //debut du calcul de traitement
            startIntegrationTime = System.nanoTime();
            
            //traiter les fichiers en entrée
            dbt.getConn().setAutoCommit(false);
            int batchSize=safeParseInt(jobIntegrator.getJobBatchSize(),1);
            
            int nbLignes;
            
            FSTools fst=new FSTools(logger);
            List<String> lstfile=fst.list_files_in_path_with_ext(getInConnectorInBoundMap(jobIntegrator, "filespath"), getInConnectorInBoundMap(jobIntegrator, "exttype"));
            
            for (String fichier:lstfile)
            {
                System.out.println("Start of integration job from : "+fichier);
                logger.log(Level.INFO, "Start of integration job from : {0}", fichier);
                
                nbLignes=1;                
                fst.open_file(fichier);
                int maxLines=fst.get_Nb_Lines_In_This_File(fichier); //count nb of lines in this file...
                
                fst.skip_header(safeParseInt(getInConnectorInBoundMap(jobIntegrator, "jumpheader"),0));
                while(fst.get_read_file_status())
                {
                    String[] col=fst.read_line().split(";");
                    
                    //TODO: transform processing...
                    
                    //TODO: filter processing...
                    
                    String hashCode=hash_code_calculate(concatenate_col(col));
                    
                    sql=replace_template_UPSERT_Value(sqlTemplate,hashCode,col,jobIntegrator);
                    
                    //UPSERT_DATA
                    //dbt.getStmt().executeUpdate(sql);
                    dbt.getStmt().addBatch(sql);
                    
                    nbLignes++; //nombre lignes traitées
                    
                    //forcer l'écriture et décharge le tampon du Batch
                    if (nbLignes % batchSize == 0) 
                    {
                        dbt.getStmt().executeBatch();
                        if (safeParseBool(jobIntegrator.getForceIntermediateCommit(), true)) {dbt.getConn().commit();System.out.println("Batch Commited...");} //intermediate commit...;
                        dbt.getStmt().clearBatch();
                        System.out.println(nbLignes+"/"+maxLines+" Lines processed...");
                    }
                }
                fst.close_file();
                
                //finaliser les dernier enregistrement et faire un commit.
                dbt.getStmt().executeBatch(); //dernnier exce sur le batch
                dbt.getConn().commit(); //dernier commit pour les données du batch
                
                //marquer la fin de l'integration
                endIntegrationTime = System.nanoTime();

                //un peut de log pour l'utilisateur
                System.out.println(nbLignes+" line(s) processed in " + integration_duration());
                System.out.println("End of integration job from : "+fichier);
                logger.log(Level.INFO, "{0} line(s) processed in " + integration_duration(),nbLignes);
                logger.log(Level.INFO, "End of integration job from : {0}", fichier);
                
                //deplacement du fichier en backup
                if (safeParseBool(getInConnectorInBoundMap(jobIntegrator, "suppressfile"),false))
                {
                    //supprimer le fichier
                    System.out.println("Suppress file : " +fichier);
                    logger.log(Level.INFO, "Suppress file : {0}", fichier);
                    fst.delete_file(fichier);
                }
                else
                {
                    //deplacer le fichier vers le dossier backup...
                    System.out.println("Backup file : " +fichier);
                    logger.log(Level.INFO, "Backup file : {0}", fichier);
                    String fichierDst=getInConnectorInBoundMap(jobIntegrator, "backupdestinationpath")+"/"+new File(fichier).getName();
                    fst.move_file_from_to(fichier,fichierDst) ;
                }
                
                //postTraitement SQL sur la bhase de données
                if (dbt.SQLPostProcessing(jobIntegrator.getSQLPostProcessing()))
                {
                    System.out.println("End of Post Processing SQL with no Errors");
                }
                else
                {
                    System.out.println("End of Post Processing SQL with some Errors");
                }
            }
                        
            //se deconnecter de la database outobound
            dbt.close_db();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }
    
    
    /*****************************
     * Convertir en chaine une durée
     * @return 
     *****************************/
    private String integration_duration()
    {
        // Calcul de la durée en nanosecondes
        long durationNano = endIntegrationTime - startIntegrationTime;
        long durationMillis = durationNano / 1_000_000;

        // Conversion en minutes, secondes et millisecondes
        long minutes = durationMillis / (60 * 1000);
        long seconds = (durationMillis / 1000) % 60;
        long millis = durationMillis % 1000;

        return "Execution duration : " +
                           minutes + " minute(s), " +
                           seconds + " second(s), " +
                           millis + " millisecond(s)";
    }
    
    
    /************************************
     *  Safe Parse Integer
     * @param str
     * @return 
     ************************************/
    private int safeParseInt(String str,int defaultValue) 
    {
        if (str == null) {return defaultValue;}
        try {
            //doit etre positif ou = à zero
            if (Integer.parseInt(str,10)>=0) {return Integer.parseInt(str,10);} else {return defaultValue;}
        } catch (NumberFormatException e) {return defaultValue;}
    }


    /************************************
     *  Safe Parse Boolean
     * @param str
     * @return 
     ************************************/
    private boolean safeParseBool(String str,boolean defaultValue) 
    {
        if (str == null) {return defaultValue;}
        try {return Boolean.parseBoolean(str);} catch (NumberFormatException e) {return defaultValue;}
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
       for (String c:col) {resultat=resultat+c;}
       return resultat;
    }

    
    /*****************************************
     * Creer la table automatique a partir des
     * donnees de descriptions qui se trouvent
     * dans la description "fieldsOut"
     * @param jobIntegrator
     * @return 
     ****************************************/
    private String create_destination_table(Job jobIntegrator) 
    {
        String sqlCreateTable;
        
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
    private String create_template_UPSERT(Job jobIntegrator) {
        String template;
        
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
        String champ;
        int num=0;
        for (Map.Entry<String, Fields> entry : jobIntegrator.getFieldsOut().entrySet()) 
        {
            champ="%"+entry.getKey().toLowerCase()+"%";
            sqlReplace=sqlReplace.replaceAll(champ, col[num].replaceAll("'", "''"));
            num++;
        }
        
        //remplacer le hashcode
        sqlReplace=sqlReplace.replaceAll("%hashcode%", hashCode);
        
        
        return sqlReplace;
    }
  
}