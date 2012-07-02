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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Collections;
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
import ubic.pubmedgate.Config;
import ubic.pubmedgate.resolve.RDFResolvers.BagOfStemsRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.BagOfWordsRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolverImpl;
import ubic.pubmedgate.resolve.RDFResolvers.SimpleExactRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.SimpleMappingRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.StemRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.ThreeLetterRDFMatcher;
import ubic.pubmedgate.resolve.evaluation.LoadEvaluationSpreadSheets;
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
import ubic.pubmedgate.statistics.NamedEntityStats;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ResolutionRDFModel {
    protected static Log log = LogFactory.getLog( ResolutionRDFModel.class );

    protected Model model;
    boolean reason;

    public ResolutionRDFModel( Model model, boolean reason ) throws Exception {
        modelLoad( model, reason );
    }

    /**
     * Returns a resource in this RDF model.
     * 
     * @param r
     * @return
     */
    public Resource getResource( Resource r ) {
        return model.createResource( r.getURI() );
    }

    public ResolutionRDFModel() throws Exception {
        this( Config.config.getString( "resolve.Lexicon.RDF" ) );
    }

    public ResolutionRDFModel( boolean empty ) throws Exception {
        if ( empty ) {
            model = ModelFactory.createDefaultModel();
        } else {

        }
    }

    public ResolutionRDFModel( String filename ) throws Exception {
        Model modelFresh = ModelFactory.createDefaultModel();
        modelFresh.read( new FileInputStream( filename ), null );
        modelLoad( modelFresh, true );
    }

    protected void modelLoad( Model loadedModel, boolean reason ) throws Exception {
        this.model = loadedModel;
        this.reason = reason;
        log.info( "Loading ontology" );
        // model.read( Vocabulary.getLexiconURI() );

        // FIX -should be on the internet
        model.read( Config.config.getString( "whitetext.brainlinks.templocation" ) );

        model.setNsPrefix( "lexiconLinks", Vocabulary.getLexiconURI() );
        model.setNsPrefix( "TermSpace", Vocabulary.getLexiconSpaceURI() );

        reason();
        getStats();
    }

    /*
     * Given a main name label, find the corresponding Neuroname Node
     */
    public Resource getNNNodeByLabel( String label ) {
        StmtIterator iterator = model.listStatements( null, RDF.type, Vocabulary.neuroname );
        while ( iterator.hasNext() ) {
            Statement s = iterator.nextStatement();
            Resource r = s.getSubject();
            String NNlabel = r.getProperty( RDFS.label ).getLiteral().getString();
            if ( NNlabel.equals( label ) ) {
                return r;
            }
        }
        return null;
    }

    public Set<Resource> getLinkedConcepts( Resource neuroterm ) {
        Set<Resource> result = new HashSet<Resource>();
        // StmtIterator iterator = neuroterm.listProperties();
        StmtIterator iterator = model.listStatements( null, null, neuroterm );
        while ( iterator.hasNext() ) {
            Statement s = iterator.nextStatement();
            if ( s.getSubject().isResource() ) {
                Resource sbj = ( Resource ) s.getSubject().as( Resource.class );
                if ( sbj.hasProperty( RDF.type, Vocabulary.neuroOntologyEntry ) ) {
                    result.add( sbj );
                }
            }
        }
        return result;
    }

    public String toStringNew( Resource r ) {
        // print a resource
        String NNlabel = r.getProperty( RDFS.label ).getLiteral().getString();
        String result = "\"" + NNlabel + "\" ";// + r.toString();

        Set<Resource> targets = new HashSet<Resource>();

        StmtIterator iterator = r.listProperties();
        while ( iterator.hasNext() ) {
            Statement s = iterator.nextStatement();
            if ( s.getObject().isResource() ) {
                Resource obj = ( Resource ) s.getObject().as( Resource.class );
                if ( obj.hasProperty( RDF.type, Vocabulary.neuroterm ) ) {
                    targets.add( obj );
                }
            }

        }
        for ( Resource target : targets ) {
            String label = target.getProperty( RDFS.label ).getLiteral().getString();
            result += "\n   " + label + " ";
            String connecting = getConnectingPredicatesShortNames( r, target ).toString();
            result += connecting;
            result += " " + getLinkedConcepts( target );
        }
        return result;
    }

    public String toString( Resource r ) {
        // print a resource
        String NNlabel = r.getProperty( RDFS.label ).getLiteral().getString();
        String result = NNlabel + ", " + r.toString();

        StmtIterator iterator = r.listProperties();
        while ( iterator.hasNext() ) {
            Statement s = iterator.nextStatement();
            String preds = "";
            if ( s.getObject().isResource() ) {
                Resource obj = ( Resource ) s.getObject().as( Resource.class );
                if ( obj.hasProperty( RDF.type, Vocabulary.neuroterm ) ) {

                    result += "\n   " + obj.getProperty( RDFS.label ).getLiteral().getString() + " ("
                            + s.getPredicate().getLocalName() + ")";
                    preds += s.getPredicate().getLocalName();
                    // get the concepts linked to that neuroterm
                    for ( Resource rr : getLinkedConcepts( obj ) ) {
                        log.info( rr.getProperty( RDFS.label ).getLiteral().getString() + " " + rr.getURI() );
                    }
                }
            }
        }
        return result;
    }

    public Set<Resource> getTerms() {
        Set<Resource> terms = JenaUtil.getSubjects( model.listStatements( null, RDF.type, Vocabulary.neuroterm ) );
        return terms;
    }

    public Set<Resource> getMentions() {
        Set<Resource> mentions = JenaUtil.getSubjects( model.listStatements( null, RDF.type, Vocabulary.neuromention ) );
        return mentions;
    }

    public void reason() {
        // don't reason if it cause model copy issues (Neuronames Loader)
        if ( !reason ) return;
        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        InfModel infModel = ModelFactory.createInfModel( reasoner, model );
        // log.info( "Reasoning added:" + ( infModel.size() - model.size() ) + " RDF statements" );
        // log.info( "Valid? " + infModel.validate().isValid() );
        model = infModel;
    }

    public Set<Resource> getConnectionPartnerMentions() {
        Set<Resource> connectionPartners = JenaUtil.getSubjects( model.listStatements( null,
                Vocabulary.annotated_connection_partner, ( RDFNode ) null ) );
        return connectionPartners;
    }

    public Set<Resource> getUnMatchedMentions() {
        Set<Resource> matched = getMatchedMentions();
        Set<Resource> allMentions = getMentions();
        allMentions.removeAll( matched );
        return allMentions;
    }

    /**
     * Counts duplicates twice
     * 
     * @param mentions
     * @return
     */
    public int getPMIDCount( Set<Resource> mentions ) {
        return getPMIDs().size();
    }

    public Set<Resource> getPMIDs() {
        StmtIterator nodeIt = model.listStatements( null, Vocabulary.in_PMID, ( Resource ) null );
        return JenaUtil.getObjects( nodeIt );
    }

    public Set<Resource> getPMIDs( Set<Resource> mentions ) {
        Set<Resource> pmids = new HashSet<Resource>();
        for ( Resource r : mentions ) {
            pmids.addAll( getPMIDs( r ) );
        }
        return pmids;
    }

    public Set<Resource> getPMIDsForSpecies( String species ) {
        Resource taxon = model.createResource( "http://bio2rdf.org/taxon:" + species );
        ResIterator resIt = model.listResourcesWithProperty( Vocabulary.mentions_species, taxon );
        return resIt.toSet();
    }

    public Set<Resource> getSpeciesForPMID( Resource pmid ) {
        NodeIterator nodeIt = model.listObjectsOfProperty( pmid, Vocabulary.mentions_species );
        Set<Resource> result = new HashSet<Resource>();
        while ( nodeIt.hasNext() ) {
            result.add( ( Resource ) nodeIt.nextNode().as( Resource.class ) );
        }
        return result;
    }

    public Set<Resource> getConcepts() {
        return getConceptsBySource( Vocabulary.neuroOntologyEntry );
    }

    public Set<Resource> getNIFSTDConcepts() {
        return getConceptsBySource( Vocabulary.BIRNLexname );
    }

    public Set<Resource> getBredeConcepts() {
        return getConceptsBySource( Vocabulary.bredeName );
    }

    public Set<Resource> getBAMSConcepts() {
        return getConceptsBySource( Vocabulary.BAMSName );
    }

    public Set<Resource> getConceptsBySource( Resource type ) {
        Set<Resource> concepts = JenaUtil.getSubjects( model.listStatements( null, RDF.type, type ) );
        return concepts;
    }

    public Set<Resource> getABAConcepts() {
        return getConceptsBySource( Vocabulary.ABAName );
    }

    // Avian Brain connectivity database
    public Set<Resource> getABCDConcepts() {
        return getConceptsBySource( Vocabulary.ABCDName );
    }

    public Set<Resource> getNNConcepts() {
        return getConceptsBySource( Vocabulary.neuroname );
    }

    public Set<Resource> getMatchedMentions() {
        // remove all mentions with all node linked to a neuroterm, so if it's a neuromention linked to a term then
        // it's good to return
        Set<Resource> allMatched = JenaUtil.getSubjects( model
                .listStatements( null, Vocabulary.match, ( RDFNode ) null ) );
        return allMatched;
    }

    public boolean containsExactConnection( Resource mention, Resource neuroterm ) {
        return model.contains( new StatementImpl( mention, Vocabulary.string_match_ignorecase, neuroterm ) );
    }

    public boolean accepted( Resource mention, Resource neuroConcept ) {
        return model.contains( new StatementImpl( mention, Vocabulary.evaluation_accept, neuroConcept ) );
    }

    public boolean rejected( String mentionString, Resource neuroConcept ) {
        return rejected( makeMentionNode( mentionString ), neuroConcept );
    }

    public Resource makeMentionNode( String mentionString ) {
        return Vocabulary.makeMentionNode( mentionString, model );
    }

    public boolean rejected( Resource mention, Resource neuroConcept ) {
        return model.contains( new StatementImpl( mention, Vocabulary.evaluation_reject, neuroConcept ) );
    }

    public boolean specToGen( Resource mention, Resource neuroConcept ) {
        return model.contains( new StatementImpl( mention, Vocabulary.evaluation_specific_to_general, neuroConcept ) );
    }

    public Set<Resource> getLinkedResources( Resource subject ) {
        return JenaUtil.getObjects( subject.listProperties() );
    }

    /**
     * Realy slow, a faster method is to use getLinkedResources then intersect that set with all terms.
     * 
     * @param subject
     * @return
     */
    public Set<Resource> getLinkedNeuroTerms( Resource subject ) {
        Set<Resource> result = new HashSet<Resource>();
        StmtIterator iterator = subject.listProperties();
        while ( iterator.hasNext() ) {
            Statement s = iterator.nextStatement();
            if ( s.getObject().isResource() ) {
                Resource obj = ( Resource ) s.getObject().as( Resource.class );

                if ( obj.hasProperty( RDF.type, Vocabulary.neuroterm ) ) {
                    result.add( obj );
                    // targets.add( JenaUtil.getLabel( obj ) );
                }
            }
        }
        return result;
    }

    /**
     * Return the concepts that were evaluated against the mention
     * 
     * @param mention
     * @return
     */
    public Set<Resource> getMentionEvaluations( Resource mention ) {
        return JenaUtil.getObjects( model.listStatements( mention, Vocabulary.evaluation_result, ( RDFNode ) null ) );
    }

    // convert a concept set to it's terms
    public Set<Resource> getTermsFromConcepts( Resource concept ) {
        Set<Resource> concepts = new HashSet<Resource>();
        concepts.add( concept );
        return getTermsFromConcepts( concepts );
    }

    // convert a concept set to it's terms
    public Set<Resource> getTermsFromConcepts( Set<Resource> concepts ) {
        Set<Resource> result = new HashSet<Resource>();
        for ( Resource r : concepts ) {
            result.addAll( JenaUtil.getObjects( model.listStatements( r, null, ( RDFNode ) null ) ) );
        }
        // intersect with all neuroterms
        Set<Resource> terms = getTerms();
        result.retainAll( terms );
        return result;
    }

    // get concepts that have no matched terms
    public Set<Resource> getUnMatchedConceptsOld( Set<Resource> concepts ) {
        Set<Resource> result = new HashSet<Resource>();
        for ( Resource concept : concepts ) {
            Set<Resource> terms = getTermsFromConcepts( concept );
            Set<Resource> unMatchedTerms = getUnMatchedTerms( terms );
            // if all terms are unmatched
            if ( terms.equals( unMatchedTerms ) ) result.add( concept );
        }
        return result;
    }

    // get concepts that have no accepted matched terms
    public Set<Resource> getUnMatchedConcepts( Set<Resource> concepts ) {
        Set<Resource> result = new HashSet<Resource>();
        for ( Resource concept : concepts ) {
            // if this concept has one triple from mention to evaluation
            Set<Resource> acceptedMentions = JenaUtil.getSubjects( model.listStatements( null,
                    Vocabulary.evaluation_accept, concept ) );
            // some maybe evaled but not actually in the model!!
            Set<Resource> terms = getTermsFromConcepts( concept );
            Set<Resource> mentions = getMentionsFromTerms( terms );

            // if there are no mentions that are accepted then
            if ( Util.intersectSize( acceptedMentions, mentions ) == 0 ) {
                result.add( concept );
            }
        }
        return result;
    }

    // get terms that have no matched mentions
    public Set<Resource> getUnMatchedTerms( Set<Resource> terms ) {
        Set<Resource> result = new HashSet<Resource>();
        for ( Resource term : terms ) {
            Set<Resource> mentions = getMentionsFromTerms( term );
            if ( mentions.isEmpty() ) result.add( term );
        }
        return result;
    }

    /**
     * converts a term set to it's linked mentions
     */
    public Set<Resource> getMentionsFromTerms( Resource term ) {
        Set<Resource> terms = new HashSet<Resource>();
        terms.add( term );
        return getMentionsFromTerms( terms );
    }

    public Set<Resource> getMentionsFromTerms( Set<Resource> terms ) {
        Set<Resource> result = new HashSet<Resource>();
        for ( Resource r : terms ) {
            result.addAll( JenaUtil.getSubjects( model.listStatements( null, Vocabulary.match, r ) ) );
        }
        return result;
    }

    public Set<Resource> getConceptsFromTerms( Resource parameterTerm ) {
        Set<Resource> parameterTerms = new HashSet<Resource>();
        parameterTerms.add( parameterTerm );
        return getConceptsFromTerms( parameterTerms );
    }

    public Set<Resource> getConceptsFromTerms( Resource parameterTerm, Set<Resource> allConcepts ) {
        Set<Resource> parameterTerms = new HashSet<Resource>( 1 );
        parameterTerms.add( parameterTerm );
        return getConceptsFromTerms( parameterTerms, allConcepts );
    }

    public Set<Resource> getConceptsFromTerms( Set<Resource> parameterTerms, Set<Resource> allConcepts ) {
        Set<Resource> result = new HashSet<Resource>();

        if ( parameterTerms.isEmpty() ) return result;

        if ( allConcepts == null ) {
            allConcepts = getConcepts();
        }

        GroupSelector selector = new GroupSelector( allConcepts, parameterTerms );
        result = JenaUtil.getSubjects( model.listStatements( selector ) );

        return result;
    }

    public Set<Resource> getConceptsFromTerms( Set<Resource> parameterTerms ) {
        return getConceptsFromTerms( parameterTerms, null );
    }

    @Deprecated
    // public void createMatches() {
    //
    // // get all neuromentions
    // // StmtIterator mentions = model.listStatements( null, RDF.type, Vocabulary.neuromention );
    // // Filter hasMatchFilter = new Filter() {
    // // public boolean accept( Object t ) {
    // // Statement s = ( Statement ) t;
    // //
    // // if ( s.getProperty( arg0 ) ) return true;
    // // }
    // // };
    //
    // // get all neuroterms
    // StmtIterator termStatements = model.listStatements( null, RDF.type, Vocabulary.neuroterm );
    // Set<Statement> terms = termStatements.toSet();
    //
    // List<Resolver> resolvers = new LinkedList<Resolver>();
    // resolvers.add( new SimpleExactMatcher() );
    // resolvers.add( new BagOfWordsResolver() );
    // resolvers.add( new StemResolver() );
    //
    // boolean hit;
    // int hitCount = 0;
    // // do the iterations, very complex, number of mentions * number of terms * number of matchers, ugh
    // outer: while ( mentions.hasNext() ) {
    // hit = false;
    //
    // Resource mention = mentions.nextStatement().getSubject();
    // String mentionText = JenaUtil.getLabel( mention );
    // // log.info( mentionText );
    // for ( Statement term : terms ) {
    // Resource termResource = term.getSubject();
    // String termText = JenaUtil.getLabel( termResource );
    // for ( Resolver resolver : resolvers ) {
    // if ( resolver.matches( mentionText, termText ) ) {
    // log.info( mentionText );
    // hit = true;
    // // make a link between this mention and the term it matched with
    // mention.addProperty( resolver.getProperty(), termResource );
    // log.info( "Match with " + resolver.getName() + ": " + mentionText + " = " + termText );
    // }
    // }
    // }
    // if ( hit ) hitCount++;
    // }
    // log.info( "Number of matched mentions: " + hitCount );
    // }
    public boolean hasNNID( String id ) {
        return !model.contains( model.createResource( Vocabulary.getNNURI() + id ), RDF.type, Vocabulary.neuroname );
    }

    public Set<Resource> getPMIDs( Resource neuromention ) {
        Set<Resource> result = new HashSet<Resource>();
        StmtIterator statements = neuromention.listProperties( Vocabulary.in_PMID );
        while ( statements.hasNext() ) {
            Statement s = statements.nextStatement();
            RDFNode n = s.getObject();
            result.add( ( Resource ) n.as( Resource.class ) );
            // result.add( ( Resource ) statements.nextStatement().getLiteral().getString() );
        }
        return result;
    }

    public Set<Resource> getMentionsInPMID( Resource pmid ) {
        Set<Resource> result = new HashSet<Resource>();
        ResIterator resIt = model.listResourcesWithProperty( Vocabulary.in_PMID, pmid );
        while ( resIt.hasNext() ) {
            Resource r = resIt.nextResource();
            result.add( r );
        }
        return result;
    }

    public String getNCBIPMIDLink( Resource neuroterm ) {
        return getNCBIPMIDLink( neuroterm, Integer.MAX_VALUE );
    }

    public String getNCBIPMIDLink( Resource neuromention, int charLimit ) {
        Set<Resource> pmids = getPMIDs( neuromention );
        return getNCBIPMIDLink( pmids, charLimit );
    }

    public String getNCBIPMIDLink( Set<Resource> pmids ) {
        return getNCBIPMIDLink( pmids, Integer.MAX_VALUE );
    }

    public String getComaSepPMIDs( Set<Resource> pmids, int charLimit ) {
        String result = "";
        for ( Resource pmid : pmids ) {
            String url = pmid.getURI();
            url = url.replace( Vocabulary.getpubmedURIPrefix(), "" );
            if ( ( result.length() + url.length() ) > ( charLimit ) ) {
                break;
            }
            result += url + ",";
        }
        result = result.substring( 0, result.length() - 1 );
        return result;
    }

    public String getNCBIPMIDLink( Set<Resource> pmids, int charLimit ) {
        String endfix = "&dispmax=100";// &dopt=citation -mesh terms
        String prefix = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=retrieve&db=pubmed&list_uids=";
        return prefix + getComaSepPMIDs( pmids, charLimit - endfix.length() - prefix.length() ) + endfix;
    }

    public void writeOut() throws Exception {
        writeOut( Config.config.getString( "resolve.Lexicon.resolution.RDF" ) );
    }

    public void writeOut( String fileName ) throws Exception {
        // model.write( new FileWriter( fileName ) );
        model.write( new FileOutputStream( fileName ) );
    }

    public void loadManualMatches() {
        LoadManualMappings mapper = new LoadManualMappings();
        mapper.addToModel( model );
        reason();
    }

    public void loadExactAutomaticEvaluations() throws Exception {
        LoadEvaluationSpreadSheets sheetExact = new LoadEvaluationSpreadSheets( model );
        sheetExact.loadAutomaticExact();
        log.info( "Finished parsing spreadsheet" );
        reason();
        log.info( "Done reasoning" );
    }

    public void loadAutomaticEvaluations() throws Exception {
        LoadEvaluationSpreadSheets sheet = new LoadEvaluationSpreadSheets( model );
        sheet.loadAutomatic();

        log.info( "Finished parsing spreadsheet" );
        reason();
        log.info( "Done reasoning" );
    }

    public void loadManualEvaluations() throws Exception {
        boolean createMentions = false;
        loadManualEvaluations( createMentions );
    }

    public void loadManualEvaluations( boolean createMentions ) throws Exception {
        LoadEvaluationSpreadSheets sheet = new LoadEvaluationSpreadSheets( model );
        sheet.setCreateRDFMentions( createMentions );
        sheet.loadManual();
        log.info( "Finished parsing spreadsheet" );
        reason();
        log.info( "Done reasoning" );
    }

    public void countMatches() throws Exception {
        // load in sparql file
        File f = new File( Config.config.getString( "whitetext.sparql.directory" ) + "CountMappedTerms.txt" );

        byte[] data = new byte[( int ) f.length()];
        new DataInputStream( new FileInputStream( f ) ).readFully( data );
        String queryString = new String( data );
        // log.info( queryString );

        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, model );

        ResultSet results = qexec.execSelect();
        log.info( "Query executed" );
        // put it into a set
        int totalMatched = 0;
        int uniqueMatched = 0;

        while ( results.hasNext() ) {
            QuerySolution qTemp = results.nextSolution();
            int count = qTemp.getLiteral( "$count" ).getInt();
            String mentionLabel = qTemp.getLiteral( "?mentionlabel" ).toString();
            // log.info( mentionLabel + " -> " + count );
            uniqueMatched++;
            totalMatched += count;
        }
        log.info( "Unique matched mentions = " + uniqueMatched );
        log.info( "Total matched mentions = " + totalMatched );
    }

    public void getTermToConceptStats() {
        Set<Resource> terms = getTerms();
        int onlyOne = 0;
        int total = 0;
        int max = 0;
        Resource maxR = null;
        for ( Resource term : terms ) {
            int size = getConceptsFromTerms( term ).size();
            total += size;
            if ( size == 1 ) onlyOne++;
            if ( size > max ) {
                max = size;
                maxR = term;
            }
        }
        log.info( "total term to concept pairs:" + total );
        log.info( "only one concept:" + onlyOne );
        log.info( "max:" + max + " " + getConceptsFromTerms( maxR ) );
    }

    /**
     * Print number of abstracts that have at least one matched mention
     * 
     * @throws Exception
     */
    public void printCorpusCoverage() throws Exception {
        Set<Resource> mentions = getMatchedMentions();
        // Set<Resource> mentions = JenaUtil.getSubjects( model.listStatements( null, Vocabulary.evaluation_accept,
        // ( RDFNode ) null ) );

        Set<Resource> allPMIDS = new HashSet<Resource>();
        for ( Resource mention : mentions ) {
            allPMIDS.addAll( getPMIDs( mention ) );
        }

        log.info( "Size of pmids that have at least one resolved mention:" + allPMIDS.size() );
    }

    public void printLexiconCoverageStats() throws Exception {
        log.info( "Unmatched concepts:" + getUnMatchedConceptsOld( getConcepts() ).size() );
        log.info( " Unmatched terms:" + getUnMatchedTerms( getTerms() ).size() );
        log.info( "  NeuroNames:" + getUnMatchedConceptsOld( getNNConcepts() ).size() );
        log.info( "   linked terms:" + getUnMatchedTerms( getTermsFromConcepts( getNNConcepts() ) ).size() );
        log.info( "  BIRNLex:" + getUnMatchedConceptsOld( getNIFSTDConcepts() ).size() );
        log.info( "   linked terms:" + getUnMatchedTerms( getTermsFromConcepts( getNIFSTDConcepts() ) ).size() );
        log.info( "  Brede:" + getUnMatchedConceptsOld( getBredeConcepts() ).size() );
        log.info( "   linked terms:" + getUnMatchedTerms( getTermsFromConcepts( getBredeConcepts() ) ).size() );
        log.info( "  BAMS:" + getUnMatchedConceptsOld( getBAMSConcepts() ).size() );
        log.info( "   linked terms:" + getUnMatchedTerms( getTermsFromConcepts( getBAMSConcepts() ) ).size() );
        log.info( "  ABA:" + getUnMatchedConceptsOld( getABAConcepts() ).size() );
        log.info( "   linked terms:" + getUnMatchedTerms( getTermsFromConcepts( getABAConcepts() ) ).size() );
        log.info( "  ABCD:" + getUnMatchedConceptsOld( getABCDConcepts() ).size() );
        log.info( "   linked terms:" + getUnMatchedTerms( getTermsFromConcepts( getABCDConcepts() ) ).size() );

        log.info( "Of the evaluated sets:" );
        log.info( "Unmatched concepts:" + getUnMatchedConcepts( getConcepts() ).size() );
        log.info( "  NeuroNames:" + getUnMatchedConcepts( getNNConcepts() ).size() );
        log.info( "  BIRNLex:" + getUnMatchedConcepts( getNIFSTDConcepts() ).size() );
        log.info( "  Brede:" + getUnMatchedConcepts( getBredeConcepts() ).size() );
        log.info( "  BAMS:" + getUnMatchedConcepts( getBAMSConcepts() ).size() );
        log.info( "  ABA:" + getUnMatchedConcepts( getABAConcepts() ).size() );
        log.info( "  ABCD:" + getUnMatchedConcepts( getABCDConcepts() ).size() );

    }

    public long getStatementSize() throws Exception {
        return model.size();
    }

    public void getStats() throws Exception {
        log.info( "Model size: " + model.size() + " statements" );
        Set<Resource> terms = getTerms();
        log.info( "Neuroterm nodes:" + terms.size() );
        log.info( "Neuroconcept nodes:" + getConcepts().size() );
        log.info( " linked terms:" + getTermsFromConcepts( getConcepts() ).size() );
        log.info( "  NeuroNames:" + getNNConcepts().size() );
        log.info( "   linked terms:" + getTermsFromConcepts( getNNConcepts() ).size() );
        log.info( "  BIRNLex:" + getNIFSTDConcepts().size() );
        log.info( "   linked terms:" + getTermsFromConcepts( getNIFSTDConcepts() ).size() );
        log.info( "  Brede:" + getBredeConcepts().size() );
        log.info( "   linked terms:" + getTermsFromConcepts( getBredeConcepts() ).size() );
        log.info( "  BAMS:" + getBAMSConcepts().size() );
        log.info( "   linked terms:" + getTermsFromConcepts( getBAMSConcepts() ).size() );
        log.info( "  ABA:" + getABAConcepts().size() );
        log.info( "   linked terms:" + getTermsFromConcepts( getABAConcepts() ).size() );
        log.info( "  ABCD:" + getABCDConcepts().size() );
        log.info( "   linked terms:" + getTermsFromConcepts( getABCDConcepts() ).size() );

        Set<Resource> mentions = getMentions();
        log.info( "Neuromention nodes:" + mentions.size() );
        log.info( "  Matched mentions (mention to term):" + getMatchedMentions().size() );
        log.info( "        abstracts:" + sumAbstractFrequencies( getMatchedMentions() ) );
        log.info( "        occurrences:" + sumMentionFrequencies( getMatchedMentions() ) );

        log.info( "  Unmatched mentions:" + getUnMatchedMentions().size() );
        log.info( "          abstracts:" + sumAbstractFrequencies( getUnMatchedMentions() ) );
        log.info( "          occurrences:" + sumMentionFrequencies( getUnMatchedMentions() ) );

    }

    private void createVennDiagram() throws Exception {
        File vennFile = new File( "/grp/java/workspace/PubMedIDtoGate/LexiconVenn.txt" );
        // clear old data
        vennFile.delete();
        Set<Resource> NNTerms = getTermsFromConcepts( getNNConcepts() );
        Set<Resource> BIRNTerms = getTermsFromConcepts( getNIFSTDConcepts() );
        Set<Resource> BredeTerms = getTermsFromConcepts( getBredeConcepts() );
        Set<Resource> BAMSTerms = getTermsFromConcepts( getBAMSConcepts() );
        Set<Resource> ABATerms = getTermsFromConcepts( getABAConcepts() );
        // write to Venn Diagram
        Util.addToVennMasterFile( vennFile, NNTerms, "NeuroNames" );
        Util.addToVennMasterFile( vennFile, BredeTerms, "Brede" );
        Util.addToVennMasterFile( vennFile, BIRNTerms, "BIRNLex" );
        Util.addToVennMasterFile( vennFile, BAMSTerms, "BAMS" );
        Util.addToVennMasterFile( vennFile, ABATerms, "ABA" );

        Collection NNminusBIRN = Util.subtract( NNTerms, BIRNTerms );
        Util.addToVennMasterFile( vennFile, NNminusBIRN, "Neuronames minus BIRNLex" );

        Collection NNminusBIRNminusBAMS = Util.subtract( NNminusBIRN, BAMSTerms );
        Util.addToVennMasterFile( vennFile, NNminusBIRNminusBAMS, "Neuronames minus BIRNLex minus BAMS" );

        Collection temp;
        temp = Util.subtract( BAMSTerms, NNTerms );
        Util.addToVennMasterFile( vennFile, temp, "BAMS minus neuronames" );

        temp = Util.subtract( ABATerms, NNTerms );
        Util.addToVennMasterFile( vennFile, temp, "Allen minus neuronames" );

        Collection BAMSminusAll = Util.subtract( BAMSTerms, NNTerms, BIRNTerms, BredeTerms, ABATerms );
        Util.addToVennMasterFile( vennFile, BAMSminusAll, "BAMS minus all" );

        Collection intsersectALL = Util.intersect( NNTerms, BIRNTerms, BredeTerms, BAMSTerms, ABATerms );
        Util.addToVennMasterFile( vennFile, intsersectALL, "Intersect all" );

    }

    public void getConnectedStats() {
        Set<Resource> mentions = getConnectionPartnerMentions();
        log.info( "  Connection partner mentions:" + mentions.size() );
        log.info( "        occurances:" + sumMentionFrequencies( mentions ) );
        mentions.removeAll( getUnMatchedMentions() );
        log.info( "    Matched mentions:" + mentions.size() );
        log.info( "          occurances:" + sumMentionFrequencies( mentions ) );
    }

    public void runOnUnMatched( RDFResolver resolver ) {
        int old = getUnMatchedMentions().size();
        Set<Resource> mentions = getMentions();
        Model resolvedStatements = resolver.resolve( getUnMatchedMentions() );
        model = model.union( resolvedStatements );
        reason();
        log.info( "Newly Matched mentions:" + ( old - getUnMatchedMentions().size() ) );
    }

    public int sumAbstractFrequencies( Set<Resource> mentions ) {
        int result = 0;
        for ( Resource mention : mentions ) {
            Statement s = mention.getProperty( Vocabulary.number_of_abstracts );
            if ( s != null ) result += s.getInt();
        }
        return result;
    }

    public int sumMentionFrequencies( Set<Resource> mentions ) {
        int result = 0;

        for ( Resource mention : mentions ) {
            Statement s = mention.getProperty( Vocabulary.number_of_occurances );
            if ( s != null ) result += s.getInt();
            // int size = mention.listProperties(Vocabulary.number_of_occurances).toList().size();
            // if (size ==2 ) {
            // log.info( "Error Greater than two " + mention.getURI() );
            // }

        }
        return result;
    }

    // public void createMatches2() throws Exception {
    // Set<Resource> mentions = getMentions();
    // Set<Resource> terms = getTerms();
    //
    // List<RDFResolver> resolvers = new LinkedList<RDFResolver>();
    // resolvers.add( new SimpleExactRDFMatcher( terms ) );
    // resolvers.add( new BagOfWordsRDFMatcher( terms ) );
    // resolvers.add( new StemRDFMatcher( terms ) );
    // resolvers.add( new DirectionSplittingRDFMatcher( terms ) );
    // resolvers.add( new AdjectiveStripRDFResolver( terms ) );
    //
    // for ( RDFResolver resolver : resolvers ) {
    // Model resolvedStatements = resolver.resolve( mentions );
    // log.info( resolver.getName() + " added " + resolvedStatements.size() + " resolution statements" );
    // model = model.union( resolvedStatements );
    // }
    // reason();
    // }

    public void addMentionEditorToResolvers( Collection<RDFResolver> resolvers, MentionEditor me ) {
        log.info( "Adding mention editor: " + me.getName() );
        for ( RDFResolver resolver : resolvers ) {
            resolver.addMentionEditor( me );
        }
    }

    public void runResolver( RDFResolver resolver, Set<Resource> mentions ) {
        LinkedList<RDFResolver> resolvers = new LinkedList<RDFResolver>();
        resolvers.add( resolver );
        runResolvers( resolvers, mentions );
    }

    public void runResolvers( Collection<RDFResolver> resolvers, Set<Resource> mentions ) {
        for ( RDFResolver resolver : resolvers ) {
            Model resolvedStatements = resolver.resolve( mentions );
            log.info( resolver.getName() + " added " + resolvedStatements.size() + " resolution statements" );
            model = model.union( resolvedStatements );
        }
        reason();
        log.info( "Resolution Matched mentions:" + getMatchedMentions().size() );
        log.info( "                 occurances:" + sumMentionFrequencies( getMatchedMentions() ) );
    }

    public void connectionPartnerMatches() throws Exception {
        Set<Resource> terms = getTerms();

        List<RDFResolver> resolvers = new LinkedList<RDFResolver>();
        resolvers.add( new SimpleExactRDFMatcher( terms ) );
        resolvers.add( new BagOfWordsRDFMatcher( terms ) );
        resolvers.add( new StemRDFMatcher( terms ) );
        resolvers.add( new BagOfStemsRDFMatcher( terms ) );
        // resolvers.add( new ThreeLetterRDFMatcher( terms ) );

        // run resolvers
        // runResolvers( resolvers, getConnectionPartnerMentions() );
        log.info( "Done normal" );
        log.info( "First stage counts" );
        getStats();

        // split direction conjunctions
        addMentionEditorToResolvers( resolvers, new DirectionSplittingMentionEditor() );
        addMentionEditorToResolvers( resolvers, new HemisphereStripMentionEditor() );
        addMentionEditorToResolvers( resolvers, new CytoPrefixMentionEditor() );
        addMentionEditorToResolvers( resolvers, new BracketRemoverMentionEditor() );
        addMentionEditorToResolvers( resolvers, new NDotExpanderMentionEditor() );
        addMentionEditorToResolvers( resolvers, new OfTheRemoverMentionEditor() );

        runResolvers( resolvers, getConnectionPartnerMentions() );
        getStats();
        getConnectedStats();

    }

    public void createMatches3() throws Exception {

        Set<Resource> terms = getTerms();

        List<RDFResolver> resolvers = getAllResolvers();

        // run resolvers
        runResolvers( resolvers, getMentions() );
        log.info( "Done normal" );
        log.info( "First stage counts" );
        getStats();

        // split direction conjunctions
        addMentionEditorToResolvers( resolvers, new DirectionSplittingMentionEditor() );
        log.info( "1" );
        runResolvers( resolvers, getUnMatchedMentions() );

        // strip uninformitive adjectives (left, right..)
        addMentionEditorToResolvers( resolvers, new HemisphereStripMentionEditor() );
        log.info( "2" );
        runResolvers( resolvers, getUnMatchedMentions() );

        // three was here - annoying/cyto prefix

        addMentionEditorToResolvers( resolvers, new BracketRemoverMentionEditor() );
        log.info( "4" );
        runResolvers( resolvers, getUnMatchedMentions() );

        addMentionEditorToResolvers( resolvers, new NDotExpanderMentionEditor() );
        log.info( "5" );
        runResolvers( resolvers, getUnMatchedMentions() );

        addMentionEditorToResolvers( resolvers, new OfTheRemoverMentionEditor() );
        log.info( "6" );
        runResolvers( resolvers, getUnMatchedMentions() );

        // removed based on evaluation
        // addMentionEditorToResolvers( resolvers, new BroadmannPrefixAdderMentionEditor() );
        // log.info( "7" );
        // runResolvers( resolvers, getUnMatchedMentions() );

        // / NEW!!
        addMentionEditorToResolvers( resolvers, new RegionSuffixRemover() );
        log.info( "NEW" );
        runResolvers( resolvers, getUnMatchedMentions() );

        printMentions( getMatchedMentions(), 20 );

        log.info( "Losing information stages below" );
        // simpleMatcher.setProperty( Vocabulary.simple_mapping_match_after_loss );

        addMentionEditorToResolvers( resolvers, new CytoPrefixMentionEditor() );
        log.info( "3" );
        runResolvers( resolvers, getUnMatchedMentions() );

        addMentionEditorToResolvers( resolvers, new DirectionRemoverMentionEditor() );
        log.info( "7" );
        runResolvers( resolvers, getUnMatchedMentions() );

        addMentionEditorToResolvers( resolvers, new NucleusOfTheRemoverMentionEditor() );
        log.info( "8" );
        runResolvers( resolvers, getUnMatchedMentions() );

        addMentionEditorToResolvers( resolvers, new DirectionRemoverMentionEditor() );
        runResolvers( resolvers, getUnMatchedMentions() );
        log.info( "End stage" );
        getStats();

    }

    protected void printMentions( Set<Resource> mentions, int number ) {
        List<Resource> mentionList = new LinkedList<Resource>( mentions );
        Collections.shuffle( mentionList );
        for ( int i = 0; i < number; i++ ) {
            System.out.println( toStringNew( mentionList.get( i ) ) );
        }
    }

    public void testMentionEditors( String testString ) throws Exception {
        RDFResolverImpl resolver = new SimpleExactRDFMatcher( getTerms() );
        resolver.addMentionEditor( new DirectionSplittingMentionEditor() );
        resolver.addMentionEditor( new HemisphereStripMentionEditor() );
        resolver.addMentionEditor( new BracketRemoverMentionEditor() );
        resolver.addMentionEditor( new OfTheRemoverMentionEditor() );
        // resolver.addMentionEditor( new BroadmannPrefixAdderMentionEditor() );
        resolver.addMentionEditor( new CytoPrefixMentionEditor() );
        resolver.addMentionEditor( new RegionSuffixRemover() );
        resolver.addMentionEditor( new DirectionRemoverMentionEditor() );
        resolver.addMentionEditor( new NucleusOfTheRemoverMentionEditor() );
        resolver.addMentionEditor( new DirectionRemoverMentionEditor() );

        log.info( "Original:" + testString );
        log.info( resolver.processMentionString( testString ) );
    }

    public Set<Resource> getConnectingPredicates( Resource mention, Resource neuroTerm ) {
        Set<Resource> predicates = new HashSet<Resource>();
        StmtIterator predIterator = model.listStatements( mention, null, neuroTerm );
        while ( predIterator.hasNext() ) {
            Statement s = predIterator.nextStatement();
            predicates.add( s.getPredicate() );
        }
        return predicates;
    }

    public Set<String> getConnectingPredicatesShortNames( Resource mention, Resource neuroTerm ) {
        Set<String> predicates = new HashSet<String>();
        for ( Resource pred : getConnectingPredicates( mention, neuroTerm ) ) {
            predicates.add( pred.getLocalName() );
        }
        predicates.remove( "match" );
        return predicates;
    }

    public Map<String, String> getStatsToMap() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.put( "concepts", "" + getConcepts().size() );
        result.put( "terms", "" + getTerms().size() );

        Set<Resource> mentions = getMentions();
        result.put( "mentions", "" + mentions.size() );
        result.put( "abstracts freq", "" + sumAbstractFrequencies( mentions ) );
        result.put( "mentions freq", "" + sumMentionFrequencies( mentions ) );

        result.put( "matched mentions", "" + getMatchedMentions().size() );
        result.put( "matched abstracts freq", "" + sumAbstractFrequencies( getMatchedMentions() ) );
        result.put( "matched mentions freq", "" + sumMentionFrequencies( getMatchedMentions() ) );
        result.put( "unmatched mentions", "" + getUnMatchedMentions().size() );
        result.put( "unmatched abstracts freq", "" + sumAbstractFrequencies( getUnMatchedMentions() ) );
        result.put( "unmatched mentions freq", "" + sumMentionFrequencies( getUnMatchedMentions() ) );
        return result;
    }

    public static void tryAllFour() throws Exception {
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel();
        resolutionModel = new ResolutionRDFModel();
        resolutionModel.runResolver( new SimpleExactRDFMatcher( resolutionModel.getTerms() ),
                resolutionModel.getMentions() );

        resolutionModel = new ResolutionRDFModel();
        resolutionModel.runResolver( new StemRDFMatcher( resolutionModel.getTerms() ), resolutionModel.getMentions() );

        resolutionModel = new ResolutionRDFModel();
        resolutionModel.runResolver( new BagOfWordsRDFMatcher( resolutionModel.getTerms() ),
                resolutionModel.getMentions() );

        resolutionModel = new ResolutionRDFModel();
        resolutionModel.runResolver( new BagOfStemsRDFMatcher( resolutionModel.getTerms() ),
                resolutionModel.getMentions() );

        resolutionModel = new ResolutionRDFModel();
        resolutionModel.runResolver( new ThreeLetterRDFMatcher( resolutionModel.getTerms() ),
                resolutionModel.getMentions() );

    }

    /**
     * Get all mentions for a specific species - uses RDF not GATE.
     * 
     * @param species for example 9986
     * @return
     */
    public Set<Resource> getMentionsForSpecies( String species ) {
        Set<Resource> mentions = new HashSet<Resource>();
        Resource taxon = model.createResource( "http://bio2rdf.org/taxon:" + species );
        ResIterator resIt = model.listResourcesWithProperty( Vocabulary.mentions_species, taxon );
        for ( Resource r = resIt.nextResource(); resIt.hasNext(); r = resIt.nextResource() ) {
            // log.info( r.getURI() );
            mentions.addAll( getMentionsInPMID( r ) );
        }
        return mentions;
    }

    public List<RDFResolver> getAllResolvers() {
        Set<Resource> terms = getTerms();
        List<RDFResolver> resolvers = new LinkedList<RDFResolver>();
        resolvers.add( new SimpleExactRDFMatcher( terms ) );
        resolvers.add( new BagOfWordsRDFMatcher( terms ) );
        resolvers.add( new StemRDFMatcher( terms ) );
        resolvers.add( new BagOfStemsRDFMatcher( terms ) );
        // resolvers.add( new ThreeLetterRDFMatcher( terms ) );
        resolvers.add( new SimpleMappingRDFMatcher( terms ) );
        return resolvers;
    }

    public Set<Resource> resolveToConcepts( String mention, RDFResolver resolver, Set<Resource> allTerms,
            Set<Resource> allConcepts ) {
        Set<Resource> terms = resolveToTerms( mention, resolver, allTerms );

        Set<Resource> concepts = getConceptsFromTerms( terms, allConcepts );
        return concepts;
    }

    public Set<Resource> resolveToTerms( String mention, RDFResolver resolver, Set<Resource> allTerms ) {
        Resource r = makeMentionNode( mention );

        // r is now linked to the terms
        Model resolvedStatements = resolver.resolve( r );

        model = model.add( resolvedStatements );

        Set<Resource> terms = getLinkedResources( r );

        terms.retainAll( allTerms );
        return terms;
    }

    public Set<String> resolve( String mention, RDFResolver resolver, Set<Resource> allTerms, Set<Resource> allConcepts ) {
        Set<String> result = new HashSet<String>();
        Set<Resource> concepts = resolveToConcepts( mention, resolver, allTerms, allConcepts );
        for ( Resource concept : concepts ) {
            result.add( JenaUtil.getLabel( concept ) );
        }

        return result;
    }

    public Set<Property> getAllProperties() {
        Set<Property> properties = new HashSet<Property>();
        for ( RDFResolver r : getAllResolvers() ) {
            Property p = r.getProperty();
            properties.add( p );
        }
        return properties;
    }

    public static void main( String[] args ) throws Exception {
        String filename;
        filename = Config.config.getString( "resolve.Lexicon.RDF" );
        // filename = "/home/lfrench/WhiteText/rdf/LexiconRDF.allComp (only Mallet predictions on unseen corp).rdf";
        ResolutionRDFModel resolutionModel = new ResolutionRDFModel( filename );

        resolutionModel.createVennDiagram();
        resolutionModel.getMentionsForSpecies( "9606" );
        System.exit( 1 );
        // resolutionModel.getStats();

        // System.exit( 1 );
        // resolutionModel.testMentionEditors( "inferior and adjacent portion of the lateral pulvinar subdivisions" );
        // resolutionModel.testMentionEditors( "dorsal and lateral hippocampus" );
        // resolutionModel.testMentionEditors( "rostral part of the medial accessory olive" );
        // resolutionModel.testMentionEditors( "ventral lateral nucleus (vl) of the thalamus" );
        // resolutionModel.testMentionEditors( "areas th" );

        // log.info( resolutionModel.getNNNodeByLabel( "transverse occipital sulcus (H)" ) );
        // System.out.println( resolutionModel.toString( resolutionModel.getNNNodeByLabel( "frontal region" ) ) );
        // System.exit( 1 );

        // tryAllFour();
        // System.exit( 1 );

        resolutionModel.loadManualMatches();

        // loading in evaluations!
        // resolutionModel.loadManualEvaluations();
        // resolutionModel.loadAutomaticEvaluations();

        resolutionModel.getStats();

        resolutionModel.createMatches3();
        resolutionModel.getStats();
        // resolutionModel.writeOut( Config.config.getString( "resolve.Lexicon.resolution.RDF" ) );
        resolutionModel
                .writeOut( "/home/lfrench/WhiteText/rdf/ResolutionRDF.allComp (only Mallet predictions on unseen corp).allResolvers.rdf" );
        // resolutionModel.printMentions( resolutionModel.getUnMatchedMentions(), 20 );

        // print top and bottom 20
        // Set<Resource> x = getMatchedMentions();
        // x.removeAll( newMatches2 );
        // log.info( "newsize: " + x.size() );
        // printMentions( x, 20 );
        //

        // resolutionModel.connectionPartnerMatches();

        log.info( "Done creatematches 3 run" );
        // MappingSpreadSheet sheet = new MappingSpreadSheet( "testForStatsEvalRemovedExactKept.xls" );
        // sheet.populate( resolutionModel );
        // sheet.save();

        System.exit( 1 );

        log.info( "Tagl could start" );
        Set<Resource> mentions = new HashSet<Resource>( resolutionModel.getUnMatchedMentions() );
        boolean phrase = true;
        NamedEntityStats.makeTagCloud( mentions, phrase );
        log.info( "phrase false" );
        phrase = false;
        NamedEntityStats.makeTagCloud( mentions, phrase );

        // // tokenize and print first word
        // for ( Resource mention : mentions ) {
        // String mentionString = JenaUtil.getLabel( mention );
        // }
        // log.info( NamedEntityStats.formatPhraseForCloud( mentionString ) );
        // StringTokenizer tokens = new StringTokenizer( mentionString, BagOfWordsRDFMatcher.delims, false );
        // String lastToken = null;
        // while ( tokens.hasMoreTokens() ) {
        // lastToken = tokens.nextToken();
        // }
        // // log.info( tokens.nextToken() );
        // // log.info( lastToken );
        //
        // }
        // resolutionModel.getStats();
        // log.info( "Added:" + mentions.size() );
        // // NNmodel.writeOut();
    }
}

class GroupSelector implements Selector {
    Set<Resource> objects;
    Set<Resource> subjects;

    public GroupSelector( Set<Resource> subjects, Set<Resource> objects ) {
        this.objects = objects;
        this.subjects = subjects;
    }

    boolean isSimple( Statement s ) {
        return false;
    }

    public boolean test( Statement s ) {
        return subjects.contains( s.getSubject() ) && objects.contains( s.getObject() );
    }

    public RDFNode getObject() {
        return null;
    }

    public Property getPredicate() {
        return null;
    }

    public Resource getSubject() {
        return null;
    }

    public boolean isSimple() {
        return false;
    }
};
