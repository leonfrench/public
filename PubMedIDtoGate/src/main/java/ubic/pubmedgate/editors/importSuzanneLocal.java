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

package ubic.pubmedgate.editors;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ubic.connection.Connection;
import ubic.connection.ConnectionEditor;
import ubic.pubmedgate.GateInterface;

/*
 * This class merges a local copy of annotations with the server copy, this includes moving the connections
 * right now its hardcoded for Suzanne's annotations
 * 
 */      

public class importSuzanneLocal {

    public static List<Connection> filterConnections( String name, List<Connection> connections ) {
        List<Connection> result = new CopyOnWriteArrayList<Connection>();
        for ( Connection c : connections ) {
            if ( c.getAuthor().equalsIgnoreCase( name ) ) {
                result.add( c );
            }
        }
        return result;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        GateInterface suzanneInt = new GateInterface( "file:///home/leon/Desktop/GATEDataStore/storeFromSuzanneRedo/",
                "/home/leon/Desktop/GATEDataStore/originals/" );
        Corpus suzanneCorpus = suzanneInt.getRandomSubsetCorp();

        // regular
        GateInterface currentInt = new GateInterface();
        Corpus currentCorpus = currentInt.getRandomSubsetCorp();

        System.out.println( "current:" + currentCorpus.size() + " suzanne:" + suzanneCorpus.size() );
        AnnotationSet suzAnn, currentAnn, newAnn;
        List<Connection> conSuzLocalCopy, conSuzServerCopy, conCur;

        for ( int i = 0; i < 200; i++ ) {
            Document dSuz = ( Document ) suzanneCorpus.get( i );
            System.out.println( i + " " + dSuz.getName() );

            Document dCurrent = ( Document ) currentCorpus.get( i );
            suzAnn = dSuz.getAnnotations( "Suzanne" );
            currentAnn = dCurrent.getAnnotations( "Suzanne" );

            // deal with annotation sets
            dCurrent.removeAnnotationSet( "Suzanne" );
            // make new set and fill it with the local ones
            newAnn = dCurrent.getAnnotations( "Suzanne" );
            for ( Object o : suzAnn ) {
                Annotation a = (Annotation)o;
                newAnn.add( a );
            }

            // Deal with the connections
            // remove suzannes old connections
            conCur = Connection.getConnections( dCurrent.getFeatures() );
            if ( conCur != null ) {
                // get the connections that are suzannes
                conSuzServerCopy = filterConnections( "Suzanne", Connection.getConnections( dCurrent.getFeatures() ) );

                // from the current connection list
                if ( conSuzServerCopy != null ) {
                    for ( Connection c : conSuzServerCopy )
                        conCur.remove( c );
                }
            }

            // add the new ones
            if ( Connection.getConnections( dSuz.getFeatures() ) != null ) {
                // if it has none right now, then we gota add a set
                if ( conCur == null ) {
                    conCur = new CopyOnWriteArrayList<Connection>();
                    dCurrent.getFeatures().put( ConnectionEditor.CONFEATURENAME, conCur );
                }
                // get the connections that are suzannes
                conSuzLocalCopy = filterConnections( "Suzanne", Connection.getConnections( dSuz.getFeatures() ) );
                if ( conSuzLocalCopy != null ) {
                    for ( Connection c : conSuzLocalCopy ) {
                        c.setAuthor( "Suzanne" );
                        conCur.add( c );
                    }
                }
            }

            dCurrent.sync();
        }

    }
}
