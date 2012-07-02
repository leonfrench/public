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

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class TwiceDirectionSplittingRDFMatcher extends DirectionRemoverMatcher {
    public TwiceDirectionSplittingRDFMatcher( Set<Resource> terms ) throws Exception {
        super( terms );
    }

    public Set<String> processMention( String s ) {
        // removed directions once
        Set<String> once = super.processMention( s );
        Set<String> result = new HashSet<String>();
        for ( String oneDirRemoved : once ) {
            // removed directions twice
            result.addAll( super.processMention( oneDirRemoved ) );
        }
        if ( !result.isEmpty() ) log.info( s + " -> " + result );
        return result;
    }

    public String getName() {
        return "Double Direction prefix and suffix cutter, case insensitive";
    }

    // FIX
    // public Property getProperty() {
    // return Vocabulary.cut_direction_once;
    // }

}
