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

package ubic.pubmedgate.resolve;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Util;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.pubmedgate.Config;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.organism.SpeciesUtil;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.mentionEditors.BracketRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.CytoPrefixMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionSplittingMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.HemisphereStripMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.MentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.NDotExpanderMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.NucleusOfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.OfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.RegionSuffixRemover;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;

public class EvaluationRDFModel extends ResolutionRDFModel {
    protected static Log log = LogFactory.getLog( EvaluationRDFModel.class );

    public EvaluationRDFModel( Model model, boolean reason ) throws Exception {
        super( model, reason );
    }

    public EvaluationRDFModel() throws Exception {
        super();
    }

    public EvaluationRDFModel( boolean empty ) throws Exception {
        super( empty );
    }

    public EvaluationRDFModel( String filename ) throws Exception {
        super( filename );
    }

    public Map<String, String> evaluateConceptMapping( Property property ) {
        Set<Property> properties = new HashSet<Property>();
        properties.add( property );
        return evaluateConceptMapping( properties );
    }

    public Map<Resource, Integer> getPMIDtoYearMap() {
        Map<Resource, Integer> result = new HashMap<Resource, Integer>();
        Map<Resource, Calendar> dates = getPMIDtoDateMap();
        for ( Resource key : dates.keySet() ) {
            // result.put( key, dates.get( key ).getTimeInMillis() );
            result.put( key, dates.get( key ).get( Calendar.YEAR ) );
        }
        return result;
    }

    public Map<String, Integer> getPMIDStringtoYearMap() {
        Map<String, Integer> result = new HashMap<String, Integer>();
        Map<Resource, Calendar> dates = getPMIDtoDateMap();
        for ( Resource key : dates.keySet() ) {
            String URI = key.getURI();
            URI = URI.substring( URI.lastIndexOf( ':' ) + 1 );

            result.put( URI, dates.get( key ).get( Calendar.YEAR ) );
        }
        return result;
    }

    public Map<Resource, Calendar> getPMIDtoDateMap() {
        // ResIterator resItr = model.listResourcesWithProperty( Vocabulary.publication_date );
        StmtIterator stmtItr = model.listStatements( null, Vocabulary.publication_date, ( Literal ) ( null ) );
        Map<Resource, Calendar> result = new HashMap<Resource, Calendar>();
        while ( stmtItr.hasNext() ) {
            Statement s = stmtItr.nextStatement();

            XSDDateTime x = ( XSDDateTime ) XSDDatatype.XSDdateTime.parse( s.getLiteral().getLexicalForm() );
            Calendar date = x.asCalendar();

            // log.info( date.getTime().toString() );
            // log.info( s.getSubject() + " -> " + s.getLiteral() );

            result.put( s.getSubject(), date );

        }
        return result;
    }

    public void findRejectedExact() {
        Collection<Property> props = new LinkedList<Property>();
        props.add( Vocabulary.string_match_ignorecase );
        evaluateConceptMapping( props, getMatchedMentions() );
    }

    public Map<String, String> evaluateConceptMapping( Collection<Property> properties ) {
        return evaluateConceptMapping( properties, getMentions() );
    }

    // given a set of properties linking neuromentions to neuroterms (then neuroconcepts), find out the metrics
    public Map<String, String> evaluateConceptMapping( Collection<Property> properties, Set<Resource> mentions ) {
        Map<String, String> result = new HashMap<String, String>();
        int accept = 0;
        int reject = 0;
        int rejectFreq = 0;
        int rejectAbs = 0;
        int specToGen = 0;
        int noEvaluation = 0;
        int totalMentionToConceptLinks = 0;
        int mentionsWithOneAccept = 0;

        log.info( "Evaluating concept mappings for " + mentions.size() + " mentions" );
        // get mentions
        int count = 0;
        for ( Resource mention : mentions ) {
            if ( count++ % 100 == 0 ) log.info( "Count: " + count + " mentions" );
            Set<Resource> allNeuroTerms = getLinkedNeuroTerms( mention );

            Set<Resource> neuroTerms = new HashSet<Resource>();
            for ( Resource neuroTerm : allNeuroTerms ) {
                // select mention to concept links for these properties
                // get terms via the properties
                for ( Property p : properties ) {
                    boolean isLinked = model.contains( new StatementImpl( mention, p, neuroTerm ) );
                    if ( isLinked ) {
                        neuroTerms.add( neuroTerm );
                    }
                }
            }

            boolean hasOneAccept = false;
            // get concepts for the terms
            Set<Resource> neuroConcepts = getConceptsFromTerms( neuroTerms );
            totalMentionToConceptLinks += neuroConcepts.size();
            for ( Resource neuroConcept : neuroConcepts ) {
                boolean isAccept = model.contains( new StatementImpl( mention, Vocabulary.evaluation_accept,
                        neuroConcept ) );
                boolean isReject = model.contains( new StatementImpl( mention, Vocabulary.evaluation_reject,
                        neuroConcept ) );
                boolean hasResult = model.contains( new StatementImpl( mention, Vocabulary.evaluation_result,
                        neuroConcept ) );
                boolean isSpecToGen = model.contains( new StatementImpl( mention,
                        Vocabulary.evaluation_specific_to_general, neuroConcept ) );
                if ( isAccept ) {
                    accept++;
                    hasOneAccept = true;
                }
                if ( isReject ) {
                    log.info( "Rejected mention:" + JenaUtil.getLabel( mention ) + "->"
                            + JenaUtil.getLabel( neuroConcept ) );
                    reject++;
                    int freq = mention.getProperty( Vocabulary.number_of_occurances ).getInt();
                    rejectFreq += freq;
                    int abs = mention.getProperty( Vocabulary.number_of_abstracts ).getInt();
                    rejectAbs += abs;
                }
                if ( isSpecToGen ) specToGen++;
                if ( !hasResult ) {
                    noEvaluation++;
                    log.info( "No evaluation result: " + JenaUtil.getLabel( mention ) + " -> "
                            + JenaUtil.getLabel( neuroConcept ) + " URI: " + mention.toString() + " -> "
                            + neuroConcept.toString() );

                }
            }
            if ( hasOneAccept ) mentionsWithOneAccept++;
        }
        log.info( "Properties:" + properties.toString() );
        result.put( "properties", properties.toString() );
        log.info( "accept:" + accept );
        result.put( "accept", "" + accept );
        log.info( "reject:" + reject );
        result.put( "reject", "" + reject );
        result.put( "rejectAbs", rejectAbs + "" );
        result.put( "rejectFreq", rejectFreq + "" );
        log.info( "specToGen:" + specToGen );
        result.put( "specToGen", "" + specToGen );
        log.info( "noEvaluation:" + noEvaluation );
        result.put( "noEvaluation", "" + noEvaluation );
        log.info( "totalMentionToConceptLinks:" + totalMentionToConceptLinks );
        result.put( "totalMentionToConceptLinks", "" + totalMentionToConceptLinks );
        result.put( "mentionsWithOneAccept", "" + mentionsWithOneAccept );
        return result;
    }

    // not used anymore
    @Deprecated
    public static void createAllResoloversStats() throws Exception {
        EvaluationRDFModel evaluationModel;
        evaluationModel = new EvaluationRDFModel();

        evaluationModel.loadManualMatches();
        evaluationModel.loadManualEvaluations();
        evaluationModel.loadAutomaticEvaluations();

        // order is important here! - load only one resolver here!
        evaluationModel.createMatches3();
        // with all matches loaded, which ones contribute?
        evaluationModel.writePropertiesEval();
    }

    public static void createStats( Model model, boolean reason, boolean loadManualMatches ) throws Exception {
        // starts with a blank slate
        EvaluationRDFModel evaluationModel;
        evaluationModel = new EvaluationRDFModel( model, reason );
        if ( loadManualMatches ) evaluationModel.loadManualMatches();
        evaluationModel.loadManualEvaluations();
        evaluationModel.loadAutomaticEvaluations();

        ParamKeeper results = new ParamKeeper();

        List<RDFResolver> resolvers = evaluationModel.getAllResolvers();
        for ( RDFResolver resolver : resolvers ) {
            evaluationModel = new EvaluationRDFModel( model, reason );
            Map<String, String> params = oneResolover( evaluationModel, resolver, loadManualMatches );
            // store the last result (all mention editors)
            results.addParamInstance( params );
        }
        // add in a line for all resolvers together (slow)
        results.addParamInstance( getAllResolversStats( loadManualMatches, model, reason ) );
        results.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" ) + "AllResolversInd.xls" );
    }

    public static Map<String, String> getAllResolversStats( boolean loadManualMatches, Model model, boolean reason )
            throws Exception {
        // look at mention editors for all resolvers -- requires blank slate
        EvaluationRDFModel evaluationModel = new EvaluationRDFModel( model, reason );
        // evaluations
        if ( loadManualMatches ) evaluationModel.loadManualMatches();
        evaluationModel.loadManualEvaluations();
        evaluationModel.loadAutomaticEvaluations();
        Map<String, String> params = evaluationModel.createSpreadSheetForEditors( evaluationModel.getAllResolvers() );
        return params;
    }

    public void getStats() throws Exception {
        super.getStats();
        log.info( "Evaluation statements:"
                + model.listStatements( null, Vocabulary.evaluation_result, ( RDFNode ) null ).toSet().size() );
        log.info( "  accepts:"
                + model.listStatements( null, Vocabulary.evaluation_result, ( RDFNode ) null ).toSet().size() );
        log.info( "  rejects:"
                + model.listStatements( null, Vocabulary.evaluation_reject, ( RDFNode ) null ).toSet().size() );
        log.info( "  spec2gen:"
                + model.listStatements( null, Vocabulary.evaluation_specific_to_general, ( RDFNode ) null ).toSet()
                        .size() );
    }

    /**
     * Runs a single resolver on a new model
     * 
     * @param evaluationModel
     * @param resolver
     * @return the results for the resolvers using all mention editors
     * @throws Exception
     */
    public static Map<String, String> oneResolover( EvaluationRDFModel evaluationModel, RDFResolver resolver,
            boolean loadManualMatches ) throws Exception {

        // evaluations
        if ( loadManualMatches ) evaluationModel.loadManualMatches();
        evaluationModel.loadManualEvaluations();
        evaluationModel.loadAutomaticEvaluations();

        // put resolver in list
        Collection<RDFResolver> resolvers = new HashSet<RDFResolver>();
        resolvers.add( resolver );
        Map<String, String> params = evaluationModel.createSpreadSheetForEditors( resolvers );
        return params;
    }

    public Map<String, String> getAllStats( Property property ) throws Exception {
        Set<Property> properties = new HashSet<Property>();
        properties.add( property );
        return getAllStats( properties );
    }

    public Map<String, String> getAllStats( Set<Property> properties ) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll( evaluateConceptMapping( properties ) );
        result.putAll( getStatsToMap() );
        return result;
    }

    /*
     * This evaluates the mention editors for a specific set of resolvers, it has to start from an empty model with no
     * resolutions. returns the last set of results (all editors)
     */
    public Map<String, String> createSpreadSheetForEditors( Collection<RDFResolver> resolvers ) throws Exception {
        String name = "";
        Set<Property> properties = new HashSet<Property>();
        for ( RDFResolver r : resolvers ) {
            Property p = r.getProperty();
            properties.add( p );
            name = name + p.getLocalName() + ".";
        }
        name += "xls";

        ParamKeeper results = new ParamKeeper();

        // add mention editors one by one, and evaluate everytime
        runResolvers( resolvers, getMentions() );
        results.addParamInstance( getAllStats( properties ) );

        // should be refactored
        List<MentionEditor> editors = new LinkedList<MentionEditor>();
        editors.add( new DirectionSplittingMentionEditor() );
        editors.add( new HemisphereStripMentionEditor() );
        editors.add( new BracketRemoverMentionEditor() );
        editors.add( new NDotExpanderMentionEditor() );
        editors.add( new OfTheRemoverMentionEditor() );
        editors.add( new RegionSuffixRemover() );
        // editors.add( new BroadmannPrefixAdderMentionEditor() );
        // loosing information
        editors.add( new CytoPrefixMentionEditor() );
        editors.add( new DirectionRemoverMentionEditor() );
        editors.add( new NucleusOfTheRemoverMentionEditor() );
        editors.add( new DirectionRemoverMentionEditor() );

        // iterate mention editors to see how they perform
        Map<String, String> params = null;
        for ( MentionEditor editor : editors ) {
            addMentionEditorToResolvers( resolvers, editor );
            runResolvers( resolvers, getUnMatchedMentions() );
            params = getAllStats( properties );
            params.put( "added editor", editor.getName() );
            results.addParamInstance( params );
        }

        results.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" ) + name );
        return params;
    }

    public void speciesStats( String annotationSet ) throws Exception {
        SpeciesCounter speciesCounter = new SpeciesCounter( annotationSet );
        Set<Property> properties = getAllProperties();

        // go through unmatched and matched getting a counting set of species
        boolean frequency = false;
        log.info( "Getting normal species stats" );
        Set<Resource> matchedMentions = getMatchedMentions();
        CountingMap<String> matched = speciesCounter.getSpeciesCounts( matchedMentions, frequency, this );
        Set<Resource> unMatchedMentions = getUnMatchedMentions();
        CountingMap<String> unMatched = speciesCounter.getSpeciesCounts( unMatchedMentions, frequency, this );
        frequency = true;
        log.info( "Getting frequency species stats" );
        CountingMap<String> matchedFreq = speciesCounter.getSpeciesCounts( matchedMentions, frequency, this );
        CountingMap<String> unMatchedFreq = speciesCounter.getSpeciesCounts( unMatchedMentions, frequency, this );
        log.info( "matched size:" + matched.size() );
        log.info( "unmatched size:" + unMatched.size() );

        Map<String, Set<Resource>> mentionMap = speciesCounter.getSpeciesMentions( getMentions(), this );

        // add stats for mouse, rat, human, Rhesus and macaque
        Set<String> common = speciesCounter.getCommonSpeciesLinStrings();

        Set<Resource> mentionsForCommonSpecies = new HashSet<Resource>();
        for ( String speciesString : common ) {
            mentionsForCommonSpecies.addAll( mentionMap.get( speciesString ) );
        }
        mentionMap.put( "Common", mentionsForCommonSpecies );

        log.info( "Done getting species to mention map" );

        ParamKeeper keeper = new ParamKeeper();
        GateInterface p2g = new GateInterface();
        SpeciesUtil.SpeciesCountResult countResult = SpeciesUtil.getSpeciesStrings( p2g, p2g.getCorp() );
        StringToStringSetMap speciesStrings = countResult.strings;

        // hack to add common species data
        speciesStrings.put( "Common", "Common" );
        Map<String, String> params;

        // create a row for each species
        for ( String speciesString : speciesStrings.keySet() ) {
            log.info( speciesString );
            if ( speciesCounter.isFiltered( speciesString ) ) {
                log.info( "Skipping:" + speciesString );
                continue;
            }
            params = new HashMap<String, String>();
            params.put( "Species", speciesString );
            params.put( "SpeciesText", speciesStrings.get( speciesString ).toString() );
            params.put( "MatchedPerAbs", matched.get( speciesString ).toString() );
            params.put( "UnMatchedPerAbs", unMatched.get( speciesString ).toString() );
            params.put( "MatchedFreq", matchedFreq.get( speciesString ).toString() );
            params.put( "UnMatchedFreq", unMatchedFreq.get( speciesString ).toString() );
            Set<Resource> mentionsForSpecies = mentionMap.get( speciesString );
            // its possible an abstract has a species but no mentions
            if ( mentionsForSpecies != null ) {
                params.put( "mentionsLinkedToSpecies", mentionsForSpecies.size() + "" ); //
                // split into unmatched and matched
                params.put( "UniqueMatched", Util.intersectSize( matchedMentions, mentionsForSpecies ) + "" );
                params.put( "UniqueUnMatched", Util.intersectSize( unMatchedMentions, mentionsForSpecies ) + "" );
                params.putAll( evaluateConceptMapping( properties, mentionsForSpecies ) );
            }
            keeper.addParamInstance( params );
        }

        params = new HashMap<String, String>();

        params.put( "Species", "rat, mouse, human, rhesus, macaca f." );
        params.put( "mentionsLinkedToSpecies", mentionsForCommonSpecies.size() + "" );

        // split into unmatched and matched
        Set<Resource> commonMatched = ( Set<Resource> ) Util.intersect( matchedMentions, mentionsForCommonSpecies );
        Set<Resource> commonUnMatched = ( Set<Resource> ) Util.intersect( unMatchedMentions, mentionsForCommonSpecies );

        params.put( "UniqueMatched", commonMatched.size() + "" );
        params.put( "UniqueUnMatched", commonUnMatched.size() + "" );

        params.put( "MatchedPerAbs", "" + getPMIDCount( commonMatched ) );
        params.put( "UnMatchedPerAbs", "" + getPMIDCount( commonUnMatched ) );
        params.put( "MatchedFreq", "" + sumMentionFrequencies( commonMatched ) );
        params.put( "UnMatchedFreq", "" + sumMentionFrequencies( commonUnMatched ) );

        params.putAll( evaluateConceptMapping( properties, mentionsForCommonSpecies ) );
        keeper.addParamInstance( params );

        // add stats for all for comparison
        params = new HashMap<String, String>();
        params.put( "Species", "Ignoring species" );
        params.put( "SpeciesText", "Ignoring species" );
        params.put( "MatchedPerAbs", "" + getPMIDCount( matchedMentions ) );
        params.put( "UnMatchedPerAbs", "" + getPMIDCount( unMatchedMentions ) );
        params.put( "MatchedFreq", "" + sumMentionFrequencies( matchedMentions ) );
        params.put( "UnMatchedFreq", "" + sumMentionFrequencies( unMatchedMentions ) );
        params.put( "UniqueMatched", matchedMentions.size() + "" );
        params.put( "UniqueUnMatched", unMatchedMentions.size() + "" );
        params.putAll( evaluateConceptMapping( properties, getMentions() ) );

        keeper.addParamInstance( params );

        String filename = Config.config.getString( "whitetext.resolve.results.folder" ) + "species.evaled.xls";
        keeper.writeExcel( filename );

    }

    public static Map<String, String> convert( Map<String, ?> input ) {
        Map<String, String> result = new HashMap<String, String>();
        for ( String key : input.keySet() ) {
            result.put( key, input.get( key ).toString() );
        }
        return result;
    }

    /**
     * assumes resolution links are already made
     */
    public void writePropertiesEval() throws Exception {
        ParamKeeper results = new ParamKeeper();
        // go through all properties
        for ( Property p : getAllProperties() ) {
            Map<String, String> params = new HashMap<String, String>();
            params.putAll( getAllStats( p ) );
            params.put( "Property", p.getLocalName() );
            // write out stats on each line
            results.addParamInstance( params );
        }
        results.writeExcel( Config.config.getString( "whitetext.resolve.results.folder" ) + "AllResolvers.xls" );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        boolean loadManualMatches = false;

        // double check
        // must start with a blank lexicon file!!
        String file = "resolve.Lexicon.RDF";
        // file = "/home/lfrench/WhiteText/rdf/ResolutionRDF.allComp.allResolvers.malletOnly.rdf";
        // if ( !loadManualMatches ) file += ".noManualMatches";
        file = Config.config.getString( file );

        Model modelLoad = ModelFactory.createDefaultModel();
        modelLoad.read( new FileInputStream( file ), null );

        // getAllResolversStats( loadManualMatches );

        // iterates mention editors for each, then all resolvers (load manual assumed to be true)
        boolean reason = true;
        createStats( modelLoad, reason, loadManualMatches );
        // getAllResolversStats( loadManualMatches, modelLoad, reason );

        System.exit( 1 );

    }
}
