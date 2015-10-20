package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.ac_versailles.crdp.apiscol.previews.Conversion;
import fr.ac_versailles.crdp.apiscol.previews.resources.MapTokenResolver;
import fr.ac_versailles.crdp.apiscol.previews.resources.ResourcesLoader;
import fr.ac_versailles.crdp.apiscol.previews.resources.TokenReplacingReader;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;

public class WebPageConversionWorker extends AbstractConversionWorker {

	private String url;

	public WebPageConversionWorker(String url, File outputDir,
			List<String> outputMimeTypeList, int pageLimit,
			Conversion conversion) {
		super(null, outputDir, outputMimeTypeList, pageLimit, conversion);
		this.url = url;

	}

	@Override
	public void run() {

		convertUrlToImage();
	}

	protected void convertUrlToImage() {
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
			String extension = getOutputFileExtension(askedMimeType);
			if (extension.equals("unknown")) {
				conversion.setState(Conversion.States.ABORTED,
						"Impossible to find the required file extension for mime type : "
								+ askedMimeType);
				return false;
			}
			String scriptFilename = "phantom_js_script_" + UUID.randomUUID();
			File tempScriptFile = File.createTempFile(scriptFilename, ".js");

			String sriptTemplatePath = "scripts/phantomjs.js";
			InputStream is = null;
			is = ResourcesLoader.loadResource(sriptTemplatePath);

			if (is == null) {
				conversion.setState(Conversion.States.ABORTED,
						"Impossible to load the preview template : "
								+ sriptTemplatePath);
				return false;
			}
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("url", url);
			tokens.put("ext", extension);
			MapTokenResolver resolver = new MapTokenResolver(tokens);

			Reader source = new InputStreamReader(is);

			Reader reader = new TokenReplacingReader(source, resolver);

			FileUtils.writeDataToFile(reader, tempScriptFile.getAbsolutePath());
			String[] commande = { "phantomjs", tempScriptFile.getAbsolutePath() };
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

}
