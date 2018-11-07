package gov.nih.nci.evs.api.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.HomePageData;

@RestController
public class DefaultController {
    
	
	@Autowired
 	StardogProperties stardogProperties;

   @Autowired
   SparqlQueryManagerService sparqlQueryManagerService;
   
   
   
    
	@RequestMapping(method = RequestMethod.GET, value = "/version",produces = "application/json")
    public @ResponseBody HomePageData getEvsConceptDetail(HttpServletResponse response) throws IOException{
		HomePageData homePageData = new HomePageData();
		homePageData.setDescription("NCI EVS API");
		homePageData.setVersion("1.0.0-SNAPSHOT");
    	return homePageData;
    }

	
}