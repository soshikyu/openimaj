/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.feature.local.keypoints.face;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.openimaj.feature.FeatureVector;
import org.openimaj.feature.FloatFV;
import org.openimaj.feature.local.LocalFeature;
import org.openimaj.feature.local.Location;
import org.openimaj.image.FImage;
import org.openimaj.image.processing.face.parts.FacialKeypoint.FacialKeypointType;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.shape.Rectangle;

import Jama.Matrix;

/**
 * 
 * @author Jonathon Hare
 *
 */
public class FacialDescriptor implements Serializable, LocalFeature {
	public class FacialPartDescriptor {
		public FacialKeypointType type;
		public Point2d position;
		public float [] featureVector;
		
		public FacialPartDescriptor(FacialKeypointType type, Point2d position) {
			this.type = type;
			this.position = position;
		}
		
		public FImage getImage() {
			FImage image = new FImage(2*featureRadius+1,2*featureRadius+1);
			
			for (int i=0, rr=-featureRadius; rr<=featureRadius; rr++) {
				for (int cc=-featureRadius; cc<=featureRadius; cc++) {
					float r2 = rr*rr + cc*cc;
					
					if (r2<=featureRadius*featureRadius) { //inside circle
						float value = featureVector[i++];
						
						image.pixels[rr + featureRadius][cc + featureRadius] = value < -3 ? 0 : value >=3 ? 1 : (3f + value) / 6f;  
					}
				}
			}
			
			return image;
		}
	}
	
	static final long serialVersionUID = 1L;
		
	/**
	 * Affine projection from flat,vertically oriented face to located face space.
	 * You'll probably need to invert this if you want to use it to extract the face
	 * from the image.
	 */
	public Matrix transform;
	
	/** The size of the sampling circle for constructing individual features */
	public int featureRadius;
	
	/** A patch depicting the whole face */
	public FImage facePatch;
	
	/** A list of all the parts of the face */
	public List<FacialPartDescriptor> faceParts = new ArrayList<FacialPartDescriptor>();
	
	public Rectangle bounds;
	
	public FacialDescriptor() {}
	
	public FacialPartDescriptor getPartDescriptor(FacialKeypointType type) {
		if (faceParts.get(type.ordinal()).type == type)
			return faceParts.get(type.ordinal());
		
		for (FacialPartDescriptor part : faceParts) 
			if (part.type == type) 
				return part;
		
		return null;
	}

	@Override
	public LocalFeature readBinary(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LocalFeature readASCII(Scanner in) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] binaryHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String asciiHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeBinary(DataOutput out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeASCII(PrintWriter out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FeatureVector getFeatureVector() {
		int length = faceParts.get(0).featureVector.length;
		FloatFV fv = new FloatFV(faceParts.size() * length);
		
		for (int i=0; i<faceParts.size(); i++) {
			System.arraycopy(faceParts.get(i).featureVector, 0, fv.values, i*length, length);
		}
		
		return fv;
	}

	@Override
	public Location getLocation() {
		// TODO Auto-generated method stub
		return null;
	}
}
