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

import gate.Annotation;

import java.util.List;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

/**
 * Merges brain region annotations, does NOT deal with connections or connection predicates. So it's only good for NER
 * 
 * @author leon
 * 
 */
public class MakeMergedBrainRegions {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        GateInterface gateInt = new GateInterface();
        List<ConnectionsDocument> docs = gateInt.getDocuments();

        for ( ConnectionsDocument doc : docs ) {
            for ( Annotation ann : doc.getBrainRegionAnnotations( "Suzanne" ) ) {
                // copy to union and intersect
            }
            for ( Annotation ann : doc.getBrainRegionAnnotations( "Suzanne" ) ) {
                // copy to union and intersect
            }
            /*
             * if two in union overlap
             * remove the two
             * make a new one
             * expand to max borders
             * do above untill no more found
             */
            /*
             * if two in intersect overlap
             * remove the two
             * make a new one
             * reduce to min borders
             * do above untill no more found
             */
            
        }
    }

}
