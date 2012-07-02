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
import gate.Document;
import gate.FeatureMap;
import gate.Node;
import gate.corpora.DocumentContentImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ubic.connection.Connection;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.statistics.GetStats;

public class AnnotationCleaner {

    GateInterface p2g;

    public AnnotationCleaner() {
        p2g = new GateInterface();
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {

        boolean save = false;
        AnnotationCleaner x = new AnnotationCleaner();
        x.clean( save );
    }

    // Document specifc class
    // give it a doc it does stats or fixes spaces or whatever
    // probably fits in the plugin arhecture.
    public void clean( boolean save ) {
        int numberChanged = 0;

        for ( Document doc : p2g.getDocuments() ) {
            boolean changedDoc = false;
            FeatureMap fMap = doc.getFeatures();
            // make a map of changed connections
            Map<Annotation, Annotation> changedAnnotations = new HashMap<Annotation, Annotation>();
            DocumentContentImpl documentContent = ( DocumentContentImpl ) doc.getContent();

            // get number of brain regions and connection predicates
            Map m = doc.getNamedAnnotationSets();

            // go through all annotators
            for ( Object key : m.keySet() ) {
                AnnotationSet annotSet = ( AnnotationSet ) m.get( key );

                // if it's not suzanne or lydia, then move on.
                if ( !( key.equals( "Suzanne" ) || key.equals( "Lydia" ) ) ) {
                    continue;
                }

                // convert it to an array first so we don't get concurrent
                // modification issues
                for ( Object o : annotSet.toArray() ) {
                    Annotation ann = ( Annotation ) o;

                    // make sure we have a brain region or connectionpred type
                    if ( !( ann.getType().equals( "BrainRegion" ) || ann.getType().equals( "ConnectionPredicate" ) ) ) {
                        continue;
                    }

                    Node startNode = ann.getStartNode();
                    Node endNode = ann.getEndNode();
                    long start = startNode.getOffset();
                    long end = endNode.getOffset();

                    // we trim it then put it in the map
                    try {
                        boolean changed = false;
                        String cont = documentContent.getContent( start, end ).toString();

                        if ( cont.startsWith( " " ) ) {
                            // System.out.println(cont);
                            start++;
                            changed = true;
                        }
                        if ( cont.endsWith( "." ) || cont.endsWith( " " ) || cont.endsWith( "," ) ) {
                            end--;
                            changed = true;
                        }
                        if ( ann.getType().equals( "BrainRegion" )
                                && ( cont.startsWith( "a " ) || cont.startsWith( "A " ) ) ) {
                            // System.out.println(cont);
                            start += 2;
                            changed = true;
                        }
                        if ( cont.startsWith( "the " ) || cont.startsWith( "The " ) ) {
                            // System.out.println(cont);
                            start += 4;
                            changed = true;
                        }
                        if ( cont.startsWith( "midbrain " ) || cont.startsWith( "Midbrain " ) ) {
                            // System.out.println(cont);
                            start += 9;
                            changed = true;
                        }

                        // add the new annotation with the new span
                        int newID = 0;
                        try {
                            newID = annotSet.add( start, end, ann.getType(), ann.getFeatures() );
                        } catch ( Exception e ) {
                            e.printStackTrace();
                            System.out.println( "|" + cont + "|" );
                            System.out.println( "start:" + start );
                            System.out.println( "end:" + end );
                            System.exit( 1 );
                        }
                        // get the object of what we just made
                        Annotation newAnn = annotSet.get( newID );
                        // take note of the change
                        changedAnnotations.put( ann, newAnn );
                        if ( changed ) {
                            changedDoc = true;
                            System.out.println( doc.getName() );

                            System.out.println( "|" + cont + "|->|"
                                    + documentContent.getContent( start, end ).toString() + "|" + key );
                            numberChanged++;
                        }
                        annotSet.remove( ann );
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }
                    // Annotation newAnn =

                } // end of annotations

            } // end of annotation sets

            List<Connection> connections = Connection.getConnections( fMap );
            if ( connections != null ) {
                for ( Connection connection : connections ) {
                    // change annotation pointers
                    connection.updateAnnotationLinks( changedAnnotations );
                }
            }

            // save document
            try {
                if ( save && changedDoc ) doc.sync();
            } catch ( Exception e ) {
                e.printStackTrace();
                System.exit( 1 );
            }
        } // end of documents
        System.out.println( "Number of changed annotations:" + numberChanged );
    }

}
