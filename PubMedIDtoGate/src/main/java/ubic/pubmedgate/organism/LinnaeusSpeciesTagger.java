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

import gate.AnnotationSet;
import gate.Corpus;
import gate.FeatureMap;
import gate.Gate;
import gate.util.SimpleFeatureMapImpl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.SetupParameters;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.mallet.GateReseter;
import uk.ac.man.entitytagger.EntityTagger;
import au.com.bytecode.opencsv.CSVReader;

public class LinnaeusSpeciesTagger {
    protected static Log log = LogFactory.getLog( LinnaeusSpeciesTagger.class );

    GateInterface p2g;
    Corpus corp;
    String workingDirectory;
    String outputFile;
    public static String ANNOTATIONSET = "Linnaeus";
    public static String SPECIESTAG = "SpeciesMention";
    public static String NCBIFEATURE = "ncbiID";

    public LinnaeusSpeciesTagger( GateInterface p2g, Corpus corp ) {
        this.p2g = p2g;
        this.corp = corp;
        workingDirectory = Config.config.getString( "whitetext.linnaeus.workingdir" );
        outputFile = "mentions.tsv";
    }

    public void createInputFiles() throws Exception {
        // iterate gate files
        for ( ConnectionsDocument doc : GateInterface.getDocuments( corp ) ) {
            // make text file
            String filename = workingDirectory + doc.getPMID() + ".txt";
            FileOutputStream fos = new FileOutputStream( filename );
            OutputStreamWriter osw = new OutputStreamWriter( fos );
            BufferedWriter bw = new BufferedWriter( osw );
            // get document text plus title
            String titleText = doc.getAnnotationText( doc.getTitle() );
            String abstractText = doc.getAnnotationText( doc.getAbstract() );
            bw.write( titleText );
            bw.newLine();
            bw.write( abstractText );
            bw.close();
            fos.close();
        }
    }

    public void loadIntoGate() throws Exception {
        // open mentions.tsv
        GateReseter reset = new GateReseter( corp, ANNOTATIONSET );
        reset.reset();

        CSVReader csvReader = new CSVReader( new FileReader( workingDirectory + outputFile ), '\t' );
        String[] line;
        csvReader.readNext(); // eat header row
        while ( ( line = csvReader.readNext() ) != null ) {

            Set<String> species = getSpeciesFromCSVLine( line );

            String documentPMID = line[1];
            int start = Integer.parseInt( line[2] );
            int end = Integer.parseInt( line[3] );

            ConnectionsDocument doc = p2g.getByPMID( documentPMID );
            long titleStart = doc.getTitle().getStartNode().getOffset();
            start += titleStart;
            end += titleStart;

            AnnotationSet aSet = doc.getAnnotations( ANNOTATIONSET );
            for ( String spec : species ) {
                FeatureMap features = new SimpleFeatureMapImpl();
                features.put( NCBIFEATURE, spec );
                aSet.add( ( long ) start, ( long ) end, SPECIESTAG, features );
            }
            doc.sync();
            // log.info( doc.getLinnaeusSpecies().toString() );
        }
        // tab separate, then pipe separate first column

    }

    private Set<String> getSpeciesFromCSVLine( String[] line ) {
        Set<String> species = new HashSet<String>();
        StringTokenizer speciesString = new StringTokenizer( line[0], "|?" );
        while ( speciesString.hasMoreTokens() ) {
            String token = speciesString.nextToken();
            if ( token.startsWith( "species:ncbi:" ) ) {
                species.add( token );
                // System.out.println( token );
            }
        }
        return species;
    }

    public void tagSpecies() throws Exception {

        String newArgs[] = new String[6];
        newArgs[0] = "--textDir";
        newArgs[1] = workingDirectory;
        // set file name
        newArgs[2] = "--properties";
        newArgs[3] = Config.config.getString( "whitetext.linnaeus.properties" );
        newArgs[4] = "--out";
        newArgs[5] = workingDirectory + outputFile;
        // --report 100 --threads 4
        // run entity tagger, dump to temp outputfile
        EntityTagger.main( newArgs );
        // put temp outfile info into document?
    }

    public Set<String> tagText( String text ) throws Exception {
        File temp = File.createTempFile( "speciesTagger", ".txt" );
        FileOutputStream fos = new FileOutputStream( temp );
        OutputStreamWriter osw = new OutputStreamWriter( fos );
        BufferedWriter bw = new BufferedWriter( osw );
        bw.write( text );
        bw.newLine();
        bw.close();
        fos.close();

        String newArgs[] = new String[6];
        newArgs[0] = "--text";
        newArgs[1] = temp.getAbsolutePath();
        // set file name
        newArgs[2] = "--properties";
        newArgs[3] = Config.config.getString( "whitetext.linnaeus.properties" );
        ;
        newArgs[4] = "--out";
        newArgs[5] = workingDirectory + temp.getName() + "." + outputFile;
        EntityTagger.main( newArgs );

        Set<String> result = new HashSet<String>();
        CSVReader csvReader = new CSVReader( new FileReader( newArgs[5] ), '\t' );
        String[] line;
        csvReader.readNext(); // eat header row
        while ( ( line = csvReader.readNext() ) != null ) {
            result.addAll( getSpeciesFromCSVLine( line ) );
        }
        return result;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {

        // java -Xmx4G -jar linnaeus-1.3.jar --textDir /textdir/ --properties
        // /grp/java/apps/linnaeus/species/properties.conf --out mentions.tsv --report 100 --threads 4
        GateInterface p2g = new GateInterface();

        LinnaeusSpeciesTagger runner = new LinnaeusSpeciesTagger( p2g, p2g.getUnseenCorp() );
        // log.info( runner.tagText( "rat" ) );

        log.info( "Start creating files" );
        runner.createInputFiles();
        log.info( "Done creating files" );
        runner.tagSpecies();
        log.info( "Done tagging" );
        runner.loadIntoGate();
    }
}
