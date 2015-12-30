package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.File;
import java.util.List;
import java.util.Map;

import fr.ac_versailles.crdp.apiscol.previews.Conversion;

public class PhantomJsWebPageConversionWorker extends
		AbstractWebPageConversionWorker {

	public PhantomJsWebPageConversionWorker(String url, File outputDir,
			List<String> outputMimeTypeList, int pageLimit,
			Conversion conversion, Map<String, String> conversionParameters) {
		super(url, outputDir, outputMimeTypeList, pageLimit, conversion,
				conversionParameters);

	}

	@Override
	protected String getScriptTemplatePath() {
		return "scripts/phantomjs.js";
	}

	@Override
	protected String[] getFileExecutionCommand(File tempScriptFile) {
		String[] command = { "phantomjs", "--ssl-protocol=any",
				"--local-to-remote-url-access=true", "--web-security=false",
				tempScriptFile.getAbsolutePath() };
		return command;
	}

}
