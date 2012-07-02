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
import gate.FeatureMap;
import ubic.pubmedgate.mallet.GATEToken;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class GATEAnnotationInWithFeatures extends GATEAnnotationIn {
    boolean featureKeyOnly;
    protected boolean itselfIsFeature;
    public static final long serialVersionUID = 1l;

    public GATEAnnotationInWithFeatures( String annotationSet, String annotationType, boolean featureKeyOnly ) {
        super( annotationSet, annotationType );
        this.featureKeyOnly = featureKeyOnly;
        itselfIsFeature = true;
    }

    public GATEAnnotationInWithFeatures( String annotationSet, String annotationType ) {
        this( annotationSet, annotationType, true );
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
            AnnotationSet overlapping = annotations.get( a.getStartNode().getOffset(), a.getEndNode().getOffset() );
            if ( overlapping.size() != 0 ) {
                if (itselfIsFeature) token.setFeatureValue( featureName, 1 );
                for ( Annotation overlap : overlapping ) {
                    // it's in the annotation
                    FeatureMap GATEFeatures = overlap.getFeatures();
                    for ( Object key : GATEFeatures.keySet() ) {
                        if ( acceptFeature( key.toString() ) ) {
                            String malletFeature = featureName + "." + key.toString();
                            if ( !featureKeyOnly ) malletFeature = malletFeature + "=" + GATEFeatures.get( key );
                            token.setFeatureValue( malletFeature, 1 );
                        }
                    }
                }
            }
        }
        return carrier;
    }
    public boolean acceptFeature(String key) {
        return true;
    }
}
