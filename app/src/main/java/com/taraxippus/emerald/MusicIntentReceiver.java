package com.taraxippus.emerald;

import android.content.Context;
import android.content.Intent;
import com.taraxippus.emerald.MainService;

public class MusicIntentReceiver extends android.content.BroadcastReceiver
{
	@Override
	public void onReceive(Context ctx, Intent intent) 
	{
		if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY))
		{
			Intent intent1 = new Intent(ctx.getApplicationContext(), MainService.class);
			intent1.setAction(MainService.ACTION_AUDIO_NOISY);
			ctx.startService(intent1);
		}
	}
}
	

