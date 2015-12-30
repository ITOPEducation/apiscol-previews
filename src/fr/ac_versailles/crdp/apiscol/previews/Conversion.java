package fr.ac_versailles.crdp.apiscol.previews;

import java.util.List;
import java.util.UUID;

import fr.ac_versailles.crdp.apiscol.previews.workers.IConversionWorker;

public class Conversion {
	private final UUID jobId;
	private States state;
	private String message;
	private final String fileName;
	private final List<String> outputMimeTypeList;
	private IConversionWorker worker;

	public Conversion(UUID newJobId, String fileName,
			List<String> outputMimeTypeList) {
		this.jobId = newJobId;
		this.fileName = fileName;
		this.outputMimeTypeList = outputMimeTypeList;
		this.message="";
		
	}

	public enum States {
		ABORTED("aborted"), INITIATED("initiated"), SUCCESS("success"), FILE_RECEIVED(
				"file received"), FILE_SCANNED("file scanned"), RUNNING(
				"running");
		private String value;

		private States(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

	}

	public void setState(States state, String message) {
		this.state = state;
		this.message += message + "\n";
	}

	public UUID getJobId() {
		return jobId;
	}

	public String getFileName() {
		return fileName;
	}

	public void attachWorker(IConversionWorker worker) {
		this.worker = worker;
	}

	public String getMessage() {
		return message;
	}

	public States getState() {
		return state;
	}

	public List<String> getOutputMimeTypeList() {
		return outputMimeTypeList;
	}

}
