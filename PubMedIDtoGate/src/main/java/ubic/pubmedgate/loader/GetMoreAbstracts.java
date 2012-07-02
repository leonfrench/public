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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.description.BibliographicReference;

public class GetMoreAbstracts {
    public static long seed = 13L;
    public static int grabAmount = 100;
    // public static String query = "\"Neural Pathways\"[MeSH]";
    static Random randGen;
    
    

    public static void main( String args[] ) throws Exception {
        randGen = new Random( seed );
        PubMedSearch pSearch = new PubMedSearch();
        // PubMedIDtoGate p2g = new PubMedIDtoGate("file:///C:\\Documents and
        // Settings\\lfrench\\Desktop\\GATEDataStore\\test\\May8\\", "C:\\Documents and
        // Settings\\lfrench\\Desktop\\GATEDataStore\\test\\May8Originals");
        PubMedIDtoGate p2g = new PubMedIDtoGate();
        List<String> currentIDs = p2g.getLoadedPMIDs();
        
        // System.out.println(currentIDs.toString());
        // System.out.println(currentIDs.size());

        // System.exit(0);

        String query;
        // "\"Neural Pathways\"[MeSH]"
        // 0021-9967 = ISSN for journal of comparative neurology
        query = "0021-9967";

        Collection<String> PMIDs = pSearch.searchAndRetrieveIdsByHTTP( query );
        // System.out.println( PMIDs.size() );

        String[] IDs = PMIDs.toArray( new String[PMIDs.size()] );
        List<String> chosen = new ArrayList<String>();

        // now we can randomly select the first grab amount... check for abstracts?
        for ( int i = 0; i < grabAmount; i++ ) {
            int next = ( int ) ( randGen.nextDouble() * IDs.length );
            chosen.add( IDs[next] );
        }

        Collection<BibliographicReference> refs = null;
        refs = pSearch.searchAndRetrieveIdByHTTP( chosen );
        List<String> notLoaded = new ArrayList<String>();
        List<String> loaded = new ArrayList<String>();
        String forWiki = "";

        for ( BibliographicReference ref : refs ) {
            String PMID = ref.getPubAccession().getAccession();
            String abstractText = ref.getAbstractText();

            // if it has no abstract or is already loaded, then don't load it
            if ( abstractText == null || currentIDs.contains( PMID ) ) {
                notLoaded.add( PMID );
                continue;
            }

           
            // its good, load it in
            p2g.loadReftoGate( ref /*);*/ , p2g.getUnseenCorp() );
            // for the wiki
            forWiki += "|" + PMID + "|" + "Query=" + query + ",seed=" + seed + ",size=" + grabAmount + "|\n";
            System.out.println( "|" + PMID + "|" + "Query=" + query + ",seed=" + seed + ",size=" + grabAmount + "|" );
            currentIDs.add( PMID );
            loaded.add( PMID );
        }

        System.out.println( "-----START CUT-------" );
        System.out.println( forWiki );
        System.out.println( "-----END CUT-------" );

        System.out.println( notLoaded.size() + " not loaded, because they were loaded already or had no abstract" );
        System.out.println( loaded.size() + " loaded" );

    }
}
