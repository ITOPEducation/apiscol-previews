package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.previews.Conversion;
import fr.ac_versailles.crdp.apiscol.previews.resources.MapTokenResolver;
import fr.ac_versailles.crdp.apiscol.previews.resources.ResourcesLoader;
import fr.ac_versailles.crdp.apiscol.previews.resources.TokenReplacingReader;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;

public abstract class AbstractWebPageConversionWorker extends
		AbstractConversionWorker {

	private String url;
	private Map<String, String> conversionParameters;

	public AbstractWebPageConversionWorker(String url, File outputDir,
			List<String> outputMimeTypeList, int pageLimit,
			Conversion conversion, Map<String, String> conversionParameters) {
		super(null, outputDir, outputMimeTypeList, pageLimit, conversion);
		this.url = url;
		this.conversionParameters = conversionParameters;

	}

	@Override
	public void run() {

		convertUrlToImage();
	}

	protected abstract String getScriptTemplatePath();

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
			String scriptFilename = "web_snapshot_js_script_"
					+ UUID.randomUUID();
			File tempScriptFile = File.createTempFile(scriptFilename, ".js");

			String sriptTemplatePath = "scripts/slimerjs.js";
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
			tokens.put("timeout", conversionParameters
					.get(ParametersKeys.webSnapshotTimeout.toString()));
			tokens.put("viewport_width", conversionParameters
					.get(ParametersKeys.webSnapshotViewportWidth.toString()));
			tokens.put("viewport_height", conversionParameters
					.get(ParametersKeys.webSnapshotViewportHeight.toString()));
			MapTokenResolver resolver = new MapTokenResolver(tokens);

			Reader source = new InputStreamReader(is);

			Reader reader = new TokenReplacingReader(source, resolver);

			String absolutePath = tempScriptFile.getAbsolutePath();
			FileUtils.writeDataToFile(reader, absolutePath);

			String[] commande = getFileExecutionCommand(tempScriptFile);

			Process p = Runtime.getRuntime().exec(commande, null, outputDir);
			BufferedReader output = getOutput(p);
			BufferedReader error = getError(p);
			String ligne = "";
			while ((ligne = output.readLine()) != null) {
				System.out.println("[Web snapshot output] " + ligne);
			}

			while ((ligne = error.readLine()) != null) {
				System.out.println("[Web snapshot error] " + ligne);
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

	abstract protected String[] getFileExecutionCommand(File tempScriptFile);

}
