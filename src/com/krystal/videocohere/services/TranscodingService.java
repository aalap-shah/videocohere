package com.krystal.videocohere.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.krystal.videocohere.VideoCohereApplication;
import com.krystal.videocohere.database.Video;

public class TranscodingService extends IntentService {

	public String extStorePath;
	public static String ActionMergeAllVideos = "ActionMergeAllVideos";

	public TranscodingService() {
		super("TranscodingService");
	}

	public void createOutput(Intent intent) {

		Log.d ("qwe", "Entered createOutput");
		String mOutputPath = null;

		Intent broadcastIntent = new Intent(
				TranscodingService.ActionMergeAllVideos);

		mOutputPath = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
				.getAbsolutePath() + "/Output.mp4";

		File file = new File(VideoCohereApplication.convertPath(mOutputPath));
		if (file.exists())
			file.delete();

		file = new File(mOutputPath);
		if (file.exists())
			file.delete();

		List<Video> videoList = ((VideoCohereApplication) getApplication())
				.getDBA().getVideos();

		if (videoList == null) {
			broadcastIntent.putExtra("OutputFileName", "");
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					broadcastIntent);
			return;
		}

		int j = 0;
		Video v = null;
		ArrayList<Movie> inMovies = new ArrayList<Movie>();

		//Collections.reverse(videoList);
		for (j = 0; j < videoList.size(); j++) {
			v = videoList.get(j);
			Log.d ("qwe","Video path = " + v.path
					+ "video start = " + v.startTime +
					"video end = " + v.endTime);
			Movie trimmedMovie = trimVideo(v.path, (int) v.startTime,
					(int) v.endTime);
			inMovies.add(trimmedMovie);
		}

		List<Track> videoTracks = new LinkedList<Track>();
		List<Track> audioTracks = new LinkedList<Track>();

		for (Movie m : inMovies) {
			for (Track t : m.getTracks()) {
				if (t.getHandler().equals("soun")) {
					audioTracks.add(t);
				}
				if (t.getHandler().equals("vide")) {
					videoTracks.add(t);
				}
			}
		}

		try {
			Movie result = new Movie();

			if (audioTracks.size() > 0) {
				result.addTrack(new AppendTrack(audioTracks
						.toArray(new Track[audioTracks.size()])));
			}
			if (videoTracks.size() > 0) {
				result.addTrack(new AppendTrack(videoTracks
						.toArray(new Track[videoTracks.size()])));
			}

			IsoFile out = new DefaultMp4Builder().build(result);
			FileOutputStream fos = new FileOutputStream(file);
			FileChannel fc = fos.getChannel();
			fc.position(0);
			out.getBox(fc);
			fos.close();
			fc.close();
		} catch (IOException e) {
			broadcastIntent.putExtra("OutputFileName", "");
			broadcastIntent.putExtra("Error",
					"Error creating video. Try removing recent imports.");
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					broadcastIntent);

			e.printStackTrace();
			return;
		}

		VideoCohereApplication.mTL.convertThumbnail(mOutputPath,
				MediaStore.Video.Thumbnails.MINI_KIND);

		Log.d("qwe", "Sending broadcast");
		broadcastIntent.putExtra("OutputFileName", mOutputPath);
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
	
	}

	protected static long getDuration(Track track) {
		long duration = 0;
		for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
			duration += entry.getCount() * entry.getDelta();
		}
		return duration;
	}

	/*
	 * private static double correctTimeToSyncSample(Track track, double
	 * cutHere, boolean next) { double[] timeOfSyncSamples = new
	 * double[track.getSyncSamples().length]; long currentSample = 0; double
	 * currentTime = 0; for (int i = 0; i <
	 * track.getDecodingTimeEntries().size(); i++) { TimeToSampleBox.Entry entry
	 * = track.getDecodingTimeEntries().get(i); for (int j = 0; j <
	 * entry.getCount(); j++) { if (Arrays.binarySearch(track.getSyncSamples(),
	 * currentSample + 1) >= 0) { // samples always start with 1 but we start
	 * with zero // therefore +1 timeOfSyncSamples[Arrays.binarySearch(
	 * track.getSyncSamples(), currentSample + 1)] = currentTime; } currentTime
	 * += (double) entry.getDelta() / (double)
	 * track.getTrackMetaData().getTimescale(); currentSample++; } } double
	 * previous = 0; for (double timeOfSyncSample : timeOfSyncSamples) { if
	 * (timeOfSyncSample > cutHere) { if (next) { return timeOfSyncSample; }
	 * else { return previous; } } previous = timeOfSyncSample; } return
	 * timeOfSyncSamples[timeOfSyncSamples.length - 1]; }
	 */

	public Movie trimVideo(String filePath, int startTime1, int endTime1) {

		Movie movie = null;
		try {
			movie = MovieCreator.build(new FileInputStream(filePath)
					.getChannel());
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		List<Track> tracks = movie.getTracks();
		movie.setTracks(new LinkedList<Track>());

		for (Track track : tracks) {
			long currentSample = 0;
			double currentTime = 0;
			double lastTime = -1;
			long startSample1 = -1;
			long endSample1 = -1;

			for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
				TimeToSampleBox.Entry entry = track.getDecodingTimeEntries()
						.get(i);
				for (int j = 0; j < entry.getCount(); j++) {

					if (currentTime > lastTime && currentTime <= startTime1) {
						// current sample is still before the new starttime
						startSample1 = currentSample;
					}
					if (currentTime > lastTime && currentTime <= endTime1) {
						// current sample is after the new start time and still
						// before the new endtime
						endSample1 = currentSample;
					}
					lastTime = currentTime;
					currentTime += (double) entry.getDelta()
							/ (double) track.getTrackMetaData().getTimescale();
					currentSample++;
				}
			}

			try {
				movie.addTrack(new AppendTrack(new CroppedTrack(track,
						startSample1, endSample1)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return movie;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("qwe", "Calling create output");
		createOutput(intent);
	}
}
