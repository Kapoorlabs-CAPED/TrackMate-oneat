/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2022 - 2023 TrackMate developers.
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

import java.util.Map;
import javax.swing.JPanel;
import org.jdom2.Element;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.real.FloatType;


/**
 * Interface for track corrector factories that need to process all the tracks at
 * once. They return new tracks.
 * 
 * @author Varun Kapoor
 */
public interface  TrackCorrectorFactory  extends TrackMateModule
 {

		public TrackCorrector create(  ImgPlus< FloatType > img,  Model model, TrackMate trackmate, Settings modelsettings, DisplaySettings displaysettings,
				final Map< String, Object > settings, final Logger logger, final double[] calibration, final boolean addDisplay );

		/**
		 * Returns a new GUI panel able to configure the settings suitable for the
		 * target tracker identified by the key parameter.
		 *
		 * @param model
		 *            the model that will be modified by the target tracker.
		 * @return a new configuration panel.
		 */
		public JPanel getTrackCorrectorConfigurationPanel(final Settings settings,Map<String, Object> trackmapsettings,
				Map<String, Object> detectorsettings,  final Model model);

		/**
		 * Marshalls a settings map to a JDom element, ready for saving to XML. The
		 * element is <b>updated</b> with new attributes.
		 * <p>
		 * Only parameters specific to the concrete tracker factory are marshalled.
		 * The element also always receive an attribute named
		 * {@value TrackerKeys#XML_ATTRIBUTE_TRACKER_NAME} that saves the target
		 * {@link SpotTracker} key.
		 *
		 * @return true if marshalling was successful. If not, check
		 *         {@link #getErrorMessage()}
		 */
		public boolean marshall( final Map< String, Object > settings, final Element element );

		/**
		 * Un-marshall a JDom element to update a settings map, and sets the target
		 * tracker of this provider from the element.
		 * <p>
		 * Concretely: the the specific settings map for the targeted tracker is
		 * updated from the element.
		 *
		 * @param element
		 *            the JDom element to read from.
		 * @param settings
		 *            the map to update. Is cleared prior to updating, so that it
		 *            contains only the parameters specific to the target tracker.
		 * @return true if unmarshalling was successful. If not, check
		 *         {@link #getErrorMessage()}
		 */
		public boolean unmarshall( final Element element, final Map< String, Object > settings );

		/**
		 * A utility method that builds a string representation of a settings map
		 * owing to the currently selected tracker in this provider.
		 *
		 * @param sm
		 *            the map to echo.
		 * @return a string representation of the map.
		 */
		public String toString( final Map< String, Object > sm );



		/**
		 * Checks the validity of the given settings map for the tracker. The
		 * validity check is strict: we check that all needed parameters are here
		 * and are of the right class, and that there is no extra unwanted
		 * parameters.
		 *
		 * @return true if the settings map can be used with the target factory. If
		 *         not, check {@link #getErrorMessage()}
		 */
		public boolean checkSettingsValidity( final Map< String, Object > settings );

		/**
		 * Returns a meaningful error message for the last action on this factory.
		 *
		 * @return an error message.
		 * @see #marshall(Map, Element)
		 * @see #unmarshall(Element, Map)
		 * @see #checkSettingsValidity(Map)
		 */
		public String getErrorMessage();

		/**
		 * Returns a copy the current instance.
		 * 
		 * @return a new instance of this tracker factory.
		 */
		public TrackCorrectorFactory copy();
	}
