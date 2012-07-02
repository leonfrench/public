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
import gate.Document;
import gate.FeatureMap;

import java.util.List;
import java.util.Random;

import ubic.pubmedgate.ConnectionsDocument;

/**
 * Gets a set amount of random documents from the main Datastore corpus (PubMed), and adds them into the RandomSubset
 * corpus
 * 
 * @author lfrench
 */
public class GetRandomDocuments {

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        PubMedIDtoGate p2g = new PubMedIDtoGate();
        // print out the header for CSV

        List<ConnectionsDocument> docs = p2g.getDocuments();
        Random randGen = new Random( 1 );

        Corpus corp = p2g.getCorp();
        Corpus randomCorp = p2g.getRandomSubsetCorp();
        System.out.println( "Random size:" + randomCorp.size() );

        // System.exit(1);
        int added = 0;
        int notadded = 0;
        int randGetSize = 100;

        do {
            int spot = ( int ) ( randGen.nextDouble() * docs.size() );
            // need to check if its there already
            // just add it to the other corpus

            // !!make sure you are adding a document, not a ConnectionsDocument
            ConnectionsDocument cDoc = docs.get( spot );
            Document doc = cDoc.getDocument();
            FeatureMap fMap = doc.getFeatures();
            if ( randomCorp.contains( doc ) ) {
                System.out.println( "Not Adding:" + corp.indexOf( doc ) + " PMID:" + fMap.get( "PMID" ) );
                notadded++;
            } else {
                System.out.println( "Adding:" + corp.indexOf( doc ) + " PMID:" + fMap.get( "PMID" ) );
                // System.out.println( corp.indexOf( doc ) );
                randomCorp.add( doc );
                randomCorp.getDataStore().sync( randomCorp );
                added++;
                randGetSize--;
            }
        } while ( randGetSize > 0 );
        System.out.println( added + " added to the RandomSubset Corpus" );
        System.out.println( notadded + "Not added to the RandomSubset Corpus" );
    }

}
