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
import gate.util.ContingencyTable;
import gate.util.FMeasure;
import gate.util.IaaCalculation;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Util;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.params.ParameterGrabber;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

public class AnnotationComparator {
    protected static Log log = LogFactory.getLog( AnnotationComparator.class );

    public String annotationSetName1;
    public String annotationSetName2;
    public String annotationType;

    public String getAnnotationType() {
        return annotationType;
    }

    public AnnotationComparator( String annotationSetName1, String annotationSetName2, String annotationType ) {
        super();
        this.annotationSetName1 = annotationSetName1;
        this.annotationSetName2 = annotationSetName2;
        this.annotationType = annotationType;
    }

    public FMeasure compute( ConnectionsDocument doc ) {
        List<ConnectionsDocument> l = new LinkedList<ConnectionsDocument>();
        l.add( doc );
        return computeFMeasure( l );
    }

    public FMeasure computeFMeasure( List<ConnectionsDocument> docs ) {
        IaaCalculation IAACalcRegion = compute( docs );
        IAACalcRegion.pairwiseIaaFmeasure();
        return IAACalcRegion.fMeasureOverall;
    }

    public ContingencyTable computeKappa( List<ConnectionsDocument> docs ) {
        IaaCalculation IAACalcRegion = compute( docs );
        IAACalcRegion.pairwiseIaaKappa();
        return IAACalcRegion.contingencyOverall;
    }

    public IaaCalculation compute( List<ConnectionsDocument> docs ) {
        log.info( "Comparing " + annotationType + " annotations between " + annotationSetName1 + " and "
                + annotationSetName2 );

        AnnotationSet[][] annMatrix = getAnnotationSetMatrix( docs );

        IaaCalculation IAACalcRegion = new IaaCalculation( annotationType, annMatrix, 1 );
        return IAACalcRegion;
    }

    private AnnotationSet[][] getAnnotationSetMatrix( List<ConnectionsDocument> docs ) {
        AnnotationSet[][] annMatrix = new AnnotationSet[docs.size()][2];
        Document[] docArray = docs.toArray( new Document[0] );

        for ( int i = 0; i < annMatrix.length; i++ ) {
            Document doc = docArray[i];
            annMatrix[i][0] = doc.getAnnotations( annotationSetName1 );
            annMatrix[i][1] = doc.getAnnotations( annotationSetName2 );
        }
        return annMatrix;
    }

    public FMeasure getStats( Document doc ) {
        AnnotationSet[][] singleMatrix = new AnnotationSet[1][2];
        singleMatrix[0][0] = doc.getAnnotations( annotationSetName1 );
        singleMatrix[0][1] = doc.getAnnotations( annotationSetName2 );
        IaaCalculation IAACalc = new IaaCalculation( annotationType, singleMatrix, 1 );
        IAACalc.pairwiseIaaFmeasure();
        FMeasure f = IAACalc.fMeasureOverall;
        return f;
    }

    public DoubleMatrix<String, String> getMatrix( List<ConnectionsDocument> docs ) {
        log.info( "Comparing " + annotationType + " annotations between " + annotationSetName1 + " and "
                + annotationSetName2 );
        Document[] docArray = docs.toArray( new Document[0] );

        DoubleMatrix<String, String> resultMatrix = null;

        for ( int i = 0; i < docArray.length; i++ ) {
            Document doc = docArray[i];
            String name = doc.getName();
            FMeasure f = getStats( doc );

            Map<String, String> results = ParameterGrabber.getParams( f.getClass(), f );
            results.put( "index", "" + i );
            List<String> keys = new LinkedList<String>( results.keySet() );

            if ( i == 0 ) {
                // set up matrix on first document
                // sort before adding results
                Collections.sort( keys );
                resultMatrix = new DenseDoubleMatrix<String, String>( docArray.length, keys.size() );
                resultMatrix.setColumnNames( keys );
            }
            resultMatrix.addRowName( name );
            for ( String key : keys ) {
                resultMatrix.setByKeys( name, key, Double.parseDouble( results.get( key ) ) );
            }
        }
        return resultMatrix;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        GateInterface p2g = new GateInterface();
        AnnotationComparator ac;
        FMeasure f;
        String tokenType = "BrainRegion";

        ac = new AnnotationComparator( "Suzanne", "Lydia", tokenType );
        f = ac.computeFMeasure( p2g.getDocuments( p2g.getRandomSubsetCorp() ) );
        System.out.println( f.f1 );
        System.out.println( f.printResults() );

    }

    public static void main2( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        GateInterface p2g = new GateInterface();
        AnnotationComparator ac;
        FMeasure f;
        String tokenType = "BrainRegion";

        if ( args.length == 3 ) {
            tokenType = args[2];
        }
        log.info( "Token type = " + tokenType );
        if ( args.length > 1 ) {
            System.out.println( "Command line args:" + args[0] + args[1] );
            ac = new AnnotationComparator( args[0], args[1], tokenType );
            f = ac.computeFMeasure( p2g.getTrainingDocuments() );
            System.out.println( f.f1 );
            System.out.println( f.printResults() );
            System.exit( 1 );
        }

        // ac = new AnnotationComparator( "UnionMerge", "TextPressoLookup", tokenType );
        // f = ac.computeFMeasure( p2g.getDocuments(p2g.getNoAbbrevCorp()) );
        // System.out.println( f.f1 );
        // System.out.println( f.printResults() );
        // System.exit( 1 );

        ac = new AnnotationComparator( "UnionMerge", "NNLookup", tokenType );
        f = ac.computeFMeasure( p2g.getDocuments( p2g.getCorp() ) );
        System.out.println( f.printResults() );
        DoubleMatrix<String, String> matrix = ac.getMatrix( p2g.getDocuments() );
        // matrix.toString();
        Util.writeRTable( "Docs.txt.csv", matrix );
        System.exit( 1 );

        System.out.println( "docs with zero Mallet brain regions:"
                + ac.countZeroAnnotationDocuments( "Mallet", p2g.getTrainingDocuments(), tokenType ) );
        System.out.println( "docs with zero MalletReverse brain regions:"
                + ac.countZeroAnnotationDocuments( "MalletReverse", p2g.getTrainingDocuments(), tokenType ) );
    }

    public int countZeroAnnotationDocuments( String setName, List<ConnectionsDocument> docs, String tokenType ) {
        int result = 0;
        for ( ConnectionsDocument doc : docs ) {
            if ( doc.getAnnotationsByType( setName, tokenType ).size() == 0 ) {
                result++;
                // System.out.println( "------No annots-------------" );
                // System.out.println( doc.getName() );
                // System.out.println( f.printResults() );
                // System.out.println( "----------------------" );
            }
        }
        return result;
    }
}
