package fr.ac_versailles.crdp.apiscol.previews.workers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.ac_versailles.crdp.apiscol.previews.Conversion;
import fr.ac_versailles.crdp.apiscol.previews.utils.OsCheck;

public class SlimerJsWebPageConversionWorker extends
		AbstractWebPageConversionWorker {

	public SlimerJsWebPageConversionWorker(String url, File outputDir,
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
		OsCheck.OSType ostype = OsCheck.getOperatingSystemType();
		ArrayList<String> commandeArrayList = new ArrayList<String>();
		switch (ostype) {
		case Windows:
			commandeArrayList.add("mod_slimerjs.bat");
			break;

		case Linux:
			commandeArrayList.add("xvfb-run");
			commandeArrayList.add("slimerjs");
			break;
		case MacOS:
		default:
			conversion.setState(Conversion.States.ABORTED,
					"Slimer js support only implemented for windows and Linux");
			return null;

		}
		commandeArrayList.add("\"" + tempScriptFile.getAbsolutePath() + "\"");
		String[] commande = (String[]) commandeArrayList
				.toArray(new String[commandeArrayList.size()]);
		return commande;
	}
}
