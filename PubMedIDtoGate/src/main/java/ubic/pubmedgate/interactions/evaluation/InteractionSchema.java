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

package ubic.pubmedgate.interactions.evaluation;

import ubic.basecode.io.excel.SpreadSheetSchema;

public class InteractionSchema extends SpreadSheetSchema {
    public InteractionSchema() {
        super();
        positions.put( "sentence", 0 );
        positions.put( "LinkToAbstract", 1 );
        positions.put( "Reject", 2 );
        positions.put( "Efferent/Outgoing", 3 );
        positions.put( "Afferent/Incoming", 4 );
        positions.put( "Named entity error", 5 );
        positions.put( "Abbreviation error", 6 );
        positions.put( "Ambiguous", 7 );
        positions.put( "Comment", 8 );
        positions.put( "PairID", 9 );
        positions.put( "PMID", 10 );
        // not always used
        positions.put( "RegionAName", 11 );
        positions.put( "RegionAResolve", 12 );
        positions.put( "Resolve Evaluation A", 13 );
        positions.put( "RegionBName", 14 );
        positions.put( "RegionBResolve", 15 );
        positions.put( "Resolve Evaluation B", 16 );

        positions.put( "Direction", 17 );
        positions.put( "Prediction", 18 );
        positions.put( "Previous Annotation", 19 );
        positions.put( "Score", 20 );
    }
}
