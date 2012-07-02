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

import java.util.HashSet;
import java.util.Set;

import ubic.BAMSandAllen.Vocabulary;
import ubic.pubmedgate.resolve.RDFResolvers.SimpleExactRDFMatcher;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

@Deprecated
public class RegionPartRemoveMatcher extends SimpleExactRDFMatcher {
    Set<String> toRemove;

    public RegionPartRemoveMatcher( Set<Resource> terms ) throws Exception {
        super( terms );
        toRemove = new HashSet<String>();
        toRemove.add( "part of the " );
        toRemove.add( "parts of the " );
        toRemove.add( "region of the " );
        toRemove.add( "regions of the " );
        toRemove.add( "portion of the " );
        toRemove.add( "portions of the " );
        toRemove.add( "division of the " );
        toRemove.add( "divisions of the " );

    }

    public Set<String> processMention( String s ) {
        Set<String> result = new HashSet<String>();
        // trim and lowercase
        s = processTerm( s );

        for ( String remove : toRemove ) {
            if ( s.contains( remove ) ) {
                s = s.replace( remove, "" );
                result.add( s );
            }
        }
        return result;
    }

    public String getName() {
        return "RegionPartRemoveMatcher, case insensitive";
    }

    public Property getProperty() {
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub

    }

}
