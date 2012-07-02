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

package ubic.pubmedgate.editors;

import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.DocumentContent;
import gate.corpora.DocumentContentImpl;
import gate.corpora.DocumentImpl;
import gate.util.SimpleFeatureMapImpl;

import java.util.List;

import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.mallet.JoinMalletPredictions;
import ubic.pubmedgate.ner.TokenTargetLabeller;

public class RevertAbbreviations {
    GateInterface p2g;
    Corpus corp;

    public RevertAbbreviations( GateInterface p2g ) {
        this.p2g = p2g;
        corp = p2g.getNoAbbrevCorp();
    }

    public void revert( Document doc, String annotatorA ) throws Exception {
        String type = "BrainRegion";

        AnnotationSet aSetA = doc.getAnnotations( annotatorA );
        aSetA = aSetA.get( type );

        AnnotationSet shortAbbrevs = doc.getAnnotations( "Original markups" );
        shortAbbrevs = shortAbbrevs.get( "AbbrevShort" );

        AnnotationSet longAbbrevs = doc.getAnnotations( "Original markups" );
        longAbbrevs = longAbbrevs.get( "Abbrev" );

        DocumentContent cont = doc.getContent();

        Document newDoc = new DocumentImpl();
        newDoc.setName( doc.getName() + "ABBREV" );

        String newCont = "";

        AnnotationSet temp = newDoc.getAnnotations( "temp" );

        // go through each character
        for ( long i = 0; i < cont.size(); i++ ) {
            try {
                long start = i;
                long end = i + 1;
                String text = cont.getContent( start, end ).toString();
                boolean inShortAbbrev = shortAbbrevs.get( start, end ).size() > 0;
                boolean inLongAbbrev = longAbbrevs.get( start, end ).size() > 0;
                boolean isBraket = text.equals( ")" ) || text.equals( "(" );

                inShortAbbrev = inShortAbbrev && !isBraket;
                if ( inLongAbbrev && !inShortAbbrev ) {
                    // do not add it to content

                } else {
                    // add it
                    newCont += cont.getContent( start, end );
                    newDoc.setContent( new DocumentContentImpl( newCont ) );

                    // now for annotations
                    boolean inAnnotation = aSetA.get( start, end ).size() > 0;
                    if ( inAnnotation ) {
                        temp.add( ( long ) newCont.length() - 1, ( long ) newCont.length(),
                                TokenTargetLabeller.BRAIN_TARGET, new SimpleFeatureMapImpl() );
                    } else {
                        temp.add( ( long ) newCont.length() - 1, ( long ) newCont.length(),
                                TokenTargetLabeller.OUTSIDE_TARGET, new SimpleFeatureMapImpl() );
                    }
                }
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        }

        // delete temp

        // add the doc to a corpus, datastore

        doc.setFeatures( new SimpleFeatureMapImpl() );
        corp.add( newDoc );
        p2g.syncDataStore( corp );

        // join
        JoinMalletPredictions joiner = new JoinMalletPredictions( Config.config
                .getString( "whitetext.malletJoin.jape.location" ), temp.getName(), annotatorA, null );
        joiner.execute( newDoc );
        newDoc.removeAnnotationSet( "temp" );

        doc.sync();
    }

    public static void main( String[] args ) throws Exception {
        GateInterface p2g = new GateInterface();
        RevertAbbreviations revert = new RevertAbbreviations( p2g );

        List<ConnectionsDocument> docs = p2g.getDocuments();
        int count = 0;
        for ( ConnectionsDocument cdoc : docs ) {
            Document doc = cdoc.getDocument();
            revert.revert( doc, "UnionMerge" );
            System.out.println( count++ );
        }
        System.out.println( "Done" );
    }
}
