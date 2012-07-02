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

import gate.Corpus;
import gate.Document;
import gate.jape.Batch;

import java.net.URL;

import ubic.pubmedgate.Config;
import ubic.pubmedgate.GateInterface;

public class JoinMalletPredictions {

    String inputAS;
    String outputAS;
    Batch jape;
    Corpus corp;

    /**
     * @param fileName jape filename
     * @param inputAS input annotationset name
     * @param outputAS output annotationset name
     * @param corp corpus to process
     * @throws Exception
     */
    public JoinMalletPredictions( String fileName, String inputAS, String outputAS, Corpus corp ) throws Exception {
        this.inputAS = inputAS;
        this.outputAS = outputAS;
        jape = new Batch( new URL( "file:///" + fileName ), "UTF-8" );
        this.corp = corp;
    }

    /**
     * Uses the mallet join jape file and mallet annotation sets
     * 
     * @param corp
     * @throws Exception
     */
    public JoinMalletPredictions( Corpus corp ) throws Exception {
        this( Config.config.getString( "whitetext.malletJoin.jape.location" ),
                MalletPredictionsToGate.MALLET_PREDICTION_SET, MalletPredictionsToGate.MALLET_PREDICTION_SET, corp );
    }

    public String getOutputAS() {
        return outputAS;
    }

    public void execute() throws Exception {
        // jape.setVerbose(true);
        // jape.setEnableDebugging(true);
        for ( Object o : corp ) {
            Document doc = ( Document ) ( o );
            execute( doc );
        }
    }

    public void execute( Document doc ) throws Exception {
        jape.transduce( doc, doc.getAnnotations( inputAS ), doc.getAnnotations( outputAS ) );
        doc.sync();
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        GateInterface p2g = new GateInterface();
        Corpus corp = p2g.getTrainingCorp();
        // JoinMalletPredictions jm = new JoinMalletPredictions( corp );
        // jm.execute();

        JoinMalletPredictions jm = new JoinMalletPredictions( Config.config
                .getString( "whitetext.malletJoin.jape.location" ), MalletPredictionsToGate.MALLET_PREDICTION_SET,
                "MalletReverse", corp );
        jm.execute();

    }

}
