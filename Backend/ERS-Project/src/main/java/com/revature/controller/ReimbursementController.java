package com.revature.controller;

import java.io.InputStream;
import java.util.List;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.revature.constant.EndpointConstants;
import com.revature.model.Reimbursement;
import com.revature.model.User;
import com.revature.service.AuthorizationService;
import com.revature.service.ReimbursementService;
import com.revature.utility.ValidateUtil;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;

public class ReimbursementController implements Controller {

	private Logger logger = LoggerFactory.getLogger(ReimbursementController.class);

	private ReimbursementService reimbService;
	private AuthorizationService authService;

	// Constructor
	public ReimbursementController() {
		this.reimbService = new ReimbursementService();
		this.authService = new AuthorizationService();
	}

	// getReimbursements
	private Handler getReimbursements = (ctx) -> {
		logger.info("ReimbursementController.getReimbursements() invoked");

		// guard the endpoint to roles permitted
		User currentlyLoggedInUser = (User) ctx.req.getSession().getAttribute("currentUser");
		this.authService.authorizeFinanceManagerAndEmployee(currentlyLoggedInUser);

		/*-
		 * if the authService.authorizeAssociateAndTrainer(...) method did not
		 * throw an exception, the program will continue to proceed to the below
		 * functionality
		 */
		List<Reimbursement> reimbursements = this.reimbService.getReimbursements(currentlyLoggedInUser);

		ctx.json(reimbursements);
	};

	// getReimbursementsByStatus
	private Handler getReimbursementsByStatus = (ctx) -> {
		logger.info("ReimbursementController.getReimbursementsByStatus() invoked");

		User currentlyLoggedInUser = (User) ctx.req.getSession().getAttribute("currentUser");
		this.authService.authorizeFinanceManagerAndEmployee(currentlyLoggedInUser);

		String status = ctx.pathParam("status");

		List<Reimbursement> reimbursements = this.reimbService.getReimbursementsByStatus(currentlyLoggedInUser, status);

		ctx.json(reimbursements);
	};

	// addReimbursements
	private Handler addReimbursements = (ctx) -> {
		logger.info("ReimbursementController.addReimbursements() invoked");

		User currentlyLoggedInUser = (User) ctx.req.getSession().getAttribute("currentUser");
		this.authService.authorizeFinanceManagerAndEmployee(currentlyLoggedInUser);

		Double amount = Double.parseDouble(ctx.formParam("amount"));
		
		String type = ctx.formParam("type");
		String description = ctx.formParam("description");

		// Extracting file from HTTP Request
		UploadedFile file = ctx.uploadedFile("receipt");
		
		// call the class without instantiate it because the method is 'static'
		ValidateUtil.verifyAddReimb(amount, type, description,  file);

		// set the actual content of the file to 'receipt'
		InputStream receipt = file.getContent();
		
		// Apache Tika is a content detection and analysis framwork
		Tika tika = new Tika();

		// file type of the content is detect by tika and store as 'mimeType'
		// mimeType = Multipurpose Internet Mail Extensions
		String mimeType = tika.detect(receipt);

		ValidateUtil.verifyAndSetInputSpecificity(amount, type,
				description, mimeType, receipt);
		
		Reimbursement addReimbursement = this.reimbService.addReimbursement(currentlyLoggedInUser, amount, type,
				description, mimeType, receipt);

		ctx.json(addReimbursement);
		ctx.status(201);
	};

//	// getRecieptByReimbursementId
//	private Handler getRecieptByReimbursementId = (ctx) -> {
//		logger.info("ReimbursementController.getRecieptByReimbursementId() invoked");
//
//		User currentlyLoggedInUser = (User) ctx.req.getSession().getAttribute("currentUser");
//		this.authService.authorizeFinanceManagerAndEmployee(currentlyLoggedInUser);
//
//		String reimbursementId = ctx.pathParam("reimbId");
//
//		this.reimbService.verifyReimbursement(reimbursementId);
//
//		InputStream image = this.reimbService.getRecieptByReimbursementId(currentlyLoggedInUser, reimbursementId);
//
//		// tika detect and analyze the content
//		Tika tika = new Tika();
//		String mimeType = tika.detect(image);
//
//		// specifying the type of content to the client
//		ctx.contentType(mimeType);
//
//		// sending image back to the client
//		ctx.result(image);
//	};
//
//	// updateReimbursements
//	private Handler updateReimbursements = (ctx) -> {
//		logger.info("ReimbursementController.updateReimburseHandler() invoked");
//
//		User currentlyLoggedInUser = (User) ctx.req.getSession().getAttribute("currentUser");
//		this.authService.authorizeFinanceManager(currentlyLoggedInUser);
//
//		String reimbursementId = ctx.pathParam("reimbId");
//
//		this.reimbService.verifyReimbursement(reimbursementId);
//
//		// take the request body and putting the data into a new object
//		UpdateReimbursementDTO dto = ctx.bodyAsClass(UpdateReimbursementDTO.class);
//
//		Reimbursement updateReimbursement = this.reimbService.updateReimbursement(currentlyLoggedInUser,
//				reimbursementId, dto.getStatus());
//		ctx.json(updateReimbursement);
//	};

	@Override
	public void mapEndpoints(Javalin app) {
		app.get(EndpointConstants.GET_REIMBURSEMENTS, getReimbursements);
		app.get(EndpointConstants.GET_REIMBURSEMENTS_BYSTATUS, getReimbursementsByStatus);
		app.post(EndpointConstants.POST_REIMBURSEMENTS, addReimbursements);
//		app.get(EndpointConstants.GET_RECEIPT, getRecieptByReimbursementId);
//		app.patch(EndpointConstants.PATCH_REIMBURSEMENTS, updateReimbursements);
	}

}