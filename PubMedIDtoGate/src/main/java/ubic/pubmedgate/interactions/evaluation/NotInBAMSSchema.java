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

public class NotInBAMSSchema extends SpreadSheetSchema {
    public NotInBAMSSchema() {
        super();
        int c = 0;
        positions.put( "Rank", c++ ); // a
        positions.put( "sentence", c++ ); // b
        positions.put( "LinkToAbstract", c++ ); // c
        positions.put( "Reject", c++ ); // d
        positions.put( "Efferent/Outgoing", c++ ); //e 
        positions.put( "Afferent/Incoming", c++ ); //f
        positions.put( "Named entity error", c++ ); //g 
        positions.put( "Abbreviation error", c++ ); //h
        positions.put( "Ambiguous", c++ ); //i
        positions.put( "Comment", c++ ); //j 
        positions.put( "PairID", c++ ); //k
        positions.put( "PMID", c++ ); //l
        // not always used
        positions.put( "RegionAName", c++ ); //m
        positions.put( "RegionAResolve", c++ ); //n
        positions.put( "Resolve Evaluation A", c++ ); //o
        positions.put( "RegionBName", c++ );
        positions.put( "RegionBResolve", c++ );
        positions.put( "Resolve Evaluation B", c++ );

        positions.put( "Direction", c++ );
        positions.put( "Prediction", c++ );
        positions.put( "Previous Annotation", c++ );
        positions.put( "Score", c++ );
    }
}
