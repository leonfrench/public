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
import gate.FeatureMap;
import ubic.pubmedgate.mallet.GATEToken;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * Converts a GATE token featureMap feature into a Mallet feature for a tokensequence
 * 
 * @author leon
 * 
 */
public class GATETokenFeature extends Pipe {
    public static final long serialVersionUID = 1l;
    String GATEFeatureName;
    boolean featureKeyOnly;

    public GATETokenFeature( String GATEFeatureName ) {
        this( GATEFeatureName, false );
    }

    public GATETokenFeature( String GATEFeatureName, boolean featureKeyOnly ) {
        this.featureKeyOnly = featureKeyOnly;
        this.GATEFeatureName = GATEFeatureName;
        this.featureKeyOnly = false;
        // assert this is not the target feature
        assert ( !GATEFeatureName.equals( TargetFromGATE.GATE_SIMPLE_TARGET_FEATURE ) );
    }

    /*
     * Assumes carrier.getData is a TokenSequence where each token is a GATEToken (non-Javadoc)
     * 
     * @see cc.mallet.pipe.Pipe#pipe(cc.mallet.types.Instance)
     */
    public Instance pipe( Instance carrier ) {
        TokenSequence tokens = ( TokenSequence ) carrier.getData();
        for ( Token tokenM : tokens ) {
            GATEToken token = ( GATEToken ) tokenM;
            Annotation tokenAnnotation = token.getGATEAnnotation();
            FeatureMap features = tokenAnnotation.getFeatures();
            for ( Object featureKey : features.keySet() ) {
                if ( acceptFeature( featureKey.toString() ) ) {
                    // the key value pair becomes many boolean key+value features
                    // eg orth = lowercase -> ortho=lowercase == 1
                    // eg orth = uppercase -> ortho=uppercase == 1
                    String malletFeature = featureKey.toString();
                    if ( !featureKeyOnly ) {
                        String featureValue = features.get( featureKey ).toString();
                        malletFeature += "=" + featureValue.toString();
                    }
                    token.setFeatureValue( malletFeature, 1 );
                }
            }
            // if it's null then just leave it - make it sparse
        }
        // modified the carrier
        return carrier;
    }

    public boolean acceptFeature( String key ) {
        return key.equals( GATEFeatureName );
    }

}
