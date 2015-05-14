package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import fr.ac_versailles.crdp.apiscol.previews.Conversion;

public class ConvertersFactory {
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
		System.out.println(mimeType);
		System.out.println(outputMimeTypeList.get(0));
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

}
