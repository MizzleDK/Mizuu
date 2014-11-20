/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.functions;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CompleteActor {

	private final String mId;
	private String mName, mBirthday, mDayOfDeath, mPlaceOfBirth, mProfilePhoto, mProfilePhotoThumb, mBiography;
	private int mKnownCreditCount;
	private List<WebMovie> mMovies = new ArrayList<WebMovie>(), mTvShows = new ArrayList<WebMovie>();
	private List<String> mPhotos = new ArrayList<String>(), mTaggedPhotos = new ArrayList<String>();
	
	public CompleteActor(String actorId) {
		mId = actorId;
	}
	
	public String getId() {
		return mId;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}
	
	public void setBiography(String bio) {
		mBiography = bio;
	}
	
	public String getBiography() {
		return mBiography;
	}
	
	public void setBirthday(String date) {
		mBirthday = date;
	}
	
	public String getBirthday() {
		return mBirthday;
	}
	
	public void setDayOfDeath(String date) {
		mDayOfDeath = date;
	}
	
	public String getDayOfDeath() {
		return mDayOfDeath;
	}
	
	public boolean isDead() {
		return !TextUtils.isEmpty(getDayOfDeath());
	}
	
	public void setPlaceOfBirth(String place) {
		mPlaceOfBirth = place;
	}
	
	public String getPlaceOfBirth() {
		return mPlaceOfBirth;
	}
	
	public void setProfilePhoto(String photo) {
		mProfilePhoto = photo;
	}
	
	public String getProfilePhoto() {
		if (TextUtils.isEmpty(mProfilePhoto))
			return null;
		return mProfilePhoto;
	}

    public void setProfilePhotoThumb(String photo) {
        mProfilePhotoThumb = photo;
    }

    public String getProfilePhotoThumb() {
        if (TextUtils.isEmpty(mProfilePhotoThumb))
            return getProfilePhoto();
        return mProfilePhotoThumb;
    }
	
	public void setKnownCreditCount(int count) {
		mKnownCreditCount = count;
	}
	
	public int getKnownCreditCount() {
		return mKnownCreditCount;
	}
	
	public void setMovies(List<WebMovie> movies) {
		mMovies = movies;
		
		Collections.sort(mMovies, MizLib.getWebMovieDateComparator());
	}
	
	public List<WebMovie> getMovies() {
		return mMovies;
	}
	
	public void setTvShows(List<WebMovie> shows) {
		mTvShows = shows;
		
		Collections.sort(mTvShows, MizLib.getWebMovieDateComparator());
	}
	
	public List<WebMovie> getTvShows() {
		return mTvShows;
	}
	
	public void setPhotos(List<String> photos) {
		mPhotos = photos;
	}
	
	public List<String> getPhotos() {
		return mPhotos;
	}
	
	public void setTaggedPhotos(List<String> taggedPhotos) {
		mTaggedPhotos = taggedPhotos;
	}
	
	public List<String> getTaggedPhotos() {
		return mTaggedPhotos;
	}
	
	public String getBackdropImage() {
		Random rndm = new Random();
		
		if (getTaggedPhotos().size() > 0) {
			int number = rndm.nextInt(getTaggedPhotos().size());
			if (number > 0)
				number--;
			return getTaggedPhotos().get(number);
		} else {
			if (getPhotos().size() > 0) {
				int number = rndm.nextInt(getPhotos().size());
				if (number > 0)
					number--;
				return getPhotos().get(number);
			} else {
				return null;
			}
		}
	}
}