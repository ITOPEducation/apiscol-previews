package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.previews.BarOrMissingParametersException;
import fr.ac_versailles.crdp.apiscol.previews.Conversion;

public class ConvertersFactory {
	private static boolean initialized = false;

	static String[] pdf = { "application/pdf" };
	static String[] officedocs = {
			"application/msword",
			"application/vnd.ms-excel",
			"application/vnd.ms-powerpoint",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"application/rtf" };
	static String[] images = { "image/tiff", "image/jpeg", "image/png" };
	static String[] videos = { "video/x-ms-wmv", "video/x-m4v", "video/flv",
			"video/x-flv", "video/ogg", "video/avi", "video/webm" };
	static String[] epub = { "application/epub+zip" };

	private static Map<String, String> conversionParameters;

	public enum MimeTypeGroups {
		PDF(pdf), OFFICE_DOCUMENTS(officedocs), IMAGES(images), VIDEOS(videos), EPUB(
				epub);

		private String[] types;

		private MimeTypeGroups(String[] types) {
			this.types = types;
		}

		public List<String> list() {
			return Arrays.asList(types);
		}

	}

	public static IConversionWorker getConversionWorker(String mimeType,
			List<String> outputMimeTypeList, File incomingFile, File OutputDir,
			int limit, Conversion conversion) {
		if (MimeTypeGroups.PDF.list().contains(mimeType))
			if (MimeTypeGroups.IMAGES.list().containsAll(outputMimeTypeList))
				return new PDFConversionWorker(incomingFile, OutputDir,
						outputMimeTypeList, limit, conversion);
		if (MimeTypeGroups.OFFICE_DOCUMENTS.list().contains(mimeType))
			if (MimeTypeGroups.IMAGES.list().containsAll(outputMimeTypeList))
				return new MsDocumentConversionWorker(incomingFile, OutputDir,
						outputMimeTypeList, limit, conversion);
		if (MimeTypeGroups.EPUB.list().contains(mimeType))
			if (MimeTypeGroups.IMAGES.list().containsAll(outputMimeTypeList))
				return new EpubConversionWorker(incomingFile, OutputDir,
						outputMimeTypeList, limit, conversion);
		if (MimeTypeGroups.VIDEOS.list().contains(mimeType))
			// if (MimeTypeGroups.VIDEOS.list().containsAll(outputMimeTypeList))
			return new VideoConversionWorker(incomingFile, OutputDir,
					outputMimeTypeList, limit, conversion);
		return null;
	}

	public static IConversionWorker getConversionWorkerForUrl(
			List<String> outputMimeTypeList, String url, File outputDir,
			int limit, Conversion conversion)
			throws BarOrMissingParametersException {
		if (MimeTypeGroups.IMAGES.list().containsAll(outputMimeTypeList)) {
			if (url.toLowerCase().endsWith(".pdf")) {
				return new RemotePDFConversionWorker(url, outputDir,
						outputMimeTypeList, limit, conversion);
			}
			String engine = conversionParameters
					.get(ParametersKeys.webSnapshotEngine.toString());
			if (StringUtils.isEmpty(engine)) {
				throw new BarOrMissingParametersException(
						"Please provide web snapshot engine name : phantomjs or slimerjs");
			}
			if (engine.equals("slimerjs")) {
				return new SlimerJsWebPageConversionWorker(url, outputDir,
						outputMimeTypeList, limit, conversion,
						conversionParameters);
			}
			if (engine.equals("phantomjs")) {
				return new PhantomJsWebPageConversionWorker(url, outputDir,
						outputMimeTypeList, limit, conversion,
						conversionParameters);
			}

			throw new BarOrMissingParametersException(
					"No web snapshot engine with name " + engine);

		}

		return null;
	}

	public static boolean isInitialized() {
		return initialized;
	}

	public static void initialize(Map<String, String> conversionParameters) {
		ConvertersFactory.conversionParameters = conversionParameters;
		initialized = true;

	}

}
