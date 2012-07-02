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

import java.util.Collections;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.TokenSequence;

public class ReversePipe extends Pipe {
    public Instance pipe( Instance carrier ) {
        MalletToGateLink m2g = ( MalletToGateLink ) carrier.getSource();
        FeatureVectorSequence fvs = ( FeatureVectorSequence ) carrier.getData();
        LabelSequence targetls = ( LabelSequence ) carrier.getTarget();
        String name = ( String ) carrier.getName();

        carrier.setName( "REVERSE-" + name );

        // make a new object for the source
        TokenSequence tokens = new TokenSequence( m2g.getTokens() );
        Collections.reverse( tokens );
        MalletToGateLink newM2g = new MalletToGateLink( m2g.getDoc(), tokens );
        carrier.setSource( newM2g );

        Label[] targetAr = new Label[targetls.size()];
        FeatureVector[] fvsAr = new FeatureVector[fvs.size()];
        for ( int i = 0; i < fvs.size(); i++ ) {
            // position in the original
            int oldPos = fvs.size() - i - 1;

            fvsAr[i] = fvs.get( oldPos );
            targetAr[i] = targetls.getLabelAtPosition( oldPos );
        }

        FeatureVectorSequence newFVS = new FeatureVectorSequence( fvsAr );
        carrier.setData( newFVS );

        LabelSequence newTarget = new LabelSequence( targetAr );
        carrier.setTarget( newTarget );

        return carrier;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub

    }
}
