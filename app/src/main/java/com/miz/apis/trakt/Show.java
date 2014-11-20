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

public class Show {

	private String mTitle, mUrl, mCountry, mOverview, mStatus, mNetwork, mAirDay, mAirTime, mCertification, mImdbId, mPoster, mFanart;
	private List<String> mGenres = new ArrayList<>();
	private int mYear, mRuntime, mTvdbId, mTvRageId, mRating;
	private long mFirstAired;
	
	public Show(String title) {
		mTitle = title;
	}
	
	public Show(JSONObject summaryJson) {
		try {
			mTitle = summaryJson.getString("title");
			mYear = summaryJson.getInt("year");
			mUrl = summaryJson.getString("url");
			mFirstAired = summaryJson.getLong("first_aired");
			mCountry = summaryJson.getString("country");
			mOverview = summaryJson.getString("overview");
			mRuntime = summaryJson.getInt("runtime");
			mStatus = summaryJson.getString("status");
			mNetwork = summaryJson.getString("network");
			mAirDay = summaryJson.getString("air_day");
			mAirTime = summaryJson.getString("air_time");
			mCertification = summaryJson.getString("certification");
			mImdbId = summaryJson.getString("imdb_id");
			mTvdbId = summaryJson.getInt("tvdb_id");
			mTvRageId = summaryJson.getInt("tvrage_id");
			mPoster = summaryJson.getString("poster");
			mFanart = summaryJson.getJSONObject("images").getString("fanart");
			mRating = summaryJson.getJSONObject("ratings").getInt("percentage");
			
			JSONArray genres = summaryJson.getJSONArray("genres");
			for (int i = 0; i < genres.length(); i++)
				mGenres.add(genres.getString(i));
			
		} catch (JSONException e) {}
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

	public String getCountry() {
		return mCountry;
	}

	public void setCountry(String mCountry) {
		this.mCountry = mCountry;
	}

	public String getOverview() {
		return mOverview;
	}

	public void setOverview(String mOverview) {
		this.mOverview = mOverview;
	}

	public String getStatus() {
		return mStatus;
	}

	public void setStatus(String mStatus) {
		this.mStatus = mStatus;
	}

	public String getNetwork() {
		return mNetwork;
	}

	public void setNetwork(String mNetwork) {
		this.mNetwork = mNetwork;
	}

	public String getAirDay() {
		return mAirDay;
	}

	public void setAirDay(String mAirDay) {
		this.mAirDay = mAirDay;
	}

	public String getAirTime() {
		return mAirTime;
	}

	public void setAirTime(String mAirTime) {
		this.mAirTime = mAirTime;
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

	public int getTvdbId() {
		return mTvdbId;
	}

	public void setTvdbId(int mTvdbId) {
		this.mTvdbId = mTvdbId;
	}

	public int getTvRageId() {
		return mTvRageId;
	}

	public void setTvRageId(int mTvRageId) {
		this.mTvRageId = mTvRageId;
	}

	public int getRating() {
		return mRating;
	}

	public void setRating(int mRating) {
		this.mRating = mRating;
	}

	public long getFirstAired() {
		return mFirstAired;
	}

	public void setFirstAired(long mFirstAired) {
		this.mFirstAired = mFirstAired;
	}
}