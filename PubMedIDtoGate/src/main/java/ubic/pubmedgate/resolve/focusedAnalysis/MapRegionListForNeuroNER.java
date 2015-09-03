package ubic.pubmedgate.resolve.focusedAnalysis;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.basecode.dataStructure.params.ParamKeeper;
import ubic.basecode.util.FileTools;
import ubic.pubmedgate.resolve.EvaluationRDFModel;
import ubic.pubmedgate.resolve.MakeLexiconRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.BagOfStemsRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.mentionEditors.BracketRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.CytoPrefixMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionSplittingMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.HemisphereStripMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.NucleusOfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.OfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.RegionSuffixRemover;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Map regions from a hardcoded input file to ABA
 * 
 * @author leon
 */
public class MapRegionListForNeuroNER {
	protected static Log log = LogFactory.getLog(PrintAndResolveBrainRegions.class);
	EvaluationRDFModel evaluationModel;
	RDFResolver resolver;
	RDFResolver resolverLossy;
	RDFResolver resolverLossyLossy;

	Set<Resource> allTerms;
	Set<Resource> allConcepts;

	public MapRegionListForNeuroNER() throws Exception {
		setupResolvers();
	}

	public void setupResolvers() throws Exception {
		resolver = getBaseResolver();

		resolverLossy = getBaseResolver();
		resolverLossy.addMentionEditor(new DirectionRemoverMentionEditor());
		resolverLossy.addMentionEditor(new NucleusOfTheRemoverMentionEditor());

		resolverLossyLossy = getBaseResolver();
		resolverLossyLossy.addMentionEditor(new DirectionRemoverMentionEditor());
		resolverLossyLossy.addMentionEditor(new NucleusOfTheRemoverMentionEditor());
		resolverLossyLossy.addMentionEditor(new DirectionRemoverMentionEditor());

	}

	public RDFResolver getBaseResolver() throws Exception {
		MakeLexiconRDFModel lexiconModel = new MakeLexiconRDFModel();

		lexiconModel.addExtendedABANodes();

		// add evaluations
		boolean reason = true;

		evaluationModel = new EvaluationRDFModel(lexiconModel.getModel(), reason);
		evaluationModel.loadManualMatches();
		boolean createMentions = true;
		evaluationModel.loadManualEvaluations(createMentions);
		evaluationModel.getStats();

		allTerms = evaluationModel.getTerms();
		allConcepts = evaluationModel.getConcepts(); // for speed

		RDFResolver resolverResult = new BagOfStemsRDFMatcher(allTerms);
		resolverResult.addMentionEditor(new DirectionSplittingMentionEditor());
		resolverResult.addMentionEditor(new HemisphereStripMentionEditor());
		resolverResult.addMentionEditor(new BracketRemoverMentionEditor());
		resolverResult.addMentionEditor(new OfTheRemoverMentionEditor());
		resolverResult.addMentionEditor(new CytoPrefixMentionEditor());
		resolverResult.addMentionEditor(new RegionSuffixRemover());
		return resolverResult;
	}

	public Set<Resource> resolveToFilteredConcepts(String mentionString) {
		return resolveToFilteredConcepts(mentionString, resolver);
	}

	public Set<Resource> resolveToFilteredConcepts(String mentionString, RDFResolver resolverToUse) {
		Set<Resource> result = new HashSet<Resource>();
		Set<Resource> neuroConcepts = evaluationModel.resolveToConcepts(mentionString, resolverToUse, allTerms, allConcepts);
		for (Resource neuroConcept : neuroConcepts) {
			if (!evaluationModel.rejected(mentionString, neuroConcept)) {
				result.add(neuroConcept);
			} else {
				log.info("Rejected:" + mentionString + " -> " + JenaUtil.getLabel(neuroConcept));
			}
		}
		return result;
	}

	public void resolveForJustABA() throws Exception {
		List<String> regionStrings = FileTools.getLines("/Users/lfrench/Downloads/regions_lfrench.txt");
		int resolved = 0;
		int resolvedLossy = 0;
		log.info("Total:" + regionStrings.size());
		StopWatch s = new StopWatch();
		s.start();
		int count = 0;
		ParamKeeper keeper = new ParamKeeper();
		for (String regionString : regionStrings) {
			Map<String, String> results = new HashMap<String, String>();
			if (++count % 100 == 0) {
				log.info(count + " " + s.toString() + " of " + regionStrings.size());
				// break;
			}
			log.info("   Region string:" + regionString);
			results.put("InputLine", regionString);

			Set<Resource> concepts = resolveToFilteredConcepts(regionString, resolver);
			boolean usedLossy = false;
			if (concepts.isEmpty()) {
				usedLossy = true;
				concepts = resolveToFilteredConcepts(regionString, resolverLossy);
			}
			if (concepts.isEmpty()) {
				concepts = resolveToFilteredConcepts(regionString, resolverLossyLossy);
			} else {
				resolved++;
			}

			if (usedLossy && !concepts.isEmpty()) {
				resolvedLossy++; // resolved to one and used lossy
				log.info("Used lossy mention editors");
				results.put("Used lossy mention editor", "1");
			} else {
				results.put("Used lossy mention editor", "0");
			}

			int conceptCount = 1;
			for (Resource concept : concepts) {
				if (JenaUtil.getLabel(concept) != null) {
					log.info("       Mapped concept:" + JenaUtil.getLabel(concept) + " (" + concept.getURI() + ")");
					results.put("Mapped Concept URI " + conceptCount, concept.getURI());
					results.put("Mapped Concept Label " + conceptCount, JenaUtil.getLabel(concept));
					conceptCount++;
				}
			}
			results.put("Mapped Concept Count", "" + (conceptCount - 1));
			keeper.addParamInstance(results);
		}
		log.info("Resolved:" + resolved);
		log.info("Resolved lossy:" + resolvedLossy);
		log.info("Total:" + regionStrings.size());
		keeper.writeExcel("/Users/lfrench/Downloads/regions_lfrench.xls");
	}

	public static void main(String[] args) throws Exception {
		MapRegionListForNeuroNER resolver = new MapRegionListForNeuroNER();
		resolver.resolveForJustABA();
	}
}
