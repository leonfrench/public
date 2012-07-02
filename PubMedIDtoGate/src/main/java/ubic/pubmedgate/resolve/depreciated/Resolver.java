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

package ubic.pubmedgate.resolve.depreciated;

import com.hp.hpl.jena.rdf.model.Property;

public interface Resolver {

    // given a string resolve it to a normalized name or ID, return null if fails
    // more than one possible? rankings?
    String resolve( String text );

    Property getProperty();

    boolean matches( String a, String b );

    String getName();
}
