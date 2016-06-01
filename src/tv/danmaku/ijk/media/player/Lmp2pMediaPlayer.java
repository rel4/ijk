package tv.danmaku.ijk.media.player;

import java.lang.ref.WeakReference;

import tv.danmaku.ijk.media.player.annotations.CalledByNative;
import tv.danmaku.ijk.media.player.pragma.DebugLog;
import tv.danmaku.ijk.media.sample.widget.media.IjkVideoView;
import android.R.bool;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

/**
 * Created by limengke on 2016/4/20.
 */
public class Lmp2pMediaPlayer {

	public final static int LOG_VERBOSE = 127;
	public final static int LOG_INFO = 2;
	public final static int LOG_WARN = 6;
	public final static int LOG_ERROR = 14;
	public final static int LOG_NONE = 0;

	private final static String TAG = Lmp2pMediaPlayer.class.getSimpleName();

	private OnLmp2pMediaListener mLmP2pMediaListener = null;
	/******************* JNI回调 ****************************/
	// 准备完成开始播放
	public final static int PREPARE_TO_PLAYP2P_FILE = -1;
	// p2p下载失败
	public final static int DOWNLOAD_P2PFILE_FAILED = -2;
	// 播放失败
	public final static int PLAY_P2PFILE_FAILED = -3;
	// 缓存进度
	public final static int PLAY_P2PFILE_BUFFERING = -4;
	// 隐藏进度布局
	public final static int FLAG_SHOW_PROGRESS = 1;

	private static IjkVideoView mVideoView;
	private static String LMhash;

	public static interface OnLmp2pMediaListener {
		void onPrepareToPlayP2pFile(String fileHash, String filePath,
				long fileSize);

		void onDownloadP2pFileFailed(String fileHash, int failedCode,
				String failedPrompt);

		void onPlayP2pFileFailed(String fileHash, int failedCode,
				String failedPrompt);

		void onPlayP2pBuffering(String fileHash, int precent);
	}

	private static Lmp2pMediaPlayerHandler mPlayerHandler;

	private class Lmp2pMediaPlayerHandler extends Handler {
		private WeakReference<Lmp2pMediaPlayer> mWeakPlayer;

		public Lmp2pMediaPlayerHandler(Lmp2pMediaPlayer player, Looper looper) {
			super(looper);
			mWeakPlayer = new WeakReference<Lmp2pMediaPlayer>(player);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PREPARE_TO_PLAYP2P_FILE:
				DebugLog.e(TAG, "接受到播放指令");
				if (mWeakPlayer == null || mWeakPlayer.get() == null) {
					break;
				}
				Object obj = msg.obj;
				if (obj != null && obj instanceof MessageBody) {
					MessageBody msgb = (MessageBody) obj;
					mWeakPlayer.get().prepareToPlay(msgb.getFilePath(),
							msgb.getFileHash());
					if (mLmP2pMediaListener != null) {
						mLmP2pMediaListener.onPrepareToPlayP2pFile(
								msgb.getFileHash(), msgb.getFilePath(),
								msgb.getFileSize());
					}
				}
				break;
			case DOWNLOAD_P2PFILE_FAILED:
				DebugLog.e(TAG, "接受到下载失败指令");
				if (mWeakPlayer == null || mWeakPlayer.get() == null) {
					break;
				}
				Object obj1 = msg.obj;
				if (obj1 instanceof MessageBody) {
					MessageBody msgb = (MessageBody) obj1;
					mWeakPlayer.get().upDataPorgressShow("网络不稳定,下载超时啦");
					if (mLmP2pMediaListener != null) {
						mLmP2pMediaListener.onDownloadP2pFileFailed(
								msgb.getFileHash(), msgb.getFailedCode(),
								msgb.getFailedPrompt());
					}
				}

				break;

			case PLAY_P2PFILE_FAILED:
				DebugLog.e(TAG, "接受到播放p2p失败指令");
				if (mWeakPlayer == null || mWeakPlayer.get() == null) {
					break;
				}
				Object obj2 = msg.obj;
				if (obj2 instanceof MessageBody) {
					MessageBody msgb = (MessageBody) obj2;
					mWeakPlayer.get().upDataPorgressShow("播放失败啦");
					if (mLmP2pMediaListener != null) {
						mLmP2pMediaListener.onPlayP2pFileFailed(
								msgb.getFileHash(), msgb.getFailedCode(),
								msgb.getFailedPrompt());
					}
				}

				break;

			case PLAY_P2PFILE_BUFFERING:
				DebugLog.e(TAG, "接受到缓存指令");
				if (mWeakPlayer == null || mWeakPlayer.get() == null) {
					break;
				}
				Object obj3 = msg.obj;
				if (obj3 instanceof MessageBody) {
					MessageBody msgb = (MessageBody) obj3;
					mWeakPlayer.get().upDataPorgressShow(
							"正在缓冲 （"
									+ (msgb.getPercent() < 0 ? 0 : msgb
											.getPercent()) + "%）");
					if (mLmP2pMediaListener != null) {
						mLmP2pMediaListener.onPlayP2pBuffering(
								msgb.getFileHash(), msgb.getPercent());
					}
				}

				break;
			case FLAG_SHOW_PROGRESS:
				if (mTextView != null) {
					mTextView.setVisibility(View.INVISIBLE);
					mTextView.setText("正在缓冲 （ 0 %）");
				}
				break;
			}
		}
	}

	/**
	 * 设置hash
	 * 
	 * @param hash
	 * @param ijkVideoView
	 */
	public void setLMPlayHash(String hash, IjkVideoView ijkVideoView) {
		if (TextUtils.isEmpty(hash)) {
			throw new RuntimeException("path not null");
		}
		if (ijkVideoView == null) {
			throw new RuntimeException("ijkVideoView not null");
		}
		mPlayerHandler = new Lmp2pMediaPlayerHandler(this, Looper.myLooper());
		LMhash = hash;
		DebugLog.e(TAG, "hash=========" + hash);
		_setP2pLogLevel("/sdcard/limao.txt", Lmp2pMediaPlayer.LOG_VERBOSE);
		mVideoView = ijkVideoView;
		initialize();
		_prepareToPlayP2pFile(hash, "mp4", 0, "123");

	}

	/**
	 * 设置缓存进度显示
	 * 
	 * @param textView
	 */
	private static TextView mTextView;

	public void setPorgressShow(TextView textView) {
		mTextView = textView;
	}

	private void upDataPorgressShow(String content) {
		if (mTextView != null && !TextUtils.isEmpty(content)) {
			if (!mTextView.isShown()) {
				mPlayerHandler.removeMessages(FLAG_SHOW_PROGRESS);
				mPlayerHandler
						.sendEmptyMessageDelayed(FLAG_SHOW_PROGRESS, 2000);
			} else {
				mTextView.setVisibility(View.VISIBLE);
			}
			mTextView.setText(content);

		}
	}

	/**
	 * 返回
	 * 
	 * @return
	 */
	public static String getLMHash() {
		return LMhash;
	}

	private static boolean isStartPlay = false;

	/**
	 * 
	 * @param filePath
	 */
	private void prepareToPlay(String filePath, String hash) {
		DebugLog.e(TAG, "------播放行为------");
		if (isStartPlay) {
			DebugLog.e(TAG, "------开始播放------");
			return;
		}
		if (TextUtils.isEmpty(hash) || !hash.equals(getLMHash())) {

			DebugLog.e(TAG, "--------播放hash不正确------");
			DebugLog.e(TAG, "------------》底层hash：" + hash);
			DebugLog.e(TAG, "------------》上层hash：" + getLMHash());
			return;
		}
		if (TextUtils.isEmpty(filePath)) {
			DebugLog.e(TAG, "播放路径为空");
			return;
		}

		isStartPlay = true;
		if (mVideoView != null) {
			DebugLog.e(TAG, "filePath :" + filePath + "开始播放");
			mVideoView.setVideoPath(filePath);
			mVideoView.start();
			mPlayerHandler.removeMessages(PLAY_P2PFILE_BUFFERING);

		}
		// if (mTextView != null) {
		// mTextView.setVisibility(View.INVISIBLE);
		// }
	}

	public Lmp2pMediaPlayer() {

	}

	private void initialize() {
		isStartPlay = false;
		isStopP2pDownload = false;
		_native_init();
	}

	private boolean isStopP2pDownload = false;

	public void finalize(String LmHash) {
		if (!TextUtils.isEmpty(LMhash) && !isStopP2pDownload) {
			DebugLog.e(TAG, "Lmp2pMediaPlayer停止下载文件 hash: " + LmHash);
			_stopP2pDownload(LmHash);
			isStopP2pDownload = true;
			DebugLog.e(TAG, "停止播放");
			LMhash = null;
		}
		_native_finalize();
		if (mTextView != null) {
			mTextView = null;
		}
		if (mVideoView != null) {
			mVideoView = null;
		}
		mPlayerHandler.removeCallbacksAndMessages(null);

		// mHandler = null;
	}

	public void SetLmP2pMediaListener(OnLmp2pMediaListener listener) {
		this.mLmP2pMediaListener = listener;
	}

	// P2P error code

	// const int P2P_ERRNO_SUCCESS = 0; //operation ok 成功
	// const int P2P_ERRNO_ERROR = -1; //general error 一般错误
	// const int P2P_ERRNO_INVALID_PARAM = -101; //pointer is null, etc 无效参数
	// const int P2P_ERRNO_INVALID_URI = -102; //wrong hash format 无效哈希
	// const int P2P_ERRNO_CONNECT_FAIL = -103; //socket connect fail 连接失败
	// const int P2P_ERRNO_SOCKET_ERROR = -104; //socket exception 套接字错误
	// const int P2P_ERRNO_SOCKET_TIMEOUT = -105; //socket send, recv timeout
	// 链接超时
	// const int P2P_ERRNO_FILE_NOT_FOUND = -106;文件没找到
	// const int P2P_ERRNO_DOWNLOAD_TIMEOUT = -107; //waitfinish timeout 下载超时
	// const int P2P_ERRNO_MEMORY_SHORT = -108; //disk memory is not enough
	// /内存不足

	@CalledByNative
	static void onPrepareToPlayP2pFile(String fileHash, String filePath,
			long fileSize) {
		DebugLog.e(TAG, "onPrepareToPlayP2pFile 播放指令--------->fileHash:"
				+ filePath + " -------fileSize:" + fileSize);

		Message msg = Message.obtain();
		msg.what = PREPARE_TO_PLAYP2P_FILE;
		MessageBody messageBody = new MessageBody();
		messageBody.setFileHash(fileHash);
		messageBody.setFilePath(filePath);
		messageBody.setFileSize(fileSize);
		msg.obj = messageBody;
		mPlayerHandler.removeMessages(PREPARE_TO_PLAYP2P_FILE);
		mPlayerHandler.sendMessage(msg);
		DebugLog.e(TAG, "发送播放指令");

	}

	@CalledByNative
	static void onDownloadP2pFileFailed(String fileHash, int failedCode,
			String failedPrompt) {
		DebugLog.e(TAG, "p2p下载失败------>fileHash:" + fileHash + "failedCode:"
				+ failedCode);
		Message msg = Message.obtain();
		msg.what = DOWNLOAD_P2PFILE_FAILED;
		MessageBody messageBody = new MessageBody();
		messageBody.setFileHash(fileHash);
		messageBody.setFailedCode(failedCode);
		messageBody.setFailedPrompt(failedPrompt);
		msg.obj = messageBody;

		mPlayerHandler.sendMessage(msg);

	}

	@CalledByNative
	static void onPlayP2pFileFailed(String fileHash, int failedCode,
			String failedPrompt) {
		DebugLog.e(TAG, "播放失败------->fileHash:" + fileHash + "failedCode:"
				+ failedCode);
		Message msg = Message.obtain();
		msg.what = PLAY_P2PFILE_FAILED;
		MessageBody body = new MessageBody();
		body.setFileHash(fileHash);
		body.setFailedCode(failedCode);
		body.setFailedPrompt(failedPrompt);
		msg.obj = body;
		mPlayerHandler.sendMessage(msg);

	}

	@CalledByNative
	static void onPlayP2pFileBuffering(String fileHash, int percent) {
		DebugLog.e(TAG, "onPlayP2pFileBuffering----->fileHash :" + fileHash
				+ "----percent:" + percent);
		Message msg = Message.obtain();
		msg.what = PLAY_P2PFILE_BUFFERING;
		MessageBody body = new MessageBody();
		body.setFileHash(fileHash);
		body.setPercent(percent);
		msg.obj = body;
		mPlayerHandler.sendMessage(msg);

	}

	private static native void _native_init();

	private static native void _native_finalize();

	public static native void _prepareToPlayP2pFile(String fileHash,
			String filenameExtension, long fileSize, String reserved);

	public static native int _getP2pDownloadSpeed(String fileHash);

	public static native void _stopP2pDownload(String fileHash);

	public static native void _setP2pLogLevel(String logFilePath, int level);
}
