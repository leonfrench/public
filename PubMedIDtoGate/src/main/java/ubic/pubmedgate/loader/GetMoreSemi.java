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

package ubic.pubmedgate.loader;

import gate.Corpus;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ANNIEConstants;
import gate.creole.SerialAnalyserController;
import gate.util.Out;
import ubic.pubmedgate.GateInterface;

@Deprecated
public class GetMoreSemi {

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {

        GateInterface p2g = new GateInterface();
        Corpus corp = p2g.getUnseenCorp();

        Out.prln( "Initialising ANNIE..." );
        // GateReseter reset = new GateReseter(p2g.getUnseenCorp(), "");

        // create a serial analyser controller to run ANNIE with
        SerialAnalyserController annieController = ( SerialAnalyserController ) Factory.createResource(
                "gate.creole.SerialAnalyserController", Factory.newFeatureMap(), Factory.newFeatureMap(), "ANNIE_"
                        + Gate.genSym() );

        for ( int i = 0; i < ANNIEConstants.PR_NAMES.length; i++ ) {
            FeatureMap params = Factory.newFeatureMap(); // use default parameters
            // add the PR to the pipeline controller
            System.out.println( ANNIEConstants.PR_NAMES[i] );
            String use = "gate.creole.annotdelete.AnnotationDeletePR";
            use = "gate.creole.tokeniser.DefaultTokeniser";
            if ( ANNIEConstants.PR_NAMES[i].equals( use ) ) {
                System.out.println( "Loading:" + ANNIEConstants.PR_NAMES[i] );
                ProcessingResource pr = ( ProcessingResource ) Factory.createResource( ANNIEConstants.PR_NAMES[i],
                        params );
                pr.setParameterValue( "annotationSetName", "GATETokens" );
                // LinkedList<String> l = new LinkedList<String>();
                // l.add( "GATETokens" );
                // pr.setParameterValue( "setsToKeep", l );
                annieController.add( pr );
            }
        } // for each ANNIE PR

        Out.prln( "...ANNIE loaded" );
        annieController.setCorpus( corp );
        annieController.execute();
    }

    @Deprecated
    public static void getMoreRandomAbstracts( String[] args ) {
        for ( int i = 0; i < 20; i++ ) {
            try {
                GetMoreAbstracts.seed = i + 70;
                GetMoreAbstracts.main( args );
                // sleep?
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }
}
