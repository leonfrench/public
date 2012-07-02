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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cc.mallet.types.Sequence;
import cc.mallet.types.SequencePairAlignment;

public abstract class BidirectionalMerge {
    protected static Log log = LogFactory.getLog( BidirectionalMerge.class );

    public static void main( String[] args ) {
        // TODO Auto-generated method stub

    }

    protected abstract Sequence doMerge( List<SequencePairAlignment<java.lang.Object, java.lang.Object>> aResult,
            List<SequencePairAlignment<java.lang.Object, java.lang.Object>> bResult );

    // given two result lists, return one output sequence
    // so each input is an instance, or a sentence, with its input sequence, output sequence and the weight
    public Sequence merge( List<SequencePairAlignment<java.lang.Object, java.lang.Object>> aResult,
            List<SequencePairAlignment<java.lang.Object, java.lang.Object>> bResult ) {

        // verify it's all same input - how?? length?
        Set<Integer> inputSet = new HashSet<Integer>();
        for ( SequencePairAlignment align : aResult ) {
            inputSet.add( align.input().size() );
        }
        for ( SequencePairAlignment align : bResult ) {
            inputSet.add( align.input().size() );
        }
        if ( inputSet.size() != 1 ) {
            log.info( inputSet );
            throw new RuntimeException( "Error input lengths are not all the same" );
        }
        return doMerge( aResult, bResult );
    }
    public static boolean equalSequences( Sequence outputA, Sequence outputB ) {
        for ( int i = 0; i < outputA.size(); i++ ) {
            Object a = outputA.get( i );
            Object b = outputB.get( i );
            if ( !a.equals( b ) ) {
                return false;
            }
        }
        return true;
    }
    
}
