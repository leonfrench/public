/*
 * The WhiteText project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.pubmedgate.resolve;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.Vocabulary;
import ubic.BAMSandAllen.AllenDataLoaders.AllenMajorMatrices;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSDataLoader;
import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.io.excel.ExcelUtil;
import ubic.basecode.ontology.OntologyLoader;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.NeuroNamesLoader;
import ubic.pubmedgate.NeuroNamesMouseAndRatLoader;
import ubic.pubmedgate.organism.SpeciesLoader;
import ubic.pubmedgate.resolve.depreciated.BIRNLexResolver;
import ubic.pubmedgate.resolve.depreciated.ResolveBrianRegions;
import ubic.pubmedgate.resolve.focusedAnalysis.SynonymSchema;
import au.com.bytecode.opencsv.CSVReader;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.org.apache.xerces.internal.parsers.XIncludeAwareParserConfiguration;

public class MakeLexiconRDFModel {
    protected static Log log = LogFactory.getLog( MakeLexiconRDFModel.class );
    Model model;

    public MakeLexiconRDFModel() {
        model = ModelFactory.createDefaultModel();
        model.setNsPrefix( "lexiconLinks", Vocabulary.getLexiconURI() );
        model.setNsPrefix( "TermSpace", Vocabulary.getLexiconSpaceURI() );
    }

    public void setModel( Model model ) {
        this.model = model;
    }

    public void writeOut() throws Exception {
        writeOut( Config.config.getString( "resolve.Lexicon.RDF" ) );
    }

    public void writeOut( String filename ) throws Exception {
        model.write( new FileWriter( filename ) );
    }

    /*
     * very rough way to load in brede database entries, testing only
     */public void addBredeNodes() throws Exception {
        // http://neuro.imm.dtu.dk/services/brededatabase/worois.xml

        // InputSource src = new InputSource( new FileInputStream( xmlFile ) );
        XIncludeAwareParserConfiguration config = new XIncludeAwareParserConfiguration();
        DOMParser prsr = new org.apache.xerces.parsers.DOMParser();
        prsr.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );

        prsr.parse( Vocabulary.getBredeURI() );
        Document XMLdocument = prsr.getDocument();

        NodeList l = XMLdocument.getElementsByTagName( "Roi" );
        for ( int i = 0; i < l.getLength(); i++ ) {
            Node node = l.item( i );
            NodeList l2 = node.getChildNodes();
            String woroi;

            Resource mainConcept = null;
            for ( int ii = 0; ii < l2.getLength(); ii++ ) {
                Node childNode = l2.item( ii );
                if ( childNode.getNodeName().equals( "woroi" ) ) {
                    // make the concept
                    woroi = AllenMajorMatrices.unmarshallText( childNode );
                    mainConcept = model.createResource( Vocabulary.getBredeURI() + woroi );
                    mainConcept.addProperty( RDF.type, Vocabulary.bredeName );
                    // log.info( woroi );
                }

                if ( childNode.getNodeName().equals( "name" ) || childNode.getNodeName().equals( "variation" ) ) {
                    String name = AllenMajorMatrices.unmarshallText( childNode );
                    name = name.toLowerCase();
                    if ( name.startsWith( "left" ) ) {
                        name = name.replaceFirst( "left ", "" );
                    }
                    if ( name.startsWith( "right" ) ) {
                        name = name.replaceFirst( "right ", "" );
                    }

                    Resource r = Vocabulary.makeNeurotermNode( name, model );

                    // link to concept
                    if ( childNode.getNodeName().equals( "name" ) ) {
                        // add its label if its the name tag
                        mainConcept.addLiteral( RDFS.label, name );
                        mainConcept.addProperty( Vocabulary.has_label_term, r );
                    }
                    if ( childNode.getNodeName().equals( "variation" ) ) {
                        mainConcept.addProperty( Vocabulary.has_synonym_term, r );
                    }
                }
            }
        }
    }

    public void addNN2007Nodes() throws Exception {
        NeuroNamesMouseAndRatLoader NNMR = new NeuroNamesMouseAndRatLoader();
        NNMR.addToModel( model );
    }

    @Deprecated
    public void addBIRNLexNodes() throws Exception {
        BIRNLexResolver birnLex = new BIRNLexResolver();
        birnLex.addToModel( model );
    }

    public void addNIFSTDNodes() throws Exception {
        NIFSTDRegionExtractor nifstd = new NIFSTDRegionExtractor();
        nifstd.addToModel( model );
    }

    public void addNIFNodes() throws Exception {

    }

    public void addABCDNodes() throws Exception {
        String file = Config.config.getString( "whitetext.lexicon.output" ) + "AvianBrainConnectivityDatabase.txt";
        CSVReader reader = new CSVReader( new FileReader( file ), '\t' );
        String[] line;
        Resource mainConcept = null;

        while ( ( line = reader.readNext() ) != null ) {
            // which is syn and which is main label?
            // log.info( line[0] + "->" + line[1] );
            String uri = line[0];
            String label = line[1];

            mainConcept = model.createResource( uri );
            mainConcept.addProperty( RDF.type, Vocabulary.ABCDName );
            Resource r = Vocabulary.makeNeurotermNode( label, model );
            // will use latest name for label - need better data from ABCD
            mainConcept.addLiteral( RDFS.label, label );
            mainConcept.addProperty( Vocabulary.has_synonym_term, r );
        }

    }

    public void addABANodes() throws Exception {
        OntModel ABA = OntologyLoader.loadMemoryModel( Vocabulary.getABAURI() );
        ExtendedIterator it = ABA.listClasses();
        Resource mainConcept = null;

        for ( OntClass i = ( OntClass ) it.next(); it.hasNext(); i = ( OntClass ) it.next() ) {
            String label = i.getLabel( null );
            if ( label == null ) {
                log.info( "Skipping:" + i.getLabel( null ) + " = " + i.getURI() );
                continue;
            }
            mainConcept = model.createResource( i.getURI() );
            mainConcept.addProperty( RDF.type, Vocabulary.ABAName );
            Resource r = Vocabulary.makeNeurotermNode( label, model );
            mainConcept.addLiteral( RDFS.label, label );
            mainConcept.addProperty( Vocabulary.has_label_term, r );
            log.info( i.getLabel( null ) + " = " + i.getURI() );
        }
    }

    public void addNN2010Nodes() throws Exception {
        // was NN2002
        // make NN nodes, with ID as URL and name as label
        // NeuroNames Ontology of Mammalian Neuroanatomy: NN2010
        NeuroNamesLoader NN2010 = new NeuroNamesLoader();
        // load main labels
        NN2010.addClassicalStructuresToModel( model );
        NN2010.addAncillaryStructuresToModel( model );
        NN2010.addSynonymsToModel( model );
    }

    public void addBAMSNodes() {
        BAMSDataLoader bamsLoader = new BAMSDataLoader();
        bamsLoader.addToModel( model );
    }

    public void extendBAMSNodes() throws Exception {
        BAMSDataLoader bamsLoader = new BAMSDataLoader();
        // open up spreadsheet
        String filename = Config.config.getString( "whitetext.resolve.addedBAMSSynonyms" );
        log.info( filename );
        HSSFSheet sheet = ExcelUtil.getSheetFromFile( filename, "Sheet0" );

        SynonymSchema schema = new SynonymSchema();

        int row = 1;
        int acceptCount = 0;
        while ( true ) {
            row++;
            String BAMSName = ExcelUtil.getValue( sheet, row, schema.getPosition( "BAMSName" ) );
            String termString = ExcelUtil.getValue( sheet, row, schema.getPosition( "Term" ) );
            if ( BAMSName == null ) {
                break;
            }
            // log.info( BAMSName );
            String acceptValue = ExcelUtil.getValue( sheet, row, schema.getPosition( "Accept" ) );
            boolean accept;
            if ( acceptValue != null ) {
                accept = acceptValue.equals( "X" );
            } else {
                accept = false;
            }

            if ( accept ) {
                acceptCount++;
                // get URI for BAMS node
                OntClass hitRegion = null;
                for ( OntClass region : bamsLoader.getAllBrianRegions() ) {
                    // any synonyms in BAMS? don't think there is any
                    String label = region.getLabel( null );
                    if ( label.equals( BAMSName ) ) {
                        hitRegion = region;
                        break;
                    }
                }
                if ( hitRegion == null ) {
                    log.info( "Cant find:" + BAMSName );
                    continue;
                }
                // create synonym node
                // create a link between them
                Resource r = model.createResource( hitRegion.getURI() );
                r.addProperty( Vocabulary.has_manual_link, Vocabulary.makeNeurotermNode( termString, model ) );
            }

        }
        log.info( "Accept:" + acceptCount + " of " + row );
        // create nodes
    }

    public void addManualLinks() throws Exception {
        LoadManualMappings manuals = new LoadManualMappings();
        manuals.addToModel( model );
    }

    public void addDoubleMentionsToModel( String annotationSetNormal, String annotationSetUnseen, String dataStore ) {
        GateInterface gateInt = new GateInterface( dataStore );

        // two sets of documents, unseen with predicted annotations and normal with annotated spans
        List<ConnectionsDocument> normalDocs = gateInt.getDocuments();
        List<ConnectionsDocument> unSeenDocs = GateInterface.getDocuments( gateInt.getUnseenCorp() );

        ResolveBrianRegions regionGetterUnseen = new ResolveBrianRegions( unSeenDocs, annotationSetUnseen );
        ResolveBrianRegions regionGetterNormal = new ResolveBrianRegions( normalDocs, annotationSetNormal );

        Collection<String> allRegions = ( Collection<String> ) Util.union( regionGetterNormal.getAllBrainRegionText(),
                regionGetterUnseen.getAllBrainRegionText() );
        log.info( "Brain regions:" + allRegions.size() );

        CountingMap<String> uniqueAnnotationTextUnSeen = regionGetterUnseen.getAllBrainRegionTextCounted();
        CountingMap<String> uniqueAnnotationTextNormal = regionGetterNormal.getAllBrainRegionTextCounted();

        StringToStringSetMap pmidMapUnseen = regionGetterUnseen.getAllBrainRegionTextToPMID();
        StringToStringSetMap pmidMapNormal = regionGetterNormal.getAllBrainRegionTextToPMID();
        log.info( "PMID MAP unseen:" + pmidMapUnseen.getExpandedSize() );
        log.info( "PMID MAP normal:" + pmidMapNormal.getExpandedSize() );

        for ( String region : allRegions ) {
            Resource r = Vocabulary.makeMentionNode( region, model );
            Integer occurancesUnseen = uniqueAnnotationTextUnSeen.get( region );
            if ( occurancesUnseen == null ) occurancesUnseen = 0;
            Integer occurancesNormal = uniqueAnnotationTextNormal.get( region );
            if ( occurancesNormal == null ) occurancesNormal = 0;

            r.addLiteral( Vocabulary.number_of_occurances, occurancesUnseen + occurancesNormal );

            Set<String> pmidsUnseen = pmidMapUnseen.get( region );
            if ( pmidsUnseen == null ) pmidsUnseen = new HashSet<String>();
            Set<String> pmidsNormal = pmidMapNormal.get( region );
            if ( pmidsNormal == null ) pmidsNormal = new HashSet<String>();
            Set<String> pmids = ( Set<String> ) Util.union( pmidsNormal, pmidsUnseen );

            r.addLiteral( Vocabulary.number_of_abstracts, pmids.size() );
            r.addLiteral( Vocabulary.annotation_set, annotationSetNormal + "|" + annotationSetUnseen );
            for ( String pmid : pmids ) {
                r.addProperty( Vocabulary.in_PMID, model.createResource( Vocabulary.getpubmedURIPrefix() + pmid ) );
            }
        }

    }

    public void addDatesToModel( String dataStore, boolean useUnseenCorp ) throws Exception {
        List<ConnectionsDocument> docs = getDataStoreDocs( dataStore, useUnseenCorp );

        GateInterface gateInt = new GateInterface( dataStore );
        for ( ConnectionsDocument doc : docs ) {
            String PMID = doc.getPMID();
            // for ( String PMID : PMIDs ) {
            // ConnectionsDocument doc = gateInt.getByPMID( PMID );
            Date date = doc.getPubDate();
            // check if it's in the RDF?

            Resource docResource = model.createResource( Vocabulary.getpubmedURIPrefix() + PMID );
            Calendar pubCal = Calendar.getInstance();
            pubCal.setTime( date );

            docResource.addLiteral( Vocabulary.publication_date, pubCal );

            log.info( docResource.getURI() + "->" + pubCal.toString() );
        }
        log.info( docs.size() + " abstracts labelled for species" );
    }

    public void addSpeciesToModel( List<ConnectionsDocument> docs ) throws Exception {
        SpeciesLoader filterLoader = new SpeciesLoader();
        for ( ConnectionsDocument doc : docs ) {
            filterLoader.addToModel( doc, model );
        }
        log.info( docs.size() + " abstracts labelled for species" );
    }


    public void addSpeciesToModel( String dataStore, boolean useUnseenCorp ) throws Exception {
        List<ConnectionsDocument> docs = getDataStoreDocs( dataStore, useUnseenCorp );
        addSpeciesToModel( docs );
    }

    private List<ConnectionsDocument> getDataStoreDocs( String dataStore, boolean useUnseenCorp ) {
        GateInterface gateInt = new GateInterface( dataStore );
        List<ConnectionsDocument> docs;
        if ( !useUnseenCorp ) {
            docs = gateInt.getDocuments();
        } else {
            docs = GateInterface.getDocuments( gateInt.getUnseenCorp() );
        }
        return docs;
    }

    public void addMentionsToModel( String annotationSet, String dataStore, boolean useUnseenCorp ) {
        List<ConnectionsDocument> docs = getDataStoreDocs( dataStore, useUnseenCorp );

        ResolveBrianRegions regionGetter = new ResolveBrianRegions( docs, annotationSet );
        log.info( "Brain regions:" + regionGetter.getAllBrainRegionText().size() );
        CountingMap<String> uniqueAnnotationText = regionGetter.getAllBrainRegionTextCounted();
        StringToStringSetMap pmidMap = regionGetter.getAllBrainRegionTextToPMID();
        for ( String region : uniqueAnnotationText.keySet() ) {
            Resource r = Vocabulary.makeMentionNode( region, model );
            r.addLiteral( Vocabulary.number_of_occurances, uniqueAnnotationText.get( region ) );
            Set<String> pmids = pmidMap.get( region );
            r.addLiteral( Vocabulary.number_of_abstracts, pmids.size() );
            r.addLiteral( Vocabulary.annotation_set, annotationSet );
            for ( String pmid : pmids ) {
                r.addProperty( Vocabulary.in_PMID, model.createResource( Vocabulary.getpubmedURIPrefix() + pmid ) );
            }
        }
    }

    public void removeMentionsFromModel( String annotationSet, String dataStore ) {
        GateInterface gateInt = new GateInterface( dataStore );
        List<ConnectionsDocument> docs = gateInt.getDocuments();
        ResolveBrianRegions regionGetter = new ResolveBrianRegions( docs, annotationSet );
        CountingMap<String> uniqueAnnotationText = regionGetter.getAllBrainRegionTextCounted();
        for ( String region : uniqueAnnotationText.keySet() ) {
            Resource r = Vocabulary.makeMentionNode( region, model );
            StmtIterator itr = model.listStatements( r, null, ( RDFNode ) null );
            model.remove( itr );
        }
    }

    public static void corpusLookTemp( String set, String store ) {
        GateInterface gateInt = new GateInterface( store );
        List<ConnectionsDocument> docs = gateInt.getDocuments();
        ResolveBrianRegions regionGetter = new ResolveBrianRegions( docs, set );
        CountingMap<String> uniqueAnnotationText = regionGetter.getAllBrainRegionTextCounted();
        log.info( set + " size:" + uniqueAnnotationText.size() );
        log.info( set + " sum:" + uniqueAnnotationText.summation() );

    }

    public void readIn() throws Exception {
        model.read( new FileInputStream( Config.config.getString( "resolve.Lexicon.RDF" ) ), null );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        String storeLoc = Config.config.getString( "whitetext.datastore.location" );
        // corpusLookTemp( "UnionMerge", Config.config.getString( "whitetext.datastore.location" ) );
        // corpusLookTemp( "UnionMerge", "/home/leon/Desktop/GATEDataStoreAfterTracts/store/" );
        // corpusLookTemp( "Mallet", "/home/leon/Desktop/fromKrusty/GATEDataStoreAfterTracts/store/" );
        // System.exit( 1 );
        boolean useUnseen = true;

        MakeLexiconRDFModel maker;
        maker = new MakeLexiconRDFModel();

        maker.readIn();
        // maker.addABCDNodes();
        maker.addDatesToModel( storeLoc, useUnseen );
        maker.writeOut();
        System.exit( 1 );

        useUnseen = false;
        maker.addSpeciesToModel( storeLoc, useUnseen );
        maker.writeOut();
        System.exit( 1 );

        // addSpeciesToModel( String annotationSet, String dataStore, boolean useUnseenCorp ) {
        ResolutionRDFModel resModel2 = new ResolutionRDFModel( maker.model, true );
        // resModel2.getStats();
        // resModel2.loadManualMatches();
        // resModel2.createMatches3();
        // resModel2.getStats();

        resModel2 = new ResolutionRDFModel( Config.config.getString( "resolve.Lexicon.resolution.RDF" ) );
        resModel2.getStats();
        System.exit( 1 );

        maker = new MakeLexiconRDFModel();
        maker.readIn();
        maker.removeMentionsFromModel( "UnionMerge", storeLoc );
        boolean useUnseenCorp = true;
        // maker.addMentionsToModel( "Mallet", storeLoc, useUnseenCorp );

        // maker.addMentionsToModel( "Mallet", storeLoc, useUnseenCorp );
        maker.addDoubleMentionsToModel( "UnionMerge", "Mallet", storeLoc );

        maker.writeOut( Config.config.getString( "resolve.Lexicon.RDF.allComp" ) );

        ResolutionRDFModel resModel = new ResolutionRDFModel( maker.model, true );
        resModel.getStats();
        System.exit( 1 );

        // from scratch - birnlex is broken
        maker.addNN2010Nodes();
        maker.addNN2007Nodes(); // mouse and rat
        //
        // maker.addBIRNLexNodes();
        // // maker.addManualLinks();
        maker.addBredeNodes();
        // maker.addBAMSNodes();
        //

        // TODO adding BIRNLex nodes is broken, need to update ontology code in basecode!!
        maker.readIn();
        // maker.addMentionsToModel( "UnionMerge", Config.config.getString( "whitetext.datastore.location" ) );
        maker.addABANodes();
        // maker.writeOut();
        // link terms using bag of words or stemming - see ResolutionRDFModel
    }

    public Model getModel() {
        return model;
    }
}
