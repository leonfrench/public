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

package ubic.pubmedgate.statistics;

import gate.AnnotationSet;
import gate.Document;
import gate.util.IaaCalculation;

import java.util.List;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.loader.PubMedIDtoGate;
/*
 * Please use AnnotationComparator instead
 */
@Deprecated
public class AnnotationDifferences {
    PubMedIDtoGate p2g;
    int howManyToCheck = 231;

    // protected static Log log = LogFactory.getLog( AnnotationDifferences.class );

    public AnnotationDifferences() throws Exception {
        p2g = new PubMedIDtoGate();
    }

    // needs document/s, AS sets/names, annotation types (String)
    // print results, get results
    
    public void useIAA(String annotationSetNameOne, String annotationSetNameTwo) {
        // two columns for Lydia and Suzanne
        // howManyToCheck rows for documents Lydia has reached

        AnnotationSet[][] annMatrix = new AnnotationSet[howManyToCheck][2];
        AnnotationSet[][] singleMatrix = new AnnotationSet[1][2];
        List<ConnectionsDocument> docs = p2g.getRandomSubsetDocuments();
        for ( int i = 0; i < annMatrix.length; i++ ) {
            Document doc = docs.get( i );
            annMatrix[i][0] = doc.getAnnotations( annotationSetNameOne );
            annMatrix[i][1] = doc.getAnnotations( annotationSetNameTwo );

            singleMatrix[0][0] = doc.getAnnotations( annotationSetNameOne );
            singleMatrix[0][1] = doc.getAnnotations( annotationSetNameTwo );
            
            System.out.println(i +" Document:"+doc.getName());
            System.out.println("ConnectionPredicate:"+doc.getName());
            IaaCalculation IAACalc = new IaaCalculation( "ConnectionPredicate", singleMatrix, 1 );
            IAACalc.pairwiseIaaFmeasure();
            System.out.println(IAACalc.fMeasureOverall.printResults());
            //IAACalc.printResultsPairwiseFmeasures();

            System.out.println("BrainRegion:"+doc.getName());
            IaaCalculation IAACalcRegion = new IaaCalculation( "BrainRegion", singleMatrix, 1 );
            IAACalcRegion.pairwiseIaaFmeasure();
            System.out.println(IAACalcRegion.fMeasureOverall.printResults());
            //IAACalcRegion.printResultsPairwiseFmeasures();
        }

        System.out.println("ConnectionPredicate:");
        IaaCalculation IAACalc = new IaaCalculation( "ConnectionPredicate", annMatrix, 1 );
        IAACalc.pairwiseIaaFmeasure();
        IAACalc.printResultsPairwiseFmeasures();

        System.out.println("BrainRegion:");
        IaaCalculation IAACalcRegion = new IaaCalculation( "BrainRegion", annMatrix, 1 );
        IAACalcRegion.pairwiseIaaFmeasure();
        IAACalcRegion.printResultsPairwiseFmeasures();

        // IAACalc.

        // IaaCalculation(String nameAnnT, AnnotationSet[][] annsA2, int verbsy) {

    }

    public static void main( String args[] ) throws Exception {
        AnnotationDifferences ad = new AnnotationDifferences();
        ad.useIAA("Suzanne", "Lydia");
    }
}
