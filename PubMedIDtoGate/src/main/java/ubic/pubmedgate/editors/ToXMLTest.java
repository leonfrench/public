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

import gate.GateConstants;

import java.io.File;
import java.io.FileWriter;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.loader.PubMedIDtoGate;
import ubic.pubmedgate.mallet.GateReseter;

public class ToXMLTest {
    protected static Log log = LogFactory.getLog( ToXMLTest.class );

    /**
     * This deletes abbreviations!! backup corpi first
     * 
     * 
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        File location = new File( Config.config.getString( "whitetext.datastore.release.location" ) ).getParentFile();
        location = new File( location, "WhiteText.xml" );
        log.info( location );

        // System.exit( 1 );
        // location = location.getParent();
        FileWriter fout = new FileWriter( location );
        fout.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + System.getProperty( "line.separator" ) );
        fout.write( "<PubmedArticles>" );

        // remove abbrevation markup to prevent crossover tags (Permanently!)
        GateInterface p2g = new GateInterface( Config.config.getString( "whitetext.datastore.release.location" ) );
        boolean keepOriginalMarkups = false;
        p2g.removeAnnotationType( "Abbrev", keepOriginalMarkups );
        p2g.removeAnnotationType( "AbbrevShort", keepOriginalMarkups );

        // // this permenently removes original markups
        // String[] removeSets = { GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME };
        // for ( String removeSet : removeSets ) {
        // log.info( "Removing " + removeSet );
        // GateReseter reset = new GateReseter( p2g.getCorp(), removeSet );
        // reset.reset();
        // }

        int bad = 0;
        for ( ConnectionsDocument doc : p2g.getDocuments() ) {
            // if ( c++ > 10 ) return;
            // log.info( doc.toXml() );
            Set annotationSet = doc.getAnnotations( "UnionMerge" );
            int originalSize = annotationSet.size();
            boolean gateFeatures = false;
            String xml = doc.toXml( annotationSet, gateFeatures );
            xml = xml.replace( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>", "" );
            int newSize = countOccurrences( xml, "<BrainRegion>" );
            if ( newSize != originalSize ) {
                log.info( "XML size:" + newSize );
                log.info( "Gate size:" + originalSize );
                log.info( xml );
                bad++;
            }
            fout.write( xml );
        }
        fout.write( "</PubmedArticles>" );
        fout.close();
        log.info( bad + " abstracts are missing mentions because of cross-over problems" );
    }

    // from http://www.dreamincode.net/code/snippet901.htm
    public static int countOccurrences( String arg1, String arg2 ) {
        int count = 0;
        int index = 0;
        while ( ( index = arg1.indexOf( arg2, index ) ) != -1 ) {
            ++index;
            ++count;
        }
        return count;
    }
}
