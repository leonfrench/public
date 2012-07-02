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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ubic.BAMSandAllen.Vocabulary;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

@Deprecated
public class PrefixCleanerRDFResolver extends AdjectiveStripRDFResolver {
    Collection<String> prefixes;

    public PrefixCleanerRDFResolver( Set<Resource> terms ) throws Exception {
        super( terms );
        adjectives = new HashSet<String>();
        adjectives.add( "magnocellular" );
        adjectives.add( "granular" );
        adjectives.add( "midbrain" );
    }

    public String getName() {
        return "Prefix cutter, case insensitive";
    }

    // FIX
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
