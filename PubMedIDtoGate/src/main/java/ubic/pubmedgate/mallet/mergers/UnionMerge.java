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

package ubic.pubmedgate.mallet.mergers;

import java.util.List;

import ubic.pubmedgate.ner.TokenTargetLabeller;
import cc.mallet.types.ArrayListSequence;
import cc.mallet.types.Sequence;
import cc.mallet.types.SequencePairAlignment;

public class UnionMerge extends BidirectionalMerge {

    public UnionMerge( boolean bio ) {
        if ( bio ) throw new RuntimeException( "Can't do BIO yet" );
    }

    @Override
    // TODO: make work for BIO
    protected Sequence doMerge( List<SequencePairAlignment<Object, Object>> aResult,
            List<SequencePairAlignment<Object, Object>> bResult ) {
        // don't need the top ten here
        Sequence outputA = aResult.get( 0 ).output();
        Sequence outputB = bResult.get( 0 ).output();
        // String brain = TokenTargetLabeller.BRAIN_TARGET;
        String outside = TokenTargetLabeller.OUTSIDE_TARGET;
        String brain = TokenTargetLabeller.BRAIN_TARGET;

        ArrayListSequence<Object> result = new ArrayListSequence<Object>();
        // log.info( "Equal:" + outputA.equals( outputB ) );
        // log.info( "Equal my own:" + BestSumOrFirstMerge.equalSequences(outputA, outputB));
        for ( int i = 0; i < outputA.size(); i++ ) {
            //log.info( outputA.get( i ) + " | " + outputB.get( i ) );
            Object a = outputA.get( i );
            Object b = outputB.get( i );
            if ( a.equals( brain ) || b.equals( brain ) ) {
               // log.info( "brains!" );
                result.add( brain );
            } else {
                result.add( outside );
            }
        }

        return result;
    }
}
