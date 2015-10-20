package fr.ac_versailles.crdp.apiscol.previews.representations;

import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.ac_versailles.crdp.apiscol.UsedNamespaces;
import fr.ac_versailles.crdp.apiscol.previews.Conversion;
import fr.ac_versailles.crdp.apiscol.previews.Conversion.States;
import fr.ac_versailles.crdp.apiscol.previews.fileSystem.FileSystemAccess;
import fr.ac_versailles.crdp.apiscol.utils.XMLUtils;

public class XMLRepresentationBuilder extends
		AbstractRepresentationBuilder<Document> {

	private static Document createXMLDocument() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		docFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Document doc = docBuilder.newDocument();
		return doc;
	}

	@Override
	public Document getConversionRepresentation(Conversion conversion,
			UriInfo uriInfo) {
		Document report = createXMLDocument();

		Element rootElement = report.createElement("report");
		Element linkElement = report.createElement("atom:link");
		linkElement.setAttribute("href",
				getConversionUri(uriInfo, conversion.getJobId()));
		Element stateElement = report.createElement("state");
		stateElement.setTextContent(conversion.getState().toString());
		Element messageElement = report.createElement("message");
		messageElement.setTextContent(conversion.getMessage());
		rootElement.appendChild(linkElement);
		rootElement.appendChild(stateElement);
		rootElement.appendChild(messageElement);
		if (conversion.getState().equals(States.SUCCESS)
				|| conversion.getState().equals(States.RUNNING)) {
			Element outputElement = report.createElement("output");
			String mimetype;
			for (Iterator<String> iterator = conversion.getOutputMimeTypeList()
					.iterator(); iterator.hasNext();) {
				mimetype = iterator.next();
				Element formatElement = report.createElement("format");
				formatElement.setAttribute("mime-type", mimetype);
				Map<Integer, String> fileNames = FileSystemAccess
						.getFilesByMimeTypeForConversion(conversion.getJobId(),
								mimetype);
				for (Iterator<Integer> iterator2 = fileNames.keySet()
						.iterator(); iterator2.hasNext();) {
					Integer num = iterator2.next();
					Element fileElement = report.createElement("file");
					fileElement.setAttribute(
							"href",
							getOutputFileUri(uriInfo, conversion.getJobId(),
									fileNames.get(num)));
					formatElement.appendChild(fileElement);

				}
				outputElement.appendChild(formatElement);
			}
			rootElement.appendChild(outputElement);
		}
		report.appendChild(rootElement);
		XMLUtils.addNameSpaces(report, UsedNamespaces.APISCOL);
		return report;
	}

	@Override
	public Document getNotFoundMessage(String conversionId) {
		Document report = createXMLDocument();
		Element rootElement = report.createElement("report");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("not found");
		Element messageElement = report.createElement("message");
		stateElement
				.setTextContent("no conversion job was found for id "
						+ conversionId
						+ ". Either you made a mistake either it's an old job that has been erased.");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		XMLUtils.addNameSpaces(report, UsedNamespaces.APISCOL);
		return report;
	}

}
