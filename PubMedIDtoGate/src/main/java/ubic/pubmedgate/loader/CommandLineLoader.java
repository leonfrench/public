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
import gate.DataStore;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;

import java.io.FileInputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class CommandLineLoader {
    public static void loadProps() throws Exception {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream( "gate.properties" );
        props.load( fis );
        System.getProperties().putAll( props );
        fis.close();
        System.out.println( "Initialising GATE..." );
        Gate.init();
        System.out.println( "Done Gate.init()..." );
    }

    public static void main( String args[] ) throws Exception {
        loadProps();
        gate.creole.gazetteer.AbstractGazetteer ag = null;

      

        DataStore dataStore = Factory.openDataStore( "gate.persist.SerialDataStore",
                "file:///\\\\Pavdesk1/GATEDataStore/store" );
        List<String> types = dataStore.getLrTypes();
        for ( String s : types ) {
            for ( String lr : ( List<String> ) dataStore.getLrIds( s ) ) {
                System.out.println( lr + "." +s );
                System.out.println(dataStore.getLr(s, lr).getName());
            }
        }

        System.out.println( "...GATE initialised" );

        // create a GATE corpus and add a document for each command-line
        // argument
        Corpus corpus = ( Corpus ) Factory.createResource( "gate.corpora.CorpusImpl" );
        for ( int i = 0; i < args.length; i++ ) {
            URL u = new URL( args[i] );
            FeatureMap params = Factory.newFeatureMap();
            params.put( "sourceUrl", u );
            params.put( "preserveOriginalContent", new Boolean( true ) );
            params.put( "collectRepositioningInfo", new Boolean( true ) );
            System.out.println( "Creating doc for " + u );
            Document doc = ( Document ) Factory.createResource( "gate.corpora.DocumentImpl", params );
            corpus.add( doc );
        } // for each of args

        dataStore.close();
    }
}
