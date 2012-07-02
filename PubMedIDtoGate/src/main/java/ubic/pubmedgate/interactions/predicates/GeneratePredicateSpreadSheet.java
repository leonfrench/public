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

package ubic.pubmedgate.interactions.predicates;

import gate.Annotation;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

public class GeneratePredicateSpreadSheet {
    GateInterface p2g;
    protected static Log log = LogFactory.getLog( GeneratePredicateSpreadSheet.class );

    public GeneratePredicateSpreadSheet() {
        p2g = new GateInterface();
    }

    public void createSheet() {
        // iterate documents
        // countingmap for predicates
        // doesnt matter if it is annotated or typed in?

        CountingMap<String> predicates = new CountingMap<String>();

        for ( ConnectionsDocument doc : p2g.getDocuments() ) {
            List<Connection> connections = doc.getConnections(); // gets with any name
            if ( connections != null ) {
                for ( Connection connection : connections ) {
                    String predicate = getPredicateString( doc, connection );
                    predicates.increment( predicate );
                }
            }

        }

        for ( String predicate : predicates.keySet() ) {
            System.out.println( predicate + "|" + predicates.get( predicate ) );
        }
        log.info( "Size:" + predicates.size() );

    }

    public static String getPredicateString( ConnectionsDocument doc, Connection connection ) {
        Annotation relAnn = connection.getRelAnn();
        String tag = connection.getRelTag();
        String predicate = "";
        if ( relAnn != null )
            predicate = doc.getAnnotationText( relAnn );
        else {
            predicate = tag;
        }
        return predicate;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        GeneratePredicateSpreadSheet sheet = new GeneratePredicateSpreadSheet();
        sheet.createSheet();

    }

}
