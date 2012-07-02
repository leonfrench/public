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

import cc.mallet.types.Sequence;
import cc.mallet.types.SequencePairAlignment;

public class ReturnFirstMerge extends BidirectionalMerge {
    /*
     * Ignores the second list of alignments and returns the top output sequence from the first (non-Javadoc)
     * 
     * @see ubic.pubmedgate.mallet.BidirectionalMerge#doMerge(java.util.List, java.util.List)
     */
    protected Sequence doMerge( List<SequencePairAlignment<Object, Object>> aResult,
            List<SequencePairAlignment<Object, Object>> bResult ) {
        return aResult.get( 0 ).output();
    }

}
