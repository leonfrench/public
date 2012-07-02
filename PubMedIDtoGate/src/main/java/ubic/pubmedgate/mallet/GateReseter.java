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

package ubic.pubmedgate.mallet;

import gate.Corpus;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

/*
 * Deletes all the annotations for a certain annotationSet
 */
public class GateReseter {
    protected static Log log = LogFactory.getLog( GateReseter.class );

    String annotationSet;
    List<ConnectionsDocument> documents;

    public GateReseter(  List<ConnectionsDocument> documents, String annotationSet ) {
        this.documents = documents;
        this.annotationSet = annotationSet;
    }

    public void reset() throws Exception {
        for ( ConnectionsDocument doc : documents ) {
            doc.removeAnnotationSet( annotationSet );
            doc.sync();
        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        GateInterface p2g = new GateInterface();
        GateReseter reset = new GateReseter( p2g.getTrainingCorp(), "Mallet" );
        reset.reset();
        reset = new GateReseter( p2g.getTrainingCorp(), "MalletReverse" );
        reset.reset();
    }
}
