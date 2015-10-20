package fr.ac_versailles.crdp.apiscol.previews.fileSystem;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;

import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class FileSystemAccess {

	private static Logger logger = null;

	private static String inputDirectory;

	private static String outputDirectory;

	private static Object maintenanceExclusion = new Object();

	private static Tika tika = new Tika();

	private static int cleaningDelay;

	public static void initialize(String filesDirectory,
			String outputDirectory, int cleaningDelay) {
		FileSystemAccess.inputDirectory = filesDirectory;
		FileSystemAccess.outputDirectory = outputDirectory;
		FileSystemAccess.cleaningDelay = cleaningDelay;
		initializeLogger();
		logger.info("The directory to store incoming files is "
				+ filesDirectory);
		logger.info("The directory for output is " + filesDirectory);
	}

	private static void initializeLogger() {
		if (logger == null) {
			logger = LogUtility.createLogger(FileSystemAccess.class
					.getCanonicalName());
		}

	}

	public static boolean isInitialized() {
		return inputDirectory != null;
	}

	public static void dumpIncomingFile(UUID newJobId,
			InputStream uploadedInputStream, String fileName)
			throws IOException {

		File incomingFile = getIncomingFile(newJobId, fileName);
		FileUtils.writeStreamToFile(uploadedInputStream, incomingFile);

	}

	public static String getMimeType(UUID jobId, String fileName)
			throws IOException {
		File incoming = getIncomingFile(jobId, fileName);

		return tika.detect(incoming);
	}

	public static File getIncomingFile(UUID jobId, String fileName) {
		String path = String.format("%s/%s/%s", inputDirectory,
				jobId.toString(), fileName);
		return new File(path);
	}

	public static File getOutputDir(UUID jobId) {
		String path = String.format("%s/%s", outputDirectory, jobId.toString());
		File file = new File(path);
		file.mkdir();
		return file;
	}

	public static Map<Integer, String> getFilesByMimeTypeForConversion(
			UUID jobId, final String mimetype) {
		File outputDir = getOutputDir(jobId);
		String extension = ExtensionsHandling.getExtension(mimetype);
		if (StringUtils.isEmpty(extension))
			return Collections.emptyMap();
		File[] files = outputDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				String extension = ExtensionsHandling.getExtension(mimetype);
				if (StringUtils.isEmpty(extension))
					return false;
				return name.toLowerCase().endsWith(extension);
			}
		});
		Map<Integer, String> fileList = new HashMap<Integer, String>();
		String fileNamePattern = ".+[^\\d](\\d*)";
		Pattern p = Pattern.compile("^" + fileNamePattern + "\\." + extension
				+ "$");

		for (int i = 0; i < files.length; i++) {
			String name = files[i].getName();
			Matcher m = p.matcher(name);

			if (m.find()) {
				int num = 0;
				try {
					num = Integer.parseInt(m.group(1));
				} catch (NumberFormatException e) {
					num = 0;
				}
				fileList.put(num, name);
			}
		}
		return fileList;
	}

	public static void cleanOldDirectories() {
		synchronized (maintenanceExclusion) {
			System.out
					.println("Preview web service is going to clean the old directories");
			if (!isInitialized()) {
				System.out
						.println("Preview web service is not yet initialized. Directory cleaning canceled.");
				return;
			}
			File outputDir = new File(outputDirectory);
			if (!outputDir.exists()) {
				System.out
						.println("Output directory does not exist. Directory cleaning canceled.");
				return;
			} else {
				cleanDirectory(outputDir);
			}
			if (Thread.interrupted())
				return;
			File inputDir = new File(inputDirectory);
			if (!inputDir.exists()) {
				System.out
						.println("Input directory does not exist. Directory cleaning canceled.");
				return;
			} else {
				cleanDirectory(inputDir);
			}

		}

	}

	private static void cleanDirectory(File outputDir) {
		System.out.println("Starting to clean old directories in "
				+ outputDir.getAbsolutePath());
		File[] files = outputDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File listedDir = files[i];
			long age = System.currentTimeMillis() - listedDir.lastModified();
			boolean destruction = age > cleaningDelay * 3600 * 1000;
			System.out.println(listedDir.getAbsolutePath()
					+ " is old "
					+ age
					/ (1000 * 60)
					+ " minutes"
					+ (destruction ? " it will be destroyed"
							: " it will not be destroyed"));
			if (destruction) {
				FileUtils.deleteDir(listedDir);

			}
			if (Thread.interrupted())
				return;
		}

	}
}
