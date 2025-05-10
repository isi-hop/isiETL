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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


/**
 *
 * @author tondeur-h
 */


public class tools {
    //variables globales
    String fileIntegratorPath="";
    boolean connected=false;
    String dburl="";
    String dblogin="";
    String dbpassword="";
    String csvPath="";
    String csvPathdst="";
    //int nbSautLigne=1;
    int numLigneEnCours=0;
    boolean deplacer=false;
    boolean controle_entete=false;
    int type_entete=1;
    boolean debug=false;
    boolean purgerDataNonSolicite=false;
    
    //ouverture et lecture fichier
    FileReader fr;
    BufferedReader br;
    List<String> listCsv=new ArrayList<>();
    String deleteSQL="";
    String laLigne="";  //ligne en cour de lecture
    
    //database
    Connection conn;
    Statement stmt;
    
    //logs
       private static final  Logger logger = Logger.getLogger(tools.class.getName());
    

    /****************
     * Constructeur 
     ****************/
    public tools(){}

    /****************************
     * Connecter la DB
     * Si non possible pas de traitement
     * @return boolean
     ****************************/
    private boolean connect_db() {
        boolean isconnected=false;
        try {
            Class.forName("org.postgresql.Driver");
       
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
    private void close_db() {
        try {
            stmt.close();
            conn.close();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }

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
    public Integrator lire_fichier_integration() {
        InputStream inputStream=null;
        Integrator integrator=null;
        try {
            //Yaml yaml=new Yaml();
            Yaml yaml = new Yaml(new Constructor(Integrator.class, new LoaderOptions()));
            inputStream = new FileInputStream(new File(fileIntegratorPath));
            integrator = yaml.load(inputStream);
            
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
        
        return integrator;
        
    }



    /********************
     * Fermer le fichier source
     ********************/
    private void fermer_fichier() {
        try {
            br.close();
            fr.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }

    
    /**********************
     * Lire une ligne du CSV
     * @return 
     **********************/
    private String lecture_ligne() {
        String ligne="";
        try {
            ligne=br.readLine();
            numLigneEnCours++;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        if (ligne.isBlank() || ligne.isEmpty()) {ligne="vide";}
        
        return ligne;
    }

    
    /*********************
     * Ouvrir fichier csv
     * prépa lecture
     * @param csvPath
     * @return 
     **********************/
    private boolean ouvrir_fichier(String csvPath) {
        boolean okfile=false;
        try {
            File f=new File(csvPath);
            if (f.exists())
            {
                fr=new FileReader(csvPath);
                br=new BufferedReader(fr);
                okfile=true;
            }
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
        return okfile;
    }

    
    /******************************
     * Lister tous les fichiers CSV
     * du dossier local fichiers
     ******************************/
    private void lister_les_fichiers_csv() {       
    File[] filesInDirectory = new File(csvPath).listFiles();
    for(File f : filesInDirectory)
    {
        String filePath = f.getAbsolutePath();
        String fileExtenstion = filePath.substring(filePath.lastIndexOf(".") + 1,filePath.length());
        if("csv".equals(fileExtenstion)){listCsv.add(filePath);}
    }       
    }

    
    /*******************************
     * Inserer les données de la ligne
     * @param vg 
     *******************************/
    /*
    private void insert_ligne(Valorisation_Glims vg) {
        try {
            
            String hashCode=calcul_code_Hashage(vg);
            
            //Construction de la requête...
            String sql="";
            
            sql="""
                       INSERT INTO public.valorisations_glims
                       ("Automate", "Date_valorisation", "Institution", 
                       "Code UF", "Prescripteur2", "Objet_patient", "No Int", 
                       "Date prvt", "Type IEP", "No IEP", "No Ext", "Code Cotation", 
                       "Labo. Exec.", "Forfait Deloc", "Non Deloc", "Forfait non inclu", 
                       "Inclu", "Urgence", "COVID", "Non COVID", 
                       "AMI", "B", "E", "HN", "HN_LC", "HN_RIHN", 
                       "KB", "PB", "TB", "Non Urgence", uf, hashcode) 
                       """;                 
                    sql=sql+ "VALUES('"+vg.getAutomate()+"','"+vg.getDate_valorisation()+"','"+vg.getInstitution()+"',";
                    sql=sql+"'"+vg.getCodeUF()+"','"+vg.getPrescripteur2()+"','"+vg.getObjet_patient()+"','"+vg.getNoInt()+"',";
                    sql=sql+"'"+vg.getDateprvt()+"','"+vg.getTypeIEP()+"','"+vg.getNoIEP()+"','"+vg.getNoExt()+"','"+vg.getCodeCotation()+"',";
                    sql=sql+"'"+vg.getLaboExec()+"','"+vg.getForfaitDeloc()+"','"+vg.getNonDeloc()+"','"+vg.getForfaitnoninclu()+"',";
                    sql=sql+"'"+vg.getInclu()+"','"+vg.getUrgence()+"','"+vg.getCOVID()+"','"+vg.getNonCOVID()+"',"; 
                    sql=sql+"'"+vg.getAMI()+"','"+vg.getB()+"','"+vg.getE()+"','"+vg.getHN()+"','"+vg.getHN_LC()+"','"+vg.getHN_RIHN()+"',";
                    sql=sql+"'"+vg.getKB()+"','"+vg.getPB()+"','"+vg.getTB()+"','"+vg.getNonUrgence()+"','"+vg.getUf()+"','"+hashCode+"') ";
                    sql=sql+"ON CONFLICT (hashcode) DO UPDATE SET ";                    
                    sql=sql+"\"Automate\"='"+vg.getAutomate()+"', \"Date_valorisation\"='"+vg.getDate_valorisation()+"', \"Institution\"='"+vg.getInstitution()+"', ";
                    sql=sql+"\"Code UF\"='"+vg.getCodeUF()+"', \"Prescripteur2\"='"+vg.getCodeUF()+"', \"Objet_patient\"='"+vg.getObjet_patient()+"', \"No Int\"='"+vg.getNoInt()+"', ";
                    sql=sql+"\"Date prvt\"='"+vg.getDateprvt()+"', \"Type IEP\"='"+vg.getTypeIEP()+"', \"No IEP\"='"+vg.getNoIEP()+"', \"No Ext\"='"+vg.getNoExt()+"', \"Code Cotation\"='"+vg.getCodeCotation()+"', ";
                    sql=sql+"\"Labo. Exec.\"='"+vg.getLaboExec()+"', \"Forfait Deloc\"='"+vg.getForfaitDeloc()+"', \"Non Deloc\"='"+vg.getNonDeloc()+"', \"Forfait non inclu\"='"+vg.getForfaitnoninclu()+"', \"Inclu\"='"+vg.getInclu()+"', ";
                    sql=sql+"\"Urgence\"='"+vg.getUrgence()+"', \"COVID\"='"+vg.getCOVID()+"', \"Non COVID\"='"+vg.getNonCOVID()+"', \"AMI\"='"+vg.getAMI()+"', \"B\"='"+vg.getB()+"', \"E\"='"+vg.getE()+"', \"HN\"='"+vg.getHN()+"', ";
                    sql=sql+"\"HN_LC\"='"+vg.getHN_LC()+"', \"HN_RIHN\"='"+vg.getHN_LC()+"', \"KB\"='"+vg.getKB()+"', \"PB\"='"+vg.getPB()+"', \"TB\"='"+vg.getTB()+"', \"Non Urgence\"='"+vg.getNonUrgence()+"', uf='"+vg.getUf()+"'";
                    
            //if (debug==true) {System.out.println(sql);}
                    
            stmt=conn.createStatement();
            stmt.executeUpdate(sql);
            if (debug==true) {System.out.println(" : OK");}

        } catch (Exception ex) {
             if (debug==true) {System.out.println(" : :-( KO");}
            logger.log(Level.SEVERE, "NUM ligne : {0} # {1}", new Object[]{numLigneEnCours, ex.getMessage()});
            logger.log(Level.SEVERE, "Erreur sur : {0}", laLigne);
        }
    }
    */
    
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
    private String cvtTopgDate(String strDate) {
        return "20"+strDate.substring(6)+"-"+strDate.substring(3,5)+"-"+strDate.substring(0, 2);
    }

 
    /*****************************
     * Supprimer le fichier traité.
     * @param cheminFichier 
     *****************************/
    private void supprimer_fichier(String cheminFichier) {
        File f=new File(cheminFichier);
        f.delete();
    }
 
    /*****************************
     * déplacer le fichier tra ité
     * @param cheminFichier 
     *****************************/
    private void deplacer_fichier(String cheminFichierSrc, String cheminFichierDest) {
        try {            
            Files.move(Paths.get(cheminFichierSrc), Paths.get(cheminFichierDest),REPLACE_EXISTING);
        } catch (IOException ex) {
            if (debug==true) {System.out.println("Erreur de d\u00e9placement du fichier source : "+cheminFichierSrc);}
            logger.log(Level.SEVERE, ex.getMessage());
            logger.log(Level.SEVERE, "Erreur de d\u00e9placement du fichier source : {0}", cheminFichierSrc);
        }
    }

    /*****************************
     * Calculer un code de hashage en SHA256
     * @param vg
     * @return 
     *****************************/
    /*
    private String calcul_code_Hashage(HashMap vg) {
        StringBuilder hash=new StringBuilder();
        //input concaténé
        String input=vg.getNoIEP()+vg.getNoInt()+vg.getCodeCotation()+vg.getDateprvt();
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
    */

    
        /*************************
     * Exploitation du fichier inegrator.yml
     */
    public void exploit_integrator(Integrator integratorGlob) {
        System.out.println("Checkfile : "+integratorGlob.getCheckfiles());
        System.out.println("Connector : "+integratorGlob.getConnector());
        System.out.println("Filespath : "+integratorGlob.getFilespath());
        System.out.println("ExtType : "+integratorGlob.getExttype());
        System.out.println("Destination : "+integratorGlob.getDestination());
        System.out.println("-------------DATABASE-----------");
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
            Logger.getLogger(isietl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }

}