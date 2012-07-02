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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cc.mallet.types.ArrayListSequence;
import cc.mallet.types.Sequence;
import cc.mallet.types.SequencePairAlignment;

public class ReverseTools {
    protected static Log log = LogFactory.getLog( SimpleMalletRunner.class );

    public static SequencePairAlignment<Object, Object> reverseSequencePairAlignment(
            SequencePairAlignment<Object, Object> a ) {
        Sequence<Object> outputRev = reverseSequence( a.output() );
        Sequence<Object> inputRev = reverseSequence( a.input() );
        return new SequencePairAlignment<Object, Object>( inputRev, outputRev, a.getWeight() );
    }

    public static Sequence<Object> reverseSequence( Sequence a ) {
        ArrayListSequence<Object> result = new ArrayListSequence<Object>();
        for ( int i = a.size() - 1; i > -1; i = i - 1 ) {
            result.add( a.get( i ) );
        }
        return result;
    }
}
