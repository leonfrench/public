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
import gate.util.FMeasure;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Util;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.statistics.AnnotationComparator;

public abstract class MalletRunner {
    protected static Log log = LogFactory.getLog( MalletRunner.class );

    String truthSet;
    Corpus corpus;
    List<ConnectionsDocument> documents;
    boolean skipAbbrev;
    boolean sentenceLevel;
    int numFolds;
    String annotationSet;
    String dataStoreLocation;
    String inputTokenSet;

    public abstract void run() throws Exception;

    public void joinAndEvaluate() throws Exception {
        join();
        evaluate();
    }

    public void join() throws Exception {
        join( corpus );
    }

    public void join( Corpus corp ) throws Exception {
        log.info( "Joining predictions" );
        JoinMalletPredictions jm = new JoinMalletPredictions( Config.config
                .getString( "whitetext.malletJoin.jape.location" ), annotationSet, annotationSet, corp );
        jm.execute();
        log.info( "Annotation Set:" + jm.getOutputAS() );
    }

    public abstract void writeOutResults();

    public FMeasure evaluate() {
        return evaluate( truthSet );
    }

    public FMeasure evaluate( String truthSet ) {
        AnnotationComparator ac = new AnnotationComparator( truthSet, annotationSet, "BrainRegion" );
        FMeasure f = ac.computeFMeasure( documents );
        System.out.println( f.f1 + " Truth set:" + truthSet );
        System.out.println( f.printResults() );
        return f;
    }

    public void writeIndividualFMeasures() throws Exception {
        AnnotationComparator ac = new AnnotationComparator( truthSet, annotationSet, "BrainRegion" );
        DoubleMatrix<String, String> matrix = ac.getMatrix( documents );
        String filename = "FMeasures." + truthSet + "." + annotationSet + "." + inputTokenSet + ".txt.csv";
        Util.writeRTable( filename, matrix );
    }

    public void reset() throws Exception {
        // reset docs
        GateReseter reset = new GateReseter( documents, annotationSet );
        reset.reset();
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public List<ConnectionsDocument> getDocuments() {
        return documents;
    }

    public int getNumFolds() {
        return numFolds;
    }

    public boolean isSentenceLevel() {
        return sentenceLevel;
    }

    public boolean isTestUnlabelled() {
        return false;
    }

    public boolean isSkipAbbrev() {
        return skipAbbrev;
    }

    public String getAnnotationSet() {
        return annotationSet;
    }

    public void setSentenceLevel( boolean sentenceLevel ) {
        this.sentenceLevel = sentenceLevel;
    }

}
