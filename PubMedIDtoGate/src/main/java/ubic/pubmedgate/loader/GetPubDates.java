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

import gate.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.pubmedgate.GateInterface;

public class GetPubDates {
    private static Log log = LogFactory.getLog( GetPubDates.class.getName() );

    // two minutes of sleep
    static final int sleeptime = 15000;

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {

        PubMedSearch pSearch = new PubMedSearch();

        GateInterface p2g = new GateInterface();
        List<String> currentIDs = p2g.getLoadedPMIDs();
        Collection<String> PMIDs = currentIDs;
        log.info( "Loaded size:" + currentIDs.size() );

        // iterate existing articles

        int grabAmount = 100;
        Iterator<String> iterator = PMIDs.iterator();
        for ( int i = 0; i < PMIDs.size(); i = i + grabAmount ) {
            ArrayList<String> chunk = new ArrayList<String>();
            for ( int j = 0; ( j < grabAmount ) && iterator.hasNext(); j++ ) {
                String pmid = iterator.next();
                chunk.add( pmid );
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
                Date pubDate = ref.getPublicationDate();

                // get the Doc in the corpus, add date
                Document doc = p2g.getByPMID( PMID );
                doc.getFeatures().put( "PublicationDate", pubDate );

                // doc.getFeatures().put( "PublicationDateString", pubDate );
                // doc.getFeatures().put( "PublicationDate", pubDate.toString() );
                log.info( PMID + " PublicationDate -> " + pubDate.toString() );
                // sync document
                doc.sync();

            }

            // sleep for two minutes
            Thread.sleep( sleeptime );

            log.info( "Iterated:" + ( i + grabAmount ) + " of " + PMIDs.size() );
        }

    }
}
