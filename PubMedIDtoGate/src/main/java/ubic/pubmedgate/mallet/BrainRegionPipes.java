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

package ubic.pubmedgate.mallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.Config;
import ubic.pubmedgate.mallet.features.GATEAnnotationIn;
import ubic.pubmedgate.mallet.features.GATETokenFeature;
import ubic.pubmedgate.mallet.features.MMTxFeatures;
import ubic.pubmedgate.mallet.features.NEPipes;
import ubic.pubmedgate.mallet.features.PositionPipe;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.tsf.FeaturesInWindow;
import cc.mallet.pipe.tsf.LexiconMembership;
import cc.mallet.pipe.tsf.RegexMatches;
import cc.mallet.pipe.tsf.TokenText;
import cc.mallet.pipe.tsf.TokenTextCharPrefix;
import cc.mallet.pipe.tsf.TokenTextCharSuffix;
import cc.mallet.pipe.tsf.TrieLexiconMembership;
import cc.mallet.share.upenn.ner.LengthBins;
import cc.mallet.share.upenn.ner.LongRegexMatches;

public class BrainRegionPipes {
    protected static Log log = LogFactory.getLog( BrainRegionPipes.class );

    static String LEXICONBASE;
    static {
        LEXICONBASE = Config.config.getString( "whitetext.lexicon.output" ) + File.separator;
    }

    List<String> usedPipeNames;
    Collection<Pipe> pipeList;

    public BrainRegionPipes() {
        usedPipeNames = new LinkedList<String>();
        pipeList = new LinkedList<Pipe>();
    }

    public int size() {
        return usedPipeNames.size();
    }

    public Collection<Pipe> addLengthPipes() throws Exception {
        usedPipeNames.add( "Length" );
        // length feature - binary bins
        pipeList.add( new LengthBins( "Length", new int[] { 1, 2, 3, 5, 8, 11, 14, 18, 22 } ) );

        // from some calcs the average brain token is 6.92 while the outside is 4.64 (~3.55 stdev)
        pipeList.add( new LengthBins( "LengthThreshold", new int[] { 6 } ) );
        return pipeList;
    }

    public Collection<Pipe> addNEPipes() {
        usedPipeNames.add( "MalletNE" );
        // add one for a single dash, as it's missing from NEPipes
        pipeList.add( new RegexMatches( "oneDash", Pattern.compile( "(-)" ) ) );
        // for stuff like V1, M4 etc..
        pipeList.add( new RegexMatches( "CapLetterNumber", Pattern.compile( "([A-Z][0-9])" ) ) );
        return pipeList;
    }

    public Collection<Pipe> addAbbreviationLexiconPipes() throws Exception {
        usedPipeNames.add( "AbbrevLex" );
        File ratMouse = new File( LEXICONBASE + "NN2007RatMouseAbbrev.txt" );
        File human = new File( LEXICONBASE + "NN2002HumanAbbrev.txt" );
        boolean ignoreCase = true;
        // should be one word only but may not..
        pipeList.add( new TrieLexiconMembership( "NNHuAbbrev", human, ignoreCase ) );
        pipeList.add( new TrieLexiconMembership( "NNRatMouseAbbrev", ratMouse, ignoreCase ) );

        addPrefixPipes( ratMouse, "NNHuAbbrevPrefix" );
        addPrefixPipes( human, "NNRatMouseAbbrevPrefix" );
        return pipeList;
    }

    public Collection<Pipe> addPrefixPipes( File file, String name ) throws Exception {
        BufferedReader f = new BufferedReader( new FileReader( file ) );
        String line;
        while ( ( line = f.readLine() ) != null ) {
            line = line.trim();
            pipeList
                    .add( new RegexMatches( name, Pattern.compile( "(" + line + ".{1,3})", Pattern.CASE_INSENSITIVE ) ) );
        }
        return pipeList;
    }

    public Collection<Pipe> addLexiconPipes() throws Exception {
        usedPipeNames.add( "Lexicons" );

        // MISC LISTS
        boolean ignoreCase = true;
        addSmallLexicons( ignoreCase );

        addTextPressoPipes( ignoreCase );

        addBrainRegionLexicons( ignoreCase );

        addPigeonLexicon( ignoreCase );

        // works better on test set 100 but worse on full test set
        // areaLexicons(pipeList, ignoreCase);

        return pipeList;
    }

    public void addPigeonLexicon( boolean ignoreCase ) throws FileNotFoundException, Exception {
        usedPipeNames.add( "PigeonLexicon" );
        // PIGEON atlas http://www.zebrafinch.org/pigeonatlase/AtlasFullText2.pdf
        pipeList
                .add( new TrieLexiconMembership( "Pigeon", new File( LEXICONBASE + "PigeonBrainAtlas.txt" ), ignoreCase ) );
        pipeList.addAll( NGramPipeFactory.getAllGramsPipes( "Pigeon", new File( LEXICONBASE + "PigeonBrainAtlas.txt" ),
                ignoreCase ) );
    }

    public void addBrainRegionLexicons( boolean ignoreCase ) throws FileNotFoundException, Exception {
        usedPipeNames.add( "BrainRegions" );
        // BRAINREGION Lexicons
        pipeList.add( new TrieLexiconMembership( "NNHu", new File( LEXICONBASE + "NN2002Human.txt" ), ignoreCase ) );
        pipeList.add( new TrieLexiconMembership( "NNMouseRat", new File( LEXICONBASE + "NN2007RatMouse.txt" ),
                ignoreCase ) );
        pipeList.add( new TrieLexiconMembership( "Allen", new File( LEXICONBASE + "Allen.txt" ), ignoreCase ) );
        pipeList.add( new TrieLexiconMembership( "BAMS", new File( LEXICONBASE + "BAMS.txt" ), ignoreCase ) );
        pipeList
                .add( new TrieLexiconMembership( "AllRegions", new File( LEXICONBASE + "AllRegions.txt" ), ignoreCase ) );

        pipeList.addAll( NGramPipeFactory.getAllGramsPipes( "AllRegions", new File( LEXICONBASE + "AllRegions.txt" ),
                ignoreCase ) );
    }

    public void addTextPressoPipes( boolean ignoreCase ) throws FileNotFoundException, Exception {
        usedPipeNames.add( "TextPresso" );
        // TEXTPRESSO files, files are split by how many tokens
        for ( int i = 1; i < 8; i++ ) {
            pipeList.add( new TrieLexiconMembership( "textPresso" + i, new File( LEXICONBASE + "TextPresso-wordLength-"
                    + i + ".txt" ), ignoreCase ) );
        }
        pipeList.add( new TrieLexiconMembership( "textPressoAll", new File( LEXICONBASE + "TextPresso-all.txt" ),
                ignoreCase ) );

        pipeList.addAll( NGramPipeFactory.getAllGramsPipes( "textPressoAll", new File( LEXICONBASE
                + "TextPresso-all.txt" ), ignoreCase ) );
    }

    public void addSmallLexicons( boolean ignoreCase ) throws FileNotFoundException {
        usedPipeNames.add( "SmallLex" );
        pipeList.add( new LexiconMembership( "chudlerListWord", new File( LEXICONBASE + "chudler.txt" ), ignoreCase ) );
        pipeList.add( new LexiconMembership( "directionWord", new File( LEXICONBASE + "directions.txt" ), ignoreCase ) );
        pipeList.add( new LexiconMembership( "extendedDirectionWord",
                new File( LEXICONBASE + "extendedDirections.txt" ), ignoreCase ) );
        pipeList.add( new LexiconMembership( "stopWord", new File( LEXICONBASE + "stop.txt" ), ignoreCase ) );
    }

    public void addFixes() {
        usedPipeNames.add( "Fixes" );
        pipeList.add( new TokenTextCharSuffix( "SUFFIX=", 3 ) );
        pipeList.add( new TokenTextCharPrefix( "PREFIX=", 3 ) );
    }

    public void addWindowContext() {
        usedPipeNames.add( "WindowContext" );
        pipeList.add( new FeaturesInWindow( "contextW=", 7, 7 ) );
    }

    public Collection<Pipe> addGateTokenizationPipes() throws Exception {
        usedPipeNames.add( "GateToken" );

        // orthographic feature from GATE
        pipeList.add( new GATETokenFeature( "orth" ) );

        // kind feature from GATE - punctuation or word - maybe useless
        pipeList.add( new GATETokenFeature( "kind" ) );

        // subkind feature from GATE - dashpunc - maybe useless
        pipeList.add( new GATETokenFeature( "subkind" ) );

        // not too sure what this feature is for, maybe position in sentence
        pipeList.add( new GATETokenFeature( "position" ) );

        return pipeList;
    }

    public Collection<Pipe> addGATEPOS() throws Exception {
        usedPipeNames.add( "Gate POS" );

        // GATE part of speech tag
        pipeList.add( new GATETokenFeature( "category" ) );

        // stemmed word
        pipeList.add( new GATETokenFeature( "stem" ) );

        return pipeList;
    }

    public Collection<Pipe> addTreeTaggerPOSPipe() throws Exception {
        usedPipeNames.add( "TreeTaggerPOS" );

        // part of speech tag
        pipeList.add( new GATETokenFeature( "category" ) );

        return pipeList;
    }

    public Collection<Pipe> addTreeTaggerLemmaPipe() throws Exception {
        usedPipeNames.add( "TreeTaggerLemma" );

        // word lemma
        pipeList.add( new GATETokenFeature( "lemma" ) );

        // stemmed word from gate stemmer plugin
        // pipeList.add( new GATETokenFeature( "stem" ) );

        return pipeList;
    }

    public Collection<Pipe> addTextPipe() throws Exception {
        usedPipeNames.add( "Text" );
        pipeList.add( new TokenText( "text=" ) );
        return pipeList;
    }

    public Collection<Pipe> addPositionPipe() throws Exception {
        usedPipeNames.add( "Position" );
        pipeList.add( new PositionPipe() );
        return pipeList;
    }

    public Collection<Pipe> addMMtxPipes() throws Exception {
        usedPipeNames.add( "MMTx" );

        pipeList.add( new MMTxFeatures() );

        // pipeList.add( new MMTxTokenFeatures() );

        return pipeList;
    }

    public Collection<Pipe> addHandMadeRegexPipes() throws Exception {
        usedPipeNames.add( "Handmade regexes" );
        pipeList.add( new LongRegexMatches( "of_The", Pattern.compile( "of the" ), 2, 2 ) );
        pipeList.add( new LongRegexMatches( "part_Of", Pattern.compile( "part of" ), 2, 2 ) );
        pipeList.add( new LongRegexMatches( "neuronEnd", Pattern.compile( "(.* neurons)" ), 2, 3 ) );
        pipeList.add( new LongRegexMatches( "nucleiEnd", Pattern.compile( "(.* nuclei)" ), 2, 3 ) );
        pipeList.add( new LongRegexMatches( "nucleusEnd", Pattern.compile( "(.* nucleus)" ), 2, 5 ) );
        pipeList.add( new LongRegexMatches( "fieldEnd", Pattern.compile( "(.* field)" ), 2, 4 ) );
        pipeList.add( new LongRegexMatches( "cortexEnd", Pattern.compile( "(.* cortex)" ), 2, 3 ) );
        pipeList.add( new LongRegexMatches( "areaEnd", Pattern.compile( "(.* area)" ), 2, 4 ) );
        pipeList
                .add( new LongRegexMatches( "territoryEnd", Pattern.compile( "(.* territory)|(.* territories)" ), 2, 4 ) );
        return pipeList;
    }

    public Collection<Pipe> addSpineRegexPipes() throws Exception {
        usedPipeNames.add( "SpineRegex" );
        // T1-T12
        // L1-L5
        // S1-S5
        // C1-C8
        // number of tokens depends on tokenizer
        // test at http://www.fileformat.info/tool/regex.htm
        pipeList.add( new LongRegexMatches( "SpinalParts",
                Pattern.compile( "([LS][1-5])|T((1[0-2]?)|[2-9])|(C[1-8])" ), 1, 2 ) );
        return pipeList;
    }

    public void addAreaLexicons( boolean ignoreCase ) throws FileNotFoundException {
        usedPipeNames.add( "Areawords" );
        pipeList.add( new LexiconMembership( "areawords", new File( LEXICONBASE + "areawords.txt" ), ignoreCase ) );
    }

    public Collection<Pipe> addSubstringRegexPipes() throws Exception {
        usedPipeNames.add( "Substring regexes" );
        String[] substrings = { "cortic", "cerebel" }; // "thalamic" and nuclie are probably in the 1-grams

        for ( String substring : substrings ) {
            pipeList.add( new RegexMatches( substring + "Regex", Pattern.compile( ".*" + substring + ".*",
                    Pattern.CASE_INSENSITIVE ) ) );
        }
        return pipeList;
    }

    public Collection<Pipe> addOriginalMarkupPipes() throws Exception {
        usedPipeNames.add( "Original markups" );
        pipeList.add( new GATEAnnotationIn( "Original markups", "Abbrev" ) );

        pipeList.add( new GATEAnnotationIn( "Original markups", "AbbrevShort" ) );

        pipeList.add( new GATEAnnotationIn( "Original markups", "AbstractText" ) );

        pipeList.add( new GATEAnnotationIn( "Original markups", "ArticleTitle" ) );

        return pipeList;
    }

    public void addMalletNEPipes() throws Exception {
        usedPipeNames.add( "Mallet NE" );
        // random pipes from general NER
        pipeList.addAll( new NEPipes().pipes() );
    }

    public Collection<Pipe> addAreaRegexPipes() throws Exception {
        usedPipeNames.add( "Area regexes" );

        // spaces were removed from regex to match the LongRegexMatches behaviour
        // test with http://www.fileformat.info/tool/regex.htm
        pipeList.add( new LongRegexMatches( "Brodmann", Pattern.compile( "areas? \\d+((, ?\\d)*,? (or|and) \\d+)?" ),
                2, 9 ) );

        // a looser version that allows just letters
        // areas? (\p{Upper}|\d)+((, ?(\p{Upper}|\d))*,? (or|and) (\p{Upper}|\d)+)?
        pipeList.add( new LongRegexMatches( "Areas", Pattern
                .compile( "areas? (\\p{Upper}|\\d)+((, ?(\\p{Upper}|\\d))*,? (or|and) (\\p{Upper}|\\d)+)?" ), 2, 9 ) );
        return pipeList;
    }

    public Collection<Pipe> getCurrentPipes() {
        return pipeList;
    }

    public void addAllGoodPipes() throws Exception {
        boolean lemma = true;
        addAllGoodPipes( lemma );
    }

    public void addAllGoodPipes( boolean lemma ) throws Exception {
        boolean ignoreCase = true;
        addTreeTaggerPOSPipe();
        if ( lemma == true ) addTreeTaggerLemmaPipe();
        addOriginalMarkupPipes();
        addAreaRegexPipes();
        // this catches tracts, change?
        addSubstringRegexPipes();
        addSpineRegexPipes();

        addSmallLexicons( ignoreCase );
        addTextPressoPipes( ignoreCase );
        addBrainRegionLexicons( ignoreCase );
        addPigeonLexicon( ignoreCase );
        // addAbbreviationLexiconPipes();
        addAreaLexicons( ignoreCase );

        addHandMadeRegexPipes();
        addLengthPipes();
        addMalletNEPipes();

    }

    public Collection<Pipe> addAllGoodPipesOld() throws Exception {

        // make the actual text of the token a binary feature (true if it is that text)
        addTextPipe();

        // Treetagger features - POS and lemma
        addTreeTaggerPOSPipe();
        addTreeTaggerLemmaPipe();

        addOriginalMarkupPipes();

        addAreaRegexPipes();

        // check for "thalamic", "cortic", "cerebel", "nuclei" substrings
        addSubstringRegexPipes();

        // Lexicons
        addLexiconPipes();

        // check for suffixes like neurons or nuclei or area
        addHandMadeRegexPipes();

        // two length pipes
        addLengthPipes();

        // Mallet NE pipes
        addMalletNEPipes();

        // ////////////////////////////
        // addGatePipes();

        // abbreviation lexicons
        // pipeList.addAll( brainPipes.getAbbreviationLexiconPipes() );

        // mmtx - seems to be bad
        // addMMtxPipes() ;

        // this takes in results from previous runs
        // pipeList.add( new GATEAnnotationIn( "MalletReverse", "BrainRegion" ) );

        return pipeList;

    }

    public void addGatePipes() throws Exception {
        // GATE tokenization features
        addGateTokenizationPipes();
        addGATEPOS();
    }

    public String toString() {
        Collections.sort( usedPipeNames );
        return usedPipeNames.toString().replaceAll( ",", " +" );
    }
}
