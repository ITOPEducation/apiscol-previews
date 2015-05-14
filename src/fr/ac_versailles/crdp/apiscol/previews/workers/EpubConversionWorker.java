package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import fr.ac_versailles.crdp.apiscol.previews.Conversion;

public class EpubConversionWorker extends PDFConversionWorker {

	public EpubConversionWorker(File incomingFile, File outputDir,
			List<String> outputMimeTypeList, int pageLimit,
			Conversion conversion) {
		super(incomingFile, outputDir, outputMimeTypeList, pageLimit,
				conversion);
	}

	@Override
	public void run() {
		conversion.setState(Conversion.States.INITIATED,
				"Conversion process from epub to pdf has been launched.");
		File tempFile = new File(incomingFile.getParentFile().getAbsolutePath()
				+ "/" + UUID.randomUUID().toString() + ".epub");

		incomingFile.renameTo(tempFile);
		System.out.println("**************renamed to "
				+ tempFile.getAbsolutePath());
		incomingFile = tempFile;
		boolean success = convertToPdf();
		String inputDir = this.incomingFile.getParentFile().getParentFile()
				.getAbsolutePath();

		String newName = inputDir
				+ "/"
				+ incomingFile.getName().substring(0,
						incomingFile.getName().lastIndexOf('.')) + ".pdf";
		System.out.println("This is the new name " + newName);

		this.incomingFile = new File(newName);
		success &= this.incomingFile.exists();
		if (success) {
			conversion.setState(Conversion.States.RUNNING,
					"Conversion has been performed to pdf");

			convertPdfToImage();
		} else {
			conversion.setState(Conversion.States.ABORTED,
					"Attemp to convert file " + this.incomingFile.getName()
							+ " was a failure");
			return;
		}
		conversion.setState(Conversion.States.SUCCESS,
				"Conversion process terminated.");
	}

	private boolean convertToPdf() {

		// try {
		// String[] commande = { realPath + "/WEB-INF/epub2pdf/epub2pdf.sh",
		// incomingFile.getAbsolutePath() };
		// System.out.println(StringUtils.join(commande, " "));
		// String[] envp = { "HOME=/tmp" };
		// Process p = Runtime.getRuntime().exec(commande, envp,
		// new File(realPath + "/WEB-INF/epub2pdf"));
		// BufferedReader output = getOutput(p);
		// BufferedReader error = getError(p);
		// String ligne = "";
		// while ((ligne = output.readLine()) != null) {
		// System.out.println("err. " + ligne);
		// }
		//
		// while ((ligne = error.readLine()) != null) {
		// System.out.println(ligne);
		// }
		//
		// p.waitFor();
		// } catch (IOException e) {
		// e.printStackTrace();
		// return false;
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// return false;
		// }
		return true;

	}
}
