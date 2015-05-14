package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.ac_versailles.crdp.apiscol.previews.Conversion;

public class PDFConversionWorker extends AbstractConversionWorker {


	public PDFConversionWorker(File incomingFile, File outputDir,
			List<String> outputMimeTypeList, int pageLimit,
			Conversion conversion) {
		super(incomingFile, outputDir, outputMimeTypeList, pageLimit, conversion);

	}

	@Override
	public void run() {
		convertPdfToImage();
	}

	protected void convertPdfToImage() {
		conversion.setState(Conversion.States.INITIATED,
				"Conversion process has been launched.");
		for (int i = 0; i < askedMimesTypes.size(); i++) {
			String askedMimeType = askedMimesTypes.get(i);
			boolean success = convertToMimeType(askedMimeType);
			if (success)
				conversion.setState(Conversion.States.RUNNING,
						"Conversion has been performed to mimeType : "
								+ askedMimeType);
		}
		conversion.setState(Conversion.States.SUCCESS,
				"Conversion process terminated.");

	}

	private boolean convertToMimeType(String askedMimeType) {
		try {
			String device = getGsDevice(askedMimeType);
			String extension = getOutputFileExtension(askedMimeType);
			String[] commande = { "gswin64", "-dNOPAUSE", "-dBATCH",
					"-sDEVICE=" + device, "-r96",
					"-sOutputFile=page%00d." + extension,
					"-dLastPage=" + pageLimit, incomingFile.getAbsolutePath() };
			String[] envp = {};
			Process p = Runtime.getRuntime().exec(commande, envp, outputDir);
			BufferedReader output = getOutput(p);
			BufferedReader error = getError(p);
			String ligne = "";
			while ((ligne = output.readLine()) != null) {
				System.out.println("err. " + ligne);
			}

			while ((ligne = error.readLine()) != null) {
				System.out.println(ligne);
			}

			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	private String getGsDevice(String askedMimeType) {
		if (askedMimeType.contains("jpeg"))
			return "jpeg";
		if (askedMimeType.contains("tiff"))
			return "tiff24nc";
		return "png16m";
	}

}
