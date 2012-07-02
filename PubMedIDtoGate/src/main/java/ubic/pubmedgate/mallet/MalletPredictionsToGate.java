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

import gate.Annotation;
import gate.AnnotationSet;
import gate.util.SimpleFeatureMapImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.ConnectionsDocument;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;
import cc.mallet.types.TokenSequence;

public class MalletPredictionsToGate {
    protected static Log log = LogFactory.getLog( MalletPredictionsToGate.class );

    public static final String MALLET_PREDICTION_SET = "Mallet";
    String annotationSet;

    public MalletPredictionsToGate() {
        this( MALLET_PREDICTION_SET );
    }

    public MalletPredictionsToGate( String annotationSet ) {
        this.annotationSet = annotationSet;
    }

    public void saveTargets( Instance i, Sequence predictions ) {
        // pretty ugly here, the GATE tokens are deeply embedded inthe instance source data
        MalletToGateLink link = ( MalletToGateLink ) i.getSource();
        ConnectionsDocument doc = link.getDoc();
        TokenSequence tokens = link.getTokens();
        AnnotationSet anotSet = doc.getAnnotations( annotationSet );

        assert ( tokens.size() == predictions.size() );
        if ( tokens.size() != predictions.size() ) throw new RuntimeException( "Sequence size doesnt match" );

        for ( int j = 0; j < predictions.size(); j++ ) {
            // the mallet prediction
            String prediction = predictions.get( j ).toString();

            // the corresponding gate token
            GATEToken token = ( GATEToken ) tokens.get( j );
            Annotation GATEAnnotation = token.getGATEAnnotation();
            
            // make a copy with a type that matches the prediction name
            anotSet.add( GATEAnnotation.getStartNode(), GATEAnnotation.getEndNode(), prediction,
                    new SimpleFeatureMapImpl() );
        }
        try {
            doc.sync();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

    }
}
