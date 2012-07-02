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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.SetupParameters;
import ubic.BAMSandAllen.Util;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;

public class ExploreLiteratureMatrices {
    protected static Log log = LogFactory.getLog( ExploreLiteratureMatrices.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        String base = "/grp/java/workspace/PubMedIDtoGate/spreadsheets/interaction.results/matrices/";
        String filename = base;
        filename += "Positives.WhiteTextUnseen.matrix.txt.propigated";
                     
        // filename += "Positives.rat.WhiteTextUnseen.matrix.txt";
        // filename += "Negatives.rat.WhiteTextUnseen.matrix.txt";

        DoubleMatrixReader matrixReader = new DoubleMatrixReader();
        DoubleMatrix<String, String> connectionMatrix = matrixReader.read( filename );
        List<String> regions = connectionMatrix.getRowNames();
        if ( !connectionMatrix.getRowNames().equals( connectionMatrix.getColNames() ) ) {
            throw new RuntimeException( "error row and col names in diff order" );
        }

        for ( String row : regions ) {
            for ( String col : regions ) {
                double connectionMatrixValue = connectionMatrix.getByKeys( row, col );
                if ( connectionMatrixValue > 0 ) {
                    connectionMatrix.setByKeys( row, col, 1d );
                } else {
                    connectionMatrix.setByKeys( row, col, 0d );
                }
            }
        }
        Util.writeImage( SetupParameters.getDataFolder() + "Literature.connectionMatrix.prop.png", connectionMatrix );

    }

}
