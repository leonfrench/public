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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

public class CountSentences {
    protected static Log log = LogFactory.getLog( CountSentences.class );

    /**
     * @param args
     */
    public static void main( String[] args ) {
        GateInterface p2g = new GateInterface();

        List<ConnectionsDocument> docs = p2g.getDocuments( p2g.getUnseenCorp() );
        int result = 0;
        for ( ConnectionsDocument doc : docs ) {
            result += doc.getAnnotationsByType( "GATETokens", "Sentence" ).size();

        }
        log.info( "Sentences:" + result );
    }

}
