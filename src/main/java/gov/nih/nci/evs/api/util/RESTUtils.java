package gov.nih.nci.evs.api.util;

import java.util.Arrays;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class RESTUtils {
	
	private String restURL;
	private String username;
	private String password;
	private int readTimeout;
	private int connectTimeout;

	public RESTUtils () {}
	
	public RESTUtils(String restURL,String username, String password,int readTimeout, int connectTimeout) {
		this.restURL = restURL;
		this.username = username;
		this.password = password;
		this.readTimeout= readTimeout;
		this.connectTimeout = connectTimeout;
	}
	
	public String runSPARQL(String query) {
		RestTemplate restTemplate = new RestTemplateBuilder().
											rootUri(restURL).
											basicAuthorization(username,password).
											setReadTimeout(readTimeout).
											setConnectTimeout(connectTimeout).
											build();
		MultiValueMap <String,String> body = new LinkedMultiValueMap<String,String>();
		body.add("query", query);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(Arrays.asList(new MediaType("application","sparql-results+json")));
		HttpEntity<?> entity = new HttpEntity<Object>(body ,headers);
		String results = restTemplate.postForObject(restURL,entity,String.class);
		return results;
	}
}
