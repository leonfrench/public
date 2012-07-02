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

package ubic.pubmedgate.organism;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;

public class SpeciesUtil {
    protected static Log log = LogFactory.getLog( SpeciesUtil.class );

    public static SpeciesCountResult getSpeciesStrings( GateInterface p2g, Corpus corp ) {
        return getSpeciesStrings( p2g.getDocuments( corp ) );
    }

    public static SpeciesCountResult getSpeciesStrings( List<ConnectionsDocument> docs ) {
        // StopWatch watch = new StopWatch();
        // watch.start();
        SpeciesUtil x = new SpeciesUtil();
        SpeciesCountResult result = x.new SpeciesCountResult();

        
        result.strings = new StringToStringSetMap();
        result.counts = new CountingMap<String>();
        
        for ( ConnectionsDocument doc : docs ) {
            AnnotationSet aSet = doc.getAnnotations( LinnaeusSpeciesTagger.ANNOTATIONSET );
            aSet = aSet.get( LinnaeusSpeciesTagger.SPECIESTAG );
            for ( Annotation annot : aSet ) {
                String speciesText = doc.getAnnotationText( annot );
                String speciesID = annot.getFeatures().get( LinnaeusSpeciesTagger.NCBIFEATURE ).toString();
                result.strings.put( speciesID, speciesText );
                result.counts.increment( speciesID );
            }
        }
        // watch.stop();
        // log.info( "Time to get species text:" + watch.getTime() + "|" + watch.toString() );
        return result;
    }
    
    public class SpeciesCountResult {
        public StringToStringSetMap strings;
        public CountingMap<String> counts;
    }
}
