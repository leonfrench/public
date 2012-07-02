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

package ubic.pubmedgate.loader;

import gate.Corpus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.util.FileTools;
import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.description.BibliographicReference;

/**
 * loads in a set of PMID's either from an pubmed search or from a file of PMID's
 */
public class GetAll {
    private static Log log = LogFactory.getLog( GetAll.class.getName() );
    public static PubMedIDtoGate p2g;

    // two minutes of sleep per block grab of abstracts
    static final int sleeptime = 60000;

    public static void main( String args[] ) throws Exception {
        p2g = new PubMedIDtoGate();
        loadFromQuery();

        // Corpus newCorp = p2g.createCorpus( "PubMedUnseenJCN" );
        // p2g.moveDataStoreDocuments( p2g.getUnseenCorp(), newCorp );
    }

    // if ( unseenCorp == null ) {
    // System.out.println( "Creating Unseen corpus" );
    // Corpus temp = Factory.newCorpus( "PubMedUnseen" );
    // unseenCorp = ( Corpus ) dataStore.adopt( temp, null );
    // dataStore.sync( unseenCorp );
    // }

    public static void loadFromQuery() throws Exception {
        PubMedSearch pSearch = new PubMedSearch();

        String query;
        // "\"Neural Pathways\"[MeSH]"
        // 0021-9967 = ISSN for journal of comparative neurology
        // 1471-4159 for Journal of Neurochemistry
        query = "1471-4159";
        Collection<String> PMIDs = pSearch.searchAndRetrieveIdsByHTTP( query );
//		PMIDs = PMIDs.subList(0, 150);

        loadPMIDs( pSearch, PMIDs, p2g.getUnseenCorp() );

    }

    public static void loadFromList() throws Exception {
        PubMedSearch pSearch = new PubMedSearch();

        List<String> PMIDs = FileTools
                .getLines( "/home/leon/Downloads/Mscanner results/JCN retrieve 10000 all features/mscanner.minus.loaded.inorder.txt" );
        // PMIDs = PMIDs.subList( 0, 100 );

        loadPMIDs( pSearch, PMIDs, p2g.getUnseenCorp() );
    }

    /**
     * Loads in a set of PMIDs into the gate database, skips those in the database and those without abstracts.
     * 
     * @param pSearch
     * @param PMIDs
     * @throws InterruptedException
     * @throws Exception
     */
    private static void loadPMIDs( PubMedSearch pSearch, Collection<String> PMIDs, Corpus corp )
            throws InterruptedException, Exception {
        List<String> currentIDs = p2g.getLoadedPMIDs();
        boolean sync = true;
        CountingMap<String> journals = new CountingMap<String>();

        int grabAmount = 100;
        int alreadyLoaded = 0;
        int astractNullCount = 0;
        Iterator<String> iterator = PMIDs.iterator();
        for ( int i = 0; i < PMIDs.size(); i = i + grabAmount ) {
            ArrayList<String> chunk = new ArrayList<String>();
            for ( int j = 0; ( j < grabAmount ) && iterator.hasNext(); j++ ) {
                String pmid = iterator.next();
                if ( currentIDs.contains( pmid ) ) {
                    log.info( pmid + " is already loaded" );
                    alreadyLoaded++;
                } else {
                    chunk.add( pmid );
                }
            }
            log.info( "Loading chunk, size:" + chunk.size() );
            Collection<BibliographicReference> refs = null;
            // ugly retry loop
            while ( true ) {
                try {
                    refs = pSearch.searchAndRetrieveIdByHTTP( chunk );
                } catch ( Exception e ) {
                    e.printStackTrace();
                    log.info( "Error, waiting for retry" );
                    Thread.sleep( sleeptime * 3 );
                    log.info( "Retrying" );
                    continue;
                }
                break;
            }

            for ( BibliographicReference ref : refs ) {
                String PMID = ref.getPubAccession().getAccession();
                String abstractText = ref.getAbstractText();
                journals.increment( ref.getPublication() );

                // if it has no abstract then don't load it
                if ( abstractText == null ) {
                    System.out.println( PMID + " has no abstract" );
                    astractNullCount++;
                    continue;
                }

                try {
                    p2g.loadReftoGate( ref, corp );
                } catch ( java.util.regex.PatternSyntaxException e ) {
                    // its good, load it in
                    boolean doAbbrev = false;
                    p2g.loadReftoGate( ref, corp, doAbbrev, sync );
                }
            }

            // sleep for two minutes
            Thread.sleep( sleeptime );

            log.info( "Iterated:" + ( i + grabAmount ) + " of " + PMIDs.size() + ", already loaded:" + alreadyLoaded
                    + ", no abstract:" + astractNullCount );
            log.info( "Corpus size: " + corp.size() );

        }

        log.info( "Journal distribution:" );
        for ( String key : journals.sortedKeyList() ) {
            log.info( key + " -> " + journals.get( key ) );
        }
    }
}
