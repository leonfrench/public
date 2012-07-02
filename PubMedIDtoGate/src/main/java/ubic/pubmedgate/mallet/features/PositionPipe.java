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

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/*
 * makes a feature for position in a sentence
 */
public class PositionPipe extends Pipe {

    public Instance pipe( Instance carrier ) {
        TokenSequence ts = ( TokenSequence ) carrier.getData();
        boolean[] marked = new boolean[ts.size()]; // avoid setting features twice

        for ( int i = 0; i < ts.size(); i++ ) {
            Token t = ts.get( i );
            t.setFeatureValue( "SentencePos" + i, 1.0 );
            t.setFeatureValue( "SentencePosRev" + ( ts.size() - 1 - i ), 1.0 );
            if ( i > ( ts.size() / 2 ) ) t.setFeatureValue( "SentenceFirstHalf", 1.0 );
        }
        return carrier;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub

    }

}
