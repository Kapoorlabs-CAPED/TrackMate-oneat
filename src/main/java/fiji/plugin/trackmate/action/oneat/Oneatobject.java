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


public class Oneatobject {

	public final int time;

	public final double Z;
	
	public final double Y;
	
	public final double X;

	public final double score;

	public final double size;

	public final double confidence;

	

	public Oneatobject(int time, double Z, double Y, double X, double score, double size, double confidence) {

		
		this.time = time;
		
		this.Z = Z;
		
		this.Y = Y;
		
		this.X = X;
		
		this.score = score;

		this.size = size;

		this.confidence = confidence;
		
		


	}
	
}
