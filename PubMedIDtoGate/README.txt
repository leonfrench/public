Below are the steps to run the brain region text mining software created by Leon French. The software extracts and normalizes brain region mentions. Further information can be found at http://www.chibi.ubc.ca/whitetext .

Also available in the wiki: https://github.com/leonfrench/public/wiki/WhiteText-installation

Tested on Ubuntu (desktop) and CentOS (server). Installation steps tested on pavnote-05 (ubuntu).

Patches for Windows coming soon.

h3. Setup steps

* download supplement file: https://docs.google.com/open?id=0B3w9lE7AjmJTVHBJcDB1clFUbmVONHFsQkVxXzNZZw (26mb)
* instal GATE, version 5.2.1 recommended but new versions should work too, http://gate.ac.uk/download/
* install maven
* install eclipse
** create M2_REPO variable to point to maven repository (usually ~/.m2)
* download source (from CVS or file)
* run mvn -compile in project home folder
** use Maven to download dependencies and eclipse for compilation
* download and unzip WhiteTextSupplements.zip
* edit WhiteText.properties (in source home folder)
** change/replace all occurances of the ..\WhiteTextSupplements\ folder to the location you unzipped the supplements
** set number of threads to use for tagging (whitetext.max_threads, only used for crossvalidation runs)
** set whitetext.GATE.home to GATE install directory in WhiteText.properties
* Linnaeus - set working directory in properties.conf file 
** found in \WhiteTextSupplements\LinnaeusFiles\
* edit ABAMS.properties
** change/replace all occurances of the ..\WhiteTextSupplements\ folder to the location you unzipped the supplements

h3. Example pipeline

The main settings for these java programs it the corpus that the program will run on. The PubMed corpus contains the manually annotated brain region spans. The PubMedUnseen corpus contains the unannotated abstracts which will be labelled with brain regions and species mentions. To find these files just press ctrl+shift+r in eclipse.

* add PMIDs to Unseen corpus - GetAll.java
** see below for details
* tokenize - manually in GATE (PubMed and PubMedUnseen corpus)
** see "Loading abstracts to GATE" below for details
* run genia - TreeTaggerRunner.java (PubMed and PubMedUnseen corpus)
* run TokenTargetLabeller.java to label PubMed corpus
* tag brain regions - MalletQuick.java (may require more memory for java -Xmx3000m or so)
* abbreviation tagging expansion - AbbreviationEditor.java (optional)
* tag species - LinnaeusSpeciesTagger.java (may require more memory for java -Xmx3000m or so)
* output results and normalize regions - PrintAndResolveBrainRegions.java
** remove some lexicons to speed up (comment out some lexiconModel.add*())
** use this program as a guide to extract and reformat the results for your purposes

h4. Loading abstracts to GATE

# download WhiteText corpus from http://www.chibi.ubc.ca/whitetext
# set whitetext.datastore.location in WhiteText.properties to the unzipped corpus file location <path>/WhiteTextGATEDataStoreV1.3/
# to download from NCBI webservices open GetAll.java
## modify either loadFromQuery or loadFromList methods to load a list of PMID's or download based on a Pubmed query
## wait and check statistics as it loads blocks of 100 abstracts
# to load from already downloaded pubmed XML use PubMedLoadFromXML.java (currently setup to load lists of PMID's from MScanner.
# upon completion it will print out the distribution of journals loaded
# open up the datastore (File->Datastore->Open Datastore) in the GATE GUI to check the size and status of the PubMedUnseen corpus

h4. Tokenization

# open Gate the datastore, the corpus you wish to tokenize
# File -> New processing resource -> Add ANNIE...
# File -> New processing resource -> ANNIE English Tokeniser
# Set name to tokenizer
# File -> New processing resource -> ANNIE Sentence Splitter
# Set name to splitter
# File -> new application -> Corpus pipeline
# Set name to tokenizer pipeline
# Click on the tokenizer processor then on ">>"
# Under the annotationSetName parameter set the value to "GATETokens"
# Click on the splitter processor then on ">>"
# Under the inputASName and outputASName parameters set the value to "GATETokens"
# in the Corpus pulldown select PubMedUnseen or PubMed
# Click run this application
# Only run once, things will break if tokens are added twice (just browse some abstracts when finished to check annotations)
