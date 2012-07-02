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

package ubic.pubmedgate.mallet.features;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.mallet.NGramPipeFactory;
import ubic.pubmedgate.resolve.depreciated.ResolveBrianRegions;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.tsf.TrieLexiconMembership;

public class InferredDictionaryPipeFactory {

    public static Collection<Pipe> getInferredPipes(List<ConnectionsDocument> docs) throws Exception {
        Collection<Pipe> pipes= new ArrayList<Pipe>();
        boolean ignoreCase = true;
        System.out.println("Using unionmerge for inferred pipes");
        ResolveBrianRegions resolver = new ResolveBrianRegions(docs, "UnionMerge");
        Set<String> regions = resolver.getAllBrainRegionTextSet();
        // write to file
        File tempFile = File.createTempFile( "infDict", ".txt" );
        // System.out.println( "Your temp file is " + tempFile.getCanonicalPath() );
        // Arrange for it to be deleted at exit.
        // tempFile.deleteOnExit();
        BufferedWriter bw = new BufferedWriter( new FileWriter( tempFile ) );
        for (String region : regions) {
            bw.write( region );
            bw.newLine();
        }
        bw.close();
        pipes.add(new TrieLexiconMembership( "InfBrainRegion", tempFile, ignoreCase )); 
        //get the N-grams for the regions
        pipes.addAll( NGramPipeFactory.getAllGramsPipes( "InfBrainRegion", tempFile, ignoreCase ) );
        return pipes;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub

    }

}
