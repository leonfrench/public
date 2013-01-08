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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Util;

public class AllUnseenEvaluationsCombined {
    protected static Log log = LogFactory.getLog( LoadInteractionSpreadsheet.class );

    LoadInteractionSpreadsheet final2000, first6000, second6000, notInBAMS;

    public AllUnseenEvaluationsCombined() throws Exception {
        final2000 = AllCuratorsCombined.getFinal2000Results();
        first6000 = AllCuratorsCombined.getFirst6000Results();
        second6000 = AllCuratorsCombined.getSecond6000Results();
        notInBAMS = AllCuratorsCombined.getNotInBAMSResults();
    }

    public Set<String> getAllAcceptedPairs() throws Exception {
        Set<String> result = new HashSet<String>();
        result.addAll( final2000.getAcceptedPairs() );
        result.addAll( first6000.getAcceptedPairs() );
        result.addAll( second6000.getAcceptedPairs() );
        result.addAll( notInBAMS.getAcceptedPairs() );
        return result;
    }

    public Set<String> getAllRejectedPairs() throws Exception {
        return ( Set<String> ) Util.subtract( getAllEvaluatedPairs(), getAllAcceptedPairs() );
    }

    public Set<String> getAllEvaluatedPairs() throws Exception {
        Set<String> result = new HashSet<String>();
        result.addAll( final2000.getAllPairs() );
        result.addAll( first6000.getAllPairs() );
        result.addAll( second6000.getAllPairs() );
        result.addAll( notInBAMS.getAllPairs() );
        return result;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        AllUnseenEvaluationsCombined test = new AllUnseenEvaluationsCombined();
        log.info( "Evaluated:" + test.getAllEvaluatedPairs().size() );
        log.info( "Accepted:" + test.getAllAcceptedPairs().size() );

    }

}
