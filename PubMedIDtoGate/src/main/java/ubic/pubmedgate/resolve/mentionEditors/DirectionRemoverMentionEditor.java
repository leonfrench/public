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

package ubic.pubmedgate.resolve.mentionEditors;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ubic.basecode.util.FileTools;
import ubic.pubmedgate.Config;

public class DirectionRemoverMentionEditor extends PrefixMentionEditor {
    Collection<String> suffixes;

    public DirectionRemoverMentionEditor() throws Exception {
        super();
        File f = new File( Config.config.getString( "whitetext.lexicon.output" ) + "directions.txt" );
        prefixes = new HashSet<String>( FileTools.getLines( f ) );

        // add the extended prefixes
        File fd = new File( Config.config.getString( "whitetext.lexicon.output" ) + "extendedDirections.txt" );
        prefixes.addAll( FileTools.getLines( fd ) );

        File fs = new File( Config.config.getString( "whitetext.lexicon.output" ) + "directionSuffixes.txt" );
        suffixes = FileTools.getLines( fs );

        log.info( "Loaded directions, prefix size " + prefixes.size() );
        log.info( "Loaded directions, suffix size " + suffixes.size() );
    }

    public Set<String> editMention( String mention ) {
        Set<String> result = super.editMention( mention );
        // try prefixes using the super class, if successfull then return
        if ( !result.isEmpty() )
            return result;
        else {
            // try endfixes
            if ( result.size() == 1 ) mention = result.iterator().next();
            if ( result.size() > 2 ) throw new RuntimeException( "Size too big" );

            for ( String direction : suffixes ) {

                if ( mention.endsWith( direction ) ) {
                    String olds = mention;
                    // cut off the end
                    mention = mention.substring( 0, mention.length() - direction.length() );
                    mention = mention.trim();
                    // log.info( "ENDFIX: " + s + " <- " + olds );
                    if ( !mention.equals( "nucleus" ) && !mention.equals( "region" ) && !mention.equals( "regions" ) ) {
                        // special case, don't allow the result to end up as just 'region' or 'nucleus'
                        result.add( mention );
                    }
                    break;
                }
            }
            return result;
        }
    }

    public String getName() {
        return "Direction prefix and suffix remover";
    }

}
