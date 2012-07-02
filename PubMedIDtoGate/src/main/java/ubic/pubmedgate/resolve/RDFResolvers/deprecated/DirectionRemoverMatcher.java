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

package ubic.pubmedgate.resolve.RDFResolvers.deprecated;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.util.FileTools;
import ubic.pubmedgate.Config;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

@Deprecated
public class DirectionRemoverMatcher extends RegionPartRemoveMatcher {
    Collection<String> prefixDirections;
    Collection<String> suffixDirections;

    public DirectionRemoverMatcher( Set<Resource> terms ) throws Exception {
        super( terms );
        File f = new File( Config.config.getString( "whitetext.lexicon.output" ) + "directions.txt" );
        prefixDirections = FileTools.getLines( f );

        // add the extended prefixes
        File fd = new File( Config.config.getString( "whitetext.lexicon.output" ) + "extendedDirections.txt" );
        prefixDirections.addAll( FileTools.getLines( f ) );

        File fs = new File( Config.config.getString( "whitetext.lexicon.output" ) + "directionSuffixes.txt" );
        suffixDirections = FileTools.getLines( fs );

        log.info( "Loaded directions, prefix size " + prefixDirections.size() );
        log.info( "Loaded directions, suffix size " + suffixDirections.size() );
    }

    public Set<String> processMention( String s ) {
        Set<String> result = new HashSet<String>();

        // trim lowercase and remove part of and regions of the
        Set<String> cleaned = super.processMention( s );

        if ( cleaned.size() == 1 ) {
            s = cleaned.iterator().next();
        }
        // otherwise use the s we got originally

        // load direcionts
        for ( String direction : prefixDirections ) {
            if ( s.startsWith( direction + " " ) ) {
                String olds = s;
                s = s.substring( direction.length() + 1 );

                // s = s.replaceFirst( direction + " ", "" );
                // log.info( s );// + " <- " + olds );
                result.add( s );
                break;
            }
        }

        for ( String direction : suffixDirections ) {
            if ( s.endsWith( direction ) ) {
                String olds = s;
                // cut off the end
                s = s.substring( 0, s.length() - direction.length() );
                // s = s.replaceFirst( direction, "" );
                s = s.trim();
                // log.info( "ENDFIX: " + s + " <- " + olds );
                result.add( s );
                break;
            }
        }

        return result;
    }

    public String getName() {
        return "Direction prefix and suffix cutter, case insensitive";
    }

    public Property getProperty() {
        return null;
        //return Vocabulary.cut_direction_once;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub

    }

}
