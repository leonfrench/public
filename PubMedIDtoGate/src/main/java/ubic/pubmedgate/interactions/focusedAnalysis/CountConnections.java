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

package ubic.pubmedgate.interactions.focusedAnalysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.ConnectionList;

public class CountConnections {
    protected static Log log = LogFactory.getLog( CountConnections.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();
        p2g.setNamedCorpNull( "PubMedUnseenMScan" );
        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );
        p2g.setNamedCorpNull( "PubMedUnseenJCN" );

        Set<ConnectionsDocument> docs = new HashSet<ConnectionsDocument>( p2g.getDocuments( p2g.getCorp() ) );
        int result = 0;
        int negFilteredResult = 0;
        ConnectionList full = new ConnectionList( "Suzanne" );
        for ( ConnectionsDocument doc : docs ) {
            List<Connection> connections = doc.getConnections( "Suzanne" );
            if ( connections != null ) {
                result += connections.size();
            }

            ConnectionList manual = new ConnectionList( "Suzanne" );
            manual.addAllFromDocument( doc );
            full.addAllFromDocument( doc );
            manual.removeSymetrics();
            negFilteredResult += manual.size();

        }

        full.removeSymetrics();
        
        log.info( "Connections:" + result );
        log.info( "negFilteredResult Connections:" + negFilteredResult );
        log.info( "full Connections:" + full.size() );

    }

}
