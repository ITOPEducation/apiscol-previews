package fr.ac_versailles.crdp.apiscol.previews;

import fr.ac_versailles.crdp.apiscol.ApiscolException;


public class BarOrMissingParametersException extends ApiscolException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BarOrMissingParametersException(String resourceId) {
		super(String.format(
				"%s is not the valid syntax for apiscol content identifiers .",
				resourceId));
	}

}
