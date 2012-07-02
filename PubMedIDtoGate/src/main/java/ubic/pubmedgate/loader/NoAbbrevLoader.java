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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

import ubic.gemma.model.common.description.BibliographicReference;

public class NoAbbrevLoader {

    /**
     * @param args
     */

    public static void main( String[] args ) throws Exception {
        // the default place to put it
        PubMedIDtoGate p2g = new PubMedIDtoGate();

        // no abbreviations
        p2g.setProcessAbbrevs( false );

        List<String> loadedPMIDs = p2g.getLoadedPMIDs();

        BufferedReader screen = new BufferedReader( new InputStreamReader( System.in ) );
        while ( true ) {
            System.out.println( "Enter a space separated list of PMID's type \"exit\" to exit" );
            String line = screen.readLine();
            if ( line.equalsIgnoreCase( "exit" ) ) break;

            // check if it exists
            if ( !loadedPMIDs.contains( line ) ) {
                System.out.println( "Error this one is not in the corpus to start" );
                continue;
            }

            // delete old one
            System.out.println("Removing..");
            try {
                p2g.remove( line );
            } catch ( Exception e ) {
                e.printStackTrace();
                System.out.println("not reloaded");
                continue;
            }
            System.out.println("Removed");

            // reload new one
            Collection<BibliographicReference> refs = PubMedIDtoGate.getRefs( line );
            for ( BibliographicReference ref : refs ) {
                p2g.loadReftoGate( ref );
            }

        }
    }

}
