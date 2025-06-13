<center><h2>IsiETL TUTORIALS</h2></center>  

**First example** 

We're going to go to the `$ISIETL_HOME/tutorials/tuto_1` folder, which contains the first example of a csv file we're going to integrate into a PosgreSQL database.

This csv file is fairly basic, containing dummy data in 4 columns and 12 rows with no header.

_Extract from file_  
`tuto_1_no_head_4_col.csv`  

|C1|C2|C3|C4|
|-----|-----|-----|-----|
|col11|col12|col13|col14|
|col21|col22|col23|col24|
|col31|col32|col33|col34|
|col41|col42|col43|col44|
|col51|col52|col53|col54|
|col61|col62|col63|col64|
|col71|col72|col73|col74|
|col81|col82|col83|col84|
|col91|col92|col93|col94|
|col101|col102|col103|col104|
|col111|col112|col113|col114|
|col121|col122|col123|col124|  

The aim of this tutorial is to automatically integrate the data from columns C1 to C4 into a table we'll call `tabletest`, using the default varchar type for each column, with a maximum length of 6 characters.

**Let's get started!**  

Let's start by creating a job file template.  

on the CLI, run the command 
$> `isietl.sh -jt`.

You'll get the file `ìntegrator_template.yml` in the local folder, rename it `integrator_tuto_1.yml`.  

$> `mv integrator_template.yml integrator_tuto_1.yml`  

> - In what follows, we'll assume that you've installed IsiETL in the `$HOME\isietl` folder. In the files that follow, you'll replace the $HOME path with the absolute path of your personal installation.  
> - We'll also assume that we've launched a postgresql database on the workstation, ideally under docker, see the following link on how to launch a postgresql database under docker .  
> - We'll also assume that when we start postgresql, we'll have created a database named `tuto1`  
> - Finally, to set up your database login and password, you'll need to adapt the following information to your configuration.

[How to use the docker Postgresql official image](https://www.docker.com/blog/how-to-use-the-postgres-docker-official-image/)  

Edit the `integrator_tuto_1.yml` file with your favorite text file editor, like this : 

``` yaml  
#-------------HEADER---------------
jobName: "tuto_1 integrator process"
jobDescription: "Get data from a CSV file : push a DB postgresql"
jobDateTime: "2025-06-11 07:45"
jobBatchSize: "20"
forceIntermediateCommit: "true"
#----------------------------------------
#------------INBOUND CONNECTOR-----------
connectorInbound:
  connectortype: 
    value: "file"
  filespath: 
    value : "$HOME/isiETL/tutorial/tuto_1"
  checkfiles: 
    value: "true"
  backupdestinationpath:
    value: "$HOME/isiETL/tutorial/backup"
  exttype:
    value: "csv"    
  nbfields:
    value: "4"
#----------------------------------------
#------------OUTBOUND CONNECTOR----------
connectorOutbound: 
  connectortype:
    value: "database"
  dbdriver:
    value: "org.postgresql.Driver"
  dburl:
    value: "jdbc:postgresql://localhost:5432/tuto1"
  dblogin:
    value: "postgres"
  dbpassword:
    value: "admin"
  targetTable:
    value: "tabletest"    
  ignoreErrors: 
    value: "false"
  ignoreDuplicates:
    value: "false"

fieldsOut:
   colname1:
    defaultValue: ""
    size: "6"
    type: "varchar"
  colname2:
    defaultValue: ""
    size: "6"
    type: "varchar"
  colname3:
    defaultValue: ""
    size: "6"
    type: "varchar"
  colname4:
    defaultValue: ""
    size: "6"
    type: "varchar"
#-------------------------------------
#-----------FMT PROCESSING------------
filteringScript: ""
mappingScript: ""
transformerScript: ""
#-------------------------------------

#-----------POSTPROCESSING------------
SQLPostProcessing: ""
#-------------------------------------
```  

**Let's unravel the file above!**  

🚧️Under construction🚧️  

**_First block of information, the header_**  


**Incoming connector in CSV format_**  


**Outgoing connector in BDD postgresql format_**  


**_Destination fields description_**  


**FMT processing_**  


**_POST SQL processing_**





