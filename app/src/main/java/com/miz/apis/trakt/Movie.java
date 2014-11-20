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

package com.miz.apis.trakt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Movie {
	
	private String mTitle, mUrl, mOverview, mTagline, mCertification, mImdbId, mPoster, mFanart;
	private List<String> mGenres = new ArrayList<>();
	private int mYear, mRuntime, mTmdbId, mRating;
	private long mReleased;
	
	public Movie(String title) {
		mTitle = title;
	}

	public Movie(JSONObject summaryJson) {
		try {
			mTitle = summaryJson.getString("title");
			mYear = summaryJson.getInt("year");
			mReleased = summaryJson.getLong("released");
			mUrl = summaryJson.getString("url");
			mOverview = summaryJson.getString("overview");
			mTagline = summaryJson.getString("tagline");
			mRuntime = summaryJson.getInt("runtime");
			mCertification = summaryJson.getString("certification");
			mTmdbId = summaryJson.getInt("tmdb_id");
			mImdbId = summaryJson.getString("imdb_id");
			mPoster = summaryJson.getString("poster");
			mFanart = summaryJson.getJSONObject("images").getString("fanart");
			mRating = summaryJson.getJSONObject("ratings").getInt("percentage");
			
			JSONArray genres = summaryJson.getJSONArray("genres");
			for (int i = 0; i < genres.length(); i++)
				mGenres.add(genres.getString(i));
			
		} catch (JSONException e) {}
	}

	public String getTagline() {
		return mTagline;
	}

	public void setTagline(String mTagline) {
		this.mTagline = mTagline;
	}

	public int getTmdbId() {
		return mTmdbId;
	}

	public void setTmdbId(int mTmdbId) {
		this.mTmdbId = mTmdbId;
	}

	public long getReleased() {
		return mReleased;
	}

	public void setReleased(long mReleased) {
		this.mReleased = mReleased;
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}

	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String mUrl) {
		this.mUrl = mUrl;
	}

	public String getOverview() {
		return mOverview;
	}

	public void setOverview(String mOverview) {
		this.mOverview = mOverview;
	}

	public String getCertification() {
		return mCertification;
	}

	public void setCertification(String mCertification) {
		this.mCertification = mCertification;
	}

	public String getImdbId() {
		return mImdbId;
	}

	public void setImdbId(String mImdbId) {
		this.mImdbId = mImdbId;
	}

	public String getPoster() {
		return mPoster;
	}

	public void setPoster(String mPoster) {
		this.mPoster = mPoster;
	}

	public String getFanart() {
		return mFanart;
	}

	public void setFanart(String mFanart) {
		this.mFanart = mFanart;
	}

	public List<String> getGenres() {
		return mGenres;
	}

	public void setGenres(List<String> mGenres) {
		this.mGenres = mGenres;
	}

	public int getYear() {
		return mYear;
	}

	public void setYear(int mYear) {
		this.mYear = mYear;
	}

	public int getRuntime() {
		return mRuntime;
	}

	public void setRuntime(int mRuntime) {
		this.mRuntime = mRuntime;
	}

	public int getRating() {
		return mRating;
	}

	public void setRating(int mRating) {
		this.mRating = mRating;
	}
}