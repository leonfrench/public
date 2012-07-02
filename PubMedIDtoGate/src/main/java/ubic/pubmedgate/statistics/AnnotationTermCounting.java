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

package ubic.pubmedgate.statistics;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

public class AnnotationTermCounting {

    protected static Log log = LogFactory.getLog( AnnotationTermCounting.class );

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        GateInterface gateInt = new GateInterface();
        List<ConnectionsDocument> docs = gateInt.getDocuments();
        CountingMap<String> brainRegions = gateInt.getAnnotationCountedMap( docs, "Suzanne", "BrainRegion", true );
        CountingMap<String> connectionPreds = gateInt.getAnnotationCountedMap( docs, "Suzanne", "ConnectionPredicate",
                true );
        CountingMap<String> connectionTags = gateInt.getConnectionTagCountedMap( docs, "Suzanne", true );

        log.info( "Brain region size:" + brainRegions.size() );
        log.info( "Brain region summation:" + brainRegions.summation() );
        log.info( "connectionPreds size:" + connectionPreds.size() );
        log.info( "connectionPreds summation:" + connectionPreds.summation() );
        log.info( "connectionTags size:" + connectionTags.size() );
        log.info( "connectionTags summation:" + connectionTags.summation() );

        log.info( prettyPrintCountingMap( brainRegions, 10 ) );
        log.info( "connectionPreds size:" + connectionPreds.size() );
        log.info( prettyPrintCountingMap( connectionPreds, 10 ) );
        log.info( "connectionTags size:" + connectionTags.size() );
        log.info( prettyPrintCountingMap( connectionTags, 39 ) );

        // connectionPreds.incrementAll( )

    }

    public String prettyPrintCountingMap( CountingMap<String> map ) {
        return prettyPrintCountingMap( map, null );
    }

    public static String prettyPrintCountingMap( CountingMap<String> map, Integer limit ) {
        if ( limit == null ) {
            limit = -1;
        }
        StringBuilder sb = new StringBuilder( "" );
        for ( String key : map.sortedKeyList( true ) ) {
            if ( limit-- == 0 ) break;
            sb.append( key.toString() + "=" + map.get( key ) + "\n" );
        }
        return sb.toString();
    }

}
