/*
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
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

package tv.danmaku.ijk.media.sample.widget.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.TextureMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaFormat;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkMediaFormat;
import tv.danmaku.ijk.media.player.pragma.DebugLog;
import tv.danmaku.ijk.media.sample.R;
import tv.danmaku.ijk.media.sample.application.Settings;
import tv.danmaku.ijk.media.sample.services.MediaPlayerService;
import tv.danmaku.ijk.media.sample.widget.media.MediaController.MediaPlayerControl;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.SettingNotFoundException;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;

public class IjkVideoView extends FrameLayout implements
		MediaController.MediaPlayerControl, MediaPlayerControl {
	private String TAG = "IjkVideoView";
	// settable by the client
	private Uri mUri;
	private Map<String, String> mHeaders;

	// all possible internal states
	private static final int STATE_ERROR = -1;
	private static final int STATE_IDLE = 0;
	private static final int STATE_PREPARING = 1;
	private static final int STATE_PREPARED = 2;
	private static final int STATE_PLAYING = 3;
	private static final int STATE_PAUSED = 4;
	private static final int STATE_PLAYBACK_COMPLETED = 5;

	// mCurrentState is a VideoView object's current state.
	// mTargetState is the state that a method caller intends to reach.
	// For instance, regardless the VideoView object's current state,
	// calling pause() intends to bring the object to a target state
	// of STATE_PAUSED.
	private int mCurrentState = STATE_IDLE;
	private int mTargetState = STATE_IDLE;

	// All the stuff we need for playing and showing a video
	private IRenderView.ISurfaceHolder mSurfaceHolder = null;
	private IMediaPlayer mMediaPlayer = null;
	// private int mAudioSession;
	private int mVideoWidth;
	private int mVideoHeight;
	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private int mVideoRotationDegree;
	private IMediaController mMediaController;
	private IMediaPlayer.OnCompletionListener mOnCompletionListener;
	private IMediaPlayer.OnPreparedListener mOnPreparedListener;
	private int mCurrentBufferPercentage;
	private IMediaPlayer.OnErrorListener mOnErrorListener;
	private IMediaPlayer.OnInfoListener mOnInfoListener;
	private int mSeekWhenPrepared; // recording the seek position while
									// preparing
	private boolean mCanPause = true;
	private boolean mCanSeekBack = true;
	private boolean mCanSeekForward = true;

	/** Subtitle rendering widget overlaid on top of the video. */
	// private RenderingWidget mSubtitleWidget;

	/**
	 * Listener for changes to subtitle data, used to redraw when needed.
	 */
	// private RenderingWidget.OnChangedListener mSubtitlesChangedListener;

	private Context mAppContext;
	private Settings mSettings;
	private IRenderView mRenderView;
	private int mVideoSarNum;
	private int mVideoSarDen;

	private InfoHudViewHolder mHudViewHolder;
	/**
	 * 缓存布局
	 */
	public View mMediaBufferingIndicator;

	public IjkVideoView(Context context) {
		super(context);
		initVideoView(context);
	}

	public IjkVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initVideoView(context);
	}

	public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initVideoView(context);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initVideoView(context);
	}

	/************************************ 自定义视频开始 *****************************************/
	private Context mContext;
	private int screenHeight;
	private int screenWidth;
	private WindowManager mWm;
	private int currentBrightness;// 屏幕亮度值
	private boolean isOpenscreenBrightnessAutomatic;
	private View mLayoutVolumeView;// 声音布局文件
	private WindowManager.LayoutParams lp;
	private View mlayoutshow;
	private int defaultVideoHeigh;
	private ProgressBar pb_show_progress;
	private ImageView iv_show_icon;

	/**
	 * 设置
	 * 
	 * @param activity
	 */
	public void setActivity(Activity activity) {
		if (activity != null) {
			lp = activity.getWindow().getAttributes();
			initCustomParameter(activity);

		}
	}

	/**
	 * 初始化自定义参数
	 * 
	 * @param context
	 */
	@SuppressWarnings("deprecation")
	private void initCustomParameter(Context context) {
		mContext = context;
		mWm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		screenWidth = mWm.getDefaultDisplay().getWidth();
		screenHeight = mWm.getDefaultDisplay().getHeight();
		mlayoutshow = View.inflate(context,
				R.layout.show_volume_and_brightness, null);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		mlayoutshow.setLayoutParams(layoutParams);
		addView(mlayoutshow);

		mlayoutshow.setVisibility(View.GONE);
		initVolume();
		screenBrightness_check();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		screenWidth = mWm.getDefaultDisplay().getWidth();
		screenHeight = mWm.getDefaultDisplay().getHeight();
		DebugLog.e(TAG, "screenWidth : " + screenWidth);
		DebugLog.e(TAG, "screenHeight : " + screenHeight);
	}

	private void getInflateHight() {

		ViewTreeObserver vto = IjkVideoView.this.getViewTreeObserver();

		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onGlobalLayout() {

				IjkVideoView.this.getViewTreeObserver()
						.removeGlobalOnLayoutListener(this);

				// 我们在每次监听前remove前一次的监听，避免重复监听。

				defaultVideoHeigh = IjkVideoView.this.getHeight();
				DebugLog.e(
						TAG,
						"addOnPreDrawListener = "
								+ IjkVideoView.this.getHeight());
				// imageView.getWidth();

			}

		});
	}

	/**
	 * 初始化音量
	 */
	private void initVolume() {
		if (mlayoutshow != null) {
			iv_show_icon = (ImageView) mlayoutshow
					.findViewById(R.id.iv_show_icon);
			pb_show_progress = (ProgressBar) mlayoutshow
					.findViewById(R.id.pb_show_progress);
			// LayoutParams showParams = (LayoutParams) mlayoutshow
			// .getLayoutParams();
			// layoutshowHeight = mlayoutshow.getMeasuredHeight();
			// layoutshowWidth = mlayoutshow.getMeasuredWidth();
			// // showParams.leftMargin = screenWidth / 2 - layoutshowWidth / 2;
			// showParams.topMargin = (defaultVideoHeigh / 2 - layoutshowHeight
			// * 2);
			// mlayoutshow.setX()
			// mlayoutshow.setLayoutParams(showParams);
		}
		mAudioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		currentVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);

	}

	public void closeCustomParmeter() {
		// setBrightness(saveStartBigth);
		if (isOpenscreenBrightnessAutomatic) {
			isOpenscreenBrightnessAutomatic = false;
			android.provider.Settings.System
					.putInt(mContext.getContentResolver(),
							android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
							android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

		}
		if (mContext != null) {
			mContext = null;
		}
		if (mMediaController != null) {
			mMediaController.hide();
		}

	}

	/**
	 * 亮度自动调节
	 */
	private void screenBrightness_check() {
		// 先关闭系统的亮度自动调节
		try {
			if (android.provider.Settings.System.getInt(
					mContext.getContentResolver(),
					android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE) == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
				isOpenscreenBrightnessAutomatic = true;
				android.provider.Settings.System
						.putInt(mContext.getContentResolver(),
								android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
								android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			}
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		// 获取当前亮度,获取失败则返回255
		currentBrightness = (int) (android.provider.Settings.System.getInt(
				mContext.getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS, 255));
		// 文本、进度条显示
		// mSeekBar_light.setProgress(intScreenBrightness);
		// mTextView_light.setText(""+intScreenBrightness*100/255);

	}

	/**
	 * 改变方向
	 * 
	 * @param changDirection
	 */
	public void setConfigurationChanged(int changDirection) {
		if (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == changDirection) {
			android.view.ViewGroup.LayoutParams layoutParams = getLayoutParams();
			layoutParams.width = LayoutParams.MATCH_PARENT;
			layoutParams.height = defaultVideoHeigh;
			setLayoutParams(layoutParams);
		} else {
			android.view.ViewGroup.LayoutParams layoutParams = getLayoutParams();
			layoutParams.width = LayoutParams.MATCH_PARENT;
			layoutParams.height = Math.min(screenHeight, screenWidth);
			setLayoutParams(layoutParams);
		}
		DebugLog.e(TAG, "切换横竖屏--------------");
	}

	/**************** 手势识别结束 ***********************/
	// float startX;
	/********************* 触摸 ********************/
	float downY = 0;

	private AudioManager mAudioManager;

	private int maxVolume;

	private int currentVolume;

	private boolean isShowMediaController = true;
	/**
	 * 
	 * @param ev
	 * @return
	 */
	int startX;

	public boolean customTouchEvent(MotionEvent ev) {
		DebugLog.e(TAG,
				"----------------customTouchEvent----------------------");
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:

			startX = (int) ev.getX();
			downY = ev.getY();
			break;
		case MotionEvent.ACTION_UP:
			int upDistanceX = (int) (ev.getX() - startX);
			int upDistanceY = (int) (ev.getY() - downY);
			if ((Math.abs(upDistanceX) < 20) && (Math.abs(upDistanceY) < 20)) {
				if (mMediaController != null && isShowMediaController) {
					mHandler.sendEmptyMessage(FLAG_HIDE_LAYOUT_SHOW);
					if (mMediaController.isShowing()) {
						mMediaController.hide();
					} else {
						mMediaController.show();
					}
				}

			}
			break;
		case MotionEvent.ACTION_MOVE:
			float moveY = ev.getY();
			float moveDistanceY = moveY - downY;// 有正有负
			// 防误触摸操作
			// 当用户滑动的距离超过touchSlop的时候，才认为是想改变音量
			if (Math.abs(moveDistanceY) < 20) {

				break;
			}
			isShowMediaController = false;
			// 2.计算出手指滑动距离的百分比
			int totalDistance = Math.min(screenWidth, screenHeight);
			float movePercent = Math.abs(moveDistanceY) / totalDistance;
			// 3.根据滑动百分比计算出滑动的音量
			float moveVolume = movePercent * maxVolume;
			// 由于计算出的moveVolume值非常小，所以设定每次滑动都改变1个单位的音量

			if (ev.getX() > screenWidth / 2) {
				// 在屏幕右半部分滑动，是改变音量
				if (moveDistanceY < 0) {
					// 往上滑动
					currentVolume += 1;
					if (currentVolume > maxVolume) {
						currentVolume = (int) maxVolume;
					}
				} else {
					currentVolume -= 1;
					if (currentVolume < 0) {
						currentVolume = 0;
					}
				}
				// isMute = false;
				updateSystemVolume();
			} else {
				// 在屏幕左半部分滑动，是改变亮度
				if (moveDistanceY < 0) {
					currentBrightness += 10;
					if (currentBrightness > 255) {
						currentBrightness = 255;
					}
				} else {
					currentBrightness -= 10;
					if (currentBrightness < 0) {
						currentBrightness = 0;
					}
				}
				if (lp != null) {
					setBrightness(currentBrightness);
				}
			}

			downY = moveY;
			break;

		}
		return true;

	}

	/*
	 * 
	 * 设置屏幕亮度 lp = 0 全暗 ，lp= -1,根据系统设置， lp = 1; 最亮
	 */
	private void setBrightness(float brightness) {
		DebugLog.e(TAG, "--------brightness-------" + brightness);

		// if (lp.screenBrightness <= 0.1) {
		// return;
		// }
		DebugLog.e(TAG, "lp.screenBrightness : " + lp.screenBrightness);
		lp.screenBrightness = brightness / 255.0f;
		if (lp.screenBrightness > 1) {
			lp.screenBrightness = 1;
			// Vibrator vibrator = (Vibrator) mContext
			// .getSystemService(Context.VIBRATOR_SERVICE);
			// long[] pattern = { 10, 200 }; // OFF/ON/OFF/ON...
			// vibrator.vibrate(pattern, -1);
		} else if (lp.screenBrightness < 0.0) {
			lp.screenBrightness = (float) 0.0;
			// Vibrator vibrator = (Vibrator) mContext
			// .getSystemService(Context.VIBRATOR_SERVICE);
			// long[] pattern = { 10, 200 }; // OFF/ON/OFF/ON...
			// vibrator.vibrate(pattern, -1);
		}
		DebugLog.e(TAG, "lp.screenBrightness= " + lp.screenBrightness);
		upProgressBar(100, (int) (lp.screenBrightness * 100),
				FLAG_TYPE_BRIGHTNESS);
		((Activity) mContext).getWindow().setAttributes(lp);
	}

	/**
	 * 更新声音大小
	 */
	private void updateSystemVolume() {
		if (mLayoutVolumeView != null && mLayoutVolumeView.isShown()) {
			mLayoutVolumeView.setVisibility(View.GONE);
		}
		if (mAudioManager != null) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					currentVolume, 0);
		}
		if (maxVolume != 0) {
			upProgressBar(100, (int) (currentVolume * 100 / maxVolume),
					FLAG_HIDE_LAYOUT_SHOW);
		}
		// if (CurrentVolumeShow != null) {
		//
		// CurrentVolumeShow.setText()
		// + "");
		// mHandler.removeMessages(FADE_OUT);
		// mHandler.sendEmptyMessageDelayed(FADE_OUT, 1000);
		// }

	}

	/**
	 * 更新进度条
	 * 
	 * @param max
	 * @param progress
	 */
	public final static int FLAG_TYPE_VOLUME = 1;
	public final static int FLAG_TYPE_BRIGHTNESS = 2;

	private void upProgressBar(int max, int progress, int showType) {
		if (iv_show_icon != null) {
			if (showType == FLAG_TYPE_BRIGHTNESS) {
				iv_show_icon.setImageResource(R.drawable.ic_brightness);
			} else {
				iv_show_icon.setImageResource(R.drawable.ic_volume);
			}
		}
		if (pb_show_progress != null) {
			pb_show_progress.setMax(max);
			pb_show_progress.setProgress(progress);
			if (!mlayoutshow.isShown()) {
				if (mMediaController != null && mMediaController.isShowing()) {
					mMediaController.hide();
				}
				mlayoutshow.setVisibility(View.VISIBLE);
			}
			mHandler.removeMessages(FLAG_HIDE_LAYOUT_SHOW);
			mHandler.sendEmptyMessageDelayed(FLAG_HIDE_LAYOUT_SHOW, 500);
		}

	}

	private final static int FLAG_HIDE_LAYOUT_SHOW = 1;
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FLAG_HIDE_LAYOUT_SHOW:
				if (mlayoutshow != null && mlayoutshow.isShown()) {
					mlayoutshow.setVisibility(View.GONE);
				}
				isShowMediaController = true;
				break;

			}

		}

	};

	/************************************ 自定义视频结束 *****************************************/
	// REMOVED: onMeasure
	// REMOVED: onInitializeAccessibilityEvent
	// REMOVED: onInitializeAccessibilityNodeInfo
	// REMOVED: resolveAdjustedSize

	private void initVideoView(Context context) {
		// initCustomParameter(context);
		mAppContext = context.getApplicationContext();
		mSettings = new Settings(mAppContext);
		initBackground();
		initRenders();
		getInflateHight();
		mVideoWidth = 0;
		mVideoHeight = 0;
		// REMOVED:
		// getHolder().addCallback(mSHCallback);
		// REMOVED:
		// getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
		// REMOVED: mPendingSubtitleTracks = new Vector<Pair<InputStream,
		// MediaFormat>>();
		mCurrentState = STATE_IDLE;
		mTargetState = STATE_IDLE;
	}

	public void setRenderView(IRenderView renderView) {
		if (mRenderView != null) {
			if (mMediaPlayer != null)
				mMediaPlayer.setDisplay(null);

			View renderUIView = mRenderView.getView();
			mRenderView.removeRenderCallback(mSHCallback);
			mRenderView = null;
			removeView(renderUIView);
		}

		if (renderView == null)
			return;

		mRenderView = renderView;
		renderView.setAspectRatio(mCurrentAspectRatio);
		if (mVideoWidth > 0 && mVideoHeight > 0)
			renderView.setVideoSize(mVideoWidth, mVideoHeight);
		if (mVideoSarNum > 0 && mVideoSarDen > 0)
			renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

		View renderUIView = mRenderView.getView();
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		renderUIView.setLayoutParams(lp);
		addView(renderUIView);

		mRenderView.addRenderCallback(mSHCallback);
		mRenderView.setVideoRotation(mVideoRotationDegree);
	}

	public void setRender(int render) {
		switch (render) {
		case RENDER_NONE:
			setRenderView(null);
			break;
		case RENDER_TEXTURE_VIEW: {
			TextureRenderView renderView = new TextureRenderView(getContext());
			if (mMediaPlayer != null) {
				renderView.getSurfaceHolder().bindToMediaPlayer(mMediaPlayer);
				renderView.setVideoSize(mMediaPlayer.getVideoWidth(),
						mMediaPlayer.getVideoHeight());
				renderView.setVideoSampleAspectRatio(
						mMediaPlayer.getVideoSarNum(),
						mMediaPlayer.getVideoSarDen());
				renderView.setAspectRatio(mCurrentAspectRatio);
			}
			setRenderView(renderView);
			break;
		}
		case RENDER_SURFACE_VIEW: {
			SurfaceRenderView renderView = new SurfaceRenderView(getContext());
			setRenderView(renderView);
			break;
		}
		default:
			DebugLog.e(TAG, String.format(Locale.getDefault(),
					"invalid render %d\n", render));
			break;
		}
	}

	// public void setHudView(TableLayout tableLayout) {
	// mHudViewHolder = new InfoHudViewHolder(getContext(), tableLayout);
	// }

	/**
	 * Sets video path.
	 * 
	 * @param path
	 *            the path of the video.
	 */
	public void setVideoPath(String path) {
		setVideoURI(Uri.parse(path));
	}

	/**
	 * Sets video URI.
	 * 
	 * @param uri
	 *            the URI of the video.
	 */
	public void setVideoURI(Uri uri) {
		setVideoURI(uri, null);
	}

	/**
	 * Sets video URI using specific headers.
	 * 
	 * @param uri
	 *            the URI of the video.
	 * @param headers
	 *            the headers for the URI request. Note that the cross domain
	 *            redirection is allowed by default, but that can be changed
	 *            with key/value pairs through the headers parameter with
	 *            "android-allow-cross-domain-redirect" as the key and "0" or
	 *            "1" as the value to disallow or allow cross domain
	 *            redirection.
	 */
	private void setVideoURI(Uri uri, Map<String, String> headers) {
		mUri = uri;
		mHeaders = headers;
		mSeekWhenPrepared = 0;
		openVideo();
		requestLayout();
		invalidate();
	}

	// REMOVED: addSubtitleSource
	// REMOVED: mPendingSubtitleTracks

	public void stopPlayback() {
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			if (mHudViewHolder != null)
				mHudViewHolder.setMediaPlayer(null);
			mCurrentState = STATE_IDLE;
			mTargetState = STATE_IDLE;
			AudioManager am = (AudioManager) mAppContext
					.getSystemService(Context.AUDIO_SERVICE);
			am.abandonAudioFocus(null);
		}
	}

	@TargetApi(Build.VERSION_CODES.M)
	private void openVideo() {
		if (mUri == null || mSurfaceHolder == null) {
			// not ready for playback just yet, will try again later
			return;
		}
		// we shouldn't clear the target state, because somebody might have
		// called start() previously
		release(false);

		AudioManager am = (AudioManager) mAppContext
				.getSystemService(Context.AUDIO_SERVICE);
		am.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);

		try {
			// mMediaPlayer = createPlayer(mSettings.getPlayer());
			// mMediaPlayer =
			// createPlayer(Settings.PV_PLAYER__AndroidMediaPlayer);
			mMediaPlayer = createPlayer(Settings.PV_PLAYER__IjkMediaPlayer);
			// TODO: create SubtitleController in MediaPlayer, but we need
			// a context for the subtitle renderers
			final Context context = getContext();
			// REMOVED: SubtitleController

			// REMOVED: mAudioSession
			mMediaPlayer.setOnPreparedListener(mPreparedListener);
			mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
			mMediaPlayer.setOnCompletionListener(mCompletionListener);
			mMediaPlayer.setOnErrorListener(mErrorListener);
			mMediaPlayer.setOnInfoListener(mInfoListener);
			mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
			mCurrentBufferPercentage = 0;
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
			} else {
				mMediaPlayer.setDataSource(mUri.toString());
			}
			bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setScreenOnWhilePlaying(true);
			mMediaPlayer.prepareAsync();
			if (mHudViewHolder != null)
				mHudViewHolder.setMediaPlayer(mMediaPlayer);

			// REMOVED: mPendingSubtitleTracks

			// we don't set the target state here either, but preserve the
			// target state that was there before.
			mCurrentState = STATE_PREPARING;
			attachMediaController();
		} catch (IOException ex) {
			DebugLog.w(TAG, "Unable to open content: " + mUri, ex);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer,
					MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
		} catch (IllegalArgumentException ex) {
			DebugLog.w(TAG, "Unable to open content: " + mUri, ex);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer,
					MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
		} finally {
			// REMOVED: mPendingSubtitleTracks.clear();
		}
	}

	public void setMediaController(IMediaController controller) {
		// if (mMediaController != null) {
		// mMediaController.hide();
		// }
		mMediaController = controller;
		attachMediaController();
	}

	public void setMediaBufferingIndicator(View mediaBufferingIndicator) {
		if (mMediaBufferingIndicator != null)
			mMediaBufferingIndicator.setVisibility(View.GONE);
		mMediaBufferingIndicator = mediaBufferingIndicator;
	}

	private void attachMediaController() {
		if (mMediaPlayer != null && mMediaController != null) {
			mMediaController.setMediaPlayer(this);
			View anchorView = this.getParent() instanceof View ? (View) this
					.getParent() : this;
			mMediaController.setAnchorView(anchorView);
			mMediaController.setEnabled(isInPlaybackState());
		}
	}

	IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new IMediaPlayer.OnVideoSizeChangedListener() {
		public void onVideoSizeChanged(IMediaPlayer mp, int width, int height,
				int sarNum, int sarDen) {
			mVideoWidth = mp.getVideoWidth();
			mVideoHeight = mp.getVideoHeight();
			mVideoSarNum = mp.getVideoSarNum();
			mVideoSarDen = mp.getVideoSarDen();
			if (mVideoWidth != 0 && mVideoHeight != 0) {
				if (mRenderView != null) {
					mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
					mRenderView.setVideoSampleAspectRatio(mVideoSarNum,
							mVideoSarDen);
				}
				// REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
				requestLayout();
			}
		}
	};

	IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
		public void onPrepared(IMediaPlayer mp) {
			mCurrentState = STATE_PREPARED;

			// Get the capabilities of the player for this stream
			// REMOVED: Metadata

			if (mOnPreparedListener != null) {
				mOnPreparedListener.onPrepared(mMediaPlayer);
			}
			if (mMediaController != null) {
				mMediaController.setEnabled(true);
			}
			mVideoWidth = mp.getVideoWidth();
			mVideoHeight = mp.getVideoHeight();

			int seekToPosition = mSeekWhenPrepared; // mSeekWhenPrepared may be
													// changed after seekTo()
													// call
			if (seekToPosition != 0) {
				seekTo(seekToPosition);
			}
			if (mVideoWidth != 0 && mVideoHeight != 0) {
				// Log.i("@@@@", "video size: " + mVideoWidth +"/"+
				// mVideoHeight);
				// REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
				if (mRenderView != null) {
					mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
					mRenderView.setVideoSampleAspectRatio(mVideoSarNum,
							mVideoSarDen);
					if (!mRenderView.shouldWaitForResize()
							|| mSurfaceWidth == mVideoWidth
							&& mSurfaceHeight == mVideoHeight) {
						// We didn't actually change the size (it was already at
						// the size
						// we need), so we won't get a "surface changed"
						// callback, so
						// start the video here instead of in the callback.
						if (mTargetState == STATE_PLAYING) {
							start();
							if (mMediaController != null) {
								mMediaController.show();
							}
						} else if (!isPlaying()
								&& (seekToPosition != 0 || getCurrentPosition() > 0)) {
							if (mMediaController != null) {
								// Show the media controls when we're paused
								// into a video and make 'em stick.
								mMediaController.show(0);
							}
						}
					}
				}
			} else {
				// We don't know the video size yet, but should start anyway.
				// The video size might be reported to us later.
				if (mTargetState == STATE_PLAYING) {
					start();
				}
			}
		}
	};

	private IMediaPlayer.OnCompletionListener mCompletionListener = new IMediaPlayer.OnCompletionListener() {
		public void onCompletion(IMediaPlayer mp) {
			mCurrentState = STATE_PLAYBACK_COMPLETED;
			mTargetState = STATE_PLAYBACK_COMPLETED;
			if (mMediaController != null) {
				mMediaController.hide();
			}
			if (mOnCompletionListener != null) {
				mOnCompletionListener.onCompletion(mMediaPlayer);
				if (mMediaBufferingIndicator != null)
					mMediaBufferingIndicator.setVisibility(View.GONE);
			}
		}
	};

	private IMediaPlayer.OnInfoListener mInfoListener = new IMediaPlayer.OnInfoListener() {
		public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
			if (mOnInfoListener != null) {
				mOnInfoListener.onInfo(mp, arg1, arg2);
			}
			DebugLog.e(TAG, "arg1 = " + arg1);

			DebugLog.e(TAG, "arg2 = " + arg2);
			switch (arg1) {
			case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
				DebugLog.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
				break;
			case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
				DebugLog.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
				if (mMediaBufferingIndicator != null)
					mMediaBufferingIndicator.setVisibility(View.GONE);
				break;
			case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
				if (mMediaBufferingIndicator != null)
					mMediaBufferingIndicator.setVisibility(View.VISIBLE);
				DebugLog.e(TAG, "MEDIA_INFO_BUFFERING_START:缓存开始");
				break;
			case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
				DebugLog.e(TAG, "MEDIA_INFO_BUFFERING_END:缓存完成");
				if (mMediaBufferingIndicator != null)
					mMediaBufferingIndicator.setVisibility(View.GONE);
				break;
			case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
				DebugLog.d(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + arg2);
				break;
			case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
				DebugLog.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
				break;
			case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
				DebugLog.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
				break;
			case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
				DebugLog.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
				break;
			case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
				DebugLog.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
				break;
			case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
				DebugLog.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
				break;
			case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
				mVideoRotationDegree = arg2;
				DebugLog.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
				if (mRenderView != null)
					mRenderView.setVideoRotation(arg2);
				break;
			case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
				DebugLog.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
				break;
			}
			return true;
		}
	};

	private IMediaPlayer.OnErrorListener mErrorListener = new IMediaPlayer.OnErrorListener() {
		public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
			DebugLog.d(TAG, "Error: " + framework_err + "," + impl_err);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			if (mMediaController != null) {
				mMediaController.hide();
			}

			/* If an error handler has been supplied, use it and finish. */
			if (mOnErrorListener != null) {
				if (mOnErrorListener.onError(mMediaPlayer, framework_err,
						impl_err)) {
					return true;
				}
			}

			/*
			 * Otherwise, pop up an error dialog so the user knows that
			 * something bad has happened. Only try and pop up the dialog if
			 * we're attached to a window. When we're going away and no longer
			 * have a window, don't bother showing the user an error.
			 */
			if (getWindowToken() != null) {
				Resources r = mAppContext.getResources();
				int messageId;

				if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
					messageId = R.string.VideoView_error_text_invalid_progressive_playback;
				} else {
					messageId = R.string.VideoView_error_text_unknown;
				}

				// new AlertDialog.Builder(getContext())
				// .setMessage(messageId)
				// .setPositiveButton(R.string.VideoView_error_button,
				// new DialogInterface.OnClickListener() {
				// public void onClick(DialogInterface dialog, int whichButton)
				// {
				// /* If we get here, there is no onError listener, so
				// * at least inform them that the video is over.
				// */
				// if (mOnCompletionListener != null) {
				// mOnCompletionListener.onCompletion(mMediaPlayer);
				// }
				// }
				// })
				// .setCancelable(false)
				// .show();
			}
			return true;
		}
	};

	private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
		public void onBufferingUpdate(IMediaPlayer mp, int percent) {
			mCurrentBufferPercentage = percent;
		}
	};

	/**
	 * Register a callback to be invoked when the media file is loaded and ready
	 * to go.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {
		mOnPreparedListener = l;
	}

	/**
	 * Register a callback to be invoked when the end of a media file has been
	 * reached during playback.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {
		mOnCompletionListener = l;
	}

	/**
	 * Register a callback to be invoked when an error occurs during playback or
	 * setup. If no listener is specified, or if the listener returned false,
	 * VideoView will inform the user of any errors.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnErrorListener(IMediaPlayer.OnErrorListener l) {
		mOnErrorListener = l;
	}

	/**
	 * Register a callback to be invoked when an informational event occurs
	 * during playback or setup.
	 * 
	 * @param l
	 *            The callback that will be run
	 */
	public void setOnInfoListener(IMediaPlayer.OnInfoListener l) {
		mOnInfoListener = l;
	}

	// REMOVED: mSHCallback
	private void bindSurfaceHolder(IMediaPlayer mp,
			IRenderView.ISurfaceHolder holder) {
		if (mp == null)
			return;

		if (holder == null) {
			mp.setDisplay(null);
			return;
		}

		holder.bindToMediaPlayer(mp);
	}

	IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
		@Override
		public void onSurfaceChanged(
				@NonNull IRenderView.ISurfaceHolder holder, int format, int w,
				int h) {
			if (holder.getRenderView() != mRenderView) {
				DebugLog.e(TAG, "onSurfaceChanged: unmatched render callback\n");
				return;
			}

			mSurfaceWidth = w;
			mSurfaceHeight = h;
			boolean isValidState = (mTargetState == STATE_PLAYING);
			boolean hasValidSize = !mRenderView.shouldWaitForResize()
					|| (mVideoWidth == w && mVideoHeight == h);
			if (mMediaPlayer != null && isValidState && hasValidSize) {
				if (mSeekWhenPrepared != 0) {
					seekTo(mSeekWhenPrepared);
				}
				start();
			}
		}

		@Override
		public void onSurfaceCreated(
				@NonNull IRenderView.ISurfaceHolder holder, int width,
				int height) {
			if (holder.getRenderView() != mRenderView) {
				DebugLog.e(TAG, "onSurfaceCreated: unmatched render callback\n");
				return;
			}

			mSurfaceHolder = holder;
			if (mMediaPlayer != null)
				bindSurfaceHolder(mMediaPlayer, holder);
			else
				openVideo();
		}

		@Override
		public void onSurfaceDestroyed(
				@NonNull IRenderView.ISurfaceHolder holder) {
			if (holder.getRenderView() != mRenderView) {
				DebugLog.e(TAG,
						"onSurfaceDestroyed: unmatched render callback\n");
				return;
			}

			// after we return from this we can't use the surface any more
			mSurfaceHolder = null;
			// REMOVED: if (mMediaController != null) mMediaController.hide();
			// REMOVED: release(true);
			releaseWithoutStop();
		}
	};

	public void releaseWithoutStop() {
		if (mMediaPlayer != null)
			mMediaPlayer.setDisplay(null);
	}

	/*
	 * release the media player in any state
	 */
	public void release(boolean cleartargetstate) {
		if (mMediaPlayer != null) {
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
			// REMOVED: mPendingSubtitleTracks.clear();
			mCurrentState = STATE_IDLE;
			if (cleartargetstate) {
				mTargetState = STATE_IDLE;
			}
			AudioManager am = (AudioManager) mAppContext
					.getSystemService(Context.AUDIO_SERVICE);
			am.abandonAudioFocus(null);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// MyGestureListener myGestureListener = new MyGestureListener();
		// GestureDetector mGesture = new GestureDetector(getContext(),
		// myGestureListener);
		// if (isInPlaybackState() && mMediaController != null) {
		// // toggleMediaControlsVisiblity();
		// }
		return customTouchEvent(ev);
		// return mGesture.onTouchEvent(ev);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		if (isInPlaybackState() && mMediaController != null) {
			toggleMediaControlsVisiblity();
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK
				&& keyCode != KeyEvent.KEYCODE_VOLUME_UP
				&& keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
				&& keyCode != KeyEvent.KEYCODE_VOLUME_MUTE
				&& keyCode != KeyEvent.KEYCODE_MENU
				&& keyCode != KeyEvent.KEYCODE_CALL
				&& keyCode != KeyEvent.KEYCODE_ENDCALL;
		if (isInPlaybackState() && isKeyCodeSupported
				&& mMediaController != null) {
			if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
				if (mMediaPlayer.isPlaying()) {
					pause();
					mMediaController.show();
				} else {
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
				if (!mMediaPlayer.isPlaying()) {
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
				if (mMediaPlayer.isPlaying()) {
					pause();
					mMediaController.show();
				}
				return true;
			} else {
				toggleMediaControlsVisiblity();
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private void toggleMediaControlsVisiblity() {
		if (mMediaController.isShowing()) {
			mMediaController.hide();
		} else {
			mMediaController.show();
		}
	}

	@Override
	public void start() {
		if (isInPlaybackState()) {
			mMediaPlayer.start();
			mCurrentState = STATE_PLAYING;
		}
		mTargetState = STATE_PLAYING;
	}

	@Override
	public void pause() {
		if (isInPlaybackState()) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.pause();
				mCurrentState = STATE_PAUSED;
			}
		}
		mTargetState = STATE_PAUSED;
	}

	public void suspend() {
		release(false);
	}

	public void resume() {
		openVideo();
	}

	@Override
	public int getDuration() {
		if (isInPlaybackState()) {
			return (int) mMediaPlayer.getDuration();
		}

		return -1;
	}

	@Override
	public int getCurrentPosition() {
		if (isInPlaybackState()) {
			return (int) mMediaPlayer.getCurrentPosition();
		}
		return 0;
	}

	@Override
	public void seekTo(int msec) {
		if (isInPlaybackState()) {
			mMediaPlayer.seekTo(msec);
			mSeekWhenPrepared = 0;
		} else {
			mSeekWhenPrepared = msec;
		}
	}

	@Override
	public boolean isPlaying() {
		return isInPlaybackState() && mMediaPlayer.isPlaying();
	}

	@Override
	public int getBufferPercentage() {
		if (mMediaPlayer != null) {
			return mCurrentBufferPercentage;
		}
		return 0;
	}

	private boolean isInPlaybackState() {
		return (mMediaPlayer != null && mCurrentState != STATE_ERROR
				&& mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
	}

	@Override
	public boolean canPause() {
		return mCanPause;
	}

	@Override
	public boolean canSeekBackward() {
		return mCanSeekBack;
	}

	@Override
	public boolean canSeekForward() {
		return mCanSeekForward;
	}

	@Override
	public int getAudioSessionId() {
		return 0;
	}

	// REMOVED: getAudioSessionId();
	// REMOVED: onAttachedToWindow();
	// REMOVED: onDetachedFromWindow();
	// REMOVED: onLayout();
	// REMOVED: draw();
	// REMOVED: measureAndLayoutSubtitleWidget();
	// REMOVED: setSubtitleWidget();
	// REMOVED: getSubtitleLooper();

	// -------------------------
	// Extend: Aspect Ratio
	// -------------------------

	private static final int[] s_allAspectRatio = {
			IRenderView.AR_ASPECT_FIT_PARENT,
			IRenderView.AR_ASPECT_FILL_PARENT,
			IRenderView.AR_ASPECT_WRAP_CONTENT,
			// IRenderView.AR_MATCH_PARENT,
			IRenderView.AR_16_9_FIT_PARENT, IRenderView.AR_4_3_FIT_PARENT };
	private int mCurrentAspectRatioIndex = 0;
	private int mCurrentAspectRatio = s_allAspectRatio[0];

	public int toggleAspectRatio() {
		mCurrentAspectRatioIndex++;
		mCurrentAspectRatioIndex %= s_allAspectRatio.length;

		mCurrentAspectRatio = s_allAspectRatio[mCurrentAspectRatioIndex];
		if (mRenderView != null)
			mRenderView.setAspectRatio(mCurrentAspectRatio);
		return mCurrentAspectRatio;
	}

	// -------------------------
	// Extend: Render
	// -------------------------
	public static final int RENDER_NONE = 0;
	public static final int RENDER_SURFACE_VIEW = 1;
	public static final int RENDER_TEXTURE_VIEW = 2;

	private List<Integer> mAllRenders = new ArrayList<Integer>();
	private int mCurrentRenderIndex = 0;
	private int mCurrentRender = RENDER_NONE;

	private void initRenders() {
		mAllRenders.clear();

		if (mSettings.getEnableSurfaceView())
			mAllRenders.add(RENDER_SURFACE_VIEW);
		if (mSettings.getEnableTextureView()
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			mAllRenders.add(RENDER_TEXTURE_VIEW);
		if (mSettings.getEnableNoView())
			mAllRenders.add(RENDER_NONE);

		if (mAllRenders.isEmpty())
			mAllRenders.add(RENDER_SURFACE_VIEW);
		mCurrentRender = mAllRenders.get(mCurrentRenderIndex);
		setRender(mCurrentRender);
	}

	public int toggleRender() {
		mCurrentRenderIndex++;
		mCurrentRenderIndex %= mAllRenders.size();

		mCurrentRender = mAllRenders.get(mCurrentRenderIndex);
		setRender(mCurrentRender);
		return mCurrentRender;
	}

	@NonNull
	public static String getRenderText(Context context, int render) {
		String text;
		switch (render) {
		case RENDER_NONE:
			text = context.getString(R.string.VideoView_render_none);
			break;
		case RENDER_SURFACE_VIEW:
			text = context.getString(R.string.VideoView_render_surface_view);
			break;
		case RENDER_TEXTURE_VIEW:
			text = context.getString(R.string.VideoView_render_texture_view);
			break;
		default:
			text = context.getString(R.string.N_A);
			break;
		}
		return text;
	}

	// -------------------------
	// Extend: Player
	// -------------------------
	public int togglePlayer() {
		if (mMediaPlayer != null)
			mMediaPlayer.release();

		if (mRenderView != null)
			mRenderView.getView().invalidate();
		openVideo();
		return mSettings.getPlayer();
	}

	@NonNull
	public static String getPlayerText(Context context, int player) {
		String text;
		switch (player) {
		case Settings.PV_PLAYER__AndroidMediaPlayer:
			text = context
					.getString(R.string.VideoView_player_AndroidMediaPlayer);
			break;
		case Settings.PV_PLAYER__IjkMediaPlayer:
			text = context.getString(R.string.VideoView_player_IjkMediaPlayer);
			break;
		case Settings.PV_PLAYER__IjkExoMediaPlayer:
			text = context
					.getString(R.string.VideoView_player_IjkExoMediaPlayer);
			break;
		default:
			text = context.getString(R.string.N_A);
			break;
		}
		return text;
	}

	public IMediaPlayer createPlayer(int playerType) {
		IMediaPlayer mediaPlayer = null;

		switch (playerType) {
		case Settings.PV_PLAYER__IjkExoMediaPlayer: {
			// IjkExoMediaPlayer IjkExoMediaPlayer = new
			// IjkExoMediaPlayer(mAppContext);
			// mediaPlayer = IjkExoMediaPlayer;
		}
			break;
		case Settings.PV_PLAYER__AndroidMediaPlayer: {
			AndroidMediaPlayer androidMediaPlayer = new AndroidMediaPlayer();
			mediaPlayer = androidMediaPlayer;
		}
			break;
		case Settings.PV_PLAYER__IjkMediaPlayer:
		default: {
			IjkMediaPlayer ijkMediaPlayer = null;
			if (mUri != null) {
				ijkMediaPlayer = new IjkMediaPlayer();
				ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

				if (mSettings.getUsingMediaCodec()) {
					ijkMediaPlayer
							.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
									"mediacodec", 1);
					if (mSettings.getUsingMediaCodecAutoRotate()) {
						ijkMediaPlayer.setOption(
								IjkMediaPlayer.OPT_CATEGORY_PLAYER,
								"mediacodec-auto-rotate", 1);
					} else {
						ijkMediaPlayer.setOption(
								IjkMediaPlayer.OPT_CATEGORY_PLAYER,
								"mediacodec-auto-rotate", 0);
					}
				} else {
					ijkMediaPlayer
							.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
									"mediacodec", 0);
				}

				if (mSettings.getUsingOpenSLES()) {
					ijkMediaPlayer.setOption(
							IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
				} else {
					ijkMediaPlayer.setOption(
							IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
				}

				String pixelFormat = mSettings.getPixelFormat();
				if (TextUtils.isEmpty(pixelFormat)) {
					ijkMediaPlayer.setOption(
							IjkMediaPlayer.OPT_CATEGORY_PLAYER,
							"overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
				} else {
					ijkMediaPlayer.setOption(
							IjkMediaPlayer.OPT_CATEGORY_PLAYER,
							"overlay-format", pixelFormat);
				}
				ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
						"framedrop", 1);
				ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
						"start-on-prepared", 0);

				ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,
						"http-detect-range-support", 0);

				ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC,
						"skip_loop_filter", 48);
			}
			mediaPlayer = ijkMediaPlayer;
		}
			break;
		}

		if (mSettings.getEnableDetachedSurfaceTextureView()) {
			mediaPlayer = new TextureMediaPlayer(mediaPlayer);
		}

		return mediaPlayer;
	}

	// -------------------------
	// Extend: Background
	// -------------------------

	private boolean mEnableBackgroundPlay = false;

	private void initBackground() {
		mEnableBackgroundPlay = mSettings.getEnableBackgroundPlay();
		if (mEnableBackgroundPlay) {
			MediaPlayerService.intentToStart(getContext());
			mMediaPlayer = MediaPlayerService.getMediaPlayer();
			if (mHudViewHolder != null)
				mHudViewHolder.setMediaPlayer(mMediaPlayer);
		}
	}

	public boolean isBackgroundPlayEnabled() {
		return mEnableBackgroundPlay;
	}

	public void enterBackground() {
		MediaPlayerService.setMediaPlayer(mMediaPlayer);
	}

	public void stopBackgroundPlay() {
		MediaPlayerService.setMediaPlayer(null);
	}

	// -------------------------
	// Extend: Background
	// -------------------------
	public void showMediaInfo() {
		if (mMediaPlayer == null)
			return;

		int selectedVideoTrack = MediaPlayerCompat.getSelectedTrack(
				mMediaPlayer, ITrackInfo.MEDIA_TRACK_TYPE_VIDEO);
		int selectedAudioTrack = MediaPlayerCompat.getSelectedTrack(
				mMediaPlayer, ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);

		TableLayoutBinder builder = new TableLayoutBinder(getContext());
		builder.appendSection(R.string.mi_player);
		builder.appendRow2(R.string.mi_player,
				MediaPlayerCompat.getName(mMediaPlayer));
		builder.appendSection(R.string.mi_media);
		builder.appendRow2(
				R.string.mi_resolution,
				buildResolution(mVideoWidth, mVideoHeight, mVideoSarNum,
						mVideoSarDen));
		builder.appendRow2(R.string.mi_length,
				buildTimeMilli(mMediaPlayer.getDuration()));

		ITrackInfo trackInfos[] = mMediaPlayer.getTrackInfo();
		if (trackInfos != null) {
			int index = -1;
			for (ITrackInfo trackInfo : trackInfos) {
				index++;

				int trackType = trackInfo.getTrackType();
				if (index == selectedVideoTrack) {
					builder.appendSection(getContext().getString(
							R.string.mi_stream_fmt1, index)
							+ " "
							+ getContext().getString(
									R.string.mi__selected_video_track));
				} else if (index == selectedAudioTrack) {
					builder.appendSection(getContext().getString(
							R.string.mi_stream_fmt1, index)
							+ " "
							+ getContext().getString(
									R.string.mi__selected_audio_track));
				} else {
					builder.appendSection(getContext().getString(
							R.string.mi_stream_fmt1, index));
				}
				builder.appendRow2(R.string.mi_type, buildTrackType(trackType));
				builder.appendRow2(R.string.mi_language,
						buildLanguage(trackInfo.getLanguage()));

				IMediaFormat mediaFormat = trackInfo.getFormat();
				if (mediaFormat == null) {
				} else if (mediaFormat instanceof IjkMediaFormat) {
					switch (trackType) {
					case ITrackInfo.MEDIA_TRACK_TYPE_VIDEO:
						builder.appendRow2(
								R.string.mi_codec,
								mediaFormat
										.getString(IjkMediaFormat.KEY_IJK_CODEC_LONG_NAME_UI));
						builder.appendRow2(
								R.string.mi_profile_level,
								mediaFormat
										.getString(IjkMediaFormat.KEY_IJK_CODEC_PROFILE_LEVEL_UI));
						builder.appendRow2(
								R.string.mi_pixel_format,
								mediaFormat
										.getString(IjkMediaFormat.KEY_IJK_CODEC_PIXEL_FORMAT_UI));
						builder.appendRow2(
								R.string.mi_resolution,
								mediaFormat
										.getString(IjkMediaFormat.KEY_IJK_RESOLUTION_UI));
						builder.appendRow2(
								R.string.mi_frame_rate,
								mediaFormat
										.getString(IjkMediaFormat.KEY_IJK_FRAME_RATE_UI));
						builder.appendRow2(R.string.mi_bit_rate, mediaFormat
								.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI));
						break;
					case ITrackInfo.MEDIA_TRACK_TYPE_AUDIO:
						builder.appendRow2(
								R.string.mi_codec,
								mediaFormat
										.getString(IjkMediaFormat.KEY_IJK_CODEC_LONG_NAME_UI));
						builder.appendRow2(
								R.string.mi_profile_level,
								mediaFormat
										.getString(IjkMediaFormat.KEY_IJK_CODEC_PROFILE_LEVEL_UI));
						builder.appendRow2(
								R.string.mi_sample_rate,
								mediaFormat
										.getString(IjkMediaFormat.KEY_IJK_SAMPLE_RATE_UI));
						builder.appendRow2(R.string.mi_channels, mediaFormat
								.getString(IjkMediaFormat.KEY_IJK_CHANNEL_UI));
						builder.appendRow2(R.string.mi_bit_rate, mediaFormat
								.getString(IjkMediaFormat.KEY_IJK_BIT_RATE_UI));
						break;
					default:
						break;
					}
				}
			}
		}

		// AlertDialog.Builder adBuilder = builder.buildAlertDialogBuilder();
		// adBuilder.setTitle(R.string.media_information);
		// adBuilder.setNegativeButton(R.string.close, null);
		// adBuilder.show();
	}

	private String buildResolution(int width, int height, int sarNum, int sarDen) {
		StringBuilder sb = new StringBuilder();
		sb.append(width);
		sb.append(" x ");
		sb.append(height);

		if (sarNum > 1 || sarDen > 1) {
			sb.append("[");
			sb.append(sarNum);
			sb.append(":");
			sb.append(sarDen);
			sb.append("]");
		}

		return sb.toString();
	}

	private String buildTimeMilli(long duration) {
		long total_seconds = duration / 1000;
		long hours = total_seconds / 3600;
		long minutes = (total_seconds % 3600) / 60;
		long seconds = total_seconds % 60;
		if (duration <= 0) {
			return "--:--";
		}
		if (hours >= 100) {
			return String.format(Locale.US, "%d:%02d:%02d", hours, minutes,
					seconds);
		} else if (hours > 0) {
			return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes,
					seconds);
		} else {
			return String.format(Locale.US, "%02d:%02d", minutes, seconds);
		}
	}

	private String buildTrackType(int type) {
		Context context = getContext();
		switch (type) {
		case ITrackInfo.MEDIA_TRACK_TYPE_VIDEO:
			return context.getString(R.string.TrackType_video);
		case ITrackInfo.MEDIA_TRACK_TYPE_AUDIO:
			return context.getString(R.string.TrackType_audio);
		case ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE:
			return context.getString(R.string.TrackType_subtitle);
		case ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT:
			return context.getString(R.string.TrackType_timedtext);
		case ITrackInfo.MEDIA_TRACK_TYPE_METADATA:
			return context.getString(R.string.TrackType_metadata);
		case ITrackInfo.MEDIA_TRACK_TYPE_UNKNOWN:
		default:
			return context.getString(R.string.TrackType_unknown);
		}
	}

	private String buildLanguage(String language) {
		if (TextUtils.isEmpty(language))
			return "und";
		return language;
	}

	public ITrackInfo[] getTrackInfo() {
		if (mMediaPlayer == null)
			return null;

		return mMediaPlayer.getTrackInfo();
	}

	public void selectTrack(int stream) {
		MediaPlayerCompat.selectTrack(mMediaPlayer, stream);
	}

	public void deselectTrack(int stream) {
		MediaPlayerCompat.deselectTrack(mMediaPlayer, stream);
	}

	public int getSelectedTrack(int trackType) {
		return MediaPlayerCompat.getSelectedTrack(mMediaPlayer, trackType);
	}

	@Override
	public void seekTo(long pos) {
		if (mMediaPlayer != null) {
			mMediaPlayer.seekTo(pos);
		}

	}
}
