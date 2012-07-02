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

package ubic.pubmedgate.resolve.evaluation;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.Util;
import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.connection.Connection;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.organism.LinnaeusSpeciesTagger;
import ubic.pubmedgate.organism.SpeciesLoader;
import ubic.pubmedgate.resolve.depreciated.ResolveBrianRegions;
import ubic.pubmedgate.statistics.GetStats;

public class SpeciesEvaluation {
    protected static Log log = LogFactory.getLog( SpeciesEvaluation.class );

    GateInterface p2g;
    ResolveBrianRegions resolver;
    GetStats stats;
    LinnaeusSpeciesTagger linTagger;

    public SpeciesEvaluation() {
        p2g = new GateInterface();
        resolver = new ResolveBrianRegions( p2g.getConnectionDocuments(), "UnionMerge" );
        stats = new GetStats( p2g );
        linTagger = new LinnaeusSpeciesTagger( p2g, p2g.getCorp() );
    }

    public void Lincooccurances() throws Exception {
        SpeciesLoader loader = new SpeciesLoader();
        List<ConnectionsDocument> documents = p2g.getDocuments();
        Set<String> allSpecies = loader.getAllIDs();

        DoubleMatrix<String, String> matrix = new DenseDoubleMatrix<String, String>( allSpecies.size(), allSpecies
                .size() );
        matrix.setRowNames( new LinkedList<String>( allSpecies ) );
        matrix.setColumnNames( new LinkedList<String>( allSpecies ) );

        for ( ConnectionsDocument doc : documents ) {
            Set<String> taggedSpecies = doc.getLinnaeusSpecies();
            for ( String speciesA : taggedSpecies ) {
                for ( String speciesB : taggedSpecies ) {
                    double value = matrix.getByKeys( speciesA, speciesB );
                    value += 1;
                    matrix.setByKeys( speciesA, speciesB, value );
                }
            }
        }

        for ( String a : matrix.getRowNames() ) {
            for ( String b : matrix.getRowNames() ) {
                if ( a.equals( b ) ) continue;
                double value = matrix.getByKeys( a, b );
                if ( value > 10 )
                    System.out.println( b + ":" + loader.getTaggedNamesFromID( b ) + " : " + a + ":"
                            + loader.getTaggedNamesFromID( a ) + " = " + value );
            }
        }

        Util.writeImage( "/grp/java/workspace/PubMedIDtoGate/SpeciesCoOccur.png", matrix );
        Util.writeRTable( "/grp/java/workspace/PubMedIDtoGate/SpeciesCoOccur.txt", matrix );
    }

    public void compare() throws Exception {
        // go through all abstracts
        // get human species IDs
        // get lineaus species IDs
        ParamKeeper params = new ParamKeeper();
        SpeciesLoader loader = new SpeciesLoader();
        List<ConnectionsDocument> documents = p2g.getDocuments();
        for ( ConnectionsDocument doc : documents ) {
            Map<String, String> result = new HashMap<String, String>();
            Set<String> taggedSpecies = doc.getLinnaeusSpecies();
            taggedSpecies.removeAll( loader.getFilteredIDs() );

            Set<String> annotatedSpecies = new HashSet<String>();
            Set<String> unMappedComments = new HashSet<String>();
            Set<String> comments = new HashSet<String>();

            if ( doc.getConnections() != null ) {
                for ( Connection c : doc.getConnections() ) {
                    // log.info( c.getComment() );
                    String anSpecies = c.getComment();
                    if ( anSpecies != null ) {
                        Set<String> mappedIDs = loader.getIDfromAnnotatedSpecies( anSpecies );
                        comments.add( anSpecies );
                        if ( mappedIDs != null ) {
                            annotatedSpecies.addAll( mappedIDs );
                        } else {
                            // if the comment/species tag cannot be mapped and is not blank, then keep track
                            if ( !anSpecies.equals( "" ) && !anSpecies.equals( "animal not identified" ) )
                                unMappedComments.add( anSpecies );
                        }
                    }
                }
            }

            result.put( "Intersection Size", "" + Util.intersectSize( taggedSpecies, annotatedSpecies ) );
            result.put( "Lineaus Size", "" + taggedSpecies.size() );
            result.put( "Mapped Annotated Size", "" + annotatedSpecies.size() );
            result.put( "Unmapped Annotated Size", "" + unMappedComments.size() );
            result.put( "Lineaus", taggedSpecies.toString() );
            result.put( "Annotated", annotatedSpecies.toString() );
            result.put( "Unmapped Annotated", unMappedComments.toString() );
            result.put( "Intersection", "" + Util.intersect( taggedSpecies, annotatedSpecies ) );
            result.put( "Annotations", comments.toString() );
            result.put( "PMID", doc.getPMID() );

            params.addParamInstance( result );
            // compute precision + recall, pre document?
            // output sizes and intersect for everyone, then use spreadsheet
        }
        params.writeExcel( "/grp/java/workspace/PubMedIDtoGate/spreadsheets/" + "speciesEvaluation.xls" );
    }

    public void iterateManual() throws Exception {
        // load in the ID to name spreadsheet
        SpeciesLoader loader = new SpeciesLoader();
        FileWriter writer = new FileWriter( "/grp/java/apps/linnaeus/workingDir/test.csv" );
        CountingMap<String> annotatedText = stats.getAnnotatedSpecies();
        for ( String key : annotatedText.sortedKeyList( true ) ) {
            Set<String> species = linTagger.tagText( key );
            for ( String spec : species ) {
                // log.info( key + " -> " + species );
                writer.write( key + "|" + spec + "|" + loader.getTaggedNamesFromID( spec ) + "\n" );
            }
            if ( species.isEmpty() ) {
                writer.write( key + "|" + "\n" );
            }
        }
        writer.close();
        System.exit( 1 );

        List<ConnectionsDocument> documents = p2g.getDocuments();
        for ( ConnectionsDocument doc : documents ) {
            Set<String> species = doc.getLinnaeusSpecies();
            if ( doc.getConnections() != null ) {
                for ( Connection c : doc.getConnections() ) {
                    log.info( c.getComment() );
                }
            }
        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        SpeciesEvaluation se = new SpeciesEvaluation();
        // se.iterateManual();
        // se.compare();
        se.Lincooccurances();
    }

}
