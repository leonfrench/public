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

import gate.Annotation;
import gate.AnnotationSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.connection.Connection;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.spanFilters.AcceptAllSpanFilter;
import ubic.pubmedgate.interactions.spanFilters.KeywordSpanFilter;
import ubic.pubmedgate.interactions.spanFilters.SpanFilter;

public class Cooccurance {
    private static Log log = LogFactory.getLog( Cooccurance.class.getName() );
    GateInterface p2g;
    // sentence level as param!
    List<ConnectionsDocument> docs;
    String spanType, spanSet;
    boolean sentenceLevel;

    public Cooccurance( boolean sentenceLevel ) {
        p2g = new GateInterface();
        // just the connection documents would be biased
        // docs = p2g.getConnectionDocuments();
        // docs = p2g.getTrainingDocuments();
        docs = p2g.getDocuments();
        // docs = p2g.getRandomSubsetDocuments();
        log.info( "Documents:" + docs.size() );

        if ( sentenceLevel ) {
            spanSet = "GATETokens";
            spanType = "Sentence";
        } else {
            spanSet = "Original markups";
            spanType = "PubmedArticle";
        }
        this.sentenceLevel = sentenceLevel;
    }

    public ConnectionList getAnnotatedConnections( String author ) {
        ConnectionList result = new ConnectionList( author );
        for ( ConnectionsDocument doc : p2g.getConnectionDocuments() ) {
            result.addAllFromDocument( doc );
        }
        result.removeSymetrics();
        return result;
    }

    // old code to check if it's working
    public void printMissingMatches() {
        IteractionEvaluator evaluator = new IteractionEvaluator();
        int total = 0;
        int equal = 0;
        for ( ConnectionsDocument doc : docs ) {
            List<ConnectionsDocument> touse = new LinkedList<ConnectionsDocument>();
            touse.add( doc );
            ConnectionList cooccur = iterate( Integer.MAX_VALUE, touse, new AcceptAllSpanFilter() );

            ConnectionList manual = new ConnectionList( "Suzanne" );
            manual.addAllFromDocument( doc );

            List<Connection> missing = evaluator.getMissingConnections( manual, cooccur );
            if ( missing.size() > 0 ) {
                log.info( "PMID:" + doc.getPMID() + " has " + missing.size() + " of "
                        + doc.getConnections( "Suzanne" ).size() );
                total += missing.size();
                for ( Connection miss : missing ) {
                    Annotation a = miss.getPartnerA();
                    Annotation b = miss.getPartnerB();
                    if ( a.coextensive( b ) ) equal++;
                    log.info( "Equal:" + a.coextensive( b ) + " Types:" + a.getType() + " -> " + b.getType() + " " );
                    if ( !a.coextensive( b ) )
                        log.info( "" + doc.getAnnotationText( a ) + " -> " + doc.getAnnotationText( b ) );
                }
            }
        }
        log.info( "Total missing:" + total );
        log.info( "Equal missing:" + equal );
    }

    public ConnectionList iterate() {
        boolean sentenceLevel = true;
        return iterate( Integer.MAX_VALUE );
    }

    // setting for abstract or sentence
    public ConnectionList iterate( int maxRegionsInSentence ) {
        return iterate( maxRegionsInSentence, docs, new AcceptAllSpanFilter() );
    }

    public ConnectionList iterate( int maxRegionsInSentence, SpanFilter filter ) {
        return iterate( maxRegionsInSentence, docs, filter );
    }

    public CountingMap<Integer> getHistogram( String annotationSet ) {
        CountingMap<Integer> histogram = new CountingMap<Integer>();
        for ( ConnectionsDocument doc : docs ) {
            List<Annotation> spans = doc.getAnnotationsByType( spanSet, spanType );
            // log.info( sentences.size() );
            for ( Annotation span : spans ) {
                AnnotationSet annotSet = doc.getAnnotations( annotationSet );
                annotSet = annotSet.get( "BrainRegion" );
                annotSet = annotSet.getContained( span.getStartNode().getOffset(), span.getEndNode().getOffset() );
                int regionCount = annotSet.size();
                histogram.increment( regionCount );
            }
        }
        log.info( "Histogram:" );
        int sum = 0;
        for ( Integer key : histogram.sortedKeyList( true ) ) {
            log.info( "  " + key + ":" + histogram.get( key ) );
            sum += histogram.get( key );
        }
        log.info( "Total:" + sum + " spans" );

        return histogram;
    }

    public ConnectionList iterate( int maxRegionsInSentence, List<ConnectionsDocument> docsToUse, SpanFilter filter ) {
        ConnectionList result = new ConnectionList( "Co-occurance." + maxRegionsInSentence + ".limit" );

        String annotationSet = "Suzanne";
        for ( ConnectionsDocument doc : docsToUse ) {
            // each sentence
            List<Annotation> spans = doc.getAnnotationsByType( spanSet, spanType );
            // log.info( sentences.size() );
            for ( Annotation span : spans ) {
                // get brain regions in each sentence, infer connection between them
                if ( !filter.accept( doc, span ) ) {
                    break;
                }
                AnnotationSet annotSet = doc.getAnnotations( annotationSet );
                annotSet = annotSet.get( "BrainRegion" );
                // log.info( annotSet.size() );
                // annotSet = annotSet.get( span.getStartNode().getOffset(), span.getEndNode().getOffset() );
                annotSet = annotSet.getContained( span.getStartNode().getOffset(), span.getEndNode().getOffset() );

                int regionCount = annotSet.size();
                if ( regionCount > 1 ) {
                    // log.info( regionCount + " " + doc.getAnnotationText( sentence ) );
                }

                if ( regionCount > 1 && regionCount <= maxRegionsInSentence ) {
                    for ( Annotation anotA : annotSet ) {
                        for ( Annotation anotB : annotSet ) {
                            // dont do symetric relations
                            if ( anotA.getId() <= anotB.getId() ) continue;
                            result.add( new Connection( anotA, anotB ), doc );
                        }
                    }

                }
            }
        }
        // log.info( result.size() + " connections made" );
        return result;
    }

    public void getAgreement() throws Exception {

        // ConnectionList suzanne = new ConnectionList( "Suzanne" );
        // for ( ConnectionsDocument doc : p2g.getRandomSubsetCorp() ) {
        // suzanne.addAll( doc.getConnections( "Suzanne" ) );
        // }
        //
        // ConnectionList lydia = new ConnectionList( "Lydia" );
        // for ( ConnectionsDocument doc : p2g.getRandomSubsetCorp() ) {
        // suzanne.addAll( doc.getConnections( "Lydia" ) );
        // }

        IteractionEvaluator evaluator = new IteractionEvaluator();

        // Map<String, String> results = evaluator.compare( suzanne, lydia );
        // log.info( results.toString() );

    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {

        boolean sentenceLevel = true;

        ParamKeeper keeper = new ParamKeeper();

        // check document list in constructor
        Cooccurance coc = new Cooccurance( sentenceLevel );
        // coc.getAgreement();
        // System.exit( 1 );

        // coc.printMissingMatches();
        // System.exit( 1 );

        ConnectionList manual = coc.getAnnotatedConnections( "Suzanne" );
        log.info( "size:" + manual.size() );

        System.exit( 1 );

        IteractionEvaluator evaluator = new IteractionEvaluator();

        CountingMap<Integer> counts = coc.getHistogram( "Suzanne" );
        Integer i = Integer.MAX_VALUE;
        boolean usePositions = false;
        // for ( Integer i : counts.sortedKeyList() ) {
        ConnectionList cooccur = coc.iterate( i, new KeywordSpanFilter() );
        Map<String, String> params = evaluator.compare( manual, cooccur, usePositions );
        params.put( "threshold", i + "" );
        params.put( "usePositions", usePositions + "" );

        keeper.addParamInstance( params );
        log.info( "Running:" + i );
        // }
        keeper.writeExcel( Config.config.getString( "whitetext.iteractions.results.folder" )
                + "Cooccurance.rule.random.txt.sentence." + sentenceLevel + ".xls" );

    }
}
