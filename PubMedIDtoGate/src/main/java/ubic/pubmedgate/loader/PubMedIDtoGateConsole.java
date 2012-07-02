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

import ubic.gemma.model.common.description.BibliographicReference;

public class PubMedIDtoGateConsole {
    public static void main( String[] args ) throws Exception {
        // the default place to put it
        PubMedIDtoGate p2g = new PubMedIDtoGate();

        /*
         * if ( args.length == 1 ) { if ( args[0].contains( "?" ) || args[0].contains( "help" ) ) { System.out.println(
         * "usage: first and only argument is folder of the GATE datastore, the default is" + dataStoreLocation );
         * System.exit( 0 ); } else dataStoreLocation = args[0]; }
         */

        BufferedReader screen = new BufferedReader( new InputStreamReader( System.in ) );
        while ( true ) {
            System.out.println( "Enter a space separated list of PMID's type \"exit\" to exit" );
            String line = screen.readLine();
            if ( line.equalsIgnoreCase( "exit" ) ) break;
            Collection<BibliographicReference> refs = PubMedIDtoGate.getRefs( line );
            for ( BibliographicReference ref : refs ) {
                p2g.loadReftoGate( ref );
            }

        }
    }
}
