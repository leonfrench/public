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

import java.util.Set;

import ubic.BAMSandAllen.ABAMSDataMatrix;
import ubic.BAMSandAllen.AnalyzeBAMSandAllenGenes.Direction;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSDataLoader;
import ubic.BAMSandAllen.adjacency.IdentityAdjacency;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.pubmedgate.interactions.NormalizePairs;

public class GetBAMSDepth {
    public static void main( String args[] ) throws Exception {
        BAMSDataLoader bamsLoader = new BAMSDataLoader();
        Set<String> BAMSRegions = bamsLoader.getRegions();
        String test = BAMSRegions.iterator().next();
        System.out.println( test );
        System.out.println( bamsLoader.getParent( test ) );
        System.out.println( bamsLoader.getParents( test ) );

        // only use connected regions!!
        Direction direction = Direction.ANYDIRECTION;
        DoubleMatrix<String, String> BAMSconnectionMatrix = NormalizePairs.getBAMSConnectionMatrix( true, direction );

        ABAMSDataMatrix BAMSMatrix = new ABAMSDataMatrix( BAMSconnectionMatrix, "BAMSconnectionMatrix",
                new IdentityAdjacency( BAMSconnectionMatrix ) );
        BAMSMatrix = BAMSMatrix.removeZeroColumns();
        BAMSMatrix = BAMSMatrix.removeZeroRows();

        System.out.println( "Size:" + BAMSMatrix.getRowNames().size() );
        int totalDepth = 0;
        for ( String region : BAMSMatrix.getRowNames() ) {
            totalDepth += bamsLoader.getParents( region ).size();
        }
        // is not based on connections! don't use
        System.out.println( "Average depth:" + ( totalDepth / ( double ) BAMSMatrix.getRowNames().size() ) );

        // StructureCatalogLoader dong = new StructureCatalogLoader();
        // Set<String> dongRegions = dong.getRegions();
        //
        // for ( String dongRegion : dongRegions ) {
        // for ( String bams : BAMSRegions ) {
        // if ( dongRegion.toLowerCase().trim().equals( bams.toLowerCase().trim() ) ) {
        // if ( dong.getBAMSMappedRegions( dongRegion ) == null ) {
        // System.out.println( bams );
        // }
        // }
        // }
        // }

    }

}
