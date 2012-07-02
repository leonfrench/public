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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cc.mallet.types.Sequence;
import cc.mallet.types.SequencePairAlignment;

public class BestSumOrFirstMerge extends BidirectionalMerge {
    protected static Log log = LogFactory.getLog( BidirectionalMerge.class );
    ReturnFirstMerge first;
    int didFirst;
    int total;

    public BestSumOrFirstMerge() {
        first = new ReturnFirstMerge();
        didFirst = 0;
        total = 0;
    }

    public String toString() {
        return "Best sum or first merge, used first:" + didFirst + " of total: " + total;
    }

    @Override
    protected Sequence doMerge( List<SequencePairAlignment<Object, Object>> aResult,
            List<SequencePairAlignment<Object, Object>> bResult ) {
        double bestWeight = Double.MIN_VALUE;
        Sequence best = null;
        int matches = 0;

        for ( SequencePairAlignment a : aResult ) {
            for ( SequencePairAlignment b : bResult ) {
                // if they have the same output

                // for some reason the equals on the sequences doesnt work, so use a manual one
                if ( BidirectionalMerge.equalSequences( a.output(), b.output() ) ) {
                    matches++;
                    // add them up
                    double weight = a.getWeight() + b.getWeight();
                    // if it's the highest then keep it
                    if ( weight > bestWeight ) {
                        bestWeight = weight;
                        best = b.output();
                    }
                }
            }
        }
        // log.info( "Matches:" + matches + " biggest=" + bestWeight );
        total++;
        if ( best != null ) {
            return best;
        } else {
            didFirst++;
            return first.doMerge( aResult, bResult );
        }
    }
}
