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

package ubic.pubmedgate;

import gate.Annotation;
import gate.Corpus;
import gate.DataStore;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ANNIEConstants;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.persist.PersistenceException;
import gate.security.SecurityException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.connection.Connection;

public class GateInterface {
    protected static Log log = LogFactory.getLog( GateInterface.class );

    static boolean gateLoaded = false;

    String dataStoreLocation;
    protected String originalsFile;
    protected DataStore dataStore;

    protected Map<String, Corpus> corpi;
    // the full corpus
    protected Corpus corp;

    // the corpus done by both Lydia and Suzanne
    protected Corpus randomSubsetCorp;

    // the corpus of unseen documents for testing
    protected Corpus unseenCorp;

    // the corpus that lacks abbreviation expansion
    protected Corpus noAbbrev;

    // the full corpus mins the randomSubset
    protected Corpus trainingCorp;

    Map<String, ConnectionsDocument> pmidToDocMap;

    public GateInterface() {
        // storeFromSuzanneRedo
        // this( "file:///home/leon/Desktop/GATEDataStore/storeFromSuzanneRedo/",
        // "/home/leon/Desktop/GATEDataStore/originals/" );
        // this( "file:///home/leon/Desktop/GATEDataStore/store/", "/home/leon/Desktop/GATEDataStore/originals/" );
        this( Config.config.getString( "whitetext.datastore.location" ), Config.config
                .getString( "whitetext.originals.location" ) );
    }

    public GateInterface( String dataStore ) {
        this( dataStore, Config.config.getString( "whitetext.originals.location" ) );
    }

    public GateInterface( String dataStoreLocation, String originalsFile ) {
        corpi = new HashMap<String, Corpus>();
        dataStoreLocation = "file://" + dataStoreLocation;
        this.dataStoreLocation = dataStoreLocation;
        this.originalsFile = originalsFile;
        pmidToDocMap = null;
        initGate();
        try {
            log.info( "Datastore location:" + dataStoreLocation );
            dataStore = Factory.openDataStore( "gate.persist.SerialDataStore", dataStoreLocation );
            List<String> corpora = dataStore.getLrIds( "gate.corpora.SerialCorpusImpl" );
            Corpus currentCorp;
            for ( String corpString : corpora ) {
                currentCorp = ( Corpus ) dataStore.getLr( "gate.corpora.SerialCorpusImpl", corpString );
                String corpusName = currentCorp.getName();
                corpi.put( corpusName, ( Corpus ) dataStore.getLr( "gate.corpora.SerialCorpusImpl", corpString ) );
                if ( corpusName.equals( "PubMed" ) ) {
                    corp = currentCorp;
                }
                if ( corpusName.equals( "PubMedRandomSubset" ) ) {
                    randomSubsetCorp = currentCorp;
                }
                if ( corpusName.equals( "PubMedTraining" ) ) {
                    trainingCorp = currentCorp;
                }
                if ( corpusName.equals( "PubMedUnseen" ) ) {
                    unseenCorp = currentCorp;
                    System.out.println( "Has Unseen corpus size:" + unseenCorp.size() );
                }
                if ( corpusName.equals( "PubMedNoAbbrev" ) ) {
                    noAbbrev = currentCorp;
                }
            }

            if ( unseenCorp == null ) {
                unseenCorp = createCorpus( "PubMedUnseen" );
            }
            log.info( "Corpi in dataset:" + corpi.keySet() );
            // what if random is null?

        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( 0 );
        }
    }

    public Corpus createCorpus( String name ) throws ResourceInstantiationException, PersistenceException,
            SecurityException {
        System.out.println( "Creating " + name + " corpus" );
        Corpus temp = Factory.newCorpus( name );
        Corpus result = ( Corpus ) dataStore.adopt( temp, null );
        dataStore.sync( result );
        return result;
    }

    public static void initGate() {

        if ( gateLoaded ) System.out.println( "Gate loaded already" );
        // a bit of a hack to prevent GATE loading plugins - related to Gate.java:366
        System.setProperty( Gate.AUTOLOAD_PLUGIN_PATH_PROPERTY_NAME, ";;;;;" );

        try {
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream( "gate.properties" );
            props.load( fis );
            System.getProperties().putAll( props );
            fis.close();
        } catch ( IOException e ) {
            e.printStackTrace();
            System.out.println( "Error loading gate.properties" );
            System.exit( 0 );
        }
        try {
            String location = Config.config.getString( "whitetext.GATE.home" );
            if ( location == null ) {
                log.error( "Error,  whitetext.GATE.home not set in WhitetText.properties" );
                System.exit( 1 );
            }
            System.setProperty( Gate.GATE_HOME_PROPERTY_NAME, Config.config.getString( "whitetext.GATE.home" ) );
            Gate.init();
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( 0 );
        }
        gateLoaded = true;
    }

    public Corpus getCorp() {
        return corp;
    }

    public Corpus getTrainingCorp() {
        return trainingCorp;
    }

    public Corpus getRandomSubsetCorp() {
        return randomSubsetCorp;
    }

    public Corpus getUnseenCorp() {
        return unseenCorp;
    }

    public Corpus getCorpusByName( String name ) {
        return corpi.get( name );
    }

    public void setUnSeenCorpNull() {
        unseenCorp = null;
        corpi.remove( "PubMedUnseen" );
    }

    public void setNamedCorpNull( String name ) {
        log.info( "Setting named corpus to null:" + name );
        corpi.remove( name );
    }

    public Corpus getNoAbbrevCorp() {
        return noAbbrev;
    }

    public void remove( Document doc ) throws Exception {
        corp.remove( doc );
        dataStore.delete( "gate.corpora.DocumentImpl", doc.getLRPersistenceId() );
        corp.sync();
    }

    /**
     * Gets the PMID's of all the documents in the pubmed gate corpus.
     * 
     * @return list of string PMID's
     */
    public List<String> getLoadedPMIDs() {
        List<String> result = new ArrayList<String>();

        for ( Corpus corp : corpi.values() ) {
            for ( ConnectionsDocument doc : getDocuments( corp ) ) {
                result.add( ( String ) doc.getFeatures().get( "PMID" ) );
            }
        }

        return result;
    }

    /**
     * Gets all the documents in the pubmed gate corpus.
     * 
     * @return list of GATE documents
     */
    public List<ConnectionsDocument> getDocuments() {
        return getDocuments( corp );
    }

    /**
     * Gets the documents in the pubmed gate corpus.
     * 
     * @return list of GATE documents
     */
    public List<ConnectionsDocument> getTrainingDocuments() {
        return getDocuments( trainingCorp );
    }

    public List<ConnectionsDocument> getConnectionDocuments() {
        List<ConnectionsDocument> docs = getDocuments();
        List<ConnectionsDocument> connectionDocs = new LinkedList<ConnectionsDocument>();

        for ( ConnectionsDocument doc : docs ) {
            List<Connection> x = doc.getConnections();
            if ( x != null && x.size() > 0 ) {
                connectionDocs.add( doc );
            }
        }
        return connectionDocs;
    }

    public List<ConnectionsDocument> getRandomSubsetDocuments() {
        return getDocuments( randomSubsetCorp );
    }

    public static List<ConnectionsDocument> getDocuments( Corpus sourceCorp ) {
        LinkedList<ConnectionsDocument> result = new LinkedList<ConnectionsDocument>();
        int count = 0;
        for ( Object o : sourceCorp ) {
            if ( count++ % 500 == 0 ) log.info( count + " of " + sourceCorp.getName() + " loaded" );
            Document doc = ( Document ) ( o );
            result.addLast( new ConnectionsDocument( doc ) );
        }
        return result;
    }

    public CountingMap<String> getConnectionTagCountedMap( String name, boolean lowerCase ) {
        return getConnectionTagCountedMap( getDocuments(), name, lowerCase );
    }

    public CountingMap<String> getConnectionTagCountedMap( List<ConnectionsDocument> docs, String name,
            boolean lowerCase ) {
        CountingMap<String> result = new CountingMap<String>();
        // go in the documents
        for ( ConnectionsDocument doc : docs ) {
            for ( String tag : doc.getConnectionTags( name ) ) {
                String text = tag;
                if ( lowerCase ) text = text.toLowerCase();
                result.increment( text );
            }
        }
        return result;
    }

    /**
     * move all the documents in one corpus to another.
     * 
     * @param oldCorpus
     * @param newCorpus
     * @throws Exception
     */
    public void moveDataStoreDocuments( Corpus oldCorpus, Corpus newCorpus ) throws Exception {
        for ( Object o : oldCorpus ) {
            newCorpus.add( o );
        }
        newCorpus.sync();
        oldCorpus.removeAll( newCorpus );
        oldCorpus.sync();
        syncDataStore( oldCorpus );
        syncDataStore( newCorpus );
        return;
    }

    public CountingMap<String> getAnnotationCountedMap( List<ConnectionsDocument> docs, String name, String type,
            boolean lowerCase ) {
        CountingMap<String> result = new CountingMap<String>();
        // go in the documents
        for ( ConnectionsDocument doc : docs ) {
            // get the annotations
            for ( Annotation ann : doc.getAnnotationsByType( name, type ) ) {
                // increment the counter
                String text = doc.getAnnotationText( ann );
                if ( lowerCase ) text = text.toLowerCase();
                result.increment( text );
            }
        }
        return result;
    }

    public ConnectionsDocument getByPMID( String PMID ) {
        if ( pmidToDocMap == null ) {
            log.info( "Creating document to PMID hashmap" );
            pmidToDocMap = new HashMap<String, ConnectionsDocument>();

            for ( String corpName : corpi.keySet() ) {
                // these two below are subsets and don't need to be loaded twice
                if ( corpName.equals( "PubMedTraining" ) || corpName.equals( "PubMedRandomSubset" ) ) continue;
                Corpus corp = corpi.get( corpName );
                for ( ConnectionsDocument doc : getDocuments( corp ) ) {
                    pmidToDocMap.put( doc.getPMID(), doc );
                }
            }
            log.info( "Done creating doc to PMID map" );
        }
        return pmidToDocMap.get( PMID );
    }

    public void syncDataStore( Corpus corp ) throws Exception {
        dataStore.sync( corp );
    }

    /**
     * @param args
     */
    public void removeAnnotationType( String type ) throws Exception {
        removeAnnotationType( type, true );
    }

    public void removeAnnotationType( String type, boolean keepOriginalMarkupsAS ) throws Exception {
        // create a serial analyser controller to run ANNIE with
        SerialAnalyserController annieController = ( SerialAnalyserController ) Factory.createResource(
                "gate.creole.SerialAnalyserController", Factory.newFeatureMap(), Factory.newFeatureMap(), "ANNIE_"
                        + Gate.genSym() );

        for ( int i = 0; i < ANNIEConstants.PR_NAMES.length; i++ ) {
            FeatureMap params = Factory.newFeatureMap(); // use default parameters
            // add the PR to the pipeline controller
            String use = "gate.creole.annotdelete.AnnotationDeletePR";
            if ( ANNIEConstants.PR_NAMES[i].equals( use ) ) {
                System.out.println( "Loading:" + ANNIEConstants.PR_NAMES[i] );
                ProcessingResource pr = ( ProcessingResource ) Factory.createResource( ANNIEConstants.PR_NAMES[i],
                        params );
                LinkedList<String> l = new LinkedList<String>();
                l.add( type );
                pr.setParameterValue( "annotationTypes", l );
                pr.setParameterValue( "keepOriginalMarkupsAS", keepOriginalMarkupsAS );
                annieController.add( pr );
            }
        }
        annieController.setCorpus( getCorp() );
        annieController.execute();
        getCorp().sync();
    }

    public static void main( String[] args ) throws Exception {
        GateInterface p2g = new GateInterface();
        System.out.println( "here" );
    }


}
