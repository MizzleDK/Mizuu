package com.miz.functions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartScheduledUpdatesService extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		MizLib.scheduleMovieUpdate(context);
		MizLib.scheduleShowsUpdate(context);
	}

}
