package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import fr.ac_versailles.crdp.apiscol.previews.Conversion;

public class MsDocumentConversionWorker extends PDFConversionWorker {

	public MsDocumentConversionWorker(File incomingFile, File outputDir,
			List<String> outputMimeTypeList, int pageLimit,
			Conversion conversion) {
		super(incomingFile, outputDir, outputMimeTypeList, pageLimit,
				conversion);
	}

	@Override
	public void run() {
		conversion.setState(Conversion.States.INITIATED,
				"Conversion process from msdocument to pdf has been launched.");
		boolean success = convertToPdf();
		String absolutePath = this.incomingFile.getAbsolutePath();
		String newName = absolutePath.substring(0,
				absolutePath.lastIndexOf('.'))
				+ ".pdf";

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

		try {
			String[] commande = { "soffice", "--headless", "--convert-to",
					"pdf", "--nofirststartwizard", "--outdir", ".",
					"./" + incomingFile.getName() };
			System.out.println(StringUtils.join(commande, " "));
			String[] envp = { "HOME=/tmp" };
			Process p = Runtime.getRuntime().exec(commande, envp,
					incomingFile.getParentFile());
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
}
