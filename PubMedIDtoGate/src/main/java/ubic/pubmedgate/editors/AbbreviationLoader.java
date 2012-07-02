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
import gate.FeatureMap;
import gate.util.SimpleFeatureMapImpl;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

public class AbbreviationLoader {
    AnnotationSet abbrevs;
    AnnotationSet shortAbbrevs;
    AnnotationSet markups;
    public final static String ABBREVSET = "Original markups";
    public final static String ABBREVTYPE = "Abbrev";
    public final static String ABBREVSHORTTYPE = "AbbrevShort";
    ConnectionsDocument doc;

    public AbbreviationLoader( ConnectionsDocument d ) {
        // load in the abbreviation annotations
        markups = d.getAnnotations( ABBREVSET );
        abbrevs = markups.get( ABBREVTYPE );
        shortAbbrevs = markups.get( ABBREVSHORTTYPE );
        this.doc = d;
    }

    /*
     * given a annotation, return the abbreviations it is in
     */
    public AnnotationSet getAbbreviations( Annotation a ) {
        return abbrevs.get( a.getStartNode().getOffset(), a.getEndNode().getOffset() );
    }

    /*
     * given a span, return the abbreviations it is in
     */
    public AnnotationSet getAbbreviations( long start, long end ) {
        return abbrevs.get( start, end );
    }

    /*
     * given a annotation, return true it's in an abbreviation (not counting the first)
     */
    public boolean isInAbbreviation( Annotation a ) {
        return getAbbreviations( a ).size() != 0;
    }

    /*
     * given a annotation(token), return true it's in an abbreviation short form (not counting the first abbreviation).
     * This probably could have been wrote better in JAPE or something
     */
    public boolean hasShortFormOverlap( Annotation a ) {
        return shortAbbrevs.get( a.getStartNode().getOffset(), a.getEndNode().getOffset() ).size() != 0;
    }

    /*
     * given a annotation(token), return true it's in an abbreviation long form (not counting the first abbreviation).
     */
    public boolean hasLongFormOverlap( Annotation a ) {
        // remember this is just for tokens
        // if its in the abbreviation
        String text = doc.getAnnotationText( a );
        if ( abbrevs.get( a.getStartNode().getOffset(), a.getEndNode().getOffset() ).size() != 0 ) {
            if ( text.equals( "(" ) || text.equals( ")" ) ) return false;
            // can't be in the short section
            return !hasShortFormOverlap( a );
        }
        return false;
    }

    /*
     * Is in the long form but not the short
     */
    public boolean insideShortForm( Annotation a ) {
        return shortAbbrevs.get( a.getStartNode().getOffset(), a.getEndNode().getOffset() ).size() > 0;
    }

    public boolean isExactShortForm( Annotation a ) {
        for ( Annotation shortVer : shortAbbrevs ) {
            if ( shortVer.getStartNode().getOffset() <= a.getStartNode().getOffset() ) {
                if ( shortVer.getEndNode().getOffset() >= a.getEndNode().getOffset() ) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasShortFormOverlap( long start, long end ) {
        return shortAbbrevs.get( start, end ).size() != 0;
    }

    public AnnotationSet getShortForms( long start, long end ) {
        return shortAbbrevs.get( start, end );
    }

    public void markShortForms() {
        // go through all abbrevs
        // find (shortform) pattern add it as shortform annotation
        for ( Annotation abbrev : abbrevs ) {
            String text = doc.getAnnotationText( abbrev );
            FeatureMap abbrevFea = abbrev.getFeatures();
            String shortName = abbrevFea.get( "short" ).toString();
            System.out.println( "-----------" );
            System.out.println( text );
            long end = abbrev.getEndNode().getOffset();
            // subtract two brakets and the shortname
            long start = end - 2 - shortName.length();
            try {
                markups.add( start, end, ABBREVSHORTTYPE, new SimpleFeatureMapImpl() );
            } catch ( Exception e ) {
                e.printStackTrace();
                System.exit( 1 );
            }

        }
    }

    // old version used to make the feature
    // public boolean isInShortForm( Annotation a ) {
    // AnnotationSet abbrev = getAbbreviations( a );
    // if ( abbrev.size() == 1 ) {
    // // should only be one
    // Annotation abbrevAnn = abbrev.iterator().next();
    // FeatureMap abbrevFea = abbrevAnn.getFeatures();
    // String shortName = abbrevFea.get( "short" ).toString();
    //
    // String text = doc.getAnnotationText( a );
    // if ( text.equals( ")" ) || text.equals( "(" ) ) return true;
    // if ( text.equals( shortName ) ) return true;
    // }
    // // anything else fails
    // return false;
    // }

    public static void main( String[] args ) throws Exception {
        GateInterface p2g = new GateInterface();
        // for ( ConnectionsDocument doc : p2g.getDocuments() ) {
        // AbbreviationLoader abbrevInterface = new AbbreviationLoader( doc );
        // abbrevInterface.markShortForms();
        // doc.sync();
        //        }
        for ( ConnectionsDocument doc : p2g.getDocuments( p2g.getUnseenCorp() ) ) {
            AbbreviationLoader abbrevInterface = new AbbreviationLoader( doc );
            abbrevInterface.markShortForms();
            doc.sync();
        }
    }
}
