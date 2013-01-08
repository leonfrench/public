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

package ubic.pubmedgate.interactions.focusedAnalysis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.AnalyzeBAMSandAllenGenes.Direction;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSDataLoader;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.pubmedgate.interactions.NormalizePairs;

public class CheckPairLevel {
    protected static Log log = LogFactory.getLog( CheckPairLevel.class );

    public static void getPairDepthForBAMS( boolean propigated ) throws Exception {
        Direction direction = Direction.ANYDIRECTION;

        DoubleMatrix<String, String> dataMatrix = NormalizePairs.getBAMSConnectionMatrix( propigated, direction );

        BAMSDataLoader bamsLoader = new BAMSDataLoader();

        int totalDepth = 0;
        int connections = 0;
        for ( String row : dataMatrix.getRowNames() ) {
            for ( String col : dataMatrix.getRowNames() ) {
                if ( dataMatrix.getByKeys( row, col ) == 1d ) {
                    // log.info( row + " " + col );
                    if ( connections % 5000 == 0 ) log.info( connections );
                    connections++;
                    totalDepth += bamsLoader.getParents( row ).size();
                    totalDepth += bamsLoader.getParents( col ).size();
                }
            }
        }
        log.info( "Depth:" + ( totalDepth / ( double ) connections )  + " propigated:" + propigated);

    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        getPairDepthForBAMS( false );
        getPairDepthForBAMS( true );

    }

}
