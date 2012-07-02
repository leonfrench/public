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

package ubic.pubmedgate.statistics;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Resource;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
import ubic.pubmedgate.resolve.depreciated.BagOfWordsResolver;
import ubic.pubmedgate.resolve.depreciated.ResolveBrianRegions;

public class NamedEntityStats {
    protected static Log log = LogFactory.getLog( NamedEntityStats.class );

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        boolean phrases = false;
        makeUnMatchedTagCloud( phrases );
    }

    public static void makeNormalTagCloud() {
        GateInterface gateInt = new GateInterface();
        // use only training docs?
        List<ConnectionsDocument> docs = gateInt.getDocuments();

        // TODO Auto-generated method stub
        ResolveBrianRegions resolver = new ResolveBrianRegions( docs, "UnionMerge" );
        List<String> regions = resolver.getAllBrainRegionText();
        String delims = BagOfWordsResolver.delims;

        // CountingMap<Integer> tokenSizeHisto = new CountingMap<Integer>();
        // for ( String region : regions ) {
        // StringTokenizer toker = new StringTokenizer( region, delims, false );
        // int tokens = 0;
        // while ( toker.hasMoreTokens() ) {
        // toker.nextToken();
        // tokens++;
        // }
        // tokenSizeHisto.increment( tokens );
        // }
        // System.out.println( tokenSizeHisto.toString() );
        System.out.println( "------------For tag cloud------------" );

        for ( String region : regions ) {
            System.out.println( formatPhraseForCloud( region ) );
        }

    }

    public static String formatPhraseForCloud( String input ) {
        return input.toLowerCase().replace( " ", "~" ).replace( ",", "" ).replace( "[(]", "" ).replace( "[)]", "" );
    }

    public static void makeUnMatchedTagCloud( boolean phrase ) throws Exception {
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel();
        // create matches
        // update using cached RDF model
        resolutionModel.loadManualMatches();
        resolutionModel.createMatches3();
        makeTagCloud( resolutionModel.getUnMatchedMentions(), phrase );
    }

    public static void makeTagCloud( Set<Resource> mentions, boolean phrase ) throws Exception {
        for ( Resource mention : mentions ) {
            // mention.get
            String label = JenaUtil.getLabel( mention );
            //if ( label.equals( "Cortex" ) ) log.info( "CORTEX" );
            if ( phrase ) label = formatPhraseForCloud( label );
            int count = mention.getProperty( Vocabulary.number_of_occurances ).getInt();
            for ( int i = 0; i < count; i++ ) {
                System.out.println( label );
            }
        }
    }

}
