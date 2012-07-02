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

package ubic.pubmedgate;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ubic.BAMSandAllen.AllenDataLoaders.StructureCatalogLoader;
import ubic.BAMSandAllen.BAMSDataLoaders.BAMSDataLoader;

public class MakeLexiconFiles {

    File location;

    public MakeLexiconFiles( File location ) {
        this.location = location;
    }

    public MakeLexiconFiles() {
        this( new File( Config.config.getString( "whitetext.lexicon.output" ) ) );
    }

    public void dumpToFile( String filename, Collection<String> terms ) {
        // default is lowercase and brakets removed
        dumpToFile( filename, terms, true, true );
    }

    public void dumpToFile( String filename, Collection<String> terms, boolean lowerCase, boolean brakets ) {
        try {
            File out = new File( location.getAbsolutePath() + File.separator + filename );
            System.out.println( out.toString() );
            FileWriter f = new FileWriter( out );
            for ( String term : terms ) {
                // remove brakets and whats inside them
                // test with http://www.fileformat.info/tool/regex.htm
                if ( brakets ) term = term.replaceAll( "[(][^)]+[)]", "" );
                term = term.trim();
                if ( lowerCase ) term = term.toLowerCase();
                f.write( term );
                f.write( "\n" );
            }
            f.close();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public void writeRegionLexicons() throws Exception {
        NeuroNamesLoader nn2010 = new NeuroNamesLoader();
        dumpToFile( "NN2010Human.txt", nn2010.getRegionsForLexicon() );
        dumpToFile( "NN2010HumanAbbrev.txt", nn2010.getAbbreviationsForLexicon() );

        NeuroNamesMouseAndRatLoader nn2007 = new NeuroNamesMouseAndRatLoader();
        dumpToFile( "NN2007RatMouse.txt", nn2007.getRegionsForLexicon() );
        dumpToFile( "NN2007RatMouseAbbrev.txt", nn2007.getAbbreviationsForLexicon() );

        // I think this has a XML Library problem, import conflict with Mallet I think
        BAMSDataLoader bams = new BAMSDataLoader();
        dumpToFile( "BAMS.txt", bams.getRegionsForLexicon() );

        StructureCatalogLoader allen = new StructureCatalogLoader();
        dumpToFile( "Allen.txt", allen.getRegionsForLexicon() );

        Set<String> all = new HashSet<String>();
        all.addAll( nn2010.getRegionsForLexicon() );
        all.addAll( nn2007.getRegionsForLexicon() );
        System.out.println( "Skipping BAMS and Allen" );
        // all.addAll( bams.getRegionsForLexicon() );
        // all.addAll( allen.getRegionsForLexicon() );
        dumpToFile( "AllRegions.txt", all );

        System.out.println( "use Textpressolexicon.java for textpresso" );

        System.out.println( "Size of all:" + all.size() );
    }

    public static void main( String argsp[] ) throws Exception {
        MakeLexiconFiles lexGen = new MakeLexiconFiles();
        lexGen.writeRegionLexicons();
    }
}
