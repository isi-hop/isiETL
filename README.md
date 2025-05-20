# isiETL

*A free open source and light Extract Transform Load (ETL) tool*  

**License**  
GPLV3
[License GPLV3](https://www.gnu.org/licenses/gpl-3.0.fr.html#license-text)  
<br> 

**Author**  
TONDEUR Hervé (ISIHOP)
[https://github.com/isi-hop](https://github.com/isi-hop)   

**Description**  
**ENGLISH:**  
    This package is a Java program with a low memory footprint, intended to be used as a small data integrator, 
    more commonly known as an ETL (Extract Transform Load) tool, which can be useful for setting up 
    a lightweight ETL project. Its objective is to be as simple as possible to configure and to retain the 
    power of a quality ETL service for the relational database ecosystem.
    Its ambition in this area is to be as complete as possible. 
        
**FRANCAIS:**  
    Ce package est un programme Java à faible empreinte mémoire, destiné a être utilisé comme un petit intégrateur 
    de données, plus communément appelé outil ETL (Extract Transform Load), celui ci peut être utile pour mettre 
    en place un projet léger ETL. Son objectif est d'être aussi simple que possible à paramétrer et de conserver 
    la puissance d'un service ETL de qualité pour l'écosystème de bases de données relationnelles.
    Il a pour ambition dans ce domaine d'être le plus complet possible.  

## Construct a job  

### General Informations    

**What you can do?**  
//TODO

### Create a job file  
//TODO  

**How to run?**  
- To be able to start a job, it is necessary that you have built it in the form of a YAML file, as mentioned above. You can either specify the full or relative path of this Job file to be executed on the command line when launching the application.  
- isiETL also offers an option to display the parameters on the execution console of your YAML 'Job' file read.   
- As well as an option to display online help.  
 
**CLI parameters, isietl.properties and défault parameters**  

The execution options are read in the following order:  
1. Parameters passed on the CLI.  
2. Parameters from the isietl.properties file which must be located in the same folder as your executable.  
3. default parameters of the program, the job file located in the executable folder, which must be named by default integration.yml and has a value of displayparameter.

**CLI usage :**  
type option --help or -h to get help from CLI.   

```shell
Usage: isiEtl
 -dp,--displayparameters            Displays the parameters of the YAML
                                    'JOB' file.
 -fip,--fileintegratorpath <file>   Path of je 'JOB' file
 -h,--help                          Help on isiEtl
```  
