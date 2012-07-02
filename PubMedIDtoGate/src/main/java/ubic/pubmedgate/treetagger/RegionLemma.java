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

package ubic.pubmedgate.treetagger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.resolve.depreciated.ResolveBrianRegions;

public class RegionLemma {
    protected static Log log = LogFactory.getLog( RegionLemma.class );

    public void RegionStemmer() {

    }

    public String stem( String text ) throws Exception {
        String result = "";
        File temp = getFile( text );
        File tokenized = TreeTaggerRunner.tokenize( temp );
        // log.info( "Inputfile after tokenization:" + tokenized.getAbsolutePath() );
        ArrayList<String> lines = TreeTaggerRunner.runTreeTagger( tokenized );

        // log.info( lines.toString() );
        // turn it into a file
        String category = null, lemma = null;
        for ( String line : lines ) {
            // System.out.println( line );
            StringTokenizer st = new StringTokenizer( line );
            String word = st.nextToken();
            if ( word.equals( "<internal_BL>" ) ) {
                word = " ";
                lemma = word;
            }
            if ( st.hasMoreTokens() ) {
                category = st.nextToken();
                if ( st.hasMoreTokens() ) lemma = st.nextToken();
                if ( lemma.startsWith( "@" ) || lemma.startsWith( "<" ) ) lemma = word;
                // log.info( word + " -> " + lemma );
                word = lemma;
            } else {
                // log.info( lemma );
            }
            result += lemma;
        }

        // tokenize the file

        // run the tagger on the file

        return result;
    }

    private File getFile( String text ) throws IOException, FileNotFoundException {
        File tempFile = File.createTempFile( "treetagger", ".txt" );
        // gateTextFile.deleteOnExit();

        FileOutputStream fos = new FileOutputStream( tempFile );
        OutputStreamWriter osw = new OutputStreamWriter( fos );
        BufferedWriter bw = new BufferedWriter( osw );

        // write it
        bw.write( text );
        bw.close();
        return tempFile;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        // TODO Auto-generated method stub
        RegionLemma x = new RegionLemma();
        x.stem( "testing" );
        x.stem( "mesostriatal fibers" );
        x.stem( "area 1/2 in the opposite hemisphere" );
        log.info( x.stem( "parasympathetic preganglionic column of the lumbosacral spinal cord" ) );
        log.info( x.stem( "primary olfactory centers" ) );
        log.info( x.stem( "sensory-motor region" ) );

        GateInterface gateInt = new GateInterface();
        List<ConnectionsDocument> docs = gateInt.getDocuments();
        ResolveBrianRegions regionGetter = new ResolveBrianRegions( docs, "UnionMerge" );
        //
        Set<String> allBrainRegionText = regionGetter.getAllBrainRegionTextSet();

        int different = 0;
        for ( String text : allBrainRegionText ) {
            String stem = x.stem( text );
            if ( !stem.equals( text ) ) {
                log.info( text + " -> " + stem );
                different++;
            }
        }
        log.info( different );
    }
}
