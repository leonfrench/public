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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import ubic.BAMSandAllen.LexiconSource;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.MakeLexiconFiles;

/**
 * Makes textpresso lexicon files
 * 
 * @author leon
 */
public class TextPressoLexicon implements LexiconSource {
    String filename;

    public TextPressoLexicon( String filename ) {
        this.filename = Config.config.getString( "whitetext.textpressoRegions.location" ) + filename;
    }

    public TextPressoLexicon( int n ) {
        this( "brainarea." + n + "-gram" );
    }

    public Set<String> getRegionsForLexicon() {
        Set<String> result = new HashSet<String>();
        try {
            BufferedReader f = new BufferedReader( new FileReader( filename ) );
            String line;
            while ( ( line = f.readLine() ) != null ) {
                // parsing the textpresso format
                if ( line.equals( "#####" ) || line.startsWith( "grammar=" ) || line.startsWith( "reference=" ) )
                    continue;
                result.add( line.trim() );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( 1 );
        }
        return result;
    }

    public static void main( String[] args ) throws Exception {
        TextPressoLexicon test = new TextPressoLexicon( 1 );
        boolean lowerCase = false;
        System.out.println( test.getRegionsForLexicon().size() );
        MakeLexiconFiles dumper = new MakeLexiconFiles();
        Set<String> all = new HashSet<String>();
        for ( int i = 1; i < 8; i++ ) {
            TextPressoLexicon textPress = new TextPressoLexicon( i );
            Set<String> textpresso = textPress.getRegionsForLexicon();
            //dumper.dumpToFile( "TextPresso-wordLength-" + i + ".txt", textpresso, lowerCase, false );
            dumper.dumpToFile( "TextPresso-wordLength-" + i + ".txt", textpresso );
            all.addAll( textpresso );
        }
        dumper.dumpToFile( "TextPresso-all-withCase.txt", all, lowerCase, false  );
    }
}
