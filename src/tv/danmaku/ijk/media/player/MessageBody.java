package tv.danmaku.ijk.media.player;

public class MessageBody {
	private String fileHash;
	private String filePath;
	private long fileSize;
	private int failedCode;
	private String failedPrompt;
	private int percent;

	public String getFileHash() {
		return fileHash;
	}

	public void setFileHash(String fileHash) {
		this.fileHash = fileHash;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public int getFailedCode() {
		return failedCode;
	}

	public void setFailedCode(int failedCode) {
		this.failedCode = failedCode;
	}

	public String getFailedPrompt() {
		return failedPrompt;
	}

	public void setFailedPrompt(String failedPrompt) {
		this.failedPrompt = failedPrompt;
	}

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		this.percent = percent;
	}

}
