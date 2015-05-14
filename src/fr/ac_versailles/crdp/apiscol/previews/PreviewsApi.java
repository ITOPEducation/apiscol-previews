package fr.ac_versailles.crdp.apiscol.previews;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import fr.ac_versailles.crdp.apiscol.ApiscolApi;
import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.ResourcesKeySyntax;
import fr.ac_versailles.crdp.apiscol.previews.fileSystem.DirectoryCleaner;
import fr.ac_versailles.crdp.apiscol.previews.fileSystem.FileSystemAccess;
import fr.ac_versailles.crdp.apiscol.previews.representations.EntitiesRepresentationBuilderFactory;
import fr.ac_versailles.crdp.apiscol.previews.representations.IEntitiesRepresentationBuilder;
import fr.ac_versailles.crdp.apiscol.previews.workers.ConvertersFactory;
import fr.ac_versailles.crdp.apiscol.previews.workers.IConversionWorker;

/**
 * 
 * @author Joachim Dornbusch This API is a low-level utility aimed to generate
 *         web-compliant format of files for preview in browsers. At this time
 *         it offers conversion services for PDF, video and current
 * 
 */
@Path("/")
public class PreviewsApi extends ApiscolApi {

	private static ExecutorService conversionExecutor;
	private static boolean isInitialized = false;
	private static int absolutePageLimit;
	private static ScheduledExecutorService sheduler;
	private int cleaningDelay;

	public PreviewsApi(@Context ServletContext context) {
		super(context);
		if (!isInitialized) {
			initializeResourceDirectoryInterface(context);
			createConversionExecutor();
			readPageLimit(context);
			sheduleDirectoryCleaning(cleaningDelay);  
			isInitialized = true;
		}
	}

	private void sheduleDirectoryCleaning(int delay) {
		if (sheduler != null)
			return;
		sheduler = Executors.newScheduledThreadPool(1);
		DirectoryCleaner cleaner = new DirectoryCleaner();
		sheduler.scheduleAtFixedRate(cleaner, delay * 3600, delay * 3600,
				TimeUnit.SECONDS); 
	}

	private void readPageLimit(ServletContext context) {
		absolutePageLimit = Integer.parseInt(getProperty(
				ParametersKeys.absolutePageLimit, context));
	}

	private void initializeResourceDirectoryInterface(ServletContext context) {
		if (!FileSystemAccess.isInitialized()) {
			String inputDirectory = getProperty(ParametersKeys.fileRepoPath,
					context);
			String outputDirectory = getProperty(
					ParametersKeys.previewsRepoPath, context);
			cleaningDelay = Integer.parseInt(getProperty(
					ParametersKeys.cleaningDelay, context));
			FileSystemAccess.initialize(inputDirectory, outputDirectory,
					cleaningDelay);
		}

	}

	private void createConversionExecutor() {
		if (conversionExecutor == null)
			conversionExecutor = Executors.newFixedThreadPool(5);
	}

	@GET
	@Path("/conversion/{convid}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getConversionState(@Context HttpServletRequest request,
			@PathParam(value = "convid") final String conversionId,
			@Context ServletContext context, @Context UriInfo uriInfo,
			@QueryParam(value = "format") final String format)
			throws IncorrectResourceKeySyntaxException {
		String requestedFormat = guessRequestedFormat(request, format);
		checkResidSyntax(conversionId);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		Conversion conversion = JobsKeeper.getConversion(UUID
				.fromString(conversionId));
		if (conversion == null)
			return Response.status(Status.NOT_FOUND)
					.entity(rb.getNotFoundMessage(conversionId)).build();
		return Response.ok(rb.getConversionRepresentation(conversion, uriInfo))
				.build();
	}

	@POST
	@Path("/conversion")
	@Produces({ MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML })
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createResource(@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@FormDataParam("output-mime-types") String outputMimeTypes,
			@FormDataParam("fname") String fileName,
			@FormDataParam(value = "format") final String format,
			@DefaultValue("10") @FormDataParam("page-limit") int limit) {
		logger.info("Conversion demandée du fichier " + fileName
				+ " vers le(s) format(s) " + outputMimeTypes);
		String requestedFormat = guessRequestedFormat(request, format);
		java.lang.reflect.Type collectionType = new TypeToken<List<String>>() {
		}.getType();
		List<String> outputMimeTypeList = null;
		try {
			outputMimeTypeList = new Gson().fromJson(outputMimeTypes,
					collectionType);
		} catch (Exception e) {
			String message = String.format(
					"The list of mimetypes %s is impossible to parse as JSON",
					outputMimeTypes);
			logger.warn(message);
			outputMimeTypeList = new ArrayList<String>();
		}
		UUID newJobId = UUID.randomUUID();
		Conversion conversion = new Conversion(newJobId, fileName,
				outputMimeTypeList);
		JobsKeeper.register(conversion);
		try {
			FileSystemAccess.dumpIncomingFile(newJobId, uploadedInputStream,
					fileName);
		} catch (IOException e) {
			e.printStackTrace();
			String message = "An error occured during reception of file "
					+ fileName + " for job id " + newJobId.toString() + ".";
			logger.error(message);
			conversion.setState(Conversion.States.ABORTED, message);
		}
		try {
			uploadedInputStream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			String message = "An error occured during reception of file "
					+ fileName + " for job id " + newJobId.toString()
					+ " : impossible to close the stream with message "
					+ e1.getMessage();
			logger.error(message);
		}
		conversion
				.setState(
						Conversion.States.FILE_RECEIVED,
						"File "
								+ fileName
								+ " has been received and is going to be submitted for conversion.");
		String mimeType = "";
		try {
			mimeType = FileSystemAccess.getMimeType(conversion.getJobId(),
					conversion.getFileName());
		} catch (IOException e) {
			e.printStackTrace();
			String message = "An error occured while scanning the file "
					+ fileName + " for conversion " + newJobId.toString() + ".";
			logger.error(message);
			conversion.setState(Conversion.States.ABORTED, message);
		}
		String message = "File " + fileName
				+ " has been succesfully scanned and mime type discovered : "
				+ mimeType;
		conversion.setState(Conversion.States.FILE_SCANNED, message);
		System.out.println(message);
		IConversionWorker worker = ConvertersFactory.getConversionWorker(
				mimeType, outputMimeTypeList,
				FileSystemAccess.getIncomingFile(newJobId, fileName),
				FileSystemAccess.getOutputDir(newJobId),
				Math.min(limit, absolutePageLimit), conversion);
		if (worker == null) {
			String message2 = "Mimetype of the file : "
					+ fileName
					+ " is "
					+ mimeType
					+ " and is not handled by the service or the ouput mime-types are not handled, check documentation.";
			logger.error(message2);
			conversion.setState(Conversion.States.ABORTED, message2);
		} else {
			conversion.attachWorker(worker);
			conversionExecutor.execute(worker);
		}

		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context);
		return Response.ok(rb.getConversionRepresentation(conversion, uriInfo))
				.build();

	}

	private void checkResidSyntax(String resourceId)
			throws IncorrectResourceKeySyntaxException {
		if (!ResourcesKeySyntax.resourceIdIsCorrect(resourceId))
			throw new IncorrectResourceKeySyntaxException(resourceId);
	}

	public static void stopCleaner() {
		if (sheduler != null)
			sheduler.shutdownNow();

	}

	public static void stopExecutors() {
		if (logger != null)
			logger.info("Thread executors are going to be stopped for Apiscol Edit Synchronisation Service.");
		if (conversionExecutor != null)
			conversionExecutor.shutdown();

	}

}
