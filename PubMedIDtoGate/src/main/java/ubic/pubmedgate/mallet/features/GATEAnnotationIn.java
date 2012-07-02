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

package ubic.pubmedgate.mallet.features;

import gate.Annotation;
import gate.AnnotationSet;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.mallet.GATEToken;
import ubic.pubmedgate.mallet.MalletToGateLink;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class GATEAnnotationIn extends Pipe {
    public static final long serialVersionUID = 1l;
    String annotationSet;
    String annotationType;
    String featureName;

    public GATEAnnotationIn( String annotationSet, String annotationType ) {
        this.annotationSet = annotationSet;
        this.annotationType = annotationType;
        featureName = "InGATE." + annotationSet.replace( " ", "" ) + "." + annotationType;
    }

    /*
     * Assumes carrier.getData is a TokenSequence where each token is a GATEToken (non-Javadoc)
     * 
     * @see cc.mallet.pipe.Pipe#pipe(cc.mallet.types.Instance)
     */
    public Instance pipe( Instance carrier ) {
        // get the document from this instance
        AnnotationSet annotations = getAnnotations( carrier );

        TokenSequence tokens = ( TokenSequence ) carrier.getData();
        for ( Token tokenM : tokens ) {
            GATEToken token = ( GATEToken ) tokenM;
            Annotation a = token.getGATEAnnotation();
            // = token.get
            if ( annotations.get( a.getStartNode().getOffset(), a.getEndNode().getOffset() ).size() != 0 ) {
                // it's in the annotation
                token.setFeatureValue( featureName, 1 );
            }
        }
        return carrier;
    }

    protected AnnotationSet getAnnotations( Instance carrier ) {
        ConnectionsDocument d = ( ( MalletToGateLink ) carrier.getSource() ).getDoc();
        AnnotationSet markups = d.getAnnotations( annotationSet );
        AnnotationSet annotations = markups.get( annotationType );
        return annotations;
    }

}
