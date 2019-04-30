package gov.nih.nci.evs.api.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.properties.ApplicationProperties;

@RestController
@RequestMapping("${nci.evs.application.contextPath}")
public class GeneratedFileController {

	@Autowired
	ApplicationProperties applicationProperties;
	
    @RequestMapping(value = "/getDataFromFile/{fileName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getDataFromFile(@PathVariable(value = "fileName") String fileName,
                    HttpServletResponse response ) throws IOException {
        String baseDirectory = applicationProperties.getGeneratedFilePath();

        File inputFile = new File(baseDirectory + fileName);
        try (FileInputStream in = new FileInputStream(inputFile)) {
            response.setHeader("Content-Disposition", "attachment; filename=" + inputFile.getName());
            return IOUtils.toByteArray(in);
        }
    }
}
