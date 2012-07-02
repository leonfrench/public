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

import gate.Annotation;
import gate.AnnotationSet;
import gate.util.FMeasure;
import gate.util.OffsetComparator;
import gate.util.SimpleFeatureMapImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.statistics.AnnotationComparator;

/*
 * Extends annotations to include the abbreviation short and long forms
 *
 * Strange examples are:
 *  PM9548557-Synaptology_of_the_direct  -last one
 * PM17460099-Subthalamic_stimulation_a - STN
 *
 *  PM9741480-Input-output_connections_ -rDAO
 *
 */
public class AbbreviationEditor {
    protected static Log log = LogFactory.getLog( AbbreviationEditor.class );
    String annotationSet;
    List<ConnectionsDocument> docs;
    boolean save;

    public AbbreviationEditor( String annotationSet, List<ConnectionsDocument> docs, boolean save ) {
        this.annotationSet = annotationSet;
        this.docs = docs;
        this.save = save;
    }

    public void run( List<ConnectionsDocument> docs ) {
        int totalHits = 0;
        int documents = 0;
        for ( ConnectionsDocument doc : docs ) {
            log.info( doc.getName() );
            int hits = run( doc );
            totalHits += hits;
            if ( hits != 0 ) {
                log.info( doc.getName() + " hits:" + hits );
                documents++;
            }
        }
        log.info( "Total Hits:" + totalHits );
        log.info( "Documents:" + documents + " of " + docs.size() );
    }

    /*
     * if abbrev is annotated by Suzanne then expand the annotation to match the abbrev and delete the old one/ones
     * don't worry about connections, just a test remove all that are in the shortform
     */
    public int run( ConnectionsDocument doc ) {
        // use treetager/gate, and match those tokens to annotations?
        List<Annotation> regions = doc.getAnnotationsByType( annotationSet, "BrainRegion" );
        AbbreviationLoader abbrevs = new AbbreviationLoader( doc );
        List<Annotation> removeRegions = new LinkedList<Annotation>();
        // List<Annotation> removeShortRegions = new LinkedList<Annotation>();
        List<Annotation> addRegions = new LinkedList<Annotation>();

        for ( Annotation region : regions ) {
            // don't do getabbrev, check to make sure it does not overlap with short
            AnnotationSet abbrevSet = abbrevs.getAbbreviations( region );
            if ( abbrevSet.size() > 0 ) {
                if ( abbrevs.isExactShortForm( region ) ) {
                    log.info( "SHORT REGION " + doc.getAnnotationText( region ) );
                    // if the abbreviation does not contain any other brain regions eg. subthalamic nucleus (*STN*)
                    // removeShortRegions.add( region );
                    // continue;
                }
                // marked it as removed
                removeRegions.add( region );
                // if it covers two then go to the end
                List<Annotation> abbrevSetSorted = new ArrayList<Annotation>( abbrevSet );
                Collections.sort( abbrevSetSorted, new OffsetComparator() );
                // so last is first
                Collections.reverse( abbrevSetSorted );
                addRegions.add( abbrevSetSorted.get( 0 ) );

                try {
                    log.info( "|" + doc.getAnnotationText( region ) + "| -> |"
                            + doc.getAnnotationText( abbrevSet.iterator().next() ) + "|" );
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        }

        // remove the old and add the new
        AnnotationSet editSet = doc.getAnnotations( annotationSet );
        // editSet = editSet.get( "BrainRegion" );

        for ( int i = 0; i < removeRegions.size(); i++ ) {
            Annotation remove = removeRegions.get( i );
            Annotation abbrevAnn = addRegions.get( i );
            try {
                long start = abbrevAnn.getStartNode().getOffset();
                long startRemove = remove.getStartNode().getOffset();
                if ( startRemove < start ) start = startRemove;

                long end = abbrevAnn.getEndNode().getOffset();
                long endRemove = remove.getEndNode().getOffset();
                if ( endRemove > end ) end = endRemove; // forthe case when the abbreviation was highlighted

                editSet.remove( remove );
                // if ( editSet.get( start, end ).size() > 0 ) log.info( "OVERLAP another" );
                int id = editSet.add( start, end, "BrainRegion", new SimpleFeatureMapImpl() );
                Annotation newAnn = editSet.get( id );
                log.info( "|" + doc.getAnnotationText( remove ) + "| -> |" + doc.getAnnotationText( newAnn ) + "|" );

            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        }

        // remove dupes
        removeRegions = new LinkedList<Annotation>();
        List<Annotation> editList = new ArrayList<Annotation>( editSet );
        for ( int iA = 0; iA < editList.size(); iA++ ) {
            Annotation one = editList.get( iA );
            // log.info( doc.getAnnotationText( one ) + " " + one.toString());
            for ( int iB = 0; iB < editList.size(); iB++ ) {
                if ( iB == iA ) continue;
                Annotation two = editList.get( iB );
                if ( two.getType().equals( "BrainRegion" ) && one.getType().equals( "BrainRegion" ) ) {
                    if ( one.getStartNode().getOffset() <= two.getStartNode().getOffset() ) {
                        if ( one.getEndNode().getOffset() >= two.getEndNode().getOffset() ) {
                            log
                                    .info( "DUPE:" + doc.getAnnotationText( two ) + " inside "
                                            + doc.getAnnotationText( one ) );
                            removeRegions.add( two );
                        }
                    }
                }
            }
        }

        editSet = doc.getAnnotations( annotationSet );
        editSet.removeAll( removeRegions );

        try {
            if ( save ) doc.sync();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        return addRegions.size();
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        GateInterface p2g = new GateInterface();
        List<ConnectionsDocument> docs = new LinkedList<ConnectionsDocument>();
        // to run on anntoated set use normal getDocuments and "UnionMerge" annotations
        AbbreviationEditor ed;
        boolean save;

        // run on annotated corpus
        docs = p2g.getDocuments( p2g.getCorp() );
        save = true;
        ed = new AbbreviationEditor( "UnionMerge", docs, save );
        ed.run( docs );

        // run on unseen corpus (after tagging brain regions), optional

        // docs = p2g.getDocuments( p2g.getUnseenCorp() );
        // save = true;
        // ed = new AbbreviationEditor( "Mallet", docs, save );
        // ed.run( docs );

    }
}
