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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ubic.BAMSandAllen.Util;
import ubic.basecode.util.FileTools;
import ubic.connection.Connection;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.evaluation.AllCuratorsCombined;
import ubic.pubmedgate.interactions.evaluation.LoadInteractionSpreadsheet;

/**
 * Just some throwaway code that loads abstracts with connections for use with mscanner
 * 
 * @author leon
 */
public class GetAbstractsWithConnections {

    /**
     * Get abstracts that had a marked connection in the final2000 evaluation
     * 
     * @throws Exception
     */

    public static void getFromEvaluation() throws Exception {
        LoadInteractionSpreadsheet sheet = AllCuratorsCombined.getFinal2000Results();

        String annotationSet = "Mallet";
        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();
        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );

        String corpusFilename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Original-Modified/WhiteTextUnseen.orig.xml";
        AirolaXMLReader reader = new AirolaXMLReader( corpusFilename, p2g, annotationSet );
        boolean useAll = true;
        AirolaForEvaluations test = new AirolaForEvaluations( reader );
        FileTools.stringsToFile( test.getAbstractsWithConnectionsIDs(),
                Config.config.getString( "whitetext.iteractions.results.folder" ) + "Eval2000ConIDs.txt" );

    }

    public static void getFromOriginal() throws Exception {
        GateInterface p2g = new GateInterface();
        p2g.setUnSeenCorpNull();
        p2g.setNamedCorpNull( "PubMedUnseenJNChem" );
        p2g.setNamedCorpNull( "PubMedUnseenJCN" );
        Set<String> result = new HashSet<String>();
        for ( ConnectionsDocument doc : p2g.getDocuments() ) {
            List<Connection> x = doc.getConnections();
            if ( x != null && x.size() > 0 ) {
                result.add( doc.getPMID() );
            }
        }
        FileTools.stringsToFile( result, Config.config.getString( "whitetext.iteractions.results.folder" )
                + "OriginalConIDs.txt" );
    }

    public static void getAllLoaded() throws Exception {
        GateInterface p2g = new GateInterface();
        FileTools.stringsToFile( p2g.getLoadedPMIDs(), Config.config.getString( "whitetext.iteractions.results.folder" )
                + "allLoadedPMIDs.txt" );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // getAllLoaded();
        // getFromEvaluation();
        // getFromOriginal();
        // System.exit( 1 );

        List<String> loaded = FileTools
                .getLines( "/grp/java/workspace/PubMedIDtoGate/spreadsheets/interaction.results/PMID Lists/AllLoadedPMIDs.txt" );
        List<String> mscanner3 = FileTools
                .getLines( "/grp/java/workspace/PubMedIDtoGate/spreadsheets/interaction.results/PMID Lists/MScanner Results 3 eval2000 input/results.PMIDs.txt" );
        List<String> mscanner2 = FileTools
                .getLines( "/grp/java/workspace/PubMedIDtoGate/spreadsheets/interaction.results/PMID Lists/Mscanner Results 2/results.PMIDs.txt" );

        Set<String> union = ( Set<String> ) Util.union( mscanner2, mscanner3 );
        union.removeAll( loaded );

        FileTools
                .stringsToFile(
                        union,
                        "/grp/java/workspace/PubMedIDtoGate/spreadsheets/interaction.results/PMID Lists/MScanner Results 3 eval2000 input/results.PMIDs.union.minus.loaded.txt" );
        System.exit( 1 );
        // TODO add in set of 2000 annotated sentences
        // TODO add in set of not in BAMS sentences
        // java -Xms6g -Xmx56g -classpath
        // commons-lang-2.3.jar:kea.jar:poi-3.5-beta6.jar:xercesImpl-2.8.1.jar:WhiteTextDeps.jar:WhiteText.jar:.
        // ubic.pubmedgate.interactions.focusedAnalysis.GetAbstractsWithConnections
    }
}
