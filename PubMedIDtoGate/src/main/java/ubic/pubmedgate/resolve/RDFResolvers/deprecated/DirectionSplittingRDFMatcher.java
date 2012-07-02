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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.util.FileTools;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.BagOfWordsRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.SimpleExactRDFMatcher;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

@Deprecated
public class DirectionSplittingRDFMatcher extends SimpleExactRDFMatcher {
    Collection<String> directions;

    public DirectionSplittingRDFMatcher( Set<Resource> terms ) throws Exception {
        super( terms );
        // load in directions
        File f = new File( Config.config.getString( "whitetext.lexicon.output" ) + "directions.txt" );
        directions = FileTools.getLines( f );

        File fd = new File( Config.config.getString( "whitetext.lexicon.output" ) + "extendedDirections.txt" );
        directions.addAll( FileTools.getLines( f ) );

        log.info( "Loaded directions, size " + directions.size() );
    }

    // split the mention using the directions
    public Set<String> processMention( String s ) {
        Set<String> result = new HashSet<String>();
        // tokenize
        s = processTerm( s );
        StringTokenizer tokens = new StringTokenizer( s, BagOfWordsRDFMatcher.delims, false );
        List<String> tokenList = new LinkedList<String>();
        if ( tokens.countTokens() < 3 ) return result;
        while ( tokens.hasMoreTokens() ) {
            tokenList.add( tokens.nextToken() );
        }
        String first = tokenList.get( 0 );
        String second = tokenList.get( 1 );
        String third = tokenList.get( 2 );
        if ( second.equals( "and" ) || second.equals( "or" ) || second.equals( "to" ) ) {
            if ( directions.contains( first ) && directions.contains( third ) ) {
                // log.info( first + " " + second + " " + third + " Full:" + s );
                int spot = s.indexOf( third ) + third.length();
                result.add( first + s.substring( spot ) );
                result.add( third + s.substring( spot ) );
            }
        }
        return result;
    }

    public String getName() {
        return "Conjunction Splitting exact Matcher, case insensitive";
    }

    public Property getProperty() {
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel();

        DirectionSplittingRDFMatcher matcher = new DirectionSplittingRDFMatcher( resolutionModel.getTerms() );

        // how many mentions match?
        for ( Resource r : resolutionModel.getMentions() ) {
            matcher.processMention( JenaUtil.getLabel( r ) );
        }
    }
}
