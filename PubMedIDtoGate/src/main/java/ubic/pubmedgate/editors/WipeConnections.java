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

import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ANNIEConstants;
import gate.creole.SerialAnalyserController;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.loader.PubMedIDtoGate;
import ubic.pubmedgate.mallet.GateReseter;
import ubic.pubmedgate.statistics.GetStats;

public class WipeConnections {
    protected static Log log = LogFactory.getLog( WipeConnections.class );

    // Do manually -
    // remove unwanted corpi
    // remove annotationsetlog

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // GateInterface p2g = new PubMedIDtoGate();
        GateInterface p2g = new GateInterface( Config.config.getString( "whitetext.datastore.release.location" ) );

        GetStats stats = new GetStats( p2g );
        boolean verbose = false;
        stats.printStats( "Suzanne", verbose );
        stats.printStats( "Lydia", verbose );
        stats.printStats( "UnionMerge", verbose );
        stats.printStats( "IntersectMerge", verbose );

        String[] removeSets = { "Leon", "Paul", "GATETokens", "TreeTagger", "NNLookup" };
        for ( String removeSet : removeSets ) {
            log.info( "Removing " + removeSet );
            GateReseter reset = new GateReseter( p2g.getCorp(), removeSet );
            reset.reset();
        }

        log.info( "Removing connection preds" );
        p2g.removeAnnotationType( "ConnectionPredicate" );

        log.info( "Removing connections" );
        for ( ConnectionsDocument doc : p2g.getDocuments() ) {
            doc.removeConnections();
            doc.sync();
        }

        log.info( "Done" );

        // run getStats
        stats = new GetStats( p2g );
        verbose = false;
        stats.printStats( "Suzanne", verbose );
        stats.printStats( "Lydia", verbose );
        stats.printStats( "UnionMerge", verbose );
        stats.printStats( "IntersectMerge", verbose );

    }

}
