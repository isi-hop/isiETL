package org.isihop.fr.isietl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 *
 * @author tondeur-h
 */

/*
CREATE TABLE valorisations_glims (
	"Automate" varchar(120) NULL,
	"Date_valorisation" timestamp NULL,
	"Institution" varchar(30) NULL,
	"Code UF" varchar(16) NULL,
	"Prescripteur2" varchar(120) NULL,
	"Objet_patient" varchar(120) NULL,
	"No Int" varchar(30) NULL,
	"Date prvt" timestamp NULL,
	"Type IEP" varchar(2) NULL,
	"No IEP" varchar(16) NULL,
	"No Ext" varchar(16) NULL,
	"Code Cotation" varchar(8) NULL,
	"Labo. Exec." varchar(120) NULL,
	"Forfait Deloc" varchar(120) NULL,
	"Non Deloc" varchar(120) NULL,
	"Forfait non inclu" varchar(120) NULL,
	"Inclu" varchar(120) NULL,
	"Urgence" varchar(120) NULL,
	"COVID" varchar(120) NULL,
	"Non COVID" varchar(120) NULL,
	"AMI" numeric NULL,
	"B" numeric NULL,
	"E" numeric NULL,
	"HN" numeric NULL,
	"HN_LC" numeric NULL,
	"HN_RIHN" numeric NULL,
	"KB" numeric NULL,
	"PB" numeric NULL,
	"TB" numeric NULL,
	"Non Urgence" varchar(120) NULL,
	uf varchar(16) NULL,
	dateintegration timestamp DEFAULT now() NULL,
	hashcode varchar NOT NULL,
	CONSTRAINT valorisations_glims_pk PRIMARY KEY (hashcode)
);
*/

public class isietl {
    //variables globales
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
       private static final  Logger logger = Logger.getLogger(isietl.class.getName());
    

    public static void main(String[] args) {
       new isietl();
    }

    /****************
     * Constructeur 
     ****************/
    public isietl() 
    {        
        try 
            {
                logger.setUseParentHandlers(false);
                FileHandler fh = new FileHandler("jic_glims%g.log", 0, 1, true);
                fh.setLevel(Level.ALL);
                fh.setFormatter(new SimpleFormatter());
                logger.addHandler(fh);
            } catch (IOException ioe) {
                Logger.getLogger(isietl.class.getName()).log(Level.SEVERE, null, ioe);
            }
        
        
        //lire les properties du système
        lire_properties();
        
        //connecter la DB
        connected=connect_db();
        
        //boucle de sondage
        if (connected==true)  //connected retourné par connect_db
        {
            if (debug==true) {System.out.println("Connecté à la database...");}
            logger.log(Level.INFO, "Connecté à la database : OK");
            
            try {
                lister_les_fichiers_csv();
                
                for (String fichiercsv:listCsv)
                {
                    if (debug==true) {System.out.println("Traitement fichier "+ fichiercsv);}
                    logger.log(Level.INFO, "Traitement fichier {0}", fichiercsv);
                    //lecture du fichier CSV
                    ouvrir_fichier(fichiercsv);
                    //supprimer les premières lignes (voir properties)
                    detecter_format_fichier();
                     
                    if (controle_entete==true)
                    {
                        //pour chaque ligne du fichier à partir de la position actuelle
                        while (br.ready())
                        {
                            //lire la ligne

                            laLigne=lecture_ligne();
                            if (laLigne.compareToIgnoreCase("vide")!=0)
                            {
                                //traiter les différents champs
                                Valorisation_Glims vg=traiter_donnees_ligne(laLigne);
                                //inserer la ligne quelquesoit la situation.
                                insert_ligne(vg);      
                                if (debug==true) {System.out.print("Insert ligne : "+numLigneEnCours);}
                            }
                        }
                    }
                    
                    if (debug==true) {System.out.println("\r\nfin du traitement");}
                    logger.log(Level.INFO, "Fin du traitement");
                   fermer_fichier();
                   
                   //déplacer ou supprimer le fichier...
                   if (deplacer==true) 
                                  {
                                    if (debug==true) {System.out.println("Deplacement du fichier "+fichiercsv+" vers "+csvPathdst+"/"+fichiercsv.substring(fichiercsv.lastIndexOf("\\")+1));}
                                    logger.log(Level.INFO, "Deplacement du fichier {0} vers {1}/{2}", new Object[]{fichiercsv, csvPathdst, fichiercsv.substring(fichiercsv.lastIndexOf("\\")+1)});
                                    deplacer_fichier(fichiercsv, csvPathdst+"/"+fichiercsv.substring(fichiercsv.lastIndexOf("\\")+1));
                                  } 
                   else 
                                  {
                                      if (debug==true) {System.out.println("Suppression du fichier "+fichiercsv);}
                                      logger.log(Level.INFO, "Suppression du fichier {0}", fichiercsv);
                                      supprimer_fichier(fichiercsv);
                                  }
                   
                   //purger les données non solicitées.
                   if (purgerDataNonSolicite==true){
                        if (debug==true) {System.out.println("Purge des données non solicités");}
                        purger_donnees_non_solicites();
                        logger.log(Level.INFO, "Purge des données non solicités");
                   }
                }
                
                //fermer la DB en fin d'intégration
                close_db();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
        }
    }

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
     *********************************/
    private void lire_properties() 
    {
        //recuperer le chemin local et le nom de l'application
        //le fichier properties doit comporter le même
        //nom que l'application.
        String currentPath=System.getProperty("user.dir");
        String programName=this.getClass().getSimpleName();
        FileInputStream is=null;
        
        try {
            is = new FileInputStream(currentPath+"/"+programName+".properties");
            Properties p=new Properties();
            try {
                //charger le fichier properties
                p.load(is);
                //lecture des variables
                dburl=p.getProperty("dburl", "jdbc:postgresql://vm296.ch-v.net:5432/shab");
                dblogin=p.getProperty("dblogin", "postgres");
                dbpassword=p.getProperty("dbpassword", "password");
                csvPath=p.getProperty("csvpath","e:/echanges/");
                deplacer=Boolean.parseBoolean(p.getProperty("deplacer", "false"));
                csvPathdst=p.getProperty("csvpathdst","e:/echanges/");
                debug=Boolean.parseBoolean(p.getProperty("debug", "false"));
                purgerDataNonSolicite=Boolean.parseBoolean(p.getProperty("purgerDataNonSolicite", "false"));
                //si le parametre ci dessus est true, alors celui ci doit être défini
                deleteSQL=p.getProperty("deletesql","e:/script/deletes.sql");
                
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

    /***************************************
     * Sauter un certains nombre de lignes.
     ***************************************/
    private void detecter_format_fichier() {
         controle_entete=false;
        //for (int l=0;l<nbSautLigne;l++)
        //{
                numLigneEnCours=0;
                while (controle_entete==false)
            {
                try {
                    String ligne=br.readLine();
                    numLigneEnCours++;
                    if (ligne.compareTo("Institution;Code UF;Prescripteur2;Objet/patient;No Int;Date prvt;Type IEP;No IEP;No Ext;Code Cotation;Labo. Exec.;Forfait Deloc;Non Deloc;Forfait non inclu;Inclu;Non Urgence;Urgence;COVID;Non COVID;AMI;B;E;HN;HN_LC;HN_RIHN;KB;PB;TB")==0)
                    {
                       controle_entete=true;
                       type_entete=1; //complet
                    }  
                    if (ligne.compareTo("Institution;Code UF;Prescripteur2;Objet/patient;No Int;Date prvt;Type IEP;No IEP;No Ext;Code Cotation;Labo. Exec.;Forfait Deloc;Non Deloc;Forfait non inclu;Inclu;Non Urgence;Urgence;COVID;Non COVID;AMI;B;E;HN;HN_LC;HN_RIHN;PB;TB")==0)
                    {
                       controle_entete=true; 
                       type_entete=2; //pas de KB
                    }     
                    if (ligne.compareTo("Institution;Code UF;Prescripteur2;Objet/patient;No Int;Date prvt;Type IEP;No IEP;No Ext;Code Cotation;Labo. Exec.;Forfait Deloc;Non Deloc;Forfait non inclu;Inclu;Non Urgence;Urgence;COVID;Non COVID;AMI;B;E;HN;HN_LC;HN_RIHN;TB")==0)
                    {
                       controle_entete=true;
                       type_entete=3; //pas de KB, pas de PB
                    }     
                    if (debug==true){System.out.println(ligne);}
                    logger.log(Level.INFO, ligne);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, ex.getMessage());
                }
            }
        //si le contrôle d'entête est impossible à réaliser alors quitter l'application.
        if (controle_entete==false)
        {
            if (debug==true){System.out.println("Type d'entête non reconnu !");}
            logger.log(Level.SEVERE,"Type d'entête non reconnu !");
            System.exit(1);
        }
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
    
    
    /*******************************
     * purger des lignes inutiles
     * @param vg 
     *******************************/
    private void purger_donnees_non_solicites() 
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
    
    
    /*****************************
     * TRaiter les données de la ligne...
     * @param localLigne
     * @return 
     *****************************/
    private Valorisation_Glims traiter_donnees_ligne(String localLigne) 
    {
        
        Valorisation_Glims vg=new Valorisation_Glims();
        try{

        //split format complet
        //Institution;Code UF;Prescripteur2;Objet/patient;
        //No Int;Date prvt;Type IEP;No IEP;No Ext;Code Cotation;
        //Labo. Exec.;Forfait Deloc;Non Deloc;Forfait non inclu;
        //Inclu;Non Urgence;Urgence;COVID;Non COVID;AMI;
        //B;E;HN;HN_LC;HN_RIHN;KB;PB;TB
        
        //Pré Transformations
        localLigne=localLigne.replaceAll(";-", ";0");
        localLigne=localLigne.replaceAll("\\.00", "");
        localLigne=localLigne.replaceAll("'", "");
        localLigne=localLigne.replaceAll("\\*", "");
        //split
        String[] splitData=localLigne.split(";");
        
        int indice=0;
        
         //transformations complémentaire
        //pos:5 dateprv
        splitData[indice+5]=cvtTopgDate(splitData[indice+5]);
        
        //désérialisation
        vg.setAutomate(""); //a revoir
        vg.setDate_valorisation(splitData[indice+5]); //a revoir car idem à date prelevement
       
        try{
            vg.setInstitution(splitData[indice]);indice++; //0
        }catch (Exception e) {vg.setInstitution("I_UNKNOWN");}

        try{
            vg.setCodeUF(splitData[indice]);indice++; //1
        }catch (Exception e) {vg.setCodeUF("UNKN");}
        
        try{
            vg.setPrescripteur2(splitData[indice]);indice++; //2
        }catch (Exception e) {vg.setPrescripteur2("UNKNOWN");}
        
        try{
            vg.setObjet_patient(splitData[indice]);indice++; //3
        }catch (Exception e) {vg.setObjet_patient("UNKNOWN");}
        
        try{
            vg.setNoInt(splitData[indice]);indice++; //4
        }catch (Exception e) {vg.setNoInt("0000000000");}
        
        //TODO : ne devrait pas être impacté
        vg.setDateprvt(splitData[indice]);indice++; //5
        
        try {
            vg.setTypeIEP(splitData[indice]);indice++; //6
        }catch (Exception e) {vg.setTypeIEP("U");}
        
        try{
            vg.setNoIEP(splitData[indice]);indice++;//7
        }catch (Exception e) {vg.setNoIEP("0");}
        
        try{
            vg.setNoExt(splitData[indice]);indice++; //8
        }catch (Exception e) {vg.setNoExt("0000000000");}
        
        try{
            vg.setCodeCotation(splitData[indice]);indice++;//9
        }catch (Exception e) {vg.setCodeCotation("0000");}
            
        try{
            vg.setLaboExec(splitData[indice]);indice++;//10
        }catch (Exception e) {vg.setLaboExec("UNKNOWN");}
        
        try{
            vg.setForfaitDeloc(splitData[indice]);indice++;//11
        }catch (Exception e) {vg.setForfaitDeloc("0");}
        
        try{
            vg.setNonDeloc(splitData[indice]);indice++;//12
        }catch (Exception e) {vg.setNonDeloc("0");}
        
        try{
            vg.setForfaitnoninclu(splitData[indice]);indice++;//13
        }catch (Exception e) {vg.setForfaitnoninclu("0");}
        
        try{
            vg.setInclu(splitData[indice]);indice++;//14
        }catch (Exception e) {vg.setInclu("0");}
        
        try{
            vg.setNonUrgence(splitData[indice]);indice++;//15
        }catch (Exception e) {vg.setNonUrgence("0");}
        
        try{
            vg.setUrgence(splitData[indice]);indice++;//16
        }catch (Exception e) {vg.setUrgence("0");}
        
        try{
            vg.setCOVID(splitData[indice]);indice++;//17
        }catch (Exception e) {vg.setCOVID("0");}
        
        try{
            vg.setNonCOVID(splitData[indice]);indice++;//18
        }catch (Exception e) {vg.setNonCOVID("0");}
        
        try{
            vg.setAMI(Float.parseFloat(splitData[indice]));indice++;//19
        }catch (Exception e) {vg.setAMI(0);}

        try{
            vg.setB(Integer.parseInt(splitData[indice],10));indice++;//20
        }catch (Exception e) {vg.setB(0);}
        
        try{
            vg.setE(Integer.parseInt(splitData[indice],10));indice++;//21
        }catch (Exception e) {vg.setE(0);}

        try{
            vg.setHN(Integer.parseInt(splitData[indice],10));indice++;//22
        }catch (Exception e) {vg.setHN(0);}
        
        try{
            vg.setHN_LC(Float.parseFloat(splitData[indice]));indice++;//23
        }catch (Exception e) {vg.setHN_LC(0);}
        
        try{
            vg.setHN_RIHN(Float.parseFloat(splitData[indice]));indice++;//24
        }catch (Exception e) {vg.setHN_RIHN(0);}
        
        /*=====================*/
        //si entête==1 => complet
        if (type_entete==1)
        {
            try{vg.setKB(Float.parseFloat(splitData[indice]));indice++;/*25 pour KB*/}catch (Exception e) {vg.setKB(0); /*sinon KB=0*/}
            try{vg.setPB(Float.parseFloat(splitData[indice]));indice++;/*26*/}catch (Exception e) {vg.setPB(0);}
            try{vg.setTB(Float.parseFloat(splitData[indice]));indice++;/*27*/}catch (Exception e) {vg.setTB(0);}
        }
        
        //si entete==2 => KB=0
        if(type_entete==2)
        {
            vg.setKB(0);
            try{vg.setPB(Float.parseFloat(splitData[indice]));indice++;/*25*/}catch (Exception e) {vg.setPB(0);}
            try{vg.setTB(Float.parseFloat(splitData[indice]));indice++;/*26*/}catch (Exception e) {vg.setTB(0);}
        }
        
        //si entete==3 => KB=0, PB=0
        if (type_entete==3)
        {
            vg.setKB(0);
            vg.setPB(0);
            try{vg.setTB(Float.parseFloat(splitData[indice]));indice++;/*25*/}catch (Exception e) {vg.setTB(0);}
        }
        
        /*=====================*/
        try{
            vg.setUf(toUF(splitData[1]));
        }catch (Exception e) {vg.setUf("0000");}
        
        
        } catch(Exception ex){
            logger.log(Level.SEVERE, "NUM ligne : {0}   {1}", new Object[]{numLigneEnCours, ex.getMessage()});
            logger.log(Level.SEVERE, "Erreur sur : {0}", laLigne);
            logger.log(Level.SEVERE, "convertie comme : {0}", localLigne);
        }
        return vg;
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

    /***************************
     * Convertir les UF selon le cas
     * @param string
     * @return 
     **************************/
    private String toUF(String struf) {
    //Si commence par LQ DE AR HA JE
    if (struf.startsWith("LQ")) {return struf.substring(2);}
    if (struf.startsWith("DE")) {return struf.substring(2);}
    if (struf.startsWith("AR")) {return struf.substring(2);}
    if (struf.startsWith("HA")) {return struf.substring(2);}
    if (struf.startsWith("JE")) {return struf.substring(2);}
    
    //si commence par A V G M C
    if (struf.startsWith("A")) {return struf.substring(1);}
    if (struf.startsWith("V")) {return struf.substring(1);}
    if (struf.startsWith("G")) {return struf.substring(1);}
    if (struf.startsWith("M")) {return struf.substring(1);}
    if (struf.startsWith("C")) {return struf.substring(1);}

    //defaut
    return struf;
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
    private String calcul_code_Hashage(Valorisation_Glims vg) {
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
    
}