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

package ubic.pubmedgate.interactions;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.pubmedgate.GateInterface;

/**
 * Class for looking at SL prediction results. Can print out error sentences with pairs in bold.
 * 
 * @author leon
 */
public class ShowSLErrors {
    protected static Log log = LogFactory.getLog( ShowSLErrors.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {

        String filename = "/home/leon/ppi-benchmark/Corpora/Original-Modified/WhiteTextMalletAbbrFix.xml";
        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();

        AirolaXMLReader XMLReader = new AirolaXMLReader( filename, p2g, "Suzanne" );
        // String baseFolder = "/home/leon/ppi-benchmark/Experiments/SL/CV/predict/WhiteText";
        String baseFolder = "/home/leon/ppi-benchmark/Experiments/SL/CV/predict/WhiteTextMalletAbbrFix";
        // String baseFolder = "/home/leon/ppi-benchmark/Experiments/SL/CV/predict/Small";
        SLOutputReader SLReader = new SLOutputReader( new File( baseFolder ) );

        writeExamples( XMLReader, SLReader, SLReader.getFalseNegatives(), baseFolder + "/getFalseNegatives.html" );
        writeExamples( XMLReader, SLReader, SLReader.getFalsePositives(), baseFolder + "/getFalsePositives.html" );
        writeExamples( XMLReader, SLReader, SLReader.getTruePositives(), baseFolder + "/getTruePositives.html" );
        writeExamples( XMLReader, SLReader, SLReader.getTrueNegatives(), baseFolder + "/getTrueNegatives.html" );

        writeExamples( XMLReader, SLReader, SLReader.getTrueNegatives(), baseFolder + "/getPositives.html" );
        writeExamples( XMLReader, SLReader, SLReader.getTrueNegatives(), baseFolder + "/getNegatives.html" );

        log.info( "Positives:" + SLReader.getPositives().size() );
        log.info( "Negatives:" + SLReader.getNegatives().size() );

        log.info( "Positive predictions:" + SLReader.getPositivePredictions().size() );
        log.info( "Negative predictions:" + SLReader.getNegativePredictions().size() );

        log.info( "False positives:" + SLReader.getFalsePositives().size() );
        log.info( "True positives:" + SLReader.getTruePositives().size() );
        log.info( "True negatives:" + SLReader.getTrueNegatives().size() );
        log.info( "False negatives:" + SLReader.getFalseNegatives().size() );

        SLReader.writeRTable();
    }

    public static void writeExamples( AirolaXMLReader XMLReader, SLOutputReader SLReader, List<String> pairs,
            String filename ) throws Exception {
        pairs = SLReader.sortPairUsingScore( pairs );
        String result = "<html>";
        for ( String pair : pairs ) {
            result += XMLReader.getUnderLineSentence( pair ) + " | " + pair + ", Score:"
                    + SLReader.getScores().get( pair ) + ", PMID:" + XMLReader.getPairIDToPMID().get( pair )
                    + "</br></br>";
        }
        result += "</html>";
        FileTools.stringToFile( result, new File( filename ) );

    }
}
