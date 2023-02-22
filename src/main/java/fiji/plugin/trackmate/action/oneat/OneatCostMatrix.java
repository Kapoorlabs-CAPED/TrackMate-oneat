/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2022 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.action.oneat;

import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.checkFeatureMap;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.CostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.FeaturePenaltyCostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.SquareDistCostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.CostMatrixCreator;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.DefaultCostMatrixCreator;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.GraphSegmentSplitter;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.ResizableDoubleArray;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.SparseCostMatrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.util.Util;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
public class OneatCostMatrix implements CostMatrixCreator< Spot, Spot >, MultiThreaded {





	private static final String BASE_ERROR_MESSAGE = "[JaqamanSegmentCostMatrixCreator] ";

	private final Map< String, Object > settings;

	private String errorMessage;

	private SparseCostMatrix scm;

	private long processingTime;

	private List< Spot > uniqueSources;

	private List< Spot > uniqueTargets;

	private final Graph< Spot, DefaultWeightedEdge > graph;

	private double alternativeCost = -1;

	private int numThreads;

	/**
	 * Instantiates a cost matrix creator for the top-left quadrant of the
	 * segment linking cost matrix.
	 * 
	 */
	public OneatCostMatrix( final Graph< Spot, DefaultWeightedEdge > graph, final Map< String, Object > settings )
	{
		this.graph = graph;
		this.settings = settings;
		setNumThreads();
	}

	@Override
	public boolean checkInput()
	{
		final StringBuilder str = new StringBuilder();
		if ( !checkSettingsValidity( settings, str ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Incorrect settings map:\n" + str.toString();
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Extract parameters
		 */

		// Gap closing.
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > gcFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		final CostFunction< Spot, Spot > gcCostFunction = getCostFunctionFor( gcFeaturePenalties );
		final int maxFrameInterval = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double gcMaxDistance = ( Double ) settings.get( KEY_GAP_CLOSING_MAX_DISTANCE );
		final double gcCostThreshold = gcMaxDistance * gcMaxDistance;
		final boolean allowGapClosing = ( Boolean ) settings.get( KEY_ALLOW_GAP_CLOSING );

	
		// Splitting
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > sFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES );
		final CostFunction< Spot, Spot > sCostFunction = getCostFunctionFor( sFeaturePenalties );
		final boolean allowSplitting = ( Boolean ) settings.get( KEY_ALLOW_TRACK_SPLITTING );
		final double sMaxDistance = ( Double ) settings.get( KEY_SPLITTING_MAX_DISTANCE );
		final double sCostThreshold = sMaxDistance * sMaxDistance;

		// Alternative cost
		final double alternativeCostFactor = ( Double ) settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		final double percentile = ( Double ) settings.get( KEY_CUTOFF_PERCENTILE );

		// Do we have to work?
		if ( !allowGapClosing && !allowSplitting  )
		{
			uniqueSources = Collections.emptyList();
			uniqueTargets = Collections.emptyList();
			scm = new SparseCostMatrix( new double[ 0 ], new int[ 0 ], new int[ 0 ], 0 );
			return true;
		}

		/*
		 * Find segment ends, starts and middle points.
		 */

		

		final GraphSegmentSplitter segmentSplitter = new GraphSegmentSplitter( graph, true );
		final List< Spot > segmentEnds = segmentSplitter.getSegmentEnds();
		final List< Spot > segmentStarts = segmentSplitter.getSegmentStarts();

		/*
		 * Generate all middle points list. We have to sort it by the same order
		 * we will sort the unique list of targets, otherwise the SCM will
		 * complains it does not receive columns in the right order.
		 */
		final List< Spot > allMiddles;
		
			final List< List< Spot > > segmentMiddles = segmentSplitter.getSegmentMiddles();
			allMiddles = new ArrayList< >();
			for ( final List< Spot > segment : segmentMiddles )
			{
				allMiddles.addAll( segment );
			}
		
		

		final Object lock = new Object();

		/*
		 * Sources and targets.
		 */
		final ArrayList< Spot > sources = new ArrayList< >();
		final ArrayList< Spot > targets = new ArrayList< >();
		// Corresponding costs.
		final ResizableDoubleArray linkCosts = new ResizableDoubleArray();

		/*
		 * A. We iterate over all segment ends, targeting 1st the segment starts
		 * (gap-closing) then the segment middles (merging).
		 */

		
		for ( final Spot source : allMiddles )
		{
			
					final int sourceFrame = source.getFeature( Spot.FRAME ).intValue();

					/*
					 * Iterate over segment starts - GAP-CLOSING.
					 */

					if ( allowGapClosing )
					{
						for ( final Spot target : segmentStarts )
						{
							// Check frame interval, must be within user
							// specification.
							final int targetFrame = target.getFeature( Spot.FRAME ).intValue();
							final int tdiff = targetFrame - sourceFrame;
							if ( tdiff < 1 || tdiff > maxFrameInterval )
							{
								continue;
							}

							// Check max distance
							final double cost = gcCostFunction.linkingCost( source, target );
							if ( cost > gcCostThreshold )
							{
								continue;
							}

							synchronized ( lock )
							{
								sources.add( source );
								targets.add( target );
								linkCosts.add( cost );
							}
						}
					}

					/*
					 * Iterate over middle points - MERGING.
					 */

					
				}
			

	
			
			for ( final Spot source : segmentEnds )
			{
				
						final int sourceFrame = source.getFeature( Spot.FRAME ).intValue();
						for ( final Spot target : segmentStarts )
						{
							// Check frame interval, must be 1.
							final int targetFrame = target.getFeature( Spot.FRAME ).intValue();
							final int tdiff = targetFrame - sourceFrame;

							if ( tdiff != 1 )
							{
								continue;
							}

							// Check max distance
							final double cost = sCostFunction.linkingCost( source, target );
							if ( cost > sCostThreshold )
							{
								continue;
							}
							synchronized ( lock )
							{
								sources.add( source );
								targets.add( target );
								linkCosts.add( cost );
							}
						}
					}
				
		
		linkCosts.trimToSize();

		/*
		 * Build a sparse cost matrix from this. If the accepted costs are not
		 * empty.
		 */

		if ( sources.isEmpty() || targets.isEmpty() )
		{
			uniqueSources = Collections.emptyList();
			uniqueTargets = Collections.emptyList();
			alternativeCost = Double.NaN;
			scm = null;
			/*
			 * CAREFUL! We return null if no acceptable links are found.
			 */
		}
		else
		{

			final DefaultCostMatrixCreator< Spot, Spot > creator = new DefaultCostMatrixCreator< >( sources, targets, linkCosts.data, alternativeCostFactor, percentile );
			if ( !creator.checkInput() || !creator.process() )
			{
				errorMessage = "Linking track segments: " + creator.getErrorMessage();
				return false;
			}
			/*
			 * Compute the alternative cost from the cost array
			 */
			
			
				if ( percentile == 1 ) 
					alternativeCost = alternativeCostFactor * Util.max( linkCosts.data);
				else	
					alternativeCost = alternativeCostFactor * Util.percentile( linkCosts.data, percentile );
		
			

			scm = creator.getResult();
			uniqueSources = creator.getSourceList();
			uniqueTargets = creator.getTargetList();
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	protected CostFunction< Spot, Spot > getCostFunctionFor( final Map< String, Double > featurePenalties )
	{
		// Link Nick Perry original non sparse LAP framework.
		final CostFunction< Spot, Spot > costFunction;
		if ( null == featurePenalties || featurePenalties.isEmpty() )
		{
			costFunction = new SquareDistCostFunction();
		}
		else
		{
			costFunction = new FeaturePenaltyCostFunction( featurePenalties );
		}
		return costFunction;
	}

	@Override
	public SparseCostMatrix getResult()
	{
		return scm;
	}

	@Override
	public List< Spot > getSourceList()
	{
		return uniqueSources;
	}

	@Override
	public List< Spot > getTargetList()
	{
		return uniqueTargets;
	}

	@Override
	public double getAlternativeCostForSource( final Spot source )
	{
		return alternativeCost;
	}

	@Override
	public double getAlternativeCostForTarget( final Spot target )
	{
		return alternativeCost;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	private static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Gap-closing
		ok = ok & checkParameter( settings, KEY_ALLOW_GAP_CLOSING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & checkFeatureMap( settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, str );
		// Splitting
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_SPLITTING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_SPLITTING_FEATURE_PENALTIES, str );
		// Merging
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_MERGING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_MERGING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_MERGING_FEATURE_PENALTIES, str );
		// Others
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );
		ok = ok & checkParameter( settings, KEY_CUTOFF_PERCENTILE, Double.class, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList< >();
		mandatoryKeys.add( KEY_ALLOW_GAP_CLOSING );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		mandatoryKeys.add( KEY_ALLOW_TRACK_SPLITTING );
		mandatoryKeys.add( KEY_SPLITTING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALLOW_TRACK_MERGING );
		mandatoryKeys.add( KEY_MERGING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		mandatoryKeys.add( KEY_CUTOFF_PERCENTILE );
		final List< String > optionalKeys = new ArrayList< >();
		optionalKeys.add( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_SPLITTING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_MERGING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

}
