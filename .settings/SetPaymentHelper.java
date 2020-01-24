package com.anthem.ols.middletier.paymentservice.helper;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.anthem.ols.middletier.paymentservice.config.ApplicationProperties;
import com.anthem.ols.middletier.paymentservice.config.PaymentIntegrationGateway;
import com.anthem.ols.middletier.paymentservice.db2.dao.impl.MAPSPaymentDAOImpl;
import com.anthem.ols.middletier.paymentservice.entity.FundAccountOwnerFullAddress;
import com.anthem.ols.middletier.paymentservice.entity.PayerId;
import com.anthem.ols.middletier.paymentservice.entity.PaymentDetails;
import com.anthem.ols.middletier.paymentservice.entity.PaymentMethod;
import com.anthem.ols.middletier.paymentservice.entity.Transaction;
import com.anthem.ols.middletier.paymentservice.exception.BusinessException;
import com.anthem.ols.middletier.paymentservice.hcentive.Code;
import com.anthem.ols.middletier.paymentservice.hcentive.GetComputedMRARequest;
import com.anthem.ols.middletier.paymentservice.hcentive.GetComputedMRAResponse;
import com.anthem.ols.middletier.paymentservice.hcentive.StateCode;
import com.anthem.ols.middletier.paymentservice.repository.ipay.PaymentDetailsRepository;
import com.anthem.ols.middletier.paymentservice.request.GetTokenRequest;
import com.anthem.ols.middletier.paymentservice.response.GetTokenResponse;
import com.anthem.ols.middletier.paymentservice.rest.bo.AccessControl;
import com.anthem.ols.middletier.paymentservice.rest.bo.Address;
import com.anthem.ols.middletier.paymentservice.rest.bo.Applicant;
import com.anthem.ols.middletier.paymentservice.rest.bo.Application;
import com.anthem.ols.middletier.paymentservice.rest.bo.ApplicationStatus;
import com.anthem.ols.middletier.paymentservice.rest.bo.ApplicationStatusEnum;
import com.anthem.ols.middletier.paymentservice.rest.bo.ApplicationTypeEnum;
import com.anthem.ols.middletier.paymentservice.rest.bo.BankAccount;
import com.anthem.ols.middletier.paymentservice.rest.bo.CreditCard;
import com.anthem.ols.middletier.paymentservice.rest.bo.Payment;
import com.anthem.ols.middletier.paymentservice.rest.bo.PaymentSelection;
import com.anthem.ols.middletier.paymentservice.rest.bo.PaymentTypeEnum;
import com.anthem.ols.middletier.paymentservice.rest.bo.Plan;
import com.anthem.ols.middletier.paymentservice.rest.bo.PlanRider;
import com.anthem.ols.middletier.paymentservice.rest.bo.PlanSelection;
import com.anthem.ols.middletier.paymentservice.rest.bo.ProductTypeEnum;
import com.anthem.ols.middletier.paymentservice.rest.bo.RelationshipTypeEnum;
import com.anthem.ols.middletier.paymentservice.rest.bo.Shopper;
import com.anthem.ols.middletier.paymentservice.rest.bo.ValidationErrors;
import com.anthem.ols.middletier.paymentservice.rest.request.SetApplicationRequestRS;
import com.anthem.ols.middletier.paymentservice.rest.request.SetPaymentRequestRS;
import com.anthem.ols.middletier.paymentservice.rest.response.SetApplicationResponseRS;
import com.anthem.ols.middletier.paymentservice.rest.response.SetPaymentResponseRS;
import com.anthem.ols.middletier.paymentservice.utils.PayModRestUtils;
import com.anthem.ols.middletier.paymentservice.utils.ServiceUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SetPaymentHelper {

	@Autowired
	private ServiceUtils serviceUtils;
	
	@Autowired
	private PaymentDetailsRepository paymentDetailsRepository;
	
	@Autowired
	private SetApplicationHelper setApplicationHelper;
	
	@Autowired
	private MAPSPaymentDAOImpl mapsPaymentDAOImpl;
	
	@Autowired
	private Mapper dozerMapper;
	
	@Autowired
	private PayModRestUtils payModRestUtils;
	
	@Autowired
	private MongoTemplate iPayDbTemplate;
	
	@Autowired
	private PaymentIntegrationGateway paymentIntegrationGateway;
	
	@Autowired
	private ApplicationProperties applicationProperties;
	
	public Object setPayment(Object objReq, boolean isForSetApplication) throws Exception {
		
		
		PlanSelection planSelection = null;
		PaymentMethod paymentMethod = null;
		//boolean isSplitRequired = false;
		
		int splitValue = 0;

		//ValidationErrors validationErrors = null;

		String paymentExceptionMsg = null;

		String acn = "";
		String userId = "";
		PaymentSelection paymentSelection = null;
		String partnerId = "";
		String applicationType = "";
		Application appObj = new Application();
		Transaction[] transactions = null;
		
		// SetPayment can only be called with either setPaymentRequest or setApplicationRequest
		
		if (!isForSetApplication) {
			// For SetPayment flow
			SetPaymentRequestRS request = (SetPaymentRequestRS) objReq;
			acn = request.getAcn();
			userId = request.getUserId();
			partnerId = request.getPartnerId();
			
			PaymentDetails paymentDetailsQueried = paymentDetailsRepository.getPaymentForAcn(acn);
			
			transactions = paymentDetailsQueried.getTransactions();

			serviceUtils.constructPaymentMethod(request.getApplication(), paymentDetailsQueried);
			
			appObj = setApplicationHelper.convertPayDetailsToApplication(paymentDetailsQueried);
			
			paymentMethod = paymentDetailsQueried.getPaymentMethod();
			applicationType = appObj.getApplicationType().value();
			
			String hcid = request.getPaymentSelection().getPaymentIdentifier();
			
			if (!partnerId.equalsIgnoreCase("MAPS")) {
				splitValue = getNoOfProducts(appObj, acn, partnerId, planSelection);
			} else {
				if (hcid != null && hcid.length() > 0) {
					splitValue = getNoOfProducts(appObj, hcid, partnerId, planSelection);
				}
			}

			paymentSelection = request.getPaymentSelection();
		}else {
			// For SetApplication flow
			
			SetApplicationRequestRS request = (SetApplicationRequestRS) objReq;
			
			userId = request.getUser().getUserId();
			partnerId = request.getPartnerId();
			acn = request.getApplication().getAcn();
			
			paymentSelection = request.getApplication().getPaymentSelection();
			
			PaymentDetails paymentDetailsQueried = paymentDetailsRepository.getPaymentForAcn(acn);
			
			transactions = paymentDetailsQueried.getTransactions();
			
			serviceUtils.constructPaymentMethod(request.getApplication(), paymentDetailsQueried);
			
			appObj = setApplicationHelper.convertPayDetailsToApplication(paymentDetailsQueried);
			
			paymentMethod = paymentDetailsQueried.getPaymentMethod();
			
			appObj.setApplicationType(request.getApplication().getApplicationType());
			applicationType = appObj.getApplicationType().value();
			appObj.setExchTransactionId(request.getApplication().getExchTransactionId());
			
			appObj.setApplicationStatus(request.getApplication().getApplicationStatus());
			
			appObj.setState(request.getApplication().getState());
			
			if (request.getApplication().getApplicationLanguage() != null) {
				appObj.setApplicationLanguage(request.getApplication().getApplicationLanguage());
			}
			
			if (!partnerId.equalsIgnoreCase("MAPS")) {
				splitValue = getNoOfProducts(appObj, acn, partnerId, planSelection);
			}
			
			if ((partnerId.equalsIgnoreCase("CAEXCH") || partnerId.equalsIgnoreCase("FFM"))
					&& ("PAYMENT".equalsIgnoreCase(request.getApplication()
							.getApplicationType().name()))) {

				/**
				 * Check for Retro calculation. Call WEM only for FFM, ONEXCH and
				 * State = CA and ApplicationType =PAYMENT
				 */

				paymentSelection = checkForCARetro(acn, request, paymentSelection);
				
				//If it is retro make payment amount = MRA amount
				if(("Y").equalsIgnoreCase(paymentSelection.getRetroInd())){
					
					Payment payment = null;
					if (null != paymentSelection.getInitialPayment() && paymentSelection.getInitialPayment().length > 0) {
						for (Payment paymentValue : paymentSelection
								.getInitialPayment()) {
							payment = paymentValue;
							break;
						}
					}
					
					payment.setPaymentAmt(paymentSelection.getComputedMRAAmount());
					Set<Payment> initialPaymentSet = new HashSet<Payment>();
					initialPaymentSet.add(payment);
					paymentSelection.setInitialPayment(initialPaymentSet.stream().toArray(Payment[]::new));
					
				}
				
			} else {
				String hcid = "";

				try {
					hcid = request.getApplication().getPaymentSelection().getPaymentIdentifier();
				} catch (Exception e) {
					log.error("INSIDE SetPayment.java: Getting HCID from request" + e.getMessage());
				}

				if (hcid != null && hcid.length() > 0) {
					splitValue = getNoOfProducts(request.getApplication(), hcid, partnerId, planSelection);
				}
			}
		}
		
		
		if (paymentSelection != null) {
			String paymentTrackingId = null;
			if (paymentSelection.getPaymentIdentifier() != null
					&& !StringUtils.isEmpty(paymentSelection.getPaymentIdentifier())
					&& appObj.getApplicationStatus()!=null
					&& !"PAYMENTFAILED".equals(appObj.getApplicationStatus().getApplicationStatus())) {
				PaymentDetails paymentDetailsByPaymentIdentifier = paymentDetailsRepository.getPaymentDetailsByPaymentIdentifier(paymentSelection.getPaymentIdentifier());
				log.debug("INSIDE SetPayment.java: getPaymentSelectionByApplicationId:"+ acn);
				log.debug("INSIDE SetPayment.java: paymentIdentifier"+ paymentSelection.getPaymentIdentifier());
				if (paymentDetailsByPaymentIdentifier != null
						&& paymentDetailsByPaymentIdentifier.getTransactions() != null) {
					for (Transaction paymentBean : paymentDetailsByPaymentIdentifier.getTransactions()) {
						if (paymentBean.getAnthemOrderId() != null) {
							paymentTrackingId = paymentBean.getAnthemOrderId();
							break;
						}
					}
					if (paymentTrackingId != null
							&& !StringUtils.isEmpty(paymentTrackingId)) {
						String language = paymentDetailsByPaymentIdentifier.getLangPref();
						if (language != null && language.equalsIgnoreCase("SPANISH")) {
							// paymentSelection.setpaymentExceptionMsg("DUPLICATE:Nuestros archivos indican que ya pagaste tu primera prima. El n�mero de referencia de tu pago es "+paymentTrackingId+".  Tu solicitud est� siendo procesada y recibir�s tus tarjetas de identificaci�n en el correo una vez que se complete tu inscripci�n.");
							paymentExceptionMsg = "DUPLICATE:Nuestros archivos indican que ya pagaste tu primera prima. El n�mero de referencia de tu pago es "
									+ paymentTrackingId
									+ ".  Tu solicitud est� siendo procesada y recibir�s tus tarjetas de identificaci�n en el correo una vez que se complete tu inscripci�n.";
						} else {
							// paymentSelection.setpaymentExceptionMsg("DUPLICATE:Our records indicate you already paid your initial premium.  Your Payment Reference ID number is "+paymentTrackingId+".  Your application is being processed and you will receive identification cards in the mail once your enrollment is complete.");
							paymentExceptionMsg = "DUPLICATE:Our records indicate you already paid your initial premium.  Your Payment Reference ID number is "
									+ paymentTrackingId
									+ ".  Your application is being processed and you will receive identification cards in the mail once your enrollment is complete.";
						}
					}
				}
			}
			
			if (paymentTrackingId == null
					&& paymentSelection.getPaymentIdentifier() != null
					&& !StringUtils.isEmpty(paymentSelection.getPaymentIdentifier())) {
				
				Query query = new Query();
				query.addCriteria(Criteria.where("acn").is(appObj.getAcn()));
				Update update = new Update();
				update.set("payment_method", paymentMethod);
				if(transactions.length == 1) {
					Payment payment = null;
					if (null != paymentSelection.getInitialPayment() && paymentSelection.getInitialPayment().length > 0) {
						for (Payment paymentValue : paymentSelection
								.getInitialPayment()) {
							payment = paymentValue;
							break;
						}
					}
					transactions[0].setPremiumAmount(payment.getPaymentAmt());
					update.set("transactions.0.premium_amount", payment.getPaymentAmt());
				}
				iPayDbTemplate.findAndModify(query, update, PaymentDetails.class);
				
				iterateTransactionAndMakePayment(appObj, transactions, splitValue, partnerId, paymentMethod, userId, paymentSelection);
				
			}
		}
		
		if (!isForSetApplication) {
			SetPaymentResponseRS response = new SetPaymentResponseRS();
			response.setAcn(acn);
			return response;
		} else {
			SetApplicationResponseRS response = new SetApplicationResponseRS();
			response.setAcn(acn);
			return response;
		}
		
	}
	
	public Object setPaymentOnExchange(Object objReq, boolean isForSetApplication) throws Exception {
		
		
		PlanSelection planSelection = null;
		PaymentMethod paymentMethod = null;
		boolean isSplitRequired = false;
		
		int splitValue = 0;

		//ValidationErrors validationErrors = null;

		String paymentExceptionMsg = null;

		String acn = "";
		String userId = "";
		PaymentSelection paymentSelection = null;
		String partnerId = "";
		String applicationType = "";
		Application appObj = new Application();
		Transaction[] transactions = null;
		
		// SetPayment can only be called with either setPaymentRequest or setApplicationRequest
		
		if (!isForSetApplication) {
			// For SetPayment flow
			SetPaymentRequestRS request = (SetPaymentRequestRS) objReq;
			acn = request.getAcn();
			userId = request.getUserId();
			partnerId = request.getPartnerId();
			
			PaymentDetails paymentDetailsQueried = paymentDetailsRepository.getPaymentForAcn(acn);
			
			transactions = paymentDetailsQueried.getTransactions();

			serviceUtils.constructPaymentMethod(request.getApplication(), paymentDetailsQueried);
			
			appObj = setApplicationHelper.convertPayDetailsToApplication(paymentDetailsQueried);
			
			paymentMethod = paymentDetailsQueried.getPaymentMethod();
			applicationType = appObj.getApplicationType().value();
			
			String hcid = request.getPaymentSelection().getPaymentIdentifier();
			
			if (!partnerId.equalsIgnoreCase("MAPS")) {
				splitValue = getNoOfProducts(appObj, acn, partnerId, planSelection);
			} else {
				if (hcid != null && hcid.length() > 0) {
					splitValue = getNoOfProducts(appObj, hcid, partnerId, planSelection);
				}
			}

			paymentSelection = request.getPaymentSelection();
		}else {
			// For SetApplication flow
			
			SetApplicationRequestRS request = (SetApplicationRequestRS) objReq;
			
			userId = request.getUser().getUserId();
			partnerId = request.getPartnerId();
			acn = request.getApplication().getAcn();
			
			paymentSelection = request.getApplication().getPaymentSelection();
			
			PaymentDetails paymentDetailsQueried = paymentDetailsRepository.getPaymentForAcn(acn);
			
			transactions = paymentDetailsQueried.getTransactions();
			
			serviceUtils.constructPaymentMethod(request.getApplication(), paymentDetailsQueried);
			
			appObj = setApplicationHelper.convertPayDetailsToApplication(paymentDetailsQueried);
			
			paymentMethod = paymentDetailsQueried.getPaymentMethod();
			
			appObj.setApplicationType(request.getApplication().getApplicationType());
			applicationType = appObj.getApplicationType().value();
			appObj.setExchTransactionId(request.getApplication().getExchTransactionId());
			
			appObj.setApplicationStatus(request.getApplication().getApplicationStatus());
			
			appObj.setState(request.getApplication().getState());
			
			if (request.getApplication().getApplicationLanguage() != null) {
				appObj.setApplicationLanguage(request.getApplication().getApplicationLanguage());
			}
			
			isSplitRequired = checkForSplit(appObj,acn,partnerId,planSelection);
		}
		
		PaymentSelection medicalPaymentSelection = null;
		PaymentSelection dentalPaymentSelection = null;
		
		if (paymentSelection != null) {
			String paymentTrackingId = null;
			if (paymentSelection.getPaymentIdentifier() != null
					&& !StringUtils.isEmpty(paymentSelection.getPaymentIdentifier())) {
				PaymentDetails paymentDetailsByPaymentIdentifier = paymentDetailsRepository.getPaymentDetailsByPaymentIdentifier(paymentSelection.getPaymentIdentifier());
				log.debug("INSIDE SetPayment.java: getPaymentSelectionByApplicationId:"+ acn);
				log.debug("INSIDE SetPayment.java: paymentIdentifier"+ paymentSelection.getPaymentIdentifier());
				if (paymentDetailsByPaymentIdentifier != null
						&& paymentDetailsByPaymentIdentifier.getTransactions() != null) {
					for (Transaction paymentBean : paymentDetailsByPaymentIdentifier.getTransactions()) {
						if (paymentBean.getAnthemOrderId() != null) {
							paymentTrackingId = paymentTrackingId + "," + paymentBean.getAnthemOrderId();
						}else {
							paymentTrackingId = paymentBean.getAnthemOrderId();
						}
					}
					
				}
			}
			
			if (paymentTrackingId != null
					&& !StringUtils.isEmpty(paymentTrackingId)) {
				String language = appObj.getLangPref();
				if (language != null && language.equalsIgnoreCase("SPANISH")) {
					// paymentSelection.setpaymentExceptionMsg("DUPLICATE:Nuestros archivos indican que ya pagaste tu primera prima. El n�mero de referencia de tu pago es "+paymentTrackingId+".  Tu solicitud est� siendo procesada y recibir�s tus tarjetas de identificaci�n en el correo una vez que se complete tu inscripci�n.");
					paymentExceptionMsg = "DUPLICATE:Nuestros archivos indican que ya pagaste tu primera prima. El n�mero de referencia de tu pago es "
							+ paymentTrackingId
							+ ".  Tu solicitud est� siendo procesada y recibir�s tus tarjetas de identificaci�n en el correo una vez que se complete tu inscripci�n.";
				} else {
					// paymentSelection.setpaymentExceptionMsg("DUPLICATE:Our records indicate you already paid your initial premium.  Your Payment Reference ID number is "+paymentTrackingId+".  Your application is being processed and you will receive identification cards in the mail once your enrollment is complete.");
					paymentExceptionMsg = "DUPLICATE:Our records indicate you already paid your initial premium.  Your Payment Reference ID number is "
							+ paymentTrackingId
							+ ".  Your application is being processed and you will receive identification cards in the mail once your enrollment is complete.";
				}
			}else {

				//check if it has medical/dental payment identifier
				boolean hasMedDentalPaymentId = paymentSelection.getPaymentIdentifier() != null && paymentSelection.getPaymentIdentifier().contains(",");
				final String origPaymentIdentifier = paymentSelection.getPaymentIdentifier();
				
				medicalPaymentSelection = createPaymentSelectionSeparate(acn,"MEDICAL", paymentSelection, appObj);
				
				if (hasMedDentalPaymentId) {
					dentalPaymentSelection = createPaymentSelectionSeparate(acn, "DENTAL", paymentSelection, appObj);
				}
				
				double medAmount = 0;
				if (medicalPaymentSelection != null) {
					for (Payment payments : medicalPaymentSelection.getInitialPayment()) {
						Payment medicalPayment = payments;
						medAmount = medicalPayment.getPaymentAmt();
						break;
					}
				}
				
				double dentalAmount = 0;
				if (dentalPaymentSelection != null) {
					for (Payment payments : dentalPaymentSelection.getInitialPayment()) {
						Payment dentalPayment = payments;
						dentalAmount = dentalPayment.getPaymentAmt();
						break;
					}
				}
				
				//make payment for med
				if(medAmount>=1){
					medicalPaymentSelection = submitPayment.callORCCSubmit(partnerId,  medicalPaymentSelection, applicationData, context);
				}
				logger.log(Level.INFO, "INSIDE SetPaymentOnExchange.java: ORCC Medical call Complete:"+acn);
				
				//check medical before making dental
				if (medicalPaymentSelection != null) {
					validationErrors = medicalPaymentSelection.getValidationErrors();
					Payment medicalPayment = null;
					for (Payment payments : medicalPaymentSelection.getInitialPayment()) {
						medicalPayment = payments;
						break;
					}
					
					if(medicalPayment!=null
							&& ((medAmount < 1)
									|| (medicalPayment.getPaymentTrackingId()!=null
									&& !medicalPayment.getPaymentTrackingId().trim().isEmpty()))){
						logger.log(Level.INFO, "INSIDE SetPaymentOnExchange.java: ORCC MedicalTrackingId: "+medicalPayment.getPaymentTrackingId() +" acn:"+acn);
						if(dentalAmount >= 1){
							dentalPaymentSelection = submitPayment.callORCCSubmit(partnerId,  dentalPaymentSelection, applicationData, context);
						}
						logger.log(Level.INFO, "INSIDE SetPaymentOnExchange.java: ORCC Dental Call Completed: "+acn);
					}
					
					logger.log(Level.INFO, "INSIDE SetPaymentOnExchange.java: PaymentSelection Persisted: "+acn);
					Payment dentalPayment = null;
					if(dentalPaymentSelection!=null){  
						for(Payment payments: dentalPaymentSelection.getInitialPayment()){
							dentalPayment = payments;
							break;
						}
						
						if(dentalPayment!=null
								&& ((medAmount < 1)
										|| (medicalPayment.getPaymentTrackingId()!=null
										&& !medicalPayment.getPaymentTrackingId().trim().isEmpty()))
										&& (dentalPayment.getPaymentTrackingId()==null
										|| dentalPayment.getPaymentTrackingId().trim().isEmpty())){
							if(medAmount >= 1){
								//Modified by Cognizant for PEGA change Sep 2017 - Start
								Map<String, String> cancelPayRes = submitPayment.callORCCCancel(medicalPayment.getPaymentTrackingId());
								boolean isCancelSuccessful = false;
								if(null != cancelPayRes && "true".equalsIgnoreCase(cancelPayRes.get("isCancelSuccessful"))) {
									isCancelSuccessful = true;
								}
								//Modified by Cognizant for PEGA change Sep 2017 - End
								if(isCancelSuccessful){
									medicalPayment.setDeletedFlag("C");
								}
							}
							validationErrors = dentalPaymentSelection.getValidationErrors();
						} else {
							validationErrors = medicalPaymentSelection.getValidationErrors();
						}
					}
					if(dentalPaymentSelection!=null){
						for(Payment pmt: dentalPaymentSelection.getInitialPayment()){
							medicalPaymentSelection.getInitialPayment().add(pmt);
						}
						for(Payment pmt: dentalPaymentSelection.getOngoingPayment()){
							medicalPaymentSelection.getOngoingPayment().add(pmt);
						}
					}
					medicalPaymentSelection.setPaymentIdentifier(origPaymentIdentifier);
					persistPayments(medicalPaymentSelection, context, acn, partnerId, userId, applicationType);
				}
			
			}
			
			if (paymentTrackingId == null
					&& paymentSelection.getPaymentIdentifier() != null
					&& !StringUtils.isEmpty(paymentSelection.getPaymentIdentifier())) {
				
				Query query = new Query();
				query.addCriteria(Criteria.where("acn").is(appObj.getAcn()));
				Update update = new Update();
				update.set("payment_method", paymentMethod);
				if(transactions.length == 1) {
					Payment payment = null;
					if (null != paymentSelection.getInitialPayment() && paymentSelection.getInitialPayment().length > 0) {
						for (Payment paymentValue : paymentSelection
								.getInitialPayment()) {
							payment = paymentValue;
							break;
						}
					}
					transactions[0].setPremiumAmount(payment.getPaymentAmt());
					update.set("transactions.0.premium_amount", payment.getPaymentAmt());
				}
				iPayDbTemplate.findAndModify(query, update, PaymentDetails.class);
				
				iterateTransactionAndMakePayment(appObj, transactions, splitValue, partnerId, paymentMethod, userId, paymentSelection);
				
			}
		}
		
		if (!isForSetApplication) {
			SetPaymentResponseRS response = new SetPaymentResponseRS();
			response.setAcn(acn);
			return response;
		} else {
			SetApplicationResponseRS response = new SetApplicationResponseRS();
			response.setAcn(acn);
			return response;
		}
		
	}
	
	private PaymentSelection createPaymentSelectionSeparate(String acn,String value, PaymentSelection paymentSelection, Application applicationData){
		PaymentSelection tempPaymentSelection = new PaymentSelection();
		dozerMapper.map(paymentSelection, tempPaymentSelection);
		String id = null;
		
		if(paymentSelection.getPaymentIdentifier()!=null){
			if(paymentSelection.getPaymentIdentifier().contains(",")){
				if("MEDICAL".equalsIgnoreCase(value)){
					id = paymentSelection.getPaymentIdentifier().split(",")[0];
				}else if("DENTAL".equalsIgnoreCase(value)){
					id = paymentSelection.getPaymentIdentifier().split(",")[1];
				}
			}else{
				id = paymentSelection.getPaymentIdentifier();
			}
		}
		lob.debug(acn+":INSIDE SetPayment.createPaymentSelection method.Product Type:"+value);
		lob.debug(acn+":INSIDE SetPayment.createPaymentSelection method.PaymentIdentifier:"+id);
		tempPaymentSelection.setPaymentIdentifier(id);
		Set<Payment> paymentUpdatedForPlanOnly = new HashSet<Payment>();
		if(tempPaymentSelection.getInitialPayment()!=null){
			for(Payment payment: tempPaymentSelection.getInitialPayment()){
				if(id.equalsIgnoreCase(payment.getPaymentIdentifier())) {
					paymentUpdatedForPlanOnly.add(payment);
					break;
				}
			}
		}
		tempPaymentSelection.setInitialPayment(paymentUpdatedForPlanOnly.stream().toArray(Payment[]::new));
		return tempPaymentSelection;
	}
	
	private boolean checkForSplit(Application application, String acn, String partnerId, PlanSelection planSelection){
		boolean isMedicalPlanAvailable = false;
		boolean isDentalPlanAvailable = false;
		log.debug("INSIDE SetPayment.checkForSplit method.partnerId:"+partnerId);
		if(!partnerId.equals("MAPS")){
			Applicant[] applicant = application.getApplicant();
			
			if(null != applicant && applicant.length > 0){
				log.debug(acn+":INSIDE SetPayment.checkForSplit method.ApplicantPlanSelectionDataBean:"+applicant.length);
				for(Applicant applicants: applicant){
					planSelection = applicants.getPlanSelection();
					break;
				}
			}
		}else{
			isMedicalPlanAvailable = true;
			log.debug(acn+":INSIDE SetPayment.checkForSplit method********calling MAPS DAO");
			Map<String, Object> mapsPaymentInfo = mapsPaymentDAOImpl.getMAPSPaymentUMUPAY5(acn);
			double medicalAmt = ((BigDecimal) mapsPaymentInfo.get("MED_AMT")).doubleValue();
			double dentalAmt = ((BigDecimal) mapsPaymentInfo.get("DTL_AMT")).doubleValue();
			List<Plan> plans = new ArrayList<Plan>();
			Plan medicalPlan = new Plan();
			medicalPlan.setProductType(ProductTypeEnum.MEDICAL);
			medicalPlan.setPremiumAmt(medicalAmt);
			plans.add(medicalPlan);
			if(dentalAmt>0){
				isDentalPlanAvailable =true;
				Plan dentalPlan = new Plan();
				dentalPlan.setProductType(ProductTypeEnum.DENTAL);
				dentalPlan.setPremiumAmt(dentalAmt);
				plans.add(dentalPlan);
			}
			log.debug(acn+":INSIDE SetPayment.checkForSplit method.no. of plans:"+plans.size());
			log.debug(acn+":INSIDE SetPayment.checkForSplit method.Medical plan Amount:"+medicalAmt);
			log.debug(acn+":INSIDE SetPayment.checkForSplit method.Dental plan Amount:"+dentalAmt);
			planSelection = new PlanSelection();
			planSelection.setPlan(plans.stream().toArray(Plan[]::new));
		}
		if(isMedicalPlanAvailable
				&& isDentalPlanAvailable){
			return true;
		}
	 return false;
	}
	
	public Object setPaymentHip(Object objReq, boolean isForSetApplication) throws Exception {
		
		
		PlanSelection planSelection = null;
		PaymentMethod paymentMethod = null;
		//boolean isSplitRequired = false;
		
		int splitValue = 0;

		//ValidationErrors validationErrors = null;

		String paymentExceptionMsg = null;

		String acn = "";
		String userId = "";
		PaymentSelection paymentSelection = null;
		String partnerId = "";
		String applicationType = "";
		Application appObj = new Application();
		
		// SetPayment can only be called with either setPaymentRequest or setApplicationRequest
		
		if (!isForSetApplication) {
			// For SetPayment flow
			SetPaymentRequestRS request = (SetPaymentRequestRS) objReq;
			acn = request.getAcn();
			userId = request.getUserId();
			partnerId = request.getPartnerId();
			
			PaymentDetails paymentDetailsQueried = paymentDetailsRepository.getPaymentForAcn(acn);

			serviceUtils.constructPaymentMethod(request.getApplication(), paymentDetailsQueried);
			
			appObj = setApplicationHelper.convertPayDetailsToApplication(paymentDetailsQueried);
			
			paymentMethod = paymentDetailsQueried.getPaymentMethod();
			applicationType = appObj.getApplicationType().value();
			
			String hcid = request.getPaymentSelection().getPaymentIdentifier();
			
			paymentSelection = request.getPaymentSelection();
		}else {
			// For SetApplication flow
			
			SetApplicationRequestRS request = (SetApplicationRequestRS) objReq;
			
			userId = request.getUser().getUserId();
			partnerId = request.getPartnerId();
			acn = request.getApplication().getAcn();
			
			paymentSelection = request.getApplication().getPaymentSelection();
			
			PaymentDetails paymentDetailsQueried = paymentDetailsRepository.getPaymentForAcn(acn);
			
			serviceUtils.constructPaymentMethod(request.getApplication(), paymentDetailsQueried);
			
			appObj = setApplicationHelper.convertPayDetailsToApplication(paymentDetailsQueried);
			
			paymentMethod = paymentDetailsQueried.getPaymentMethod();
			
			appObj.setApplicationType(request.getApplication().getApplicationType());
			applicationType = appObj.getApplicationType().value();
			appObj.setExchTransactionId(request.getApplication().getExchTransactionId());
			
			appObj.setApplicationStatus(request.getApplication().getApplicationStatus());
			
			appObj.setState(request.getApplication().getState());
			
			if (request.getApplication().getApplicationLanguage() != null) {
				appObj.setApplicationLanguage(request.getApplication().getApplicationLanguage());
			}
			
		}
		
		double totlaAmount = 0;
		if(null != paymentSelection && null != paymentSelection.getInitialPayment()){
			for(Payment payment: paymentSelection.getInitialPayment()){
				totlaAmount = payment.getPaymentAmt();;
				break;
			}
			if((null == paymentSelection.getCsrIdentifier() || paymentSelection.getCsrIdentifier().isEmpty()) && totlaAmount%10 !=0){
					throw new BusinessException("Invalid Amount");
			}
		}
		
		if (paymentSelection != null) {
			String paymentTrackingId = null;
			if (paymentSelection.getPaymentIdentifier() != null
					&& !StringUtils.isEmpty(paymentSelection.getPaymentIdentifier())
					&& appObj.getApplicationStatus()!=null
					&& !"PAYMENTFAILED".equals(appObj.getApplicationStatus().getApplicationStatus())) {
				PaymentDetails paymentDetailsByPaymentIdentifier = paymentDetailsRepository.getPaymentDetailsByPaymentIdentifier(paymentSelection.getPaymentIdentifier());
				log.debug("INSIDE SetPayment.java: getPaymentSelectionByApplicationId:"+ acn);
				log.debug("INSIDE SetPayment.java: paymentIdentifier"+ paymentSelection.getPaymentIdentifier());
				if (paymentDetailsByPaymentIdentifier != null
						&& paymentDetailsByPaymentIdentifier.getTransactions() != null) {
					for (Transaction paymentBean : paymentDetailsByPaymentIdentifier.getTransactions()) {
						if (paymentBean.getAnthemOrderId() != null) {
							paymentTrackingId = paymentBean.getAnthemOrderId();
							break;
						}
					}
					if (paymentTrackingId != null
							&& !StringUtils.isEmpty(paymentTrackingId)) {
						String language = paymentDetailsByPaymentIdentifier.getLangPref();
						if (language != null && language.equalsIgnoreCase("SPANISH")) {
							paymentExceptionMsg = "DUPLICATE:Nuestros archivos indican que ya pagaste tu primera prima. El número de referencia de tu pago es "+paymentTrackingId+".  Tu solicitud está siendo procesada y recibirás tus tarjetas de identificación en el correo una vez que se complete tu inscripción.";
						} else {
							paymentExceptionMsg = "DUPLICATE:Our records indicate you already paid your initial premium.  Your Payment Reference ID number is "+paymentTrackingId+".  Your application is being processed and you will receive identification cards in the mail once your enrollment is complete.";
						}
					}
				}
			}
			
			if (paymentTrackingId == null) {
				
				PaymentSelection hipPaymentSelection = paymentSelection;
				if(paymentSelection != null && null != paymentSelection.getCsrIdentifier() && !paymentSelection.getCsrIdentifier().isEmpty()) {
					partnerId = "INHIPCSR";
					String paymentIdentifier = paymentSelection.getPaymentIdentifier();
					for (Payment payment : paymentSelection.getInitialPayment()) {
						if(payment != null) {
							payment.setPaymentIdentifier(paymentIdentifier);
						}
					}
				} else {
					hipPaymentSelection= splitPaymentForHIP(paymentSelection);
				}
				
				Transaction[] transactions = createTransactionsHip(appObj, hipPaymentSelection, partnerId, paymentMethod, userId);

				Query query = new Query();
				query.addCriteria(Criteria.where("acn").is(appObj.getAcn()));
				Update update = new Update();
				update.set("transactions", transactions);
				update.set("payment_method", paymentMethod);
				iPayDbTemplate.findAndModify(query, update, PaymentDetails.class);
				
			}
		}
		
		if (!isForSetApplication) {
			SetPaymentResponseRS response = new SetPaymentResponseRS();
			response.setAcn(acn);
			return response;
		} else {
			SetApplicationResponseRS response = new SetApplicationResponseRS();
			response.setAcn(acn);
			return response;
		}
		
	}
	
	private PaymentSelection splitPaymentForHIP(
			PaymentSelection paymentSelection) {
		PaymentSelection hipPaymentSelection = new PaymentSelection();
		dozerMapper.map(paymentSelection, hipPaymentSelection);
		if(null != hipPaymentSelection.getPaymentIdentifier() && !hipPaymentSelection.getPaymentIdentifier().isEmpty()){
			Payment actualPayment = null;
			for(Payment payment: paymentSelection.getInitialPayment()){
				actualPayment = payment;
				break;
			}
			List<Payment> hipPayments = new ArrayList<Payment>();
		    double totlaAmount = 0.0;
		    if(null != actualPayment){
		    	totlaAmount = actualPayment.getPaymentAmt();
		    }
		    int amountPerTransaction = 10;
		    String amtPerTran = applicationProperties.getStringProperty("hip.amountPerApplicant", "10");
		    amountPerTransaction = Integer.parseInt(amtPerTran);
		    int noOfPayment = (int) (totlaAmount/amountPerTransaction);
		    String paymentIdentifier = paymentSelection.getPaymentIdentifier();
			if(actualPayment!=null){
				for(int i=1;i<=noOfPayment;i++){
					Payment paymentTemp = new Payment();
					dozerMapper.map(actualPayment, paymentTemp);
					
					paymentTemp.setPaymentAmt(amountPerTransaction);
					paymentTemp.setPaymentIdentifier(paymentIdentifier+"_"+i);
					hipPayments.add(paymentTemp);
				}
			}
			hipPaymentSelection.setInitialPayment(hipPayments.stream().toArray(Payment[]::new));
		}
		return hipPaymentSelection;
	}
	
	private void iterateTransactionAndMakePayment(Application application, Transaction[] transactions, int splitValue, String partnerId, PaymentMethod paymentMethod, String userId, PaymentSelection paymentSelection) {
		
		if(null != paymentMethod && null != paymentMethod.getPaymentType() && !paymentMethod.getPaymentType().isEmpty()) {
			if (transactions != null) {
				if (transactions.length > 0) {
					int count = 0;
					for (Transaction transaction : transactions) {
						boolean canUpdate = true;
						
						String orderId = serviceUtils.generateOrderId("STG");
						Query query = new Query();
						query.addCriteria(Criteria.where("acn").is(application.getAcn()));
						Update update = new Update();
						
						update.set("transactions."+count+".updated_id", userId);
						update.set("transactions."+count+".updated_dt", serviceUtils.mongoDateConverter(new Date()));
						
						update.set("transactions."+count+".payment_identifier", "WEB");
						update.set("transactions."+count+".transaction_status", "OPEN");
						update.set("transactions."+count+".transaction_type", "PAYMENT");
						update.set("transactions."+count+".anthem_orderid", orderId);
						
						if("CC".equalsIgnoreCase(paymentMethod.getPaymentType())) {
							GetTokenResponse getTokenResponse = triggerChaseCallForCC(paymentMethod, Double.toString(transaction.getPremiumAmount()), orderId);

							String encryptedToken = "";
							String errorCode = "";
							String errorMessage = "";
							String chaseErrorCode = "";
			
							if(getTokenResponse.getEncryptedToken() != null && !getTokenResponse.getEncryptedToken().isEmpty()) {
								encryptedToken = getTokenResponse.getEncryptedToken();
							}else if(getTokenResponse.getExceptionDetails() != null){
								errorCode = getTokenResponse.getExceptionDetails().getCode();
								chaseErrorCode = getTokenResponse.getChaseErrorCode();
								errorMessage = getTokenResponse.getExceptionDetails().getMessage();
								log.error("Exception in payModRestUtils.getTokenForCCAuthorization inside constructPerPayModSubmitRequest: " + getTokenResponse.getExceptionDetails().toString());
							}
							if(encryptedToken == null || encryptedToken.isEmpty()) {
								update.set("transactions."+count+".transaction_status", "INPROGRESS");
							}else {
								update.set("transactions."+count+".credit_card_number", getTokenResponse.getEncryptedToken());
								update.set("transactions."+count+".response_reason_code", getTokenResponse.getReasonCode());
								update.set("transactions."+count+".action_code", getTokenResponse.getActionCode());
								update.set("transactions."+count+".is_level3", getTokenResponse.getLevelTInd());
								update.set("transactions."+count+".cc_authorization_number", getTokenResponse.getAuthorizationCode());
								update.set("transactions."+count+".token_response_date", getTokenResponse.getTokenPaidDate());
								update.set("transactions."+count+".AVSAAV", getTokenResponse.getAvsaav());
								update.set("transactions."+count+".message_type", getTokenResponse.getMessageType());
								update.set("transactions."+count+".submitted_transaction_id", getTokenResponse.getSubmittedTransactionID());
								update.set("transactions."+count+".response_transaction_id", getTokenResponse.getResponseTransactionID());
								update.set("transactions."+count+".stored_credential_flag", getTokenResponse.getStoredCredentialFlag());
							}
						}
						
						if(canUpdate) {
							iPayDbTemplate.findAndModify(query, update, PaymentDetails.class);
						}
						count++;
					}
					count = 0;
				}
			}
			// To make the payment for the request which contains no plan
			/*if (splitValue == 0 && payment != null
					&& !partnerId.equalsIgnoreCase("MAPS")) {
				String planPaymentIdentifier = paymentIdentifier;

				Payment tempPayment = new Payment(); 
				dozerMapper.map(payment, tempPayment);
				tempPayment.setPaymentIdentifier(planPaymentIdentifier);

				initialPaymentSet.add(tempPayment);
				tempPaymentSelection.setInitialPayment(initialPaymentSet.stream().toArray(Payment[]::new));
			}*/
		}

	}
	
	private Transaction[] createTransactionsHip(Application application, PaymentSelection hipPaymentSelection, String partnerId, PaymentMethod paymentMethod, String userId) {
		
		Transaction[] transactions = null;
		
		PaymentSelection paymentSelection = application.getPaymentSelection();
		Payment[] payments = hipPaymentSelection.getInitialPayment();
		
		if(null != paymentMethod && null != paymentMethod.getPaymentType() && !paymentMethod.getPaymentType().isEmpty()) {
			if (payments != null) {
				if (payments.length > 0) {
					int count = 0;
					for (Payment plan : payments) {
						boolean canAdd = true;
						
						Transaction transaction = new Transaction();
						
						if (null != paymentSelection && null != paymentSelection.getCsrIdentifier()
								&& !paymentSelection.getCsrIdentifier().isEmpty()) {
							transaction.setCsr(true);
						}
						transaction.setPaymentIdentifier(plan.getPaymentIdentifier());
						if (null != paymentSelection && null != paymentSelection.getEmailAddress() &&
								!paymentSelection.getEmailAddress().isEmpty()) {
							transaction.setPaymentConfirmationEmailAddress(paymentSelection.getEmailAddress());
						}
						transaction.setCreatedDt(serviceUtils.mongoDateConverter(new Date()));
						transaction.setCreatedId(userId);
						transaction.setUpdatedDt(serviceUtils.mongoDateConverter(new Date()));
						transaction.setUpdatedId(userId);
						transaction.setPaymentChannel("WEB"); 
						transaction.setTransactionStatus("OPEN");
						transaction.setTransactionType("PAYMENT");
						
						transaction.setPremiumAmount(plan.getPaymentAmt());
						transaction.setAnthemOrderId(serviceUtils.generateOrderId("STG"));
						
						if("CC".equalsIgnoreCase(paymentMethod.getPaymentType())) {
							GetTokenResponse getTokenResponse = triggerChaseCallForCC(paymentMethod, Double.toString(plan.getPaymentAmt()), transaction.getAnthemOrderId());

							String encryptedToken = "";
							String errorCode = "";
							String errorMessage = "";
							String chaseErrorCode = "";
			
							if(getTokenResponse.getEncryptedToken() != null && !getTokenResponse.getEncryptedToken().isEmpty()) {
								encryptedToken = getTokenResponse.getEncryptedToken();
							}else if(getTokenResponse.getExceptionDetails() != null){
								errorCode = getTokenResponse.getExceptionDetails().getCode();
								chaseErrorCode = getTokenResponse.getChaseErrorCode();
								errorMessage = getTokenResponse.getExceptionDetails().getMessage();
								log.error("Exception in payModRestUtils.getTokenForCCAuthorization inside constructPerPayModSubmitRequest: " + getTokenResponse.getExceptionDetails().toString());
							}
							if(encryptedToken == null || encryptedToken.isEmpty()) {
								transaction.setTransactionStatus("INPROGRESS");
							}else {
								transaction.setCreditCardNumber(getTokenResponse.getEncryptedToken());
								transaction.setCcAuthorizationNumber(getTokenResponse.getAuthorizationCode());
								transaction.setResponseReasonCode(getTokenResponse.getReasonCode());
								transaction.setTokenDate(getTokenResponse.getTokenPaidDate());
								transaction.setActionCode(getTokenResponse.getActionCode());
								transaction.setIsLevelThree(getTokenResponse.getLevelTInd());
								transaction.setAVSAAV(getTokenResponse.getAvsaav());
								transaction.setMessageType(getTokenResponse.getMessageType());
								transaction.setStoredCredentialFlag(getTokenResponse.getStoredCredentialFlag());
								transaction.setSubmittedTransactionID(getTokenResponse.getSubmittedTransactionID());
								transaction.setResponseTransactionID(getTokenResponse.getResponseTransactionID());
							}
						}
						
						if(canAdd) {
							transactions[count] = transaction;
							count++;
						}
					}
					count = 0;
				}
			}
		}

		return transactions;
	}
	
	private GetTokenResponse triggerChaseCallForCC(PaymentMethod paymentMethod, String paymentAmount, String orderId) {
		GetTokenResponse getTokenResponse = new GetTokenResponse();

		try {
			
			GetTokenRequest getTokenRequest = new GetTokenRequest();
			
			if (paymentMethod.getPaymentType() != null)
			{
				if (paymentMethod.getPaymentType().equals("VISA"))
				{
					getTokenRequest.setMethodOfPayment("VI");
				}
				else if (paymentMethod.getPaymentType().equals("MC") || paymentMethod.getPaymentType().equals("MASTERCARD"))
				{
					getTokenRequest.setMethodOfPayment("MC");
				}
			}
			
			getTokenRequest.setAccountNumber(paymentMethod.getCreditCardNumber());
			String expString = paymentMethod.getCcExpDate();
			String[] expArr = {};
			expArr = expString.split("/");
			getTokenRequest.setExpirationDate(expArr[0] + expArr[1].substring(expArr[1].length() - 2));
			paymentAmount = paymentAmount.replaceAll("\\.", "");
			getTokenRequest.setAmount(paymentAmount);
			getTokenRequest.setIntegrityCheck(paymentMethod.getIntegrityCheck());
			getTokenRequest.setKeyID(paymentMethod.getKeyId());
			getTokenRequest.setPhaseID(paymentMethod.getPhaseId());
			getTokenRequest.setCardHolderName(paymentMethod.getNameOnFundingAccount());
			if(null != paymentMethod.getFundAccountOwnerFullAddress()) {
				getTokenRequest.setAddressLine1(paymentMethod.getFundAccountOwnerFullAddress().getAddress1());
				getTokenRequest.setCity(paymentMethod.getFundAccountOwnerFullAddress().getCity());
				getTokenRequest.setState(paymentMethod.getFundAccountOwnerFullAddress().getState());
				getTokenRequest.setPostalCode(paymentMethod.getFundAccountOwnerFullAddress().getZipcode());
			}
			
			getTokenRequest.setAnthemOrderId(orderId);
			getTokenRequest.setDivisionCode("355958");
			getTokenRequest.setMessageType("CSTO");
			getTokenRequest.setStoredCredentialFlag("N");
			
			getTokenResponse = payModRestUtils.getTokenForInitialPayments(getTokenRequest);
			
		} catch(Exception e) {
			com.anthem.ols.middletier.paymentservice.response.Exception exceptionDetails = new com.anthem.ols.middletier.paymentservice.response.Exception();
			exceptionDetails.setCode("EXCEPTION");
			exceptionDetails.setMessage("EXCEPTION");
			getTokenResponse.setExceptionDetails(exceptionDetails);
			log.error("Error in triggerChaseCallForCC: " + e);
		}
		
		return getTokenResponse;
	}
	
	/*private PaymentSelection createPaymentSelection(PaymentSelection paymentSelection, PlanSelection planSelection, int splitValue, String partnerId) {
		PaymentSelection tempPaymentSelection = new PaymentSelection();
		dozerMapper.map(paymentSelection, tempPaymentSelection);
		String paymentIdentifier = tempPaymentSelection.getPaymentIdentifier();
		Payment payment = null;
		if (null != tempPaymentSelection.getInitialPayment() && tempPaymentSelection.getInitialPayment().length > 0) {
			for (Payment paymentValue : tempPaymentSelection.getInitialPayment()) {
				payment = paymentValue;
				break;
			}
		}
		Set<Payment> initialPaymentSet = new HashSet<Payment>();

		if (payment != null && planSelection != null) {
			if (null != planSelection.getPlan() && planSelection.getPlan().length > 0) {
				for (Plan plan : planSelection.getPlan()) {
					String planPaymentIdentifier = paymentIdentifier
							.concat(plan.getProductType().name()
									.substring(0, 1));
					double amount = getPlanAmount(plan);
					Payment tempPayment = new Payment(); 
					dozerMapper.map(payment, tempPayment);
					tempPayment.setPaymentIdentifier(planPaymentIdentifier);
					tempPayment.setPaymentAmt(amount);
					//tempPayment.setP(plan.getProductType());
					initialPaymentSet.add(tempPayment);
				}
				tempPaymentSelection.setInitialPayment(initialPaymentSet.stream().toArray(Payment[]::new));
			}
		}
		// To make the payment for the request which contains no plan
		if (splitValue == 0 && payment != null
				&& !partnerId.equalsIgnoreCase("MAPS")) {
			String planPaymentIdentifier = paymentIdentifier;

			Payment tempPayment = new Payment(); 
			dozerMapper.map(payment, tempPayment);
			tempPayment.setPaymentIdentifier(planPaymentIdentifier);

			initialPaymentSet.add(tempPayment);
			tempPaymentSelection.setInitialPayment(initialPaymentSet.stream().toArray(Payment[]::new));
		}

		return tempPaymentSelection;
	}*/
	
	private double getPlanAmount(Plan plan) {
		BigDecimal amt = new BigDecimal(0.0);
		if (plan != null) {
			BigDecimal tempAmt = new BigDecimal(plan.getPremiumAmt(),
					MathContext.DECIMAL64);
			amt = amt.add(tempAmt);
		}
		return amt.doubleValue();
	}
	
	private int getNoOfProducts(Application application, String acn, String partnerId, PlanSelection planSelection) {
		int planCount = 0;
		if (!"MAPS".equalsIgnoreCase(partnerId)) {
			if(null != application.getApplicant() && application.getApplicant().length > 0) {
				if(null != application.getApplicant()[0] && null != application.getApplicant()[0].getPlanSelection()) {
					planCount = application.getApplicant()[0].getPlanSelection().getPlanLength();
				}
			}

		} else {
			log.debug(acn+ ":INSIDE SetPayment.checkForSplit method********calling MAPS DAO");
			Map<String, Object> mapsPaymentInfo = mapsPaymentDAOImpl.getMAPSPaymentUMUPAY5(acn);
			double medicalAmt = ((java.math.BigDecimal) mapsPaymentInfo.get("MED_AMT")).doubleValue();
			double dentalAmt = ((java.math.BigDecimal) mapsPaymentInfo.get("DTL_AMT")).doubleValue();
			double visionAmt = ((java.math.BigDecimal) mapsPaymentInfo.get("VSN_AMT")).doubleValue();
			double lifeAmt = ((java.math.BigDecimal) mapsPaymentInfo.get("LFE_AMT")).doubleValue();
			Set<Plan> plans = new HashSet<Plan>();
			if (medicalAmt > 0) {
				Plan medicalPlan = new Plan();
				medicalPlan.setProductType(ProductTypeEnum.MEDICAL);
				medicalPlan.setPremiumAmt(medicalAmt);
				plans.add(medicalPlan);
			}
			if (dentalAmt > 0) {
				Plan dentalPlan = new Plan();
				dentalPlan.setProductType(ProductTypeEnum.DENTAL);
				dentalPlan.setPremiumAmt(dentalAmt);
				plans.add(dentalPlan);
			}
			if (visionAmt > 0) {
				Plan visionPlan = new Plan();
				visionPlan.setProductType(ProductTypeEnum.VISION);
				visionPlan.setPremiumAmt(visionAmt);
				plans.add(visionPlan);
			}
			if (lifeAmt > 0) {
				Plan lifePlan = new Plan();
				lifePlan.setProductType(ProductTypeEnum.LIFE);
				lifePlan.setPremiumAmt(lifeAmt);
				plans.add(lifePlan);
			}
			log.debug( acn+ ":INSIDE SetPayment.checkForSplit method.no. of plans:"+ plans.size());
			log.debug(acn+ ":INSIDE SetPayment.checkForSplit method.Medical plan Amount:"+ medicalAmt);
			log.debug(acn+ ":INSIDE SetPayment.checkForSplit method.Dental plan Amount:"+ dentalAmt);
			log.debug(acn+ ":INSIDE SetPayment.checkForSplit method.Life plan Amount:"+ lifeAmt);
			for (Plan plan : plans) {
				planCount++;
			}
			planSelection = new PlanSelection();
			planSelection.setPlan(plans.stream().toArray(Plan[]::new));
		}
		return planCount;
	}
	
	public PaymentDetails convertApplicationtoPaymentDetails(Application application) throws Exception {
		PaymentDetails paymentDetails = new PaymentDetails();
		
		String partnerId = null;
		if (application.getCreatePartnerId() == null) {
			throw new BusinessException("Partner Id cannot be empty");
		} else {
			partnerId = application.getCreatePartnerId();
		}

		if (!serviceUtils.validatePartnerId(partnerId)) {
			log.error("persistPayDetailsAppData -- Invalid partner");
			throw new BusinessException("Invalid partner");
		}

		String appSource = application.getAppSource();
		String transferFlag = "N";
		Shopper user = null;
		
		paymentDetails.setAcn(application.getAcn());
		paymentDetails.setApplicationVersion(application.getApplicationVersion());
		paymentDetails.setState(application.getState());
		paymentDetails.setApplicationType(application.getApplicationType().value());
		paymentDetails.setReqEffDate(application.getReqEffDate());

		AccessControl accessControl = application.getAccessControlList()[0];
		if(null != accessControl) {
			paymentDetails.setAccessType(accessControl.getAccessType().value());
			if(null != accessControl.getUser()) {
				user = accessControl.getUser();
				paymentDetails.setUserId(accessControl.getUser().getUserId());
				paymentDetails.setUserRole(accessControl.getUser().getShopperRole().value());
			}
		}
		
		if (application.getAppSource() == null || application.getAppSource().equalsIgnoreCase("")) {
			appSource = serviceUtils.getAppSource(application, transferFlag, partnerId, user);
		}
		
		Transaction[] transactions = null;
		if(null != application.getApplicant() && application.getApplicant().length > 0
				&& null != application.getApplicant()[0].getPlanSelection()
				&& application.getApplicant()[0].getPlanSelection().getPlanLength() > 0) {
			transactions = new Transaction[application.getApplicant()[0].getPlanSelection().getPlanLength()];
		}
		
		if (null != application.getApplicant() && application.getApplicant().length > 0) {
			if (null != application.getApplicant()[0].getDemographic()) {
				paymentDetails.setApplicantFirstName(application.getApplicant()[0].getDemographic().getFirstName());
				paymentDetails.setApplicantLastName(application.getApplicant()[0].getDemographic().getLastName());
				paymentDetails.setApplicantMiddleName(application.getApplicant()[0].getDemographic().getMi());
				paymentDetails.setDateOfBirth(serviceUtils.convertDateToString(
						application.getApplicant()[0].getDemographic().getDateOfBirth(), "MM-dd-yyyy"));
				paymentDetails.setMemberCode(Integer.toString(application.getApplicant()[0].getMemberCode()));
				if(null != application.getApplicant()[0].getDemographic().getRelationshipType()) {
					paymentDetails.setRelationship(application.getApplicant()[0].getDemographic().getRelationshipType().name());
				}
			}
			if (null != application.getApplicant()[0].getPlanSelection()
					&& application.getApplicant()[0].getPlanSelection().getPlanLength() > 0) {
				int count = 0;
				for (Plan plan : application.getApplicant()[0].getPlanSelection().getPlan()) {
					Transaction transaction = new Transaction();
					serviceUtils.constructTransaction(application, paymentDetails.getUserId(), transaction);
					transaction.setPlanName(plan.getPlanName());
					if(null != plan.getPlanType()) {
						transaction.setPlanType(plan.getPlanType().name());
					}
					if(null != plan.getProductType()) {
						transaction.setProductType(plan.getProductType().name());
					}
					transaction.setContractCode(plan.getContractCode());
					if ("CAEXCH".equals(application.getCreatePartnerId())){
						if(null == application.getExchQHPId()) {
							transaction.setQhpid(plan.getPlanId());
						}else {
							transaction.setQhpid(application.getExchQHPId());
						}
					}else {
						transaction.setPlanId(plan.getPlanId());
						transaction.setQhpid(plan.getqHPId());
					}
					transaction.setQhpvariation(plan.getqHPVariation());
					if(null != plan.geteBHFlag()) {
						transaction.setEbhflag(plan.geteBHFlag().name());
					}
					transaction.setRatingServiceArea(plan.getRatingServiceArea());
					transaction.setCompanyDescr(plan.getCompanyDescr());
					transaction.setEreAppliedAPTC(plan.getEreAppliedAPTC());
					transaction.setPremiumAmount(plan.getPremiumAmt());
					transactions[count] = transaction;
				}
				paymentDetails.setTransactions(transactions);
			}else {
				Transaction transaction = new Transaction();
				serviceUtils.constructTransaction(application, paymentDetails.getUserId(), transaction);
				transactions[0] = transaction;
				paymentDetails.setTransactions(transactions);
			}
		} else {
			Transaction transaction = new Transaction();
			serviceUtils.constructTransaction(application, paymentDetails.getUserId(), transaction);
			transactions[0] = transaction;
			paymentDetails.setTransactions(transactions);
		}
		
		if (application.getApplicationType() == null || application.getApplicationType().toString().equals("")) {
			paymentDetails.setApplicationType("OFFEXCHANGE");
		} else {
			paymentDetails.setApplicationType(application.getApplicationType().name());
		}
		paymentDetails.setApplicationVersion(application.getApplicationVersion());
		paymentDetails.setAppSource(appSource);
		if(null != application.getBrandName()) {
			paymentDetails.setBrandName(application.getBrandName().name());
		}
		if(null != user && null != user.getShopperRole()) {
			paymentDetails.setCreatorRole(user.getShopperRole().name());
		}
		paymentDetails.setPartnerId(partnerId);
		paymentDetails.setLangPref("ENGLISH");
		paymentDetails.setState(application.getState());
		paymentDetails.setReqEffDate(application.getReqEffDate());
		paymentDetails.setSystem("");
		paymentDetails.setLegalEntity("");
		paymentDetails.setMarketSegment("");
		paymentDetails.setDivisionCode("");
		paymentDetails.setTransactionDivisionCode("");
		paymentDetails.setSettleAmount(application.getPremiumAmt());
		paymentDetails.setExchSubscriberId(application.getExchSubscriberId());
		paymentDetails.setExchaptcAmt(Double.toString(application.getExchAPTCAmt()));
		paymentDetails.setIpAddress(application.getIpAddress());
		paymentDetails.setExchTransactionId(application.getExchTransactionId());
		paymentDetails.setExchConsumerId(application.getExchConsumerId());
		if(null != application.getwLPConsumerId() && !application.getwLPConsumerId().isEmpty()) {
			paymentDetails.setWlpConsumerId(application.getwLPConsumerId());
		}else {
			paymentDetails.setWlpConsumerId(application.getWlpAssignedConsumerId());
		}
		if (null != application.getPaymentSelection()) {
			PayerId payer = new PayerId();
			if (null != application.getPaymentSelection().getCsrIdentifier()
					&& !application.getPaymentSelection().getCsrIdentifier().isEmpty()) {
				payer.setId(application.getPaymentSelection().getCsrIdentifier());
				payer.setType("CSR");
			} else {
				payer.setId("PPORT");
				payer.setType("PPORT");
			}
			paymentDetails.setPayerId(payer);
			Payment[] payments = application.getPaymentSelection().getInitialPayment();
			Payment payment = null;
			if(null != payments && payments.length > 0) {
				payment = payments[0];
			}
			if(null != payment) {
				PaymentMethod paymentMethod = new PaymentMethod();
				if(payment.getPaymentType() == PaymentTypeEnum.ECHECK) {
					BankAccount bankAccount = payment.getBankAcct();
					paymentMethod.setPaymentType("ACH");
					paymentMethod.setPaymentSubType(serviceUtils.convertBankAccTypeReverse(bankAccount.getAccountType()));
					paymentMethod.setNameOnFundingAccount(bankAccount.getAccountHolderName());
					paymentMethod.setBankAccountNumber(bankAccount.getAccountNo());
					paymentMethod.setBankRoutingnumber(bankAccount.getRoutingNo());
				}
				if(payment.getPaymentType() == PaymentTypeEnum.CREDITCARD) {
					CreditCard creditCard = payment.getCreditCard();
					paymentMethod.setPaymentType("CC");
					paymentMethod.setPaymentSubType(serviceUtils.convertCardTypeReverse(creditCard.getCardType()));
					paymentMethod.setNameOnFundingAccount(creditCard.getCardHolderName());
					paymentMethod.setCcExpDate(creditCard.getExpDate());
					paymentMethod.setKeyId(creditCard.getKeyID());
					paymentMethod.setPhaseId(creditCard.getPhaseID());
					paymentMethod.setIntegrityCheck(creditCard.getIntegrityCheck());
				}
				if(null != payment.getBillingAddr() && null != paymentMethod.getPaymentType()) {
					Address address = payment.getBillingAddr();
					FundAccountOwnerFullAddress addressPayment = new FundAccountOwnerFullAddress();
					addressPayment.setAddress1(address.getAddressLine1());
					addressPayment.setAddress2(address.getAddressLine2());
					addressPayment.setCity(address.getCity());
					addressPayment.setCounty(address.getCounty());
					addressPayment.setCountyCode(address.getCountyCode());
					addressPayment.setState(address.getState());
					addressPayment.setZipcode(address.getPostalCode());
					paymentMethod.setFundAccountOwnerFullAddress(addressPayment);
				}
				paymentDetails.setPaymentMethod(paymentMethod);
			}
		}
		
		return paymentDetails;
	}
	
	public void savePaymentDetails(PaymentDetails paymentDetails) {
		PaymentDetails savedObj = paymentDetailsRepository.save(paymentDetails);
	}
	
	private PaymentSelection checkForCARetro(String acn,
			SetApplicationRequestRS request, PaymentSelection paymentSelection) {
		try {

			GetComputedMRARequest mraRequest = new GetComputedMRARequest();

			if (acn != null) {

				StateCode c = new StateCode();
				Code code = new Code();
				code.setRDI("");
				code.setValue(request.getApplication().getState());
				c.setCode(code);

				mraRequest.setExchangeSubId(request.getApplication()
						.getExchSubscriberId());
				mraRequest.setStateCode(c);

				mraRequest.setQhpPlanId(getPlanID(request));

				if (request.getApplication().getReqEffDate() != null) {

					GregorianCalendar cal = new GregorianCalendar();
					cal.setTime(request.getApplication().getReqEffDate());
					XMLGregorianCalendar xmlDate = DatatypeFactory
							.newInstance().newXMLGregorianCalendarDate(
									cal.get(Calendar.YEAR),
									cal.get(Calendar.MONTH) + 1,
									cal.get(Calendar.DAY_OF_MONTH),
									DatatypeConstants.FIELD_UNDEFINED);
					log.debug("GetComputedMRA --- requestEffDate:"
									+ request.getApplication().getReqEffDate()
									+ "XMLGregorianCalendar " + xmlDate);
					mraRequest.setProposedCoverageEffectiveDate(xmlDate);
				}

				BigDecimal tPremiumAmt = new BigDecimal(request
						.getApplication().getPremiumAmt());
				tPremiumAmt = tPremiumAmt.setScale(2, RoundingMode.HALF_UP);

				//mraRequest.setTotalPremiumAmount(tPremiumAmt);
				
				BigDecimal exchTotPremiumAmt = new BigDecimal(request
						.getApplication().getExchTotPremiumAmt());
				exchTotPremiumAmt = exchTotPremiumAmt.setScale(2, RoundingMode.HALF_UP);
				
				mraRequest.setTotalPremiumAmount(exchTotPremiumAmt);

				BigDecimal exchAPTCAmt = new BigDecimal(request
						.getApplication().getExchAPTCAmt());
				exchAPTCAmt = exchAPTCAmt.setScale(2, RoundingMode.HALF_UP);
				mraRequest.setAptcAmount(exchAPTCAmt);
				
				BigDecimal appliedAPTCAmount = new BigDecimal(request
						.getApplication().getAppliedAPTCAmount());
				appliedAPTCAmount = appliedAPTCAmount.setScale(2, RoundingMode.HALF_UP);
				
				mraRequest.setOtherSubsidyAmount(appliedAPTCAmount);
				mraRequest.setPaymentTransactionId(paymentSelection
						.getPaymentIdentifier());

				mraRequest.setPartnerID("Shopperportal");

			}
			
			GetComputedMRAResponse mraResponse = paymentIntegrationGateway.getComputedMRADetails(mraRequest);
			
			if (mraResponse != null && paymentSelection != null) {
				if (mraResponse.getMraDetail() != null
						&& mraResponse.getServiceStatusMessages() == null) {


					XMLGregorianCalendar retroDate = mraResponse.getMraDetail()
							.getRetroPayToDate();

					paymentSelection.setComputedMRAAmount(mraResponse
							.getMraDetail().getComputedMRAAmount()
							.doubleValue());
					paymentSelection.setPaymentTransactionId(mraResponse
							.getMraDetail().getPaymentTransactionId());
					paymentSelection.setPolicyId(mraResponse.getMraDetail()
							.getPolicyId());
					paymentSelection.setRetroInd(mraResponse.getMraDetail()
							.getRetroInd());
					paymentSelection.setRetroPayToDate(retroDate
							.toGregorianCalendar().getTime());
					paymentSelection.setSepIndicator(mraResponse.getMraDetail()
							.getSEPDocsReceived());
					paymentSelection.setSepDocsReceived(mraResponse
							.getMraDetail().getSEPIndicator());
					paymentSelection.setStateCode(request.getApplication().getState());
					
					
					

				} 
			} else {
				// null mraResponse
				paymentSelection
						.setPaymentExceptionMsg("Encountered Error in ComputedMRA Response");
			}

		} catch (Exception e) {
			paymentSelection
					.setPaymentExceptionMsg("Error in ComputedMRA Request or Response " +e.getMessage());

		}
		return paymentSelection;
	}
	
	
	public String getPlanID(SetApplicationRequestRS request) throws Exception {
		String errorMessage = null;
		Applicant[] applicantSet = request.getApplication().getApplicant();

		for (Applicant applicant : applicantSet) {
			if (applicant != null
					&& applicant.getDemographic() != null
					&& applicant.getDemographic().getRelationshipType() == RelationshipTypeEnum.APPLICANT) {
				if (applicant.getPlanSelection() != null) {
					PlanSelection planSelection = applicant.getPlanSelection();
					Plan[] planSet = planSelection.getPlan();
					for (Plan plan : planSet) {
						if ((plan.getPlanId() != null)) {
							return plan.getPlanId();

						} else
							throw new Exception(
									"Contract Code is required for the Plan Selected");

					}
				}
			}
		}
		return errorMessage;
	}
	
}
